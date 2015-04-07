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
			--  We may observe rejected location updates with 'foreign'
			--  operators if our own operator is not receivable but has
			--  no roaming agreement with that foreign operator. Do not
			--  treat this as an incident if previous MCC/MNC are known
			--  (i.e. lu_mcc != 0 AND lu_mnc != 0) differ from MCC/MNC.
			WHEN auth = 0 AND ((mcc = lu_mcc AND mnc = lu_mnc) OR lu_mcc = 0 OR lu_mnc = 0) THEN 2.0
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
