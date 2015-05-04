DROP VIEW IF EXISTS c2;
CREATE VIEW c2 AS
SELECT
        si.id,
        si.timestamp,
        si.mcc,
        si.mnc,
        si.lac,
        si.cipher_delta as value,
	CASE WHEN si.cipher_delta > config.delta_cmcp THEN
		CASE WHEN cmc_imeisv THEN 2.0 ELSE 1.0 END
	END as score
FROM session_info AS si, config
WHERE domain = 0 AND (cipher = 1 OR cipher = 2);
