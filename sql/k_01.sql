--  K1
DROP VIEW IF EXISTS k1;
CREATE VIEW k1 AS
SELECT
        sci._id,
        sci.mcc,
        sci.mnc,
        sci.lac,
        sci.cid,
        sci.timestamp AS timestamp,
        count(*) = 0 as score
FROM serving_cell_info AS sci, neighboring_cell_info AS nci
ON nci.last_sc_id = sci._id
WHERE nci.lac > 0 AND nci.cid > 0
GROUP BY sci._id;
