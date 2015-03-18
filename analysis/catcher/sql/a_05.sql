--  All cells with a valid cell ID
DROP VIEW IF EXISTS valid_cells;
CREATE VIEW valid_cells AS
SELECT
	mcc,
	mnc,
	lac,
	cid
FROM
	session_info
WHERE
	mcc > 0 AND mnc > 0 AND lac > 0 AND cid > 0 AND mcc < 1000 AND mnc < 1000
GROUP BY
	mcc, mnc, lac, cid;

--  Query all LACs that have only a single cell. This
--  can be a sign of a catcher, e.g. when the catchers
--  invents a new LAC
DROP VIEW IF EXISTS lonesome_lacs;
CREATE VIEW lonesome_lacs AS
SELECT
	mcc,
	mnc,
	lac
FROM
	valid_cells
GROUP BY
	mcc, mnc, lac
HAVING
	count(*) = 1;

--  Match all sessions with 'lonesome LACs'
DROP VIEW IF EXISTS a5;
CREATE VIEW a5 as
SELECT 
    si.id,
    si.mcc,
    si.mnc,
    si.lac,
    si.cid,
	1.0 AS score
FROM
	session_info as si, lonesome_lacs as ll
ON
	si.mcc = ll.mcc AND
	si.mnc = ll.mnc AND
	si.lac = ll.lac;
