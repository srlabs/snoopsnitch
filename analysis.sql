--  Configuration
DROP TABLE IF EXISTS config;
CREATE TABLE config
(
        t3212_min  int,
        delta_tch  int,
        delta_cmcp int
);

INSERT INTO config VALUES
(
        10,      -- t3212_min
        1000000, -- delta_tch (FIXME: dummy value for now)
        2000     -- delta_cmcp (FIXME: dummy value for now)
);

--  Attract
.read sql/a_01.sql
.read sql/a_02.sql
.read sql/a_03.sql

--  Keep
.read sql/k_01.sql
.read sql/k_02.sql

--  Collect
.read sql/c_01.sql
.read sql/c_02.sql
.read sql/c_03.sql
.read sql/c_04.sql

--  Track
.read sql/t_01.sql
.read sql/t_03.sql
.read sql/t_04.sql

--  Output
.header on
.separator " 	"

DROP view IF EXISTS timestamps;
create view timestamps as
select distinct strftime('%Y-%m-%d %H:%M', timestamp) as timestamp from session_info
ORDER by timestamp;
