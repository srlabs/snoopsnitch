DB ?= ~/MSD/Traces/new_parsed_gsmmap.sqlite

COMMON_GP = \
		unset key; \
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
		CREATE VIEW dcount AS SELECT count(*) as d from session_info WHERE duration > 350; \
		SELECT duration/500 AS seconds, (count(*)+0.0)/dcount.d AS ratio, sum(CASE WHEN t_locupd THEN 1.0 ELSE 0.0 END)/dcount.d as locupd \
		FROM session_info, dcount \
		WHERE duration > 350 \
		GROUP BY duration/500 \
		HAVING ratio > 0.001;

REJECT_SQL = \
		SELECT ifnull(causes.text || ' \(' || si.lu_rej_cause || '\)', 'Cause value ' || si.lu_rej_cause) as cause, \
			count(*) AS count, \
			si.lu_rej_cause \
		FROM session_info as si LEFT JOIN causes \
		ON si.lu_rej_cause = causes.cause \
		WHERE t_locupd AND not lu_acc AND lu_rej_cause > 0 \
		GROUP BY lu_rej_cause \
		ORDER BY count DESC;

LU_TYPE_SQL = \
		SELECT \
			lu_type, \
			sum(CASE WHEN not lu_acc THEN 0 ELSE 1 END) as acc_count, \
			sum(CASE WHEN not lu_acc THEN 1 ELSE 0 END) as rej_count \
		FROM session_info \
		WHERE t_locupd AND lu_type < 3 \
		GROUP BY lu_type \
		ORDER BY acc_count + rej_count DESC;

PGROUPS_SQL = \
		DROP VIEW IF EXISTS gcount; \
		CREATE VIEW gcount AS SELECT count(*) as g FROM cell_info WHERE cid > 0; \
		SELECT \
			(count(*)+0.0)/gcount.g AS perc, \
			((9.0 - cell_info.agch_blocks - 6 * cell_info.combined) * cell_info.pag_mframes) AS groups \
		FROM cell_info, gcount \
			WHERE \
			cid >0 \
		GROUP BY \
			groups \
		ORDER BY \
			groups;

PAGING_RATE_SQL = \
		DROP VIEW IF EXISTS pcount; \
		CREATE VIEW pcount AS SELECT count(*) as p FROM paging_info; \
		SELECT \
			(count(*)+0.0)/pcount.p AS perc, \
			round(pag1_rate, 1) as p1 \
		FROM paging_info, pcount \
		GROUP BY \
			p1 \
		ORDER BY \
			p1;

PAGING_TIMELINE_SQL = \
		SELECT \
			strftime('%Y-%m-%d,%H:%M:%S', timestamp) as time, pag1_rate \
		FROM paging_info \
		WHERE timestamp != (SELECT MAX(timestamp) FROM paging_info) \
		ORDER BY \
			strftime('%s',timestamp);

# FIXME: This does not account for the case where SI5ter is present.
NEIGHBORS_SQL = \
	SELECT count(*), ncount \
	FROM \
		(SELECT count(*) AS ncount FROM arfcn_list WHERE source = 'SI5' GROUP BY id) \
	GROUP BY \
		ncount;

T3212_SQL = \
	SELECT count(*), t3212 \
	FROM cell_info \
	WHERE si3 \
	GROUP BY t3212;

CRO_SQL = \
	DROP VIEW IF EXISTS c; \
	CREATE VIEW c AS SELECT count(*) as count from cell_info WHERE si4 and cro > 0; \
	SELECT ((count(*)+0.0)/c.count) as perc, cro \
	FROM cell_info, c \
	WHERE si4 and cro > 0 \
	GROUP BY cro \
	ORDER BY cro;

