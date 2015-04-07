DROP VIEW IF EXISTS c3;
CREATE VIEW c3 AS
SELECT
        id,
        timestamp,
        mcc,
        mnc,
        lac,
        (CASE WHEN cmc_imeisv > 0 THEN 0 ELSE 0.5 END) as score
FROM session_info
WHERE domain = 0 AND cipher > 0;
