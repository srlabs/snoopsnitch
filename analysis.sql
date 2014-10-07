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

--  Output
.header on
.separator " 	"

DROP view IF EXISTS timestamps;
create view timestamps as
select distinct strftime('%Y-%m-%d %H:%M', timestamp) as timestamp from session_info
ORDER by timestamp;

select
        strftime('%Y-%m-%d %H:%M', timestamps.timestamp) as timestamp,
        c1.score as c1
from timestamps, c1
on  strftime('%Y-%m-%d %H:%M', timestamps.timestamp) = strftime('%Y-%m-%d %H:%M', c1.timestamp)
order by timestamp;
