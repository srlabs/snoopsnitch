DROP VIEW IF EXISTS c2;
CREATE VIEW c2 AS
SELECT
        si.id,
        si.timestamp,
        si.mcc,
        si.mnc,
        si.lac,
        si.cipher_delta as value,
        si.cipher_delta > config.delta_cmcp as score
FROM session_info AS si, config
WHERE domain = 0 AND cipher = 1;
