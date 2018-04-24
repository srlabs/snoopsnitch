/*!40101 SET storage_engine=MyISAM */;

DROP TABLE IF EXISTS session_info;
CREATE TABLE session_info (
  id integer PRIMARY KEY,		-- Transaction ID, incremental and unique in the db
  timestamp datetime NOT NULL,		-- Timestamp
  rat tinyint NOT NULL,			-- Radio access technology (GSM=0, UMTS=1, LTE=2)
  domain tinyint NOT NULL,		-- Communication domain (Circuit Switched=0, Packet Switched=1)
  mcc smallint NOT NULL,		-- Mobile country code where the transaction was recorded
  mnc smallint NOT NULL,		-- Mobile network code where the transaction was recorded
  lac int NOT NULL,			-- Location area code where the transaction was recorded
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
  lu_reject tinyint DEFAULT NULL,	-- Location update was rejected
  lu_rej_cause tinyint DEFAULT NULL,	-- Location update was rejected with this cause
  lu_mcc smallint DEFAULT NULL,		-- MCC of previous user location
  lu_mnc smallint DEFAULT NULL,		-- MNC of previous user location
  lu_lac int DEFAULT NULL,		-- LAC of previous user location
  t_abort tinyint DEFAULT NULL,		-- Transaction aborted
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

DROP TABLE IF EXISTS rand_check;
CREATE TABLE rand_check (
  sid integer NOT NULL,			-- Linked session ID
  si5 float DEFAULT NULL,		-- Content randomization ratio for SI5
  si5bis float DEFAULT NULL,		-- Content randomization ratio for SI5bis
  si5ter float DEFAULT NULL,		-- Content randomization ratio for SI5ter
  si6 float DEFAULT NULL,		-- Content randomization ratio for SI6
  nullframe float DEFAULT NULL,		-- Padding randomization for NULL frames
  sdcch_padding float DEFAULT NULL,	-- Padding randomization for non-NULL SDCCH messages
  sacch_padding float DEFAULT NULL,	-- Padding randomization for non-NULL SDCCH messages
  PRIMARY KEY(sid)
);

DROP TABLE IF EXISTS sid_appid;
CREATE TABLE sid_appid (
  sid integer PRIMARY KEY,	-- Session ID
  appid char(8) NOT NULL	-- Application ID
);
