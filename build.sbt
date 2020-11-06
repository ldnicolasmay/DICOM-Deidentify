name := "DICOM-Deidenitfy"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "ch.qos.logback" % "logback-core" % "1.2.3" % Runtime,
  "com.amazonaws" % "aws-java-sdk" % "1.7.4",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.1.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.1.1",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "info.picocli" % "picocli" % "4.2.0",
  "commons-codec" % "commons-codec" % "1.3",
  "commons-logging" % "commons-logging" % "1.1.1",
  "org.apache.httpcomponents" % "httpclient" % "4.2",
  "org.apache.httpcomponents" % "httpcore" % "4.2",
  "org.scalactic" %% "scalactic" % "3.1.1",
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "org.zeroturnaround" % "zt-zip" % "1.14",
)

Compile / unmanagedJars ++= {
  val base = baseDirectory.value
  val baseDirectories = (base / "lib")
  val customJars = (baseDirectories ** "pixelmed.jar") +++
    (baseDirectories ** "logback-core-1.2.3.jar") +++
    (baseDirectories ** "logback-classic-1.2.3.jar")
  customJars.classpath
}