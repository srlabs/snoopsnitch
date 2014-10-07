GNUPLOT = \
        set terminal pdf; \
        set output "$@"; \
        set xlabel "Delay [ms]"; \
        set xrange [0:6000]; \
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
        
all: cipher_times.pdf auth_times.pdf

cipher_times.pdf: SQL=$(CIPHER_SQL)
auth_times.pdf: SQL=$(AUTH_SQL)

%.dat: Makefile
	mysql -u root test2g_1 -e "$(SQL)" > $@

%.pdf: %.dat Makefile
	gnuplot -e '$(GNUPLOT)'

clean:
	rm -f *.pdf *.tmp *.dat

.PHONY: clean
