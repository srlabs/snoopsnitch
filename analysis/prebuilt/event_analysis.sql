DELETE FROM events;
INSERT INTO events
SELECT
	si.id,
	sm.sequence,
	si.timestamp,
	si.mcc,
	si.mnc,
	si.lac,
	si.cid,
	ifnull(sl.longitude, 0.0),
	ifnull(sl.latitude, 0.0),
	ifnull(sl.valid, 0),
	sm.smsc,
	sm.msisdn,
	CASE
		-- OTA/binary SMS
		WHEN ota AND from_network
			THEN 1
		-- silent SMS
		WHEN from_network AND
		     (dcs >= 192 AND dcs <= 207) OR
			 (pid = 64 AND info NOT LIKE '%PORT%5499%')
			THEN 2
		-- All other transaction we don't care about
		ELSE 0
	END as event_type
FROM
	session_info as si, sms_meta as sm ON sm.id = si.id LEFT JOIN
    si_loc as sl ON si.id = sl.id
WHERE
	event_type > 0 AND
	domain = 0;

INSERT INTO events
SELECT
	si.id,
	-1,
	si.timestamp,
	si.mcc,
	si.mnc,
	si.lac,
	si.cid,
	0.0,
	0.0,
	0,
	"-",
	"-",
	3 as event_type
FROM
	session_info as si
WHERE
	domain = 0 AND
	mobile_term AND NOT t_call AND NOT t_sms AND NOT t_locupd AND t_abort;
