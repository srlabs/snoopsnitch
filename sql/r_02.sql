--  R2

--  From the CatcherCatcher model:
--
--  N = (9 − BS_AG_BLKS_RES − 6 · BS_CCCH_SDCCH_COMB) · BS_PA_MFRMS
--  BS_AG_BLKS_RES:     BCCH/SI3, Cell database field cell_info.agch_blocks
--  BS_CCCH_SDCCH_COMB: BCCH/SI3, Cell database field cell_info.combined
--  BS_PA_MFRMS:        BCCH/SI3, Cell database field cell_info.pag_mframes

DROP VIEW IF EXISTS r2;
CREATE VIEW r2 AS
SELECT
        id,
        mcc,
        mnc,
        cid,
        lac,
        ((9 - cell_info.agch_blocks - 6 * cell_info.combined) * cell_info.pag_mframes) > config.n_norm as score
FROM cell_info, config;
