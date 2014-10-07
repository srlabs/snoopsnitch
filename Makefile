TIMING_GP = \
        set terminal pdf; \
        set output "$@"; \
        set xlabel "Delay [ms]"; \
        set xrange [0:6000]; \
        set key autotitle columnhead; \
        plot "$<" using 1:2 with lines;

RATE_GP = \
        set terminal pdf; \
        set output "$@"; \
        set xlabel "Rate"; \
        set key autotitle columnhead; \
        plot "$<" using 1:2 with lines;

CIPHER_SQL = \
        DROP view IF EXISTS cs; \
        CREATE VIEW cs AS \
        SELECT count(*) AS count FROM session_info \
        WHERE cipher > 0 AND rat = 0 AND cipher_delta > 0 AND cipher_delta < 10000000; \
        SELECT cipher_delta, count(*)/cs.count AS p, cipher FROM session_info, cs \
        WHERE cipher > 0 AND rat = 0 AND cipher_delta > 0 AND cipher_delta < 10000000 \
        GROUP BY cipher_delta \
        ORDER BY cipher_delta;

AUTH_SQL = \
        DROP view IF EXISTS au; \
        CREATE VIEW au AS \
        SELECT count(*) AS count FROM session_info \
        WHERE auth > 0 AND rat = 0 AND auth_delta > 0; \
        SELECT auth_delta, count(*)/au.count AS p, auth FROM session_info, au \
        WHERE auth > 0 AND rat = 0 AND auth_delta > 0 \
        GROUP BY auth_delta \
        ORDER BY auth_delta;

IMEISV_SQL = \
        DROP VIEW IF EXISTS imeisv; \
        CREATE VIEW imeisv AS \
        SELECT count(*) as count, round(sum(CASE WHEN cmc_imeisv > 0 THEN 1 ELSE 0 END)/count(*),1) as no_imeisv \
        FROM session_info \
        WHERE rat = 0 AND cipher > 0 AND mcc > 0 AND mnc > 0 \
        GROUP BY mcc, mnc, lac \
        HAVING count > 20; \
        DROP VIEW IF EXISTS tot; \
        CREATE VIEW tot AS select count(*) as count FROM imeisv; \
        SELECT no_imeisv as perc, count(*)/tot.count as frac FROM imeisv, tot GROUP by no_imeisv ORDER BY perc;

all: cipher_times.pdf auth_times.pdf imeisv_rate.pdf

cipher_times.pdf: SQL = $(CIPHER_SQL)
cipher_times.pdf: GNUPLOT=$(TIMING_GP)

auth_times.pdf:   SQL = $(AUTH_SQL)
auth_times.pdf:   GNUPLOT=$(TIMING_GP)

imeisv_rate.pdf:  SQL=$(IMEISV_SQL)
imeisv_rate.pdf:  GNUPLOT=$(RATE_GP)

%.dat: Makefile
	mysql -u root test2g_1 -e "$(SQL)" > $@

%.pdf: %.dat Makefile
	@gnuplot -e '$(GNUPLOT)'

clean:
	rm -f *.pdf *.tmp *.dat

.PHONY: clean
