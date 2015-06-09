--  Calculate the fraction of encrypted sessions
--  to know whether encryption is to be expected
--  for some MCC/MNC/RAT/lu_acc combination
DROP VIEW IF EXISTS sessions_ciphered_perc;
CREATE VIEW sessions_ciphered_perc AS
SELECT
	mcc,
	mnc,
	rat,
	lu_acc,
	sum(CASE WHEN cipher > 0 THEN 1.0 ELSE 0.0 END)/count(*) as perc
FROM session_info
WHERE cipher > 0 AND
	domain = 0 AND
	mcc > 0    AND
	mcc < 1000 AND
	mnc < 1000 AND
	(lu_acc OR call_presence OR sms_presence)
GROUP BY mcc, mnc, rat, lu_acc;

--  For every unencrypted session, check whether
--  encryption can be expected and score them
--  accordingly. The threshold of scp.perc at
--  which we consider encryption "normal" greatly
--  influences the false positive rate.
--  Note, that an unencrypted session gets a score
--  of 1.0 by default.
DROP VIEW IF EXISTS c5;
CREATE VIEW c5 AS
SELECT
	si.id,
	si.timestamp,
	si.mcc,
	si.mnc,
	si.lac,
	si.cid,
	CASE
		WHEN scp.perc > 0.9 THEN 2.0
		WHEN scp.perc > 0.8 THEN 1.5
		ELSE 1.0
	END AS score
FROM
	session_info as si, sessions_ciphered_perc as scp
ON
	si.mcc    = scp.mcc AND
	si.mnc    = scp.mnc AND
	si.rat    = scp.rat AND
	si.lu_acc = scp.lu_acc
WHERE
	si.cipher = 0 AND
	si.domain = 0 AND
	((t_locupd AND si.lu_acc AND NOT lu_reject AND NOT paging_mi) OR
	(t_call AND call_presence) OR (t_sms AND sms_presence));
