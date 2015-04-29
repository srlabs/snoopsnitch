DROP TABLE IF EXISTS catcher;
CREATE TABLE catcher
(
	id integer PRIMARY KEY,
	mcc integer,
	mnc integer,
	lac integer,
	cid integer,
	timestamp datetime,
	duration int,
	a1 FLOAT,
	a2 FLOAT,
	a4 FLOAT,
	a5 FLOAT,
	k1 FLOAT,
	k2 FLOAT,
	c1 FLOAT,
	c2 FLOAT,
	c3 FLOAT,
	c4 FLOAT,
	c5 FLOAT,
	t1 FLOAT,
	t3 FLOAT,
	t4 FLOAT,
	r1 FLOAT,
	r2 FLOAT,
	f1 FLOAT,
	longitude DOUBLE NOT NULL,
	latitude DOUBLE NOT NULL,
	valid SMALLINT,
	score FLOAT
);

DROP TABLE IF EXISTS events;
CREATE TABLE events 
(
	id integer,
	sequence integer,
	timestamp datetime NOT NULL,
	mcc smallint NOT NULL,
	mnc smallint NOT NULL,
	lac int NOT NULL,
	cid int NOT NULL,
	longitude DOUBLE NOT NULL,
	latitude DOUBLE NOT NULL,
	valid SMALLINT,
	smsc CHAR(32) NOT NULL,
	msisdn CHAR(32) NOT NULL,
	-- type of event
	--	0 - none/normal
	--	1 - OTA/binary SMS
	--	2 - silent SMS
	--	3 - null paging
	event_type integer,
	PRIMARY KEY(id, sequence, event_type)
);
