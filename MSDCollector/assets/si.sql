DROP TABLE IF EXISTS session_info;
CREATE TABLE session_info (
  id integer PRIMARY KEY,		-- Transaction ID, incremental and unique in the db
  timestamp datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,		-- Timestamp
  rat tinyint NOT NULL,			-- Radio access technology (GSM=0, UMTS=1, LTE=2)
  domain tinyint NOT NULL,		-- Communication domain (Circuit Switched=0, Packet Switched=1)
  mcc smallint NOT NULL,		-- Mobile country code where the transaction was recorded
  mnc smallint NOT NULL,		-- Mobile network code where the transaction was recorded
  lac smallint NOT NULL,		-- Location area code where the transaction was recorded
  cid int NOT NULL,			-- Cell ID where the transaction was recorded
  arfcn int DEFAULT NULL,		-- ARFCN of current MCC/MNC/LAC/CID
  psc tinyint DEFAULT NULL,		-- Primary scrambling code of current cell 
  cracked tinyint DEFAULT NULL,		-- This transaction was encrypted and cracked by Kraken
  neigh_count smallint DEFAULT NULL,	-- Neighboring cell count
  unenc smallint DEFAULT NULL,		-- # of unencrypted frames
  unenc_rand smallint DEFAULT NULL,	-- # of unencrypted frams with randomized padding
  enc smallint DEFAULT NULL,		-- # of encrypted frames
  enc_rand smallint DEFAULT NULL,	-- # of encrypted frames with randomized padding
  enc_null smallint DEFAULT NULL,	-- # of encrypted null frames (zero length)
  enc_null_rand smallint DEFAULT NULL,	-- # of encrypted null frames with randomized padding
  enc_si smallint DEFAULT NULL,		-- # of encrypted SI5/6 messages
  enc_si_rand smallint DEFAULT NULL,	-- # of encrypted and randomized SI5/6 messages
  predict smallint DEFAULT NULL,	-- # of predictable frames (usable for cracking)
  avg_power smallint DEFAULT NULL,	-- Average measured power in received messages
  uplink_avail tinyint DEFAULT NULL,	-- Uplink messages available
  initial_seq tinyint DEFAULT NULL,	-- Initial key sequence number (in PAG_RESP, LUR, CM_REQ)
  cipher_seq tinyint DEFAULT NULL,	-- Ciphering start key sequence number (at CMC)
  auth tinyint DEFAULT NULL,		-- Authentication was performed (GSM A3/A8=1, UMTS AKA=2)
  auth_req_fn int DEFAULT NULL,		-- Authentication request frame number
  auth_resp_fn int DEFAULT NULL,	-- Authentication response frame number
  auth_delta int DEFAULT NULL,		-- Delta time to complete authentication
  cipher_missing tinyint DEFAULT NULL,	-- One of the cipher messages is missing
  cipher_comp_first int DEFAULT NULL,	-- First cipher mode complete frame number
  cipher_comp_last int DEFAULT NULL,	-- Last cipher mode complete frame number
  cipher_comp_count int DEFAULT NULL,	-- # of cipher mode complete sent
  cipher_delta int DEFAULT NULL,	-- Delta time to complete ciphering
  cipher tinyint DEFAULT NULL,		-- Cipher type (A5/x, UEA/x, EEA/x)
  cmc_imeisv tinyint DEFAULT NULL,	-- IMEISV is present in cipher mode complete
  integrity tinyint DEFAULT NULL,	-- Integrity type (UIA/x, EIA/x)
  first_fn int DEFAULT NULL,		-- Frame number of first received message
  last_fn int DEFAULT NULL,		-- Frame number of last received frame
  duration int DEFAULT NULL,		-- Transaction duration in ms
  mobile_orig tinyint DEFAULT NULL,	-- Transaction was mobile originated
  mobile_term tinyint DEFAULT NULL,	-- Transaction was mobile terminated
  paging_mi tinyint DEFAULT NULL,	-- Paging mobile identity type
  t_unknown tinyint DEFAULT NULL,	-- Transaction contains unknown messages
  t_detach tinyint DEFAULT NULL,	-- Transaction contains an IMSI detach
  t_locupd tinyint DEFAULT NULL,	-- Transactino contains a location update request
  lu_acc tinyint DEFAULT NULL,		-- Location update was accepted
  lu_type tinyint DEFAULT NULL,		-- Location update type
  lu_rej_cause tinyint DEFAULT NULL,	-- Location update was rejected with this cause
  lu_mcc smallint DEFAULT NULL,		-- MCC of previous user location
  lu_mnc smallint DEFAULT NULL,		-- MNC of previous user location
  lu_lac smallint DEFAULT NULL,		-- LAC of previous user location
  t_raupd tinyint DEFAULT NULL,		-- Transaction contains a routing area update
  t_attach tinyint DEFAULT NULL,	-- Transaction contains a GPRS attach
  att_acc tinyint DEFAULT NULL,		-- GPRS attach was accepted
  t_pdp tinyint DEFAULT NULL,		-- Transaction contains an activate PDP context
  pdp_ip char(16) DEFAULT NULL,		-- User IP assigned in PDP establishment
  t_call tinyint DEFAULT NULL,		-- Transaction contains a call-related messages
  t_sms tinyint DEFAULT NULL,		-- Transaction contains an SMS-related messages
  t_ss tinyint DEFAULT NULL,		-- Transaction contains a supplementary service request
  t_tmsi_realloc tinyint DEFAULT NULL,	-- TMSI was reallocated
  t_release tinyint DEFAULT NULL,	-- Transaction properly released
  rr_cause tinyint DEFAULT NULL,	-- Release cause (normal or abnormal)
  t_gprs tinyint DEFAULT NULL,		-- Mobile is GPRS attached
  iden_imsi_ac tinyint DEFAULT NULL,	-- IMSI was requested after ciphering
  iden_imsi_bc tinyint DEFAULT NULL,	-- IMSI was requested before ciphering
  iden_imei_ac tinyint DEFAULT NULL,	-- IMEI was requested after ciphering
  iden_imei_bc tinyint DEFAULT NULL,	-- IMEI was requested before ciphering
  assign tinyint DEFAULT NULL,		-- Transaction contains an assignment to other channel
  assign_cmpl tinyint DEFAULT NULL,	-- Transaction contains an assignment complete
  handover tinyint DEFAULT NULL,	-- Transaction contains a handover to other channel
  forced_ho tinyint DEFAULT NULL,	-- Handover was forced just after ciphering
  a_timeslot tinyint DEFAULT NULL,	-- Assigned channel timeslot
  a_chan_type tinyint DEFAULT NULL,	-- Assigned channel type
  a_tsc tinyint DEFAULT NULL,		-- Assigned channel TSC
  a_hopping tinyint DEFAULT NULL,	-- Assigned channel is hopping
  a_arfcn smallint DEFAULT NULL,	-- Assigned channel ARFCN (single)
  a_hsn tinyint DEFAULT NULL,		-- Assigned channel HSN
  a_maio tinyint DEFAULT NULL,		-- Assigned channel MAIO
  a_ma_len tinyint DEFAULT NULL,	-- Assigned channel MA_LEN
  a_chan_mode tinyint DEFAULT NULL,	-- Assigned channel mode (TCH)
  a_multirate tinyint DEFAULT NULL,	-- Assigned channel multirate conf (TCH)
  call_presence tinyint DEFAULT NULL,	-- Transaction contains a call setup
  sms_presence tinyint DEFAULT NULL,	-- Transaction contains SMS data
  service_req tinyint DEFAULT NULL,	-- Transaction contains a service request
  imsi char(32) DEFAULT NULL,		-- IMSI digits
  imei char(32) DEFAULT NULL,		-- IMEI digits
  tmsi char(32) DEFAULT NULL,		-- TMSI figits
  new_tmsi char(32) DEFAULT NULL,	-- Newly allocated TMSI digits
  tlli char(32) DEFAULT NULL,		-- TLLI digits
  msisdn char(32) DEFAULT NULL,		-- Caller/called MSISDN
  ms_cipher_mask smallint DEFAULT NULL,	-- Supported ciphering algorithms for GSM
  ue_cipher_cap smallint DEFAULT NULL,	-- supported ciphering algorithms for UMTS
  ue_integrity_cap smallint DEFAULT NULL -- Supported integrity algorithms for UMTS
);
DROP TABLE IF EXISTS serving_cell_info;
CREATE TABLE serving_cell_info (
  _id integer PRIMARY KEY,		-- Transaction ID, incremental and unique in the db
  timestamp datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,		-- Timestamp
  mcc integer NOT NULL,			-- mobile country code
  mnc integer NOT NULL,			-- mobile network code
  network_type integer NOT NULL,	-- Network type as returned by http://developer.android.com/reference/android/telephony/TelephonyManager.html#getNetworkType%28%29
  lac integer NULL,			-- location area code of current serving cell
  cid integer NULL,			-- cell id of current serving cell
  psc integer NULL			-- primary scrambling code of current serving cell
);

