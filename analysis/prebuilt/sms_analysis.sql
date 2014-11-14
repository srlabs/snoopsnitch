INSERT INTO sms
SELECT
	sm.id,
	si.timestamp,
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
