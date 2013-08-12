if [ -z "$BIN_DIR" ]; then
  BIN_DIR=../bin;
fi

java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M -jar ${BIN_DIR}/sbt-launch_0.10.jar $*
