TTLS  := ${shell find data/submaps -name "*.xml" | sed -e 's/xml/ttl/' }
VALIDS := ${shell find . -name "*.ttl" | grep -v all.ttl | sed -e 's/ttl/txt/' }

.PRECIOUS: %.uris %.json

all: all.ttl

validation: ${VALIDS}

%.ttl: %.xml convert.groovy
	@echo "Creating RDF for $<"
	@groovy convert.groovy $< > $@

%.txt: %.ttl
	@echo "Validating $<"
	@cat $< | rapper -i turtle -t -q - . > /dev/null

clean:
	# deletes computed files, but not downloaded files (data, shapes)
	@rm -f all.ttl

all.ttl: ${TTLS}
	@cat ${TTLS} > all.ttl


