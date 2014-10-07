--  A2
DROP VIEW IF EXISTS a2;
CREATE VIEW a2 AS
SELECT
        sci._id,
        sci.mcc,
        sci.mnc,
        sci.lac,
        sci.cid,
        sci.timestamp AS timestamp,
        sum(CASE WHEN sci.lac != nci.lac THEN 1.0 ELSE 0.0 END) / count(*) as score
FROM serving_cell_info AS sci, neighboring_cell_info AS nci
ON nci.last_sc_id = sci._id
WHERE nci.lac > 0 AND nci.cid > 0 AND sci.mcc = nci.mcc AND sci.mnc = sci.mnc
GROUP BY sci._id;


