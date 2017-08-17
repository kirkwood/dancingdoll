// scalaVersion := "2.10.6"
scalaVersion := "2.12.2"

PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value,
  scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
)

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)

// addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.8")
// libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.0-pre5"

// val scalazVersion = "7.2.11"

// PB.targets in Compile := Seq(
//   scalapb.gen() -> (sourceManaged in Compile).value
// )

// libraryDependencies += "com.mdialog" %% "scala-zeromq" % "1.2.0"
// resolvers += "Sonatype (releases)" at "https://oss.sonatype.org/content/repositories/releases/"
// libraryDependencies += "org.zeromq" %% "zeromq-scala-binding" % "0.0.6"

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "3.0.3"
)

val http4sVersion = "0.15.16a"
libraryDependencies ++= Seq(
  "org.zeromq" % "jeromq" % "0.4.2",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  // "io.circe" %% "circe-core" % "0.8.0",
  "io.circe" %% "circe-generic" % "0.8.0",
  "io.circe" %% "circe-parser" % "0.8.0",
  "io.circe" %% "circe-literal" % "0.8.0"

  // "io.netty" %% "netty-all" % "4.1.10.Final"
)

// resolvers += "RichRelevance Bintray" at "http://dl.bintray.com/rr/releases"
// libraryDependencies += "org.scalaz.netty" %% "scalaz-netty" % "0.4.3"
