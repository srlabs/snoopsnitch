DROP VIEW IF EXISTS c4;
CREATE VIEW c4 AS
SELECT
        id,
        timestamp,
        mcc,
        mnc,
        lac,
        cid,
        (CASE
			WHEN auth = 0 THEN 2.0
			WHEN auth = 1 THEN 0.5
			ELSE 0.0
		END) *
		(CASE
			WHEN (iden_imei_bc = 0 AND iden_imei_ac = 0) THEN 1.0
			ELSE 3.0
		END)
			as score
FROM session_info
WHERE
	domain = 0  AND
	cipher = 0  AND
	t_locupd    AND
	lu_reject   AND
	iden_imsi_bc;
