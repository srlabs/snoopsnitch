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
  ota tinyint NOT NULL,			-- Message contains an OTA command or response 
  concat tinyint NOT NULL,		-- Message is a fragment of a concatenated message 
  smsc CHAR(32) NOT NULL,		-- SMSC that handled or will handle the message
  msisdn CHAR(32) NOT NULL,		-- Destination or source number of message
  info CHAR(255) NOT NULL,		-- Decoded information in human readable form
  length smallint NOT NULL,		-- User data length
  data BINARY(255) NOT NULL,		-- User data in binary form
  PRIMARY KEY(id, sequence)
);
