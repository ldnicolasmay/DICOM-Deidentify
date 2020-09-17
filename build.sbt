name := "DICOM-Deidenitfy"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "info.picocli" % "picocli" % "4.2.0",
  "org.zeroturnaround" % "zt-zip" % "1.14",
  "org.scalactic" %% "scalactic" % "3.1.1",
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "ch.qos.logback" % "logback-core" % "1.2.3" % Runtime,
  "org.scalameta" %% "scalameta" % "4.3.22"
)

Compile / unmanagedJars ++= {
  val base = baseDirectory.value
  val baseDirectories = (base / "lib")
  val customJars = (baseDirectories ** "pixelmed.jar") +++
    (baseDirectories ** "logback-core-1.2.3.jar") +++
    (baseDirectories ** "logback-classic-1.2.3.jar")
  customJars.classpath
}