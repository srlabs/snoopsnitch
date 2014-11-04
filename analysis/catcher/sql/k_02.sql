--  K2
drop view if exists k2;
create view k2 as
select
        id,
        mcc,
        mnc,
        lac,
        cid,
        cro as value,
        (cro > config.cro_max) as score
from cell_info, config
where mcc > 0 and mnc > 0 and lac > 0 and cid > 0 and cro > 0;
