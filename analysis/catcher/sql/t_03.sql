DROP VIEW IF EXISTS t3;
CREATE VIEW t3 AS
SELECT
        id,
        mcc,
        mnc,
        lac,
        cid,
        CASE WHEN
        (paging_mi        AND
         mobile_term      AND
         t_release        AND
         NOT auth         AND
         NOT iden_imei_ac AND
         NOT iden_imei_bc AND
         NOT iden_imsi_ac AND
         NOT iden_imsi_bc AND
         NOT cipher > 0   AND
         NOT sms_presence AND
         NOT call_presence) THEN 1 ELSE 0 END as score
FROM session_info;