PAGING_GROUPS_SQL = \
	DROP VIEW IF EXISTS c; \
	CREATE VIEW c AS SELECT count(*) as count from cell_info WHERE si3; \
	SELECT (count(*)+0.0)/c.count as perc, (9 - agch_blocks - 6 * combined) * (pag_mframes + 2) AS pgroups \
	FROM cell_info, c \
	WHERE si3 \
	GROUP BY pgroups;

all: ../prebuilt/catcher_analysis.sql

OPERATOR_DOCS := \
	$(PREFIX)_paging_timeline.pdf \
	$(PREFIX)_paging_rate.pdf \

validate: \
	validate_eplus \
	validate_o2 \
	validate_telekom \
	test_3_3_1 \
	test_3_11 \
	neighbors \
	t3212 \
	cro \
	cipher_delay \
	auth_delay \
	imeisv_rate \
	paging_groups

paging_groups:
	$(MAKE) DB=~/MSD/Traces/reparsed_xgold.sqlite PREFIX=xgold xgold_paging_groups.pdf

imeisv_rate:
	$(MAKE) PREFIX=gsmmap gsmmap_imeisv_rate.pdf

cipher_delay:
	$(MAKE) PREFIX=gsmmap gsmmap_cipher_times.pdf

auth_delay:
	$(MAKE) PREFIX=gsmmap gsmmap_auth_times.pdf

cro:
	$(MAKE) DB=~/MSD/Traces/reparsed_xgold.sqlite PREFIX=xgold xgold_cro.pdf

t3212:
	$(MAKE) DB=~/MSD/Traces/reparsed_xgold.sqlite PREFIX=xgold xgold_t3212.pdf

neighbors:
	$(MAKE) PREFIX=gsmmap gsmmap_neighbors.pdf

test_3_11:
	$(MAKE) PREFIX=test_3_11 DB=$(PWD)/../doc/test-III-3_11/db.sqlite do_validate

test_3_3_1:
	$(MAKE) PREFIX=test_3_3_1 DB=$(PWD)/../doc/test-III-3_3_1/db.sqlite do_validate

validate_eplus:
	$(MAKE) PREFIX=eplus DB=$(PWD)/../doc/validate-EPlus-DD_B-20141117/db.sqlite do_validate

validate_o2:
	$(MAKE) PREFIX=o2 DB=$(PWD)/../doc/validate-O2-B_DD-20141117/db.sqlite do_validate

validate_telekom:
	$(MAKE) PREFIX=telekom DB=$(PWD)/../doc/validate-Telekom-DD_B-20141118/db.sqlite do_validate

do_validate: $(OPERATOR_DOCS)

