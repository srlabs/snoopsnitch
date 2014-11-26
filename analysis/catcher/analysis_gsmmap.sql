.read analysis_si.sql

DROP TABLE IF EXISTS catcher_si;
CREATE TABLE catcher_si
(
	id integer PRIMARY KEY,
	mcc integer,
	mnc integer,
	lac integer,
	cid integer,
	timestamp datetime,
	duration int,
	c1 FLOAT,
	c2 FLOAT,
	c3 FLOAT,
	c4 FLOAT,
	c5 FLOAT,
	t3 FLOAT,
	t4 FLOAT,
	score FLOAT
);

DELETE FROM catcher_si;
INSERT INTO catcher_si
SELECT
	si.id,
	si.mcc,
	si.mnc,
	si.lac,
	si.cid,
	si.timestamp,
	si.duration,
	si.c1,
	si.c2,
	si.c3,
	si.c4,
	si.c5,
	si.t3,
	si.t4,
	si.c1 +
	si.c2 +
	si.c3 +
	si.c4 +
	si.c5 +
	si.t3 +
	si.t4 as score
FROM si;
