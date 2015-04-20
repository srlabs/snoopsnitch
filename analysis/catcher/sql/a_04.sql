--  A4

--  Join cell info with itself on all entries that have the same
--  MCC/MNC/LAC/CID, but were observed on different ARFCNs. This
--  my happen when cells are reconfigured, but this should be rare.
--  FIXME: When more test data is available we could decide to make
--  the score dependent on the timestamp delta between the occurrences
--  of the conflicting cell infos.
DROP VIEW IF EXISTS a4;
CREATE VIEW a4 as
SELECT
        l.id,
        l.mcc,
        l.mnc,
        l.lac,
        l.cid,
        count(*) AS value,
        CASE WHEN count(*) > 1 THEN 1 ELSE 0 END AS score
FROM cell_info AS l LEFT JOIN cell_info AS r
ON
        l.mcc = r.mcc AND
        l.mnc = r.mnc AND
        l.lac = r.lac AND
        l.cid = r.cid AND
        l.rat = r.rat AND
        l.bcch_arfcn != r.bcch_arfcn AND
        l.first_seen < r.first_seen
WHERE
        l.mcc > 0 AND
        l.lac > 0 AND
        l.cid > 0 AND
        r.bcch_arfcn != null
GROUP BY l.mcc, l.mnc, l.lac, l.cid;
