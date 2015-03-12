DROP VIEW IF EXISTS t1;
CREATE VIEW t1 AS
SELECT
        id,
        mcc,
        mnc,
        lac,
        cid,
        t3212 AS value,
		CASE
			WHEN t3212 < 5 THEN 1.5
			WHEN t3212 < 9 THEN 0.7
			ELSE 0.0
		END AS score
FROM cell_info
WHERE t3212 > 0;
