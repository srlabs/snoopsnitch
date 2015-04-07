--  A2
DROP VIEW IF EXISTS a2;
CREATE VIEW a2 AS
SELECT
	cell.id,
	cell.mcc,
	cell.mnc,
	cell.lac,
	cell.cid,
	--  If none of the observed neighbors has the same LAC
	--  as this cell the score is 0.5. Once ARFCN correlation
	--  is more reliable/tested we could increase that to 1.0
	((count(*) - sum(case when cell.lac != neig.lac then 1 else 0 end)) = 0) * 0.5 as score
FROM
	cell_info  AS cell,
	arfcn_list AS al,
	cell_info  AS neig
ON
	cell.id = al.id AND
	al.arfcn = neig.bcch_arfcn AND
	cell.bcch_arfcn != neig.bcch_arfcn AND
	cell.mcc = neig.mcc AND
	cell.mnc = neig.mnc AND
	--  Consider only neighboring information collected within the last
	--  10 minutes as valid (ARFCNs are reused!).
	abs(strftime('%s', cell.last_seen) - strftime('%s', neig.last_seen)) < 600
WHERE
	cell.mcc > 0 AND
	cell.mnc > 0 AND
	cell.lac > 0 AND
	cell.cid > 0 AND
	neig.mcc > 0 AND
	neig.mnc > 0 AND
	neig.lac > 0 AND
	neig.cid > 0
GROUP BY
	cell.mcc,
	cell.mnc,
	cell.lac,
	cell.cid
HAVING
	--  At least 3 neighbors should be known
	count(*) > 2;
