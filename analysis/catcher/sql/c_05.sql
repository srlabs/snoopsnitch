--  Select all sessions with a valid cell ID that
--  contain call data, SMS or a accepted location
--  update. Consider only values within the last
--  day.
DROP VIEW IF EXISTS valid_sessions;
CREATE VIEW valid_sessions AS
SELECT * FROM session_info
WHERE
	domain = 0 AND
	mcc > 0    AND
	mcc < 1000 AND
	mnc < 1000 AND
	(lu_acc OR call_presence OR sms_presence) AND
	datetime(timestamp, 'unixepoch') > datetime('now', '-1 day');

--  Count number of sessions, grouped by country,
--  operator, RAT and location update
--  Rationale: Location updates are often treated
--  differently than other transaction (e.g. they
--  may be unencrypted while other sessions use
--  A5/1)
--  We cases where do not have at least a sample
--  size of 10.
DROP VIEW IF EXISTS sessions_total;
CREATE VIEW sessions_total AS
SELECT
	mcc,
	mnc,
	lu_acc,
	rat,
	count(*) AS count
FROM valid_sessions
GROUP BY mcc, mnc, rat, lu_acc
HAVING count > 10;

--  Calculate the fraction of encrypted sessions
--  to know whether encryption is to be expected
--  for some MCC/MNC/RAT/lu_acc combination
DROP VIEW IF EXISTS sessions_ciphered_perc;
CREATE VIEW sessions_ciphered_perc AS
SELECT
	vs.mcc,
	vs.mnc,
	vs.rat,
	vs.lu_acc,
	st.count as samples,
	(count(*)+0.0)/st.count AS perc
FROM valid_sessions AS vs, sessions_total AS st
ON
	vs.mcc = st.mcc AND
	vs.mnc = st.mnc AND
	vs.rat = st.rat AND
	vs.lu_acc = st.lu_acc
WHERE cipher > 0
GROUP BY vs.mcc, vs.mnc, vs.rat, vs.lu_acc;

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
	cipher = 0 AND
	NOT lu_reject AND
	NOT paging_mi AND
	NOT (t_locupd AND NOT si.lu_acc);
