DROP VIEW IF EXISTS c3;
CREATE VIEW c3 AS
SELECT
        id,
        timestamp,
        mcc,
        mnc,
        lac,
        0 as score
FROM session_info
WHERE domain = 0 AND cipher > 0;