DROP TABLE IF EXISTS neighboring_cell_info;
CREATE TABLE neighboring_cell_info(
  _id integer PRIMARY KEY,		-- Transaction ID, incremental and unique in the db
  timestamp datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,		-- Timestamp
  last_sc_id integer NOT NULL,		-- Reference to the last entry in serving_cell_info about the current serving cell
  mcc integer NOT NULL,			-- mobile country code
  mnc integer NOT NULL,			-- mobile network code
  lac integer NOT NULL,			-- location area code of this neighboring cell
  cid integer NOT NULL,			-- cell id of this neighboring cell
  psc integer NULL			-- primary scrambling code of this neighboring cell
);

DROP TABLE IF EXISTS location_info;
CREATE TABLE location_info(
  _id integer PRIMARY KEY,		-- Transaction ID, incremental and unique in the db
  timestamp datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,		-- Timestamp
  accuracy DOUBLE NULL,			-- Accuracy in meters, from http://developer.android.com/reference/android/location/Location.html#getAccuracy(), may be NULL if hasAccuracy()==false
  latitude DOUBLE NOT NULL,		-- Latitude in degrees, from http://developer.android.com/reference/android/location/Location.html#getLatitude()
  longitude DOUBLE NOT NULL,		-- Longitude in degrees, from http://developer.android.com/reference/android/location/Location.html#getLongitude()
  altitude DOUBLE NULL,			-- Altitude in meters, from http://developer.android.com/reference/android/location/Location.html#getAltitude(), may be NULL if hasAltitude()==false
  provider_name char(32) DEFAULT NULL	-- Name of the provider which has generated the location, from http://developer.android.com/reference/android/location/Location.html#getProvider%28%29
);

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
