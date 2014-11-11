.read analysis_si.sql
.read analysis_ci.sql

--  All unavailable criteria

--  Attract
.read sql/a_03.sql

--  Track
.read sql/t_07.sql

DELETE FROM catcher;
INSERT INTO catcher
SELECT
        ci.mcc,
        ci.mnc,
        ci.lac,
        ci.cid,
        si.timestamp  AS timestamp,
        ci.a1,
        ci.a2,
        ci.a4,
        ci.k1,
        ci.k2,
        si.c1,
        si.c2,
        si.c3,
        si.c4,
        ci.t1,
        si.t3,
        si.t4,
        ci.r1,
        ci.r2,
        ci.a1 + ci.a2 + ci.a4 + ci.k1 + ci.k2 +
        si.c1 + si.c2 + si.c3 + si.c4 + ci.t1 +
        si.t3 + si.t4 + ci.r1 + ci.r2 as score
FROM si, ci
ON
        ci.mcc = si.mcc AND
        ci.mnc = si.mnc AND
        ci.lac = si.lac AND
        ci.cid = si.cid AND
        strftime('%s', si.timestamp) - strftime('%s', ci.last_seen) >= 0 AND
        strftime('%s', si.timestamp) - strftime('%s', ci.last_seen) < 10;
