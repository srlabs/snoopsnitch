-- R1

--  For every cell info for which a MCC/MNC/LAC/CID was recorded,
--  add a column with the neighboring ARFCN for each entry created
--  from BCCH/SI2 with the same ID in the arfcn_list table.
DROP VIEW IF EXISTS cells_with_neig_arfcn;
CREATE VIEW cells_with_neig_arfcn AS
SELECT
        ci.id,
        ci.mcc,
        ci.mnc,
        ci.lac,
        ci.cid,
        ci.bcch_arfcn,
        al.arfcn as neig_arfcn
FROM cell_info AS ci LEFT JOIN arfcn_list AS al
ON ci.id = al.id
WHERE ci.mcc > 0 AND ci.mnc > 0 AND ci.lac > 0 AND ci.cid > 0 AND al.source = "SI2";

--  Join the cells_with_neig_arfcn table above with itself, such
--  that that for every cell in cell_info we get the respective 
--  neighboring cell information, if present. This is the case
--  when the neighbor ARFCN (neig_arfcn) equals the main ARFCN
--  (bcch_arfcn) of the joined table.
DROP VIEW IF EXISTS cells_with_neig_cell;
CREATE VIEW cells_with_neig_cell AS
SELECT DISTINCT
        c.id,
        c.mcc as cell_mcc,
        c.mnc as cell_mnc,
        c.lac as cell_lac,
        c.cid as cell_cid,
        c.bcch_arfcn as cell_arfcn,
        n.id as neig_id
FROM cells_with_neig_arfcn as c, cells_with_neig_arfcn as n
ON c.neig_arfcn = n.bcch_arfcn;

--  Count the number of neighboring cells recorded for every cell
--  in the table. Note, that this is the number of neighboring
--  cells we actually received SI2 information for (as an inner
--  join is used above). This count will later be used as the
--  number of neighboring cells that should have the cell in their
--  own ARFCN list.
--  This makes sense, as you may not receive BCCH info from ap
--  neighboring cell on the far other end of you current cell.
--  This is normal and should not lead to a high score. This is
--  only the case if a (supposed) neighboring cell that we
--  receive SI2 info from does not announce the current cell as
--  their own neighbor.
DROP VIEW IF EXISTS cells_neig_count;
CREATE VIEW cells_neig_count AS
SELECT *, count(*) as count
FROM cells_with_neig_cell 
GROUP BY id, cell_mcc, cell_mnc, cell_lac, cell_cid;

--  This is the list neighboring cells that reference a cell.
--  It is similar to cells_with_neig_cell, just that we also
--  join on the neigboring cells ARFCN from SI2 equalling 
--  the current cells main ARFCN.
DROP VIEW IF EXISTS cells_ref_by_neig_cell;
CREATE VIEW cells_ref_by_neig_cell AS
SELECT DISTINCT
        c.id,
        c.mcc as cell_mcc,
        c.mnc as cell_mnc,
        c.lac as cell_lac,
        c.cid as cell_cid,
        c.bcch_arfcn as cell_arfcn,
        n.id as neig_id
FROM cells_with_neig_arfcn as c, cells_with_neig_arfcn as n
ON c.neig_arfcn = n.bcch_arfcn AND c.bcch_arfcn = n.neig_arfcn;

--  Count by how many neigboring cells a cell is announce
--  as neighboring cell.
DROP VIEW IF EXISTS cells_ref_by_neig_count;
CREATE VIEW cells_ref_by_neig_count AS
SELECT id, count(*) as count 
FROM cells_ref_by_neig_cell
GROUP BY cell_mcc, cell_mnc, cell_lac, cell_cid, cell_arfcn
ORDER BY id;

--  Join both counts. For each cell, divide the number of
--  reachable neigboring cells by which the cell is announced
--  by the number of neighboring cells that should announce
--  it.
DROP VIEW IF EXISTS r1;
CREATE VIEW r1 AS
SELECT 
        cn.cell_mcc,
        cn.cell_mnc,
        cn.cell_lac,
        cn.cell_cid,
        1 - (crbn.count + 0.0)/cn.count as score
FROM cells_neig_count as cn, cells_ref_by_neig_count as crbn
ON cn.id = crbn.id;
