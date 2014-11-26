--  Valid operators
DROP VIEW IF EXISTS valid_op;
CREATE VIEW valid_op AS
SELECT
	si.mcc,
	si.mnc,
	c.name as country,
	n.name as operator
FROM
	session_info as si,
	mnc as n,
	mcc as c
WHERE
	si.mcc = n.mcc AND
	si.mnc = n.mnc AND
	c.mcc = n.mcc  AND
 	NOT (si.mcc >= 1000 or si.mnc >= 1000) AND
 	NOT (si.mcc = 0 or si.mnc = 0)         AND
 	NOT (si.mcc = 262 and si.mnc = 10)     AND
	NOT (si.mcc = 204 and si.mnc = 21)     AND
	NOT (si.mcc = 228 and si.mnc = 6)      AND
	NOT (si.mcc = 244 and si.mnc = 17)     AND
	NOT (si.mcc = 208 and si.mnc = 14)     AND
	NOT (si.mcc = 901)
GROUP BY
	si.mcc,
	si.mnc;

--  Valid 3G sessions
--  FIXME: Looks like we see not t_release for 3G???
DROP VIEW IF EXISTS valid_si;
CREATE VIEW valid_si AS
SELECT
	session_info.*,
	CASE WHEN(t_call OR (mobile_term AND NOT t_sms)) AND call_presence THEN 1 ELSE 0 END as is_call,
	CASE WHEN t_sms AND sms_presence                                   THEN 1 ELSE 0 END as is_sms,
	CASE WHEN t_locupd AND lu_acc                                      THEN 1 ELSE 0 END as is_lu,
	CASE WHEN cipher > 0                                               THEN 1 ELSE 0 END as is_enc,
	CASE WHEN auth > 0                                                 THEN 1 ELSE 0 END as is_auth
FROM
	session_info
WHERE
	rat = 1 and domain = 0;

--  Scores per operator
DELETE FROM risk_3g;
INSERT INTO risk_3g
SELECT
	valid_si.mcc,
	valid_si.mnc,
	valid_op.country,
	valid_op.operator,
	count(*) as samples,
	(sum(is_call+0.0))/count(*) as call_perc,
	(sum(is_sms+0.0))/count(*)  as sms_perc,
	(sum(is_lu+0.0))/count(*)   as lu_perc,
	(sum(is_enc+0.0))/count(*)  as enc_perc,
	(sum(is_auth+0.0))/count(*) as auth_perc,
	sum(CASE
		WHEN cipher = 0 THEN 0.0
	                    ELSE 1.0 END)/count(*) as intercept,
	sum(CASE
		WHEN auth = 2 THEN 1.0	-- UMTS authentication
		WHEN auth = 1 THEN 0.7  -- GSM authentication
				      ELSE 0.0  -- No authentication
		END)/count(*) as impersonation
FROM
	valid_op, valid_si
WHERE
	valid_op.mcc = valid_si.mcc AND
	valid_op.mnc = valid_si.mnc AND
	(is_call OR is_sms OR is_lu)
GROUP BY
	valid_si.mcc,
	valid_si.mnc;

