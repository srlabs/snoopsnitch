-- security metrics v2.5beta



-- FIXME: The orignal avg_of_* functions had a different semantics, which
-- resulted in NULL if both, a and b is NULL. Also, if a parameter is NULL the
-- other parameters value is inherited.






-- As per definition of VAR_POP SQL function





-- available valid operators population
delete from valid_op;
insert into valid_op
 select session_info.mcc     as mcc,
	session_info.mnc     as mnc,
	country              as country,
	network              as network,
	date(min(timestamp)) as oldest,
	date(max(timestamp)) as latest,
	0                    as cipher
 from session_info, mnc
 where	mnc.mcc = session_info.mcc and
	mnc.mnc = session_info.mnc
 and ((t_locupd and (lu_acc or cipher > 1 or rat > 0)) or
      (t_sms and (t_release or cipher > 1 or rat > 0)) or
      (t_call and (assign or cipher > 1 or rat > 0)))
 and (duration > 350 or cipher > 0 or rat > 0)
 group by session_info.mcc, session_info.mnc
 order by session_info.mcc, session_info.mnc;

-- Ignore test/reserved networks
delete from valid_op
 where mcc < 200 or mcc >= 1000
 or mnc >= 1000;

-- Ignore railway networks (GSM-R)
delete from valid_op
 where (mcc = 204 and mnc = 21)
 or (mcc = 208 and mnc = 14)
 or (mcc = 216 and mnc = 99)
 or (mcc = 222 and mnc = 30)
 or (mcc = 228 and mnc = 6)
 or (mcc = 230 and mnc = 98)
 or (mcc = 231 and mnc = 99)
 or (mcc = 232 and mnc = 91)
 or (mcc = 234 and mnc = 12)
 or (mcc = 234 and mnc = 13)
 or (mcc = 235 and mnc = 95)
 or (mcc = 238 and mnc = 23)
 or (mcc = 240 and mnc = 21)
 or (mcc = 242 and mnc = 20)
 or (mcc = 242 and mnc = 21)
 or (mcc = 244 and mnc = 17)
 or (mcc = 246 and mnc = 5)
 or (mcc = 262 and mnc = 10)
 or (mcc = 262 and mnc = 60)
 or (mcc = 284 and mnc = 7)
 or (mcc = 420 and mnc = 21)
 or (mcc = 460 and mnc = 20)
 or (mcc = 505 and mnc = 13);

-- Ignore non-stationary networks
delete from valid_op
 where (mcc = 901)
 or (mcc = 262 and mnc = 42);

-- Expand to every cipher configuration
insert into valid_op select distinct mcc,mnc,country,network,oldest,latest,1 from valid_op;
insert into valid_op select distinct mcc,mnc,country,network,oldest,latest,2 from valid_op;
insert into valid_op select distinct mcc,mnc,country,network,oldest,latest,3 from valid_op;

--

-- Clean up invalid rand values
delete from rand_check where sid >= 8000000;

-- Call averages
delete from call_avg;
insert into call_avg
  select mcc, mnc, lac, strftime( "%Y-%m",timestamp) as month, cipher,
	 count(*) as count,
	 sum(CASE WHEN mobile_orig THEN 1 ELSE 0 END) as mo_count,
	 avg(cracked) as success,
	 avg(nullframe) as rand_null_perc,
	 ((IFNULL(((IFNULL(si5,0) + IFNULL( si5bis,0)) / 2),0) + IFNULL( ((IFNULL(si5ter,0) + IFNULL( si6,0)) / 2),0)) / 2) as rand_si_perc,
	 avg(enc_null - enc_null_rand) as nulls,
	 avg(predict) as pred,
	 avg(cmc_imeisv) as imeisv,
	 avg(CASE WHEN mobile_term THEN CASE WHEN auth > 0 THEN 1 ELSE 0 END ELSE NULL END) as auth_mt,
	 avg(CASE WHEN mobile_orig THEN CASE WHEN auth > 0 THEN 1 ELSE 0 END ELSE NULL END) as auth_mo,
	 avg(t_tmsi_realloc) as tmsi,
	 avg(iden_imsi_bc) as imsi
  from session_info as s left outer join rand_check as r on (s.id = r.sid and r.sid < 8000000)
  where rat = 0 and ((t_call or (mobile_term and t_sms = 0)) and
	(call_presence or (cipher=1 and cracked=0) or cipher>1)) and
	(cipher > 0 or duration > 350)
  group by mcc, mnc, lac, month, cipher
  order by mcc, mnc, lac, month, cipher;

