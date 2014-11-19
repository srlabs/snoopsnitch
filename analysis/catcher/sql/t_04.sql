DROP VIEW IF EXISTS t4;
CREATE VIEW t4 AS
SELECT
        id,
        mcc,
        mnc,
        lac,
        cid,
        duration,
        CASE WHEN
         ((assign_cmpl OR mobile_term) AND
          t_release                    AND
          NOT auth                     AND
          NOT iden_imei_ac             AND
          NOT iden_imei_bc             AND
          NOT iden_imsi_ac             AND
          NOT iden_imsi_bc             AND
          NOT cipher > 0               AND
          NOT sms_presence             AND
          NOT call_presence            AND
          duration > config.delta_tch) THEN 1 ELSE 0 END as score
FROM session_info, config
WHERE domain = 0 AND duration > 0;
