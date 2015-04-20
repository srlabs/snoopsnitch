DROP VIEW IF EXISTS max_cipher;
CREATE VIEW max_cipher AS
SELECT mcc, mnc, lac, cid, lu_acc, max(cipher) as max
FROM session_info
WHERE domain = 0 AND mcc > 0 AND lac > 0 AND cid > 0 AND (lu_acc OR call_presence OR sms_presence)
GROUP BY mcc, mnc, lac, cid, lu_acc;

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
ON si.mcc = mc.mcc AND si.mnc = mc.mnc AND si.lac = mc.lac AND si.cid = mc.cid AND si.lu_acc = mc.lu_acc
WHERE domain = 0 AND (si.lu_acc OR si.call_presence OR si.sms_presence);
