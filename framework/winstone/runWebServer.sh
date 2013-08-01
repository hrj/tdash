PORT="8085"
LOGGING="--accessLoggerClassName=winstone.accesslog.SimpleAccessLogger"

java  -jar ./winstone-0.9.10.jar ../../build --directoryListings=false ${LOGGING} --httpPort=${PORT}
