FILES = SecurityMetrics.pdf

all: $(FILES)

clean:
	latexmk -c *.tex
	rm -f $(FILES) *.bbl bibliography.bib

%.pdf: %.tex bibliography.bib
	latexmk -pdf $<

%.pdf: %.dot
	dot -Tpdf -o $@ $<

bibliography.bib:
	echo '%%% DO NOT EDIT  --  GENERATED AUTOMATICALLY %%%'  > $@
	echo '%%% REFER TO bibliography/ DIRECTORY INSTEAD %%%' >> $@
	cat bibliography/*                                      >> $@
