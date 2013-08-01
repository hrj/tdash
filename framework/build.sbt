name := "tdash"

version := "0.2"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-http" % "0.7.8",
  "net.databinder" %% "dispatch-oauth" % "0.7.8",
  "net.databinder" %% "dispatch-json" % "0.7.8",
  "net.databinder" %% "dispatch-twitter" % "0.7.8",
  "commons-fileupload" % "commons-fileupload" % "1.3",
  "org.apache.httpcomponents" % "httpclient" % "4.2.5"
)
