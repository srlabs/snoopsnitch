--  All session_info-based criteria

--  Collect
.read sql/c_01.sql
.read sql/c_02.sql
.read sql/c_03.sql
.read sql/c_04.sql

--  Track
.read sql/t_03.sql
.read sql/t_04.sql

--  Fingerprint
.read sql/f_01.sql

--  Result
DROP VIEW IF EXISTS si;
CREATE VIEW si AS
SELECT
        si.id,
        si.timestamp,
        si.mcc,
        si.mnc,
        si.lac,
        si.cid,
        ifnull(c1.score, 0) as c1,
        ifnull(c2.score, 0) as c2,
        ifnull(c3.score, 0) as c3,
        ifnull(c4.score, 0) as c4,
        ifnull(t3.score, 0) as t3,
        ifnull(t4.score, 0) as t4,
        ifnull(f1.score, 0) as f1,
        (ifnull(c1.score, 0) +
         ifnull(c2.score, 0) +
         ifnull(c3.score, 0) +
         ifnull(c4.score, 0) +
         ifnull(t3.score, 0) +
         ifnull(t4.score, 0) +
         ifnull(f1.score, 0)) as score
FROM session_info as si LEFT JOIN
    c1 ON si.id = c1.id LEFT JOIN
    c2 ON si.id = c2.id LEFT JOIN
    c3 ON si.id = c3.id LEFT JOIN
    c4 ON si.id = c4.id LEFT JOIN
    t3 ON si.id = t3.id LEFT JOIN
    t4 ON si.id = t4.id LEFT JOIN
	f1 ON si.id = f1.id
WHERE si.mcc > 0 and si.mnc > 0 and si.lac > 0 and si.cid > 0;
