ifndef DEBUG
COMPRESS_CMD=java -jar ../tools/yuicompressor-2.4.2.jar
else
COMPRESS_CMD=cat
endif

.PHONY : compile scripts

all:

scripts:
	rm -rf ${STATIC_DIR}/scripts
	mkdir ${STATIC_DIR}/scripts
	cp scripts/jquery-1.3.2.min.js ${STATIC_DIR}/scripts/
# cp scripts/jquery.scrollTo-min.js ${STATIC_DIR}/scripts/
	cp scripts/oauth.js ${STATIC_DIR}/scripts/
# cp scripts/bitly.js ${STATIC_DIR}/scripts/
	cp scripts/sha1.js ${STATIC_DIR}/scripts/
# cp scripts/persist-all-min.js ${STATIC_DIR}/scripts/
# cp scripts/jquery.simplemodal-1.3.3.min.js ${STATIC_DIR}/scripts/
	cp scripts/compound2.js ${STATIC_DIR}/scripts/
	cp scripts/indexComp2.js ${STATIC_DIR}/scripts/
	${COMPRESS_CMD} -v scripts/dash-new4.js > ${STATIC_DIR}/scripts/dash-new4.js
	${COMPRESS_CMD} scripts/jquery.rotate.1-1-a.min.js > ${STATIC_DIR}/scripts/jquery.rotate.1-1-a.min.js
	${COMPRESS_CMD} scripts/pic.js > ${STATIC_DIR}/scripts/pic.js
#${COMPRESS_CMD} scripts/index.js > ${STATIC_DIR}/scripts/index.js
	${COMPRESS_CMD} scripts/raphael-combined.js > ${STATIC_DIR}/scripts/raphael-combined.js
	${COMPRESS_CMD} scripts/jquery.date_input.js > ${STATIC_DIR}/scripts/jquery.date_input.js

compile: all scripts
	rm -rf ${STATIC_DIR}/css
	rm -rf ${STATIC_DIR}/images
	rm	-f ${STATIC_DIR}/*.html
	mkdir -p ${STATIC_DIR}/usrspc
ifndef DEBUG	
	mkdir -p ${STATIC_DIR}/css/
	${COMPRESS_CMD} css/common.css > ${STATIC_DIR}/css/common.css
	${COMPRESS_CMD} css/index.css > ${STATIC_DIR}/css/index.css
	${COMPRESS_CMD} css/pic.css > ${STATIC_DIR}/css/pic.css
	${COMPRESS_CMD} css/api.css > ${STATIC_DIR}/css/api.css
	${COMPRESS_CMD} css/stats.css > ${STATIC_DIR}/css/stats.css
else
	cp -R css/ ${STATIC_DIR}
endif
	cp -R images/ ${STATIC_DIR}
	cp *.html ${STATIC_DIR}
	cp *.xml ${STATIC_DIR}
	cp persist.swf ${STATIC_DIR}
#cp robots.txt ${STATIC_DIR}
	cp *.ico ${STATIC_DIR}
	cp ErrorPage.html ${SERVLET_DIR}
	
clean:
	
