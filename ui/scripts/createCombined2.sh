if [ -z "$COMPRESSOR" ]; then
  COMPRESSOR="java -jar ${BIN_DIR}/yuicompressor-2.4.2.jar";
fi

echo $COMPRESSOR

$COMPRESSOR scripts/jquery.scrollTo-min.js > scripts/compound2.js
$COMPRESSOR scripts/jquery.ba-longurl.min.js >> scripts/compound2.js
$COMPRESSOR scripts/bitly.js >> scripts/compound2.js
$COMPRESSOR scripts/oauth_july2010.js >> scripts/compound2.js
$COMPRESSOR scripts/sha1.js >> scripts/compound2.js
$COMPRESSOR scripts/persist-new.js >> scripts/compound2.js
$COMPRESSOR scripts/jquery.simplemodal-1.3.3.min.js >> scripts/compound2.js
