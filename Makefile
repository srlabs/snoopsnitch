COMMON_GP = \
		unset key; \
        set datafile separator "|"; \
        set terminal pdf; \
        set output "$@"; \
		set grid ytics lc rgb "grey" lw 1 lt 0; \
		set grid xtics lc rgb "grey" lw 1 lt 0;

CIPHER_SQL = \
        DROP view IF EXISTS cs; \
        CREATE VIEW cs AS \
        SELECT count(*) AS count FROM session_info \
        WHERE cipher > 0 AND rat = 0 AND cipher_delta > 0 AND cipher_delta < 10000000; \
        SELECT cipher_delta, (count(*)+0.0)/cs.count AS ratio, cipher \
		FROM session_info, cs \
        WHERE cipher > 0 AND rat = 0 AND cipher_delta > 0 AND cipher_delta < 10000000 \
        GROUP BY cipher_delta \
		HAVING ratio > 0.001 \
        ORDER BY cipher_delta \
		;

AUTH_SQL = \
        DROP view IF EXISTS au; \
        CREATE VIEW au AS \
        SELECT count(*) AS count FROM session_info \
        WHERE auth > 0 AND rat = 0 AND auth_delta > 0; \
        SELECT auth_delta, (count(*)+0.0)/au.count AS ratio, auth FROM session_info, au \
        WHERE auth > 0 AND rat = 0 AND auth_delta > 0 \
        GROUP BY auth_delta \
		HAVING ratio > 0.001 \
        ORDER BY auth_delta \
		;

IMEISV_SQL = \
        DROP VIEW IF EXISTS imeisv; \
        CREATE VIEW imeisv AS \
        SELECT count(*) as count, round(sum(CASE WHEN cmc_imeisv > 0 THEN 1.0 ELSE 0.0 END)/count(*),1) as no_imeisv \
        FROM session_info \
        WHERE rat = 0 AND cipher > 0 AND mcc > 0 AND mnc > 0 \
        GROUP BY mcc, mnc \
        HAVING count > 20; \
        DROP VIEW IF EXISTS tot; \
        CREATE VIEW tot AS select count(*) as count FROM imeisv; \
        SELECT no_imeisv, (count(*)+0.0)/tot.count as ratio \
		FROM imeisv, tot \
		GROUP by no_imeisv \
		;

DURATION_SQL = \
		DROP VIEW IF EXISTS dcount; \
		CREATE VIEW dcount AS SELECT count(*) as d from session_info; \
		SELECT duration/1000 AS seconds, (count(*)+0.0)/dcount.d AS ratio, sum(CASE WHEN t_locupd THEN 1.0 ELSE 0.0 END)/dcount.d as locupd \
		FROM session_info, dcount \
		GROUP BY duration/1000 \
		HAVING ratio > 0.001;

all: \
	cipher_times.pdf \
	auth_times.pdf \
	imeisv_rate.pdf \
	duration.pdf

cipher_times.pdf: SQL     = $(CIPHER_SQL)
cipher_times.pdf: GNUPLOT = set xlabel "Cipher delay [ms]"; \
							plot "$<" using 1:2 with lines;

auth_times.pdf:   SQL     = $(AUTH_SQL)
auth_times.pdf:   GNUPLOT =  set xlabel "Authentication delay [ms]"; \
							plot "$<" using 1:2 with lines;

imeisv_rate.pdf:  SQL     = $(IMEISV_SQL)
imeisv_rate.pdf:  GNUPLOT = set xlabel "Fraction of IMEISV requests in CIPHER MODE COMMAND"; \
							plot "$<" using 1:2 with lines;

duration.pdf:     SQL     = $(DURATION_SQL)
duration.pdf:     GNUPLOT = set xlabel "Session duration [s]"; \
							set key; \
							plot "$<" using 1:2 with lines title "Total", \
								 "$<" using 1:3 with lines title "Location updates";
%.dat: Makefile
	@echo [SQL] $@.
	@sqlite3 -header ../../Traces/new_parsed_gsmmap.sqlite "$(SQL)" > $@

%.pdf: %.dat Makefile
	@echo [GPL] $@.
	@gnuplot -e '$(COMMON_GP)$(GNUPLOT)'

clean:
	rm -f *.pdf *.tmp *.dat

.PHONY: clean
