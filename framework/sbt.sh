if [ -z "$BIN_DIR" ]; then
  BIN_DIR=../bin;
fi

# scala -cp tools/sbt_0.5.2_for_2.7.5.jar sbt.Main $*
java -jar ${BIN_DIR}/sbt-launch_0.10.jar $*
