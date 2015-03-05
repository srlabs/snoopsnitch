/*!40101 SET storage_engine=MyISAM */;

DROP TABLE IF EXISTS sms_meta;
CREATE TABLE sms_meta (
  id integer NOT NULL,			-- Transaction ID where this message was found
  sequence tinyint NOT NULL,		-- Sequential message counter inside for this transaction
  from_network tinyint NOT NULL,	-- Radio access technology (GSM=0, UMTS=1, LTE=2)
  pid smallint NOT NULL,		-- Protocol identifier
  dcs smallint NOT NULL,		-- Data coding scheme
  alphabet tinyint NOT NULL,		-- Coding alphabet used for user data
  class tinyint NOT NULL,		-- Which device receives the message: display, ME, SIM, TE
  udhi tinyint NOT NULL,		-- User data header indicator
  concat tinyint NOT NULL,		-- Message is composed of several fragments
  concat_frag smallint NOT NULL,	-- Sequential index of this fragment
  concat_total smallint NOT NULL,	-- Total number of fragments 
  src_port integer NOT NULL,		-- Source port for application addressing
  dst_port integer NOT NULL,		-- Destination port for application addressing
  ota tinyint NOT NULL,			-- Message contains an OTA command or response 
  ota_iei tinyint NOT NULL,		-- Security header IEI
  ota_enc tinyint NOT NULL,		-- OTA payload is encrypted
  ota_enc_algo tinyint NOT NULL,	-- OTA encryption algorithm 
  ota_sign tinyint NOT NULL,		-- OTA integrity protection type
  ota_sign_algo tinyint NOT NULL,	-- OTA signature algorithm 
  ota_counter tinyint NOT NULL,		-- OTA counter type/presence
  ota_counter_value CHAR(10),		-- OTA counter value (hex)
  ota_tar CHAR(6),			-- OTA TAR value (hex)
  ota_por smallint NOT NULL,		-- OTA POR status value
  smsc CHAR(32) NOT NULL,		-- SMSC that handled or will handle the message
  msisdn CHAR(32) NOT NULL,		-- Destination or source number of message
  info CHAR(255) NOT NULL,		-- Decoded information in human readable form
  length smallint NOT NULL,		-- User data length
  udh_length smallint NOT NULL,		-- User data header length
  real_length smallint NOT NULL,	-- Received data length
  data BINARY(255) NOT NULL,		-- User data in binary form
  PRIMARY KEY(id, sequence)
);
