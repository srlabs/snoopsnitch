DELETE FROM sms;
INSERT INTO sms
SELECT
	si.id,
	si.timestamp,
	si.mcc,
	si.mnc,
	si.lac,
	si.cid,
	si_loc.longitude,
	si_loc.latitude,
	si_loc.valid,
	sm.smsc,
	sm.msisdn,
	CASE
		WHEN ota                       THEN 0  -- OTA (binary SMS)
		WHEN dcs >= 192 AND dcs <= 207 THEN 1  -- silent SMS
		ELSE 2                                 -- regular SMS
	END as sms_type
FROM
	sms_meta as sm, session_info as si, si_loc
ON
	sm.id = si.id AND
	si.id = si_loc.id
WHERE
	sms_type != 2 AND
	from_network;
