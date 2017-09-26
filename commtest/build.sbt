scalaVersion := "2.12.3"

PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value,
  scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
)

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)

resolvers += "Sonatype (releases)" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "3.0.3"
)

val http4sVersion = "0.15.16a"
libraryDependencies ++= Seq(
  // Http4s
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  // "io.circe" %% "circe-core" % "0.8.0",
  "io.circe" %% "circe-generic" % "0.8.0",
  "io.circe" %% "circe-parser" % "0.8.0",
  "io.circe" %% "circe-literal" % "0.8.0"
  // "io.netty" % "netty-all" % "4.1.14.Final"
  // "io.netty" %% "netty-all" % "3.7.0.Final"
)

libraryDependencies ++= Seq(
  "com.madgag.spongycastle" % "core" % "1.56.0.0",
  "com.madgag.spongycastle" % "prov" % "1.56.0.0"
  // "org.spongycastle" % "core" % "1.58.0.0",
  // "org.spongycastle" % "crypto" % "1.58.0.0",
  // "org.spongycastle" % "util" % "1.58.0.0"
)

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