-- SMS averages
delete from sms_avg;
insert into sms_avg
  select mcc, mnc, lac, strftime( "%Y-%m",timestamp) as month, cipher,
	 count(*) as count,
	 sum(CASE WHEN mobile_orig THEN 1 ELSE 0 END) as mo_count,
	 avg(cracked) as success,
	 avg(nullframe) as rand_null_perc,
	 ((IFNULL(((IFNULL(si5,0) + IFNULL( si5bis,0)) / 2),0) + IFNULL( ((IFNULL(si5ter,0) + IFNULL( si6,0)) / 2),0)) / 2) as rand_si_perc,
	 avg(enc_null - enc_null_rand) as nulls,
	 avg(predict) as pred,
	 avg(cmc_imeisv) as imeisv,
	 avg(CASE WHEN mobile_term THEN CASE WHEN auth > 0 THEN 1 ELSE 0 END ELSE NULL END) as auth_mt,
	 avg(CASE WHEN mobile_orig THEN CASE WHEN auth > 0 THEN 1 ELSE 0 END ELSE NULL END) as auth_mo,
	 avg(t_tmsi_realloc) as tmsi,
	 avg(iden_imsi_bc) as imsi
  from session_info as s left outer join rand_check as r on (s.id = r.sid and r.sid < 8000000)
  where rat = 0 and (t_sms and (sms_presence or (cipher=1 and cracked=0) or cipher>1))
  group by mcc, mnc, lac, month, cipher
  order by mcc, mnc, lac, month, cipher;

-- LUR averages
delete from loc_avg;
insert into loc_avg
  select mcc, mnc, lac, strftime( "%Y-%m",timestamp) as month, cipher,
	 count(*) as count,
	 sum(CASE WHEN mobile_orig THEN 1 ELSE 0 END) as mo_count,
	 avg(cracked) as success,
	 avg(nullframe) as rand_null_perc,
	 ((IFNULL(((IFNULL(si5,0) + IFNULL( si5bis,0)) / 2),0) + IFNULL( ((IFNULL(si5ter,0) + IFNULL( si6,0)) / 2),0)) / 2) as rand_si_perc,
	 avg(enc_null - enc_null_rand) as nulls,
	 avg(predict) as pred,
	 avg(cmc_imeisv) as imeisv,
	 avg(CASE WHEN mobile_term THEN CASE WHEN auth > 0 THEN 1 ELSE 0 END ELSE NULL END) as auth_mt,
	 avg(CASE WHEN mobile_orig THEN CASE WHEN auth > 0 THEN 1 ELSE 0 END ELSE NULL END) as auth_mo,
	 avg(t_tmsi_realloc) as tmsi,
	 avg(iden_imsi_bc) as imsi
  from session_info as s left outer join rand_check as r on (s.id = r.sid and r.sid < 8000000)
  where rat = 0 and t_locupd and (lu_acc or cipher > 1)
  group by mcc, mnc, lac, month, cipher
  order by mcc, mnc, lac, month, cipher;

