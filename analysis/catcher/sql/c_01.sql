DROP VIEW IF EXISTS max_cipher;
CREATE VIEW max_cipher AS
SELECT mcc, mnc, lac, cid, max(cipher) as max
FROM session_info
WHERE mcc > 0 and mnc > 0 and lac > 0 and cid > 0
GROUP BY mcc, mnc, lac, cid;

DROP VIEW IF EXISTS c1;
CREATE VIEW c1 AS
SELECT
        si.id,
        si.timestamp,
        si.mcc,
        si.mnc,
        si.lac,
        si.cid,
        cipher as value,
        mc.max as threshold,
        cipher < mc.max AS score
FROM session_info AS si, max_cipher as mc
ON si.mcc = mc.mcc AND si.mnc = mc.mnc AND si.lac = mc.lac AND si.cid = mc.cid
WHERE domain = 0;
