DROP TABLE IF EXISTS catcher;
CREATE TABLE catcher
(
	mcc integer,
	mnc integer,
	lac integer,
	cid integer,
	timestamp datetime,
	a1 FLOAT,
	a2 FLOAT,
	a4 FLOAT,
	k1 FLOAT,
	k2 FLOAT,
	c1 FLOAT,
	c2 FLOAT,
	c3 FLOAT,
	c4 FLOAT,
	t1 FLOAT,
	t3 FLOAT,
	t4 FLOAT,
	r1 FLOAT,
	r2 FLOAT,
	f1 FLOAT,
	score FLOAT
);

.read ../prebuilt/config.sql
.read analysis.sql

.headers on
.separator "	"
SELECT * from catcher;
