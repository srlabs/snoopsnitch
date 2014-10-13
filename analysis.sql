.read config.sql

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

--  Reject
.read sql/r_01.sql
.read sql/r_02.sql

--  Output
.header on
.separator " 	"

DROP view IF EXISTS timestamps;
create view timestamps as
select distinct strftime('%Y-%m-%d %H:%M', timestamp) as timestamp from session_info
ORDER by timestamp;
