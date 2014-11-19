--  FIXME: Disable this test until we have the ARFCN/cell collection issue
--  sorted out, as it leads to false positives.

--  K1
DROP VIEW IF EXISTS k1;
CREATE VIEW k1 AS
SELECT
	0 as id,
	0 as mcc,
	0 as mnc,
	0 as lac,
	0 as cid,
	0.0 as score;

--  CREATE VIEW k1 AS
--  SELECT
--          cell.id,
--          cell.mcc,
--          cell.mnc,
--          cell.lac,
--          cell.cid,
--          sum(CASE WHEN ifnull(al.arfcn, 0) > 0 THEN 0 ELSE 1 END) = 0 as score
--  FROM cell_info AS cell LEFT JOIN arfcn_list AS al
--  ON cell.id = al.id
--  GROUP BY cell.id;
