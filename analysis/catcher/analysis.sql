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
	max(si.f1),
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
	max(si.f1) as score
FROM si, ci, config
ON
	ci.mcc = si.mcc AND
	ci.mnc = si.mnc AND
	ci.lac = si.lac AND
	ci.cid = si.cid AND
	strftime('%s', ci.last_seen) - strftime('%s', si.timestamp) < 3600 AND
	strftime('%s', si.timestamp) - strftime('%s', ci.last_seen) < 3600
GROUP BY
	ci.mcc, ci.mnc, ci.lac, ci.cid
HAVING
	score > config.catcher_min_score;
