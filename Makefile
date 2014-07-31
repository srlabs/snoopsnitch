FILES = SecurityMetrics.pdf

all: $(FILES)

clean:
	latexmk -c *.tex
	rm -f $(FILES)

%.pdf: %.tex
	latexmk -pdf $<
