/*!40101 SET storage_engine=MyISAM */;

DROP TABLE IF EXISTS cell_info;
CREATE TABLE cell_info (
  id integer PRIMARY KEY,		-- Unique cell index
  first_seen datetime NOT NULL,		-- First seen timestamp
  last_seen datetime NOT NULL,		-- Last seen timestamp
  -- DIAG or Android
  mcc smallint DEFAULT NULL,		-- Mobile country code
  mnc smallint DEFAULT NULL,		-- Mobile network code
  lac smallint DEFAULT NULL,		-- Location area code
  cid int DEFAULT NULL,			-- Cell ID
  rat tinyint DEFAULT NULL,		-- Radio access technology (GSM=0, UMTS=1, LTE=2)
  bcch_arfcn int DEFAULT NULL,		-- Main ARFCN for this cell
  c1 smallint DEFAULT NULL,		-- C1 parameter
  c2 smallint DEFAULT NULL,		-- C2 parameter
  power_sum int DEFAULT NULL,		-- Power measurement accumulator
  power_count int DEFAULT NULL,		-- Power measurement count
  gps_lon float DEFAULT NULL,		-- GPS longitude
  gps_lat float DEFAULT NULL,		-- GPS latitude
  -- SI3
  msc_ver smallint DEFAULT NULL,	-- MSC release version
  combined smallint DEFAULT NULL,	-- Cell uses BCCH combined mode
  agch_blocks smallint DEFAULT NULL,	-- # of blocks reserved to AGCH
  pag_mframes smallint DEFAULT NULL,	-- # of multi frames used for paging
  t3212 smallint DEFAULT NULL,		-- Location update timer
  dtx smallint DEFAULT NULL,		-- DTX allowed for uplink
  -- SI3 and SI4
  cro smallint DEFAULT NULL,		-- Cell reselection offset
  temp_offset smallint DEFAULT NULL,	-- Temporary offset
  pen_time smallint DEFAULT NULL,	-- Penalty time
  pwr_offset smallint DEFAULT NULL,	-- Power offset
  gprs smallint DEFAULT NULL,		-- GPRS indicator
  -- ARFCN counters
  ba_len smallint DEFAULT NULL,		-- BA ARFCN count derived from SI1
  neigh_2 smallint DEFAULT NULL,	-- Neighboring cell count derived from SI2
  neigh_2b smallint DEFAULT NULL,	-- Neighboring cell count derived from SI2bis
  neigh_2t smallint DEFAULT NULL,	-- Neighboring cell count derived from SI2ter
  neigh_2q smallint DEFAULT NULL,	-- Neighboring cell count derived from SI2quater
  neigh_5 smallint DEFAULT NULL,	-- Neighboring cell count derived from SI5
  neigh_5b smallint DEFAULT NULL,	-- Neighboring cell count derived from SI5bis
  neigh_5t smallint DEFAULT NULL,	-- Neighboring cell count derived from SI5ter
  -- Message counters
  count_si1 integer DEFAULT NULL,	-- # of SI1 received for this cell
  count_si2 integer DEFAULT NULL,	-- # of SI2 received for this cell
  count_si2b integer DEFAULT NULL,	-- # of SI2bis received for this cell
  count_si2t integer DEFAULT NULL,	-- # of SI2ter received for this cell
  count_si2q integer DEFAULT NULL,	-- # of SI2quater received for this cell
  count_si3 integer DEFAULT NULL,	-- # of SI3 received for this cell
  count_si4 integer DEFAULT NULL,	-- # of SI4 received for this cell
  count_si5 integer DEFAULT NULL,	-- # of SI5 received for this cell
  count_si5b integer DEFAULT NULL,	-- # of SI5bis received for this cell
  count_si5t integer DEFAULT NULL,	-- # of SI5ter received for this cell
  count_si6 integer DEFAULT NULL,	-- # of SI6 received for this cell
  count_si13 integer DEFAULT NULL,	-- # of SI13 received for this cell
  -- Message payloads
  si1 char(41) DEFAULT NULL,		-- Raw message (hex) SI1
  si2 char(41) DEFAULT NULL,		-- Raw message (hex) SI2
  si2b char(41) DEFAULT NULL,		-- Raw message (hex) SI2bis
  si2t char(41) DEFAULT NULL,		-- Raw message (hex) SI2ter
  si2q char(41) DEFAULT NULL,		-- Raw message (hex) SI2quater
  si3 char(41) DEFAULT NULL,		-- Raw message (hex) SI3
  si4 char(41) DEFAULT NULL,		-- Raw message (hex) SI4
  si5 char(41) DEFAULT NULL,		-- Raw message (hex) SI5
  si5b char(41) DEFAULT NULL,		-- Raw message (hex) SI5bis
  si5t char(41) DEFAULT NULL,		-- Raw message (hex) SI5ter
  si6 char(41) DEFAULT NULL,		-- Raw message (hex) SI6
  si13 char(41) DEFAULT NULL		-- Raw message (hex) SI113
);

DROP TABLE IF EXISTS arfcn_list;
CREATE TABLE arfcn_list (
  id integer NOT NULL,			-- Unique cell index
  source char(4) NOT NULL,		-- Source message (SIxx)
  arfcn integer	NOT NULL,		-- Neighboring ARFCN
  PRIMARY KEY(id, source, arfcn)
);

DROP TABLE IF EXISTS paging_info;
CREATE TABLE paging_info (
  timestamp DATETIME PRIMARY KEY,	-- End of measurement timestamp
  pag1_rate float NOT NULL,		-- Paging1 rate (paging/s)
  pag2_rate float NOT NULL,		-- Paging2 rate (paging/s)
  pag3_rate float NOT NULL,		-- Paging3 rate (paging/s)
  imsi_rate float NOT NULL,		-- IMSI paging rate (IMSI/s)
  tmsi_rate float NOT NULL		-- TMSI paging rate (TMSI/s)
);
