if [ -z "$COMPRESSOR" ]; then
  COMPRESSOR="java -jar $HOME/local/bin/yuicompressor-2.4.2.jar";
fi

echo $COMPRESSOR

$COMPRESSOR jquery.scrollTo-min.js > compound2.js
$COMPRESSOR jquery.ba-longurl.min.js >> compound2.js
$COMPRESSOR bitly.js >> compound2.js
$COMPRESSOR oauth_july2010.js >> compound2.js
$COMPRESSOR sha1.js >> compound2.js
$COMPRESSOR persist-new.js >> compound2.js
$COMPRESSOR jquery.simplemodal-1.3.3.min.js >> compound2.js