../prebuilt/catcher_analysis.sql: analysis.sql analysis_ci.sql analysis_si.sql sql/*.sql scripts/resolve_read.pl
	./scripts/resolve_read.pl $< > $@.tmp
	mv $@.tmp $@

catcher.txt: validate.sql
	sqlite3 $(DB) < $< > $@.tmp
	mv $@.tmp $@

$(PREFIX)_cipher_times.dat: SQL     = $(CIPHER_SQL)
$(PREFIX)_cipher_times.pdf: GNUPLOT = set xlabel "Cipher delay [ms]"; \
	plot "$<" using 1:2 with lines;

$(PREFIX)_auth_times.dat:   SQL     = $(AUTH_SQL)
$(PREFIX)_auth_times.pdf:   GNUPLOT = set xlabel "Authentication delay [ms]"; \
	plot "$<" using 1:2 with lines;

$(PREFIX)_imeisv_rate.dat:  SQL     = $(IMEISV_SQL)
$(PREFIX)_imeisv_rate.pdf:  GNUPLOT = set xlabel "Fraction of IMEISV requests in CIPHER MODE COMMAND"; \
	plot "$<" using 1:2 with lines;

$(PREFIX)_duration.dat: SQL     = $(DURATION_SQL)
$(PREFIX)_duration.pdf: GNUPLOT = set xlabel "Session duration [s]"; \
	set key; \
	plot "$<" using 1:2 with lines title "Total", \
	"$<" using 1:3 with lines title "Location updates";

$(PREFIX)_lu_reject.dat: SQL	  = $(REJECT_SQL)
$(PREFIX)_lu_reject.pdf: GNUPLOT = set xlabel "Reject causes"; \
	unset grid; \
	set format y "%1.0f"; \
	set style fill solid border -1; \
	set xtics rotate by -45; \
	set style data histograms; \
	plot "$<" using 2:xtic(1) title col, \
	"" using ($$0-1.0):($$2+2.5):2 with labels;

$(PREFIX)_lu_type.dat: SQL	  = $(LU_TYPE_SQL)
$(PREFIX)_lu_type.pdf: GNUPLOT = set xlabel "Location update types"; \
	set key; \
	unset grid; \
	set style data histograms; \
	set format y "%1.0f"; \
	set style fill solid border -1; \
	set xtics rotate by -45; \
	plot "$<" using 3:xtic(1) title "Rejected", \
	"" using 2:xtic(1) title "Accepted"\

$(PREFIX)_pgroups.dat: SQL     = $(PGROUPS_SQL)
$(PREFIX)_pgroups.pdf: GNUPLOT = set xlabel "Number of paging groups"; \
	set style fill solid; \
	set boxwidth 0.5; \
	plot "$<" using 2:1 with boxes;

$(PREFIX)_paging_rate.dat: SQL     = $(PAGING_RATE_SQL)
$(PREFIX)_paging_rate.pdf: GNUPLOT = set xlabel "Number of type-1 paging requests per second"; \
	plot "$<" using 2:1 with lines;

$(PREFIX)_paging_timeline.dat: SQL     = $(PAGING_TIMELINE_SQL)
$(PREFIX)_paging_timeline.pdf: GNUPLOT = set xlabel "Paging request frequency over time"; \
	set xdata time; \
	set timefmt "%Y-%m-%d,%H:%M:%S"; \
	plot "$<" using 1:2 index 0 with lines;

$(PREFIX)_neighbors.dat: SQL     = $(NEIGHBORS_SQL)
$(PREFIX)_neighbors.pdf: GNUPLOT = set xlabel "Number of neighbor cells announced in SI5"; \
	set key; \
	plot "$<" using 2:1 with lines title "No. of Neighbors"

$(PREFIX)_t3212.dat: SQL     = $(T3212_SQL)
$(PREFIX)_t3212.pdf: GNUPLOT = set xlabel "Cell reselect timeout"; \
	set key; \
	plot "$<" using 2:1 with lines title "Timeout in deci-hours"

$(PREFIX)_cro.dat: SQL     = $(CRO_SQL)
$(PREFIX)_cro.pdf: GNUPLOT = set xlabel "Cell reselect offset"; \
	unset key; \
	unset grid; \
	set format x "%1.0f"; \
	set style data histograms; \
	set style fill solid border -1; \
	plot "$<" using 1:xtic(2) title "CRO"

$(PREFIX)_paging_groups.dat: SQL     = $(PAGING_GROUPS_SQL)
$(PREFIX)_paging_groups.pdf: GNUPLOT = set xlabel "No. of paging groups"; \
	unset key; \
	unset grid; \
	set format x "%1.0f"; \
	set style data histograms; \
	set style fill solid border -1; \
	plot "$<" using 1:xtic(2) title "paging groups"

%.dat: Makefile
	@echo [SQL] $@.
	@test "x$(SQL)" != "x"
	@sqlite3 -header $(DB) -separator '	' "$(SQL)" > $@.tmp
	@mv $@.tmp $@

%.pdf: %.dat Makefile
	@echo [GPL] $@.
	@gnuplot -e '$(COMMON_GP)$(GNUPLOT)'
	@rm -f $<

clean:
	rm -f *.pdf *.tmp *.dat catcher.txt

.PHONY: clean catcher.txt
