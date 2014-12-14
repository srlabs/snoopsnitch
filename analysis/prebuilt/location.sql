DROP VIEW IF EXISTS si_loc;
CREATE VIEW si_loc AS
SELECT
	si.id,
	ifnull(min(abs(strftime('%s', si.timestamp) - strftime('%s', li.timestamp))) < config.loc_max_delta, 0) as valid,
	ifnull(li.latitude, 0.0) as latitude,
	ifnull(li.longitude, 0.0) as longitude
FROM
	session_info AS si LEFT JOIN
	location_info AS li,
	config
GROUP BY id
;
