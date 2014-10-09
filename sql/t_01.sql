DROP VIEW IF EXISTS t1;
CREATE VIEW t1 AS
SELECT
        mcc,
        mnc,
        lac,
        cid,
        t3212 AS value,
        t3212 < config.t3212_min AS score
FROM cell_info, config
WHERE t3212 > 0;
