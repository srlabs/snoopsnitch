DELETE FROM events;
INSERT INTO events
SELECT
	si.id,
	si.timestamp,
	si.mcc,
	si.mnc,
	si.lac,
	si.cid,
	sl.longitude,
	sl.latitude,
	sl.valid,
	--  FIXME: Handle SMSC/MSISDN null values in UI properly
	--  by destinguishing different event types
	ifnull(sm.smsc, "-"),
	ifnull(sm.msisdn, "-"),
	CASE
		-- OTA/binary SMS
		WHEN ota AND from_network
			THEN 1
		-- silent SMS
		WHEN dcs >= 192 AND dcs <= 207 AND from_network
			THEN 2
		-- Null paging
		WHEN mobile_term AND NOT t_call AND NOT t_sms AND NOT t_locupd AND t_abort
			THEN 3
		-- All other transaction we don't care about
		ELSE 0
	END as event_type
FROM
	session_info as si,
	si_loc as sl LEFT JOIN
	sms_meta as sm
ON
	sm.id = si.id AND
	si.id = sl.id
WHERE
	event_type > 0 AND
	domain = 0;
