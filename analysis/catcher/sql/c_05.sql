DROP VIEW IF EXISTS c5;
CREATE VIEW c5 AS
SELECT
        si.id,
        si.timestamp,
        si.mcc,
        si.mnc,
        si.lac,
        si.cid,
        CASE WHEN cipher = 0 THEN 1 ELSE 0 END score
FROM session_info AS si
WHERE
	domain = 0 AND
	((t_locupd AND lu_acc) OR t_call OR t_sms);
