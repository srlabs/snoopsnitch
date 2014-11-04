.read config.sql
.read analysis_si.sql
.read analysis_ci.sql

--  All unavailable criteria

--  Attract
.read sql/a_03.sql

--  Track
.read sql/t_07.sql

DROP TABLE IF EXISTS catcher;
CREATE TABLE catcher
(
	mcc integer,
	mnc integer,
	lac integer,
	cid integer,
	timestamp datetime,
	a1 integer,
	a2 integer,
	a4 integer,
	k1 integer,
	k2 integer,
	c1 integer,
	c2 integer,
	c3 integer,
	c4 integer,
	t1 integer,
	t3 integer,
	t4 integer,
	r1 integer,
	r2 integer,
	score integer
);
	
INSERT INTO catcher
SELECT
        ci.mcc,
        ci.mnc,
        ci.lac,
        ci.cid,
        si.timestamp  AS timestamp,
        ci.a1,
        ci.a2,
        ci.a4,
        ci.k1,
        ci.k2,
        si.c1,
        si.c2,
        si.c3,
        si.c4,
        ci.t1,
        si.t3,
        si.t4,
        ci.r1,
        ci.r2,
        ci.a1 + ci.a2 + ci.a4 + ci.k1 + ci.k2 +
        si.c1 + si.c2 + si.c3 + si.c4 + ci.t1 +
        si.t3 + si.t4 + ci.r1 + ci.r2 as score
FROM si, ci
ON
        ci.mcc = si.mcc AND
        ci.mnc = si.mnc AND
        ci.lac = si.lac AND
        ci.cid = si.cid AND
        strftime('%s', si.timestamp) - strftime('%s', ci.last_seen) >= 0 AND
        strftime('%s', si.timestamp) - strftime('%s', ci.last_seen) < 10
ORDER BY score DESC;

DROP VIEW IF EXISTS val;
CREATE VIEW val AS
SELECT
	SUM(CASE WHEN a1 > 0 THEN 1 ELSE 0 END) as a1,
	SUM(CASE WHEN a2 > 0 THEN 1 ELSE 0 END) as a2,
	SUM(CASE WHEN a4 > 0 THEN 1 ELSE 0 END) as a4,
	SUM(CASE WHEN k1 > 0 THEN 1 ELSE 0 END) as k1,
	SUM(CASE WHEN k2 > 0 THEN 1 ELSE 0 END) as k2,
	SUM(CASE WHEN c1 > 0 THEN 1 ELSE 0 END) as c1,
	SUM(CASE WHEN c2 > 0 THEN 1 ELSE 0 END) as c2,
	SUM(CASE WHEN c3 > 0 THEN 1 ELSE 0 END) as c3,
	SUM(CASE WHEN c4 > 0 THEN 1 ELSE 0 END) as c4,
	SUM(CASE WHEN t1 > 0 THEN 1 ELSE 0 END) as t1,
	SUM(CASE WHEN t3 > 0 THEN 1 ELSE 0 END) as t3,
	SUM(CASE WHEN t4 > 0 THEN 1 ELSE 0 END) as t4,
	SUM(CASE WHEN r1 > 0 THEN 1 ELSE 0 END) as r1,
	SUM(CASE WHEN r2 > 0 THEN 1 ELSE 0 END) as r2
FROM
	catcher;

.headers on
.separator "	"
SELECT * from val;
