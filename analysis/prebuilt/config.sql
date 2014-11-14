--  Configuration
DROP TABLE IF EXISTS config;
CREATE TABLE config
(
        cro_max     int,
        t3212_min   int,
        delta_tch   int,
        delta_cmcp  int,
        n_norm      int,
        delta_arfcn int, --  Time in seconds we assume is needed to move from one place
                         --  an ARFCN is used by a cell to another place where the same
                         --  ARFCN is used for a *different* cell.
        min_pag1_rate float
);

INSERT INTO config VALUES
(
        40,    -- cro_max (FIXME: dummy value for now)
        10,    -- t3212_min
        10000, -- delta_tch (FIXME: dummy value for now)
        2000,  -- delta_cmcp (FIXME: dummy value for now)
        42,    -- n_norm (FIXME: dummy value - we need sample data)
        1800,  -- delta_arfcn (FIXME: dummy value - we need sample data)
        0.4    -- min_pag1_rate (FIXME: Need better sampes)
);
