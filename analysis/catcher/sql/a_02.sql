--  A2
DROP VIEW IF EXISTS a2;
CREATE VIEW a2 AS
SELECT DISTINCT
	cell.id,
	cell.mcc,
	cell.mnc,
	cell.lac,
	cell.cid,
	--  If none of the observed neighbors has the same LAC as this cell the score is 1.0
	0.0 + ((count(*) - sum(case when cell.lac != neig.lac then 1 else 0 end)) = 0) as score
FROM
	cell_info  AS cell,
	arfcn_list AS al,
	cell_info  AS neig,
	config
ON
	cell.id = al.id AND
	al.arfcn = neig.bcch_arfcn AND
	cell.bcch_arfcn != neig.bcch_arfcn AND
	cell.mcc = neig.mcc AND
	cell.mnc = neig.mnc
WHERE
	cell.mcc > 0 AND
	cell.mnc > 0 AND
	cell.lac > 0 AND
	cell.cid > 0 AND
	neig.lac > 0
GROUP BY cell.id;