-- Cell entropy averages
delete from entropy_cell;
insert into entropy_cell
  select mcc, mnc, lac, cid, strftime( "%Y-%m",timestamp) as month, cipher,
	avg(a_ma_len + 1 - a_hopping) as a_len,
	((sum(((a_ma_len + 1 - a_hopping)/64)*((a_ma_len + 1 - a_hopping)/64)) - (sum((a_ma_len + 1 - a_hopping)/64) * sum((a_ma_len + 1 - a_hopping)/64)) / count((a_ma_len + 1 - a_hopping)/64)) / count((a_ma_len + 1 - a_hopping)/64)) as v_len,
	((sum((a_hsn/64)*(a_hsn/64)) - (sum(a_hsn/64) * sum(a_hsn/64)) / count(a_hsn/64)) / count(a_hsn/64)) as v_hsn,
	((sum((a_maio/64)*(a_maio/64)) - (sum(a_maio/64) * sum(a_maio/64)) / count(a_maio/64)) / count(a_maio/64)) as v_maio,
	((sum((a_timeslot/8)*(a_timeslot/8)) - (sum(a_timeslot/8) * sum(a_timeslot/8)) / count(a_timeslot/8)) / count(a_timeslot/8)) as v_ts,
	((sum((a_tsc/8)*(a_tsc/8)) - (sum(a_tsc/8) * sum(a_tsc/8)) / count(a_tsc/8)) / count(a_tsc/8)) as v_tsc
  from session_info
  where rat = 0 and (assign or handover) and
  (cipher > 0 or duration > 350)
  group by mcc, mnc, lac, cid, month, cipher;

-- LAC entropy averages
delete from entropy;
insert into entropy
  select mcc, mnc, lac, month, cipher,
	 avg(a_len) as ma_len,
	 avg(v_len) as var_len,
	 avg(v_hsn) as var_hsn,
	 avg(v_maio) as var_maio,
	 avg(v_ts) as var_ts,
	 avg(v_tsc) as var_tsc
    from entropy_cell
    group by mcc, mnc, lac, month, cipher
    order by mcc, mnc, lac, month, cipher;

-- "sec_params" population
delete from sec_params;
insert into sec_params
 select
        va.mcc                         as mcc,
        va.mnc                         as mnc,
        va.country                     as country,
        va.network                     as network,
        c.lac                          as lac,
        c.month                        as month,
        va.cipher                      as cipher,
        c.count                        as call_count,
        c.mo_count                     as call_mo_count,
        s.count                        as sms_count,
        s.mo_count                     as sms_mo_count,
        l.count                        as loc_count,
        c.success                      as call_success,
        s.success                      as sms_success,
        l.success                      as loc_success,
        c.rand_null_perc               as call_null_rand,
        s.rand_null_perc               as sms_null_rand,
        l.rand_null_perc               as loc_null_rand,
        c.rand_si_perc                 as call_si_rand,
        s.rand_si_perc                 as sms_si_rand,
        l.rand_si_perc                 as loc_si_rand,
        c.nulls                        as call_nulls,
        s.nulls                        as sms_nulls,
        l.nulls                        as loc_nulls,
        c.pred                         as call_pred,
        s.pred                         as sms_pred,
        l.pred                         as loc_pred,
        c.imeisv                       as call_imeisv,
        s.imeisv                       as sms_imeisv,
        l.imeisv                       as loc_imeisv,
        ((IFNULL(c.auth_mt,0) + IFNULL( s.auth_mt,0)) / 2) as pag_auth_mt,
        c.auth_mo                      as call_auth_mo,
        s.auth_mo                      as sms_auth_mo,
        l.auth_mo                      as loc_auth_mo,
        c.tmsi                         as call_tmsi,
        s.tmsi                         as sms_tmsi,
        l.tmsi                         as loc_tmsi,
        c.imsi                         as call_imsi,
        s.imsi                         as sms_imsi,
        l.imsi                         as loc_imsi,
        e.ma_len                       as ma_len,
        e.var_len                      as var_len,
        e.var_hsn                      as var_hsn,
        e.var_maio                     as var_maio,
        e.var_ts                       as var_ts,
        h.rand_imsi                    as rand_imsi,
        h.home_routing                 as home_routing
 from
        valid_op as va
        left outer join call_avg as c on (va.mcc = c.mcc and va.mnc = c.mnc and va.cipher = c.cipher)
        left outer join sms_avg  as s on (va.mcc = s.mcc and va.mnc = s.mnc and va.cipher = s.cipher and c.lac = s.lac and c.month = s.month)
        left outer join loc_avg  as l on (va.mcc = l.mcc and va.mnc = l.mnc and va.cipher = l.cipher and c.lac = l.lac and c.month = l.month)
        left outer join entropy  as e on (va.mcc = e.mcc and va.mnc = e.mnc and va.cipher = e.cipher and c.lac = e.lac and c.month = e.month)
        left outer join hlr_info as h on (va.mcc = h.mcc and va.mnc = h.mnc)
 where c.lac <> 0 and c.month <> ""
 order by mcc, mnc, lac, month, cipher; 

