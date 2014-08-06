FILES = SecurityMetrics.pdf

all: $(FILES)

clean:
	latexmk -c *.tex
	rm -f $(FILES) *.bbl

%.pdf: %.tex
	latexmk -pdf $<
