--  K1
DROP VIEW IF EXISTS k1;
CREATE VIEW k1 AS
SELECT
        cell.mcc,
        cell.mnc,
        cell.lac,
        cell.cid,
        sum(CASE WHEN ifnull(al.arfcn, 0) > 0 THEN 1 ELSE 0 END) = 0 as score
FROM cell_info AS cell LEFT JOIN arfcn_list AS al
ON cell.id = al.id
GROUP BY cell.id;
