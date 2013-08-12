if [ -n "$ROOT_DIR" ]; then
  SRC_DIR="scripts/";
fi
if [ -z "$BIN_DIR" ]; then
  BIN_DIR=../../bin;
fi

set -x
set -e

java -jar ${BIN_DIR}/yuicompressor-2.4.2.jar ${SRC_DIR}oauth_july2010.js > ${SRC_DIR}indexComp2.js
java -jar ${BIN_DIR}/yuicompressor-2.4.2.jar ${SRC_DIR}sha1.js >> ${SRC_DIR}indexComp2.js
java -jar ${BIN_DIR}/yuicompressor-2.4.2.jar ${SRC_DIR}index2.js >> ${SRC_DIR}indexComp2.js
