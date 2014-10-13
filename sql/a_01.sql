--  A1

--  Join cell info with itself on all entries that have the same
--  MCC/MNC/LAC/CID, but were observed on different ARFCNs. This
--  my happen when cells are reconfigured, but this should be rare.
--  FIXME When more test data is available we could decide to make
--  the score dependent on the timestamp delta between the occurrences
--  of the conflicting cell infos.
CREATE VIEW a1 as
SELECT
        l.mcc as mcc,
        l.mnc as mnc,
        l.lac as lac,
        l.cid as cid,
        count(*) as value,
        CASE WHEN count(*) > 1 THEN 1 ELSE 0 END as score
FROM cell_info AS l LEFT JOIN cell_info AS r
ON
        l.mcc = r.mcc AND
        l.mnc = r.mnc AND
        l.lac = r.lac AND
        l.cid = r.cid AND
        l.bcch_arfcn != r.bcch_arfcn AND
        l.first_seen < r.first_seen
GROUP BY l.mcc, l.mnc, l.lac, l.cid;

DROP VIEW IF EXISTS dup_arfcns;
CREATE VIEW dup_arfcns AS
SELECT
        l.bcch_arfcn as bcch_arfcn,
        min(l.first_seen) as first_seen,
        max(l.last_seen) as last_seen,
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

DROP VIEW IF EXISTS a1b;
CREATE VIEW a1b AS
SELECT
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
