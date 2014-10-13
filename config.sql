--  Configuration
DROP TABLE IF EXISTS config;
CREATE TABLE config
(
        cro_max     int,
        t3212_min   int,
        delta_tch   int,
        delta_cmcp  int,
        n_norm      int,
        delta_arfcn int
);

INSERT INTO config VALUES
(
        40,      -- cro_max (FIXME: dummy value for now)
        10,      -- t3212_min
        1000000, -- delta_tch (FIXME: dummy value for now)
        2000,    -- delta_cmcp (FIXME: dummy value for now)
        42,      -- n_norm (FIXME: dummy value - we need sample data)
        1800     -- delta_arfcn (FIXME: dummy value - we need sample data)
);
