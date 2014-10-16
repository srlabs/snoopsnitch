--  A2

DROP VIEW IF EXISTS a2;
CREATE VIEW a2 AS
SELECT DISTINCT
        cell.id,
        cell.mcc,
        cell.mnc,
        cell.lac,
        cell.cid,
        sum(CASE WHEN cell.lac != neig.lac THEN 1.0 ELSE 0.0 END)/count(*) as score
FROM cell_info AS cell, arfcn_list AS al, cell_info AS neig, config
ON cell.id = al.id AND al.arfcn = neig.bcch_arfcn
WHERE
        cell.mcc > 0 AND cell.mnc > 0 AND cell.lac > 0 AND cell.cid > 0
GROUP BY cell.id;
