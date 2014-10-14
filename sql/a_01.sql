--  A1

--  Select all cell_info entries which have been observed within
--  'delta_arfcn' seconds broadcasting a different MCC/MNC/LAC/CID
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
        (strftime('%s', l.first_seen) - strftime('%s', r.last_seen)) >= 0 AND
        (strftime('%s', l.first_seen) - strftime('%s', r.last_seen)) < config.delta_arfcn
WHERE
        l.mcc > 0 AND l.mnc > 0 AND l.lac > 0 AND l.cid > 0 AND
        r.mcc > 0 AND r.mnc > 0 AND r.lac > 0 AND r.cid > 0
GROUP BY l.bcch_arfcn;

--  Associate a1 score
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
        strftime('%s', ci.first_seen) >= strftime('%s', da.first_seen) AND
        strftime('%s', ci.last_seen)  <= strftime('%s', da.last_seen)
WHERE
        ci.mcc > 0 AND ci.mnc > 0 and ci.lac > 0 and ci.cid > 0;
