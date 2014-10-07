-- FIXME: Select sane value for for delta threshold or
-- even create propability function.

DROP VIEW IF EXISTS c2;
CREATE VIEW c2 AS
SELECT
        si.id,
        si.timestamp,
        si.mcc,
        si.mnc,
        si.lac,
        si.cipher_delta as c2_value,
        si.cipher_delta > 100 as c2
FROM session_info AS si;

select * from c2;
