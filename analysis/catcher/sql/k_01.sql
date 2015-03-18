--  K1
DROP VIEW IF EXISTS k1;
CREATE VIEW k1 AS
SELECT
	id,
	mcc,
	mnc,
	lac,
	cid,
	CASE
		WHEN (neigh_5 + neigh_5b + neigh_5t) > 1 THEN 0.0
		ELSE 1.0
	END as score
	FROM cell_info
	WHERE si5 OR si5b OR si5t;
