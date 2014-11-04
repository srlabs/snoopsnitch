DROP VIEW IF EXISTS c4;
CREATE VIEW c4 AS
SELECT
        id,
        timestamp,
        mcc,
        mnc,
        lac,
        cid,
        (CASE WHEN iden_imsi_bc AND iden_imei_bc THEN 1 WHEN iden_imsi_bc OR iden_imei_bc THEN 0.7 ELSE 0 END) as score
FROM session_info;
