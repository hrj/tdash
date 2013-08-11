ROOT_DIR:=$(shell pwd)

export BIN_DIR=${ROOT_DIR}/bin
export OUT_DIR=${ROOT_DIR}/build
export STATIC_DIR=${OUT_DIR}/static
export SERVLET_DIR=${OUT_DIR}/servlet
export DOMAIN="local"

.PHONY : compile

all: compile

compile:
	make -C ui compile
	make -C framework compile

dist: all
	if [[ -n "${DEBUG}" ]] ; then echo "No DEBUG allowed for dist target"; exit 1 ; fi
	if [[ "${DOMAIN}" != "live" ]] ; then echo "DOMAIN should be live"; exit 1 ; fi
	./tools/emitVersion.sh > ${STATIC_DIR}/version.html
	
run: all build/lib/scala-library.jar
	./run.sh
	
clean:
	rm -rf ${SERVLET_DIR}/*
	make -C framework clean
	make -C ui clean