--

-- count calls, SMSes and location updates

drop view if exists lac_session_type_count;
create view lac_session_type_count as
 select mcc, mnc, lac, month,
	sum(call_count) as call_tot,
	sum(sms_count) as sms_tot,
	sum(loc_count) as loc_tot
 from sec_params
 group by mcc,mnc,lac,month;

-- "attack_component" population

delete from attack_component_x4;
insert into attack_component_x4
 select s.mcc, s.mnc, s.lac, s.month, s.cipher,

	s.call_count / t.call_tot as call_perc,

	s.sms_count  / t.sms_tot  as sms_perc,

	s.loc_count  / t.loc_tot  as loc_perc,

	((IFNULL(
                
 CASE WHEN call_nulls >  5 THEN 0 ELSE 1 - call_nulls /  5 END,0) + IFNULL(
                
 CASE WHEN sms_nulls  > 10 THEN 0 ELSE 1 - sms_nulls  / 10 END
        ,0)) / 2)
        

        as realtime_crack,

	((IFNULL(
                
 CASE WHEN call_pred > 10 THEN 0 ELSE 1 - call_pred / 10 END,0) + IFNULL(
                
 CASE WHEN sms_pred  > 15 THEN 0 ELSE 1 - sms_pred  / 15 END
        ,0)) / 2) as offline_crack,

	

 pag_auth_mt as key_reuse_mt,

	((IFNULL(call_auth_mo,0) + IFNULL( sms_auth_mo,0)) / 2) as key_reuse_mo,

	0.4 * ((IFNULL(call_tmsi,0) + IFNULL( sms_tmsi,0) + IFNULL( loc_tmsi,0)) / 3) +
        0.2 * CASE WHEN loc_imsi < 0.05 THEN 1 - loc_imsi * 20 ELSE 0 END
           as track_tmsi,

	0.5 * rand_imsi + 0.5 * home_routing
           as hlr_inf,

	0.2 * CASE WHEN ma_len   < 8    THEN       ma_len  / 8 ELSE 1 END +
	0.2 * CASE WHEN var_len  < 0.01 THEN 100 * var_len     ELSE 1 END +
	0.2 * CASE WHEN var_hsn  < 0.01 THEN 100 * var_hsn     ELSE 1 END +
	0.2 * CASE WHEN var_maio < 0.1  THEN  10 * var_maio    ELSE 1 END +
	0.2 * CASE WHEN var_ts   < 0.1  THEN  10 * var_ts      ELSE 1 END
           as freq_predict

  from sec_params as s, lac_session_type_count as t
  where s.mcc = t.mcc and s.mnc = t.mnc and
	s.lac = t.lac and s.month = t.month
  order by s.mcc,s.mnc,s.lac,s.month,s.cipher;

