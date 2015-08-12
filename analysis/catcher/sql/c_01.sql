DROP VIEW IF EXISTS max_cipher;
CREATE VIEW max_cipher AS
SELECT mcc, mnc, lac, cid, lu_acc, max(cipher) as max
FROM session_info
WHERE domain = 0 AND mcc > 0 AND lac > 0 AND cid > 0 AND (lu_acc OR call_presence OR sms_presence)
GROUP BY mcc, mnc, lac, cid, lu_acc;

DROP VIEW IF EXISTS best_cipher;
CREATE VIEW best_cipher AS
SELECT mc.mcc, mc.mnc, mc.lac, mc.cid, mc.lu_acc, mc.max, min(si.timestamp) as upgraded_since
FROM max_cipher as mc, session_info as si
ON si.mcc = mc.mcc AND si.mnc = mc.mnc AND si.lac = mc.lac AND si.cid = mc.cid AND si.lu_acc = mc.lu_acc and si.cipher = mc.max
WHERE domain = 0
GROUP BY mc.mcc, mc.mnc, mc.lac, mc.cid, mc.lu_acc;

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
        bc.max as threshold,
        (si.timestamp > bc.upgraded_since) AND (si.cipher < bc.max) AS score
FROM session_info AS si, best_cipher as bc
ON si.mcc = bc.mcc AND si.mnc = bc.mnc AND si.lac = bc.lac AND si.cid = bc.cid AND si.lu_acc = bc.lu_acc
WHERE rat = 0 AND domain = 0 AND (si.lu_acc OR si.call_presence OR si.sms_presence);
