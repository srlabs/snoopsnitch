FILES = SecurityMetrics.pdf

all: $(FILES)

clean:
	latexmk -c *.tex
	rm -f $(FILES) *.bbl bibliography.bib

%.pdf: %.tex bibliography.bib
	latexmk -pdf $<

bibliography.bib:
	cat bibliography/* > $@