delete from attack_component;
insert into attack_component
 select mcc, mnc, lac, month,

        sum(CASE
               WHEN cipher=3 THEN
                  1.0 / 2 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) +
                  CASE
                     WHEN ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) = 1.0 THEN
                        realtime_crack / 2
                     ELSE
                        realtime_crack / 4 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)
                  END
               WHEN cipher=2 THEN
                  0.2 / 2
               WHEN cipher=1 THEN
                  0.5 / 2 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) +
                  CASE
                     WHEN ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) = 1.0 THEN
                        realtime_crack / 2
                     ELSE
                        realtime_crack / 4 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)
                  END
               ELSE
                  0
            END) as realtime_crack,

        sum(CASE
               WHEN cipher=3 THEN
                  1.0 / 2 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) +
                  CASE
                     WHEN ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) = 1.0 THEN
                        offline_crack / 2
                     ELSE
                        offline_crack / 4 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)
                  END
               WHEN cipher=2 THEN
                  0.2 / 2
               WHEN cipher=1 THEN
                  0.5 / 2 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) +
                  CASE
                     WHEN ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) = 1.0 THEN
                        offline_crack / 2
                     ELSE
                        offline_crack / 4 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)
                  END
               ELSE
                  0
            END) as offline_crack,

        sum(((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)*key_reuse_mt) /
			sum(((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2))
			as key_reuse_mt,

        sum(((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)*key_reuse_mo) /
			sum(((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2))
			as key_reuse_mo,

        sum(CASE
               WHEN cipher=3 THEN
                    1 * 0.4 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)
               WHEN cipher=2 THEN
                  0.2 * 0.4 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2)
               WHEN cipher=1 THEN
                  0.5 * 0.4 * ((IFNULL(call_perc,0) + IFNULL( sms_perc,0)) / 2) + track_tmsi
               ELSE
                  0
            END) as track_tmsi,

        avg(hlr_inf) as hlr_inf,

        sum(call_perc * freq_predict) as freq_predict

 from attack_component_x4
 group by mcc, mnc, lac, month
 order by mcc, mnc, lac, month;

--

-- "risk_intercept" population
delete from risk_intercept;
insert into risk_intercept
 select mcc, mnc, lac, month,
        0.4  * realtime_crack +
        0.25 * offline_crack +
        0.20 * ((IFNULL(key_reuse_mt,0) + IFNULL( key_reuse_mo,0)) / 2) +
        0.15 * freq_predict
           as voice,
        offline_crack
           as sms
 from attack_component
 order by mcc, mnc, lac, month;

--

-- "risk_impersonation" population
delete from risk_impersonation;
insert into risk_impersonation
 select mcc, mnc, lac, month,
	((IFNULL(offline_crack,0) + IFNULL( key_reuse_mo,0)) / 2) as make_calls,
	((IFNULL(offline_crack,0) + IFNULL( key_reuse_mt,0)) / 2) as recv_calls
 from attack_component
 order by mcc, mnc, lac, month;

--

-- "risk_tracking" population
delete from risk_tracking;
insert into risk_tracking
 select mcc, mnc, lac, month,
	track_tmsi as local_track,
	hlr_inf as global_track
 from attack_component
 order by mcc, mnc, lac, month;

--

-- "risk_category" population
delete from risk_category;
insert into risk_category
 select inter.mcc, inter.mnc, inter.lac, inter.month,

        0.8 * inter.voice +
        0.2 * inter.sms
            as intercept,

        0.7 * imper.make_calls +
        0.3 * imper.recv_calls
            as impersonation,

        0.7 * track.global_track +
        0.3 * track.local_track
            as tracking
 from	risk_intercept as inter,
	risk_impersonation as imper,
	risk_tracking as track
 where inter.mcc = imper.mcc and imper.mcc = track.mcc
   and inter.mnc = imper.mnc and imper.mnc = track.mnc
   and inter.lac = imper.lac and imper.lac = track.lac
   and inter.month = imper.month and imper.month = track.month
 order by inter.mcc, inter.mnc, inter.lac, inter.month;


