DELETE FROM sms;
INSERT INTO sms
SELECT
	si.id,
	si.timestamp,
	si.mcc,
	si.mnc,
	si.lac,
	si.cid,
	0.0 as longitude,	-- TODO: Get longitude from GPS table
	0.0 as latitude,	-- TODO: Get latitude from GPS table
	sm.smsc,
	sm.msisdn,
	CASE
		WHEN ota                       THEN 0  -- OTA (binary SMS)
		WHEN dcs >= 192 AND dcs <= 207 THEN 1  -- silent SMS
		ELSE 2                                 -- regular SMS
	END as sms_type
FROM
	sms_meta as sm, session_info as si
ON
	sm.id = si.id
WHERE
	sms_type != 2 AND
	from_network;
