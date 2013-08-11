set -x
set -e

java -jar ${BIN_DIR}/yuicompressor-2.4.2.jar scripts/oauth_july2010.js > scripts/indexComp2.js
java -jar ${BIN_DIR}/yuicompressor-2.4.2.jar scripts/sha1.js >> scripts/indexComp2.js
java -jar ${BIN_DIR}/yuicompressor-2.4.2.jar scripts/index2.js >> scripts/indexComp2.js
