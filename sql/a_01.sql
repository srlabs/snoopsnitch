--  A1

DROP VIEW IF EXISTS dup_arfcns;
CREATE VIEW dup_arfcns AS
SELECT
        l.bcch_arfcn AS bcch_arfcn,
        min(l.first_seen) AS first_seen,
        max(l.last_seen) AS last_seen,
        count(*) AS value
FROM cell_info AS l, cell_info AS r, config
ON
        l.bcch_arfcn = r.bcch_arfcn AND
        (l.mcc != r.mcc OR
         l.mnc != r.mnc OR
         l.lac != r.lac OR
         l.cid != r.cid) AND
        (r.first_seen - l.last_seen) < config.delta_arfcn
GROUP BY l.bcch_arfcn;

DROP VIEW IF EXISTS a1;
CREATE VIEW a1 AS
SELECT
        ci.id,
        ci.mcc,
        ci.mnc,
        ci.lac,
        ci.cid,
        ifnull(da.value, 0) as value,
        CASE WHEN da.value > 1 THEN 1 ELSE 0 END as score
FROM cell_info AS ci LEFT JOIN dup_arfcns AS da
ON
        ci.bcch_arfcn = da.bcch_arfcn AND
        ci.first_seen >= da.first_seen AND
        ci.last_seen  <= da.last_seen;
