set -x
set -e

java -jar ~/local/bin/yuicompressor-2.4.2.jar oauth_july2010.js > indexComp2.js
java -jar ~/local/bin/yuicompressor-2.4.2.jar sha1.js >> indexComp2.js
java -jar ~/local/bin/yuicompressor-2.4.2.jar index2.js >> indexComp2.js
