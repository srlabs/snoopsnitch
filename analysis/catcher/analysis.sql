.read analysis_si.sql
.read analysis_ci.sql

--  All unavailable criteria

--  Attract
.read sql/a_03.sql

--  Track
.read sql/t_07.sql

DELETE FROM catcher;
INSERT INTO catcher
SELECT
	si.id,
	ci.mcc,
	ci.mnc,
	ci.lac,
	ci.cid,
	si.timestamp,
	si.duration,
	max(ci.a1),
	max(ci.a2),
	max(ci.a4),
	max(ci.k1),
	max(ci.k2),
	max(si.c1),
	max(si.c2),
	max(si.c3),
	max(si.c4),
	max(si.c5),
	max(ci.t1),
	max(si.t3),
	max(si.t4),
	max(ci.r1),
	max(ci.r2),
	max(ci.f1),
	0.0,		-- TODO: Add latitude
	0.0,		-- TODO: Add longitude
	max(ci.a1) +
	max(ci.a2) +
	max(ci.a4) +
	max(ci.k1) +
	max(ci.k2) +
	max(si.c1) +
	max(si.c2) +
	max(si.c3) +
	max(si.c4) +
	max(si.c5) +
	max(ci.t1) +
	max(si.t3) +
	max(si.t4) +
	max(ci.r1) +
	max(ci.r2) +
	max(ci.f1) as score
FROM si LEFT JOIN ci
ON
	ci.mcc = si.mcc AND
	ci.mnc = si.mnc AND
	ci.lac = si.lac AND
	ci.cid = si.cid,
config
ON
	abs(strftime('%s', ci.last_seen) - strftime('%s', si.timestamp)) < 10000
GROUP BY
	id
HAVING
	score > config.catcher_min_score;
