--  All cell_info-based criteria
.read config.sql

--  Attract
.read sql/a_01.sql
.read sql/a_02.sql
.read sql/a_04.sql

--  Keep
.read sql/k_01.sql
.read sql/k_02.sql

--  Track
.read sql/t_01.sql

--  Reject
.read sql/r_01.sql
.read sql/r_02.sql

--  Result
DROP VIEW IF EXISTS ci;
CREATE VIEW ci AS
SELECT DISTINCT
        ci.id,
        ci.first_seen,
        ci.last_seen,
        ci.mcc,
        ci.mnc,
        ci.lac,
        ci.cid,
        ifnull(a1.score, 0) as a1,
        ifnull(a2.score, 0) as a2,
        ifnull(a4.score, 0) as a4,
        ifnull(k1.score, 0) as k1,
        ifnull(k2.score, 0) as k2,
        ifnull(t1.score, 0) as t1,
        ifnull(r1.score, 0) as r1,
        ifnull(r2.score, 0) as r2,
        (ifnull(a1.score, 0) +
         ifnull(a2.score, 0) +
         ifnull(a4.score, 0) +
         ifnull(k1.score, 0) +
         ifnull(k2.score, 0) +
         ifnull(t1.score, 0) +
         ifnull(r1.score, 0) +
         ifnull(r2.score, 0)) as score
FROM cell_info as ci LEFT JOIN
 a1 ON ci.id = a1.id LEFT JOIN
 a2 ON ci.id = a2.id LEFT JOIN
 a4 ON ci.id = a4.id LEFT JOIN
 k1 ON ci.id = k1.id LEFT JOIN
 k2 ON ci.id = k2.id LEFT JOIN
 t1 ON ci.id = t1.id LEFT JOIN
 r1 ON ci.id = r1.id LEFT JOIN
 r2 ON ci.id = r2.id
WHERE ci.mcc > 0 and ci.mnc > 0 and ci.lac > 0 and ci.cid > 0;
