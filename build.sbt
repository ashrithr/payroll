name := """payroll-mongo"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

resolvers ++= Seq(
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.play" %% "play-mailer" % "5.0.0-M1",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.11",
  "com.mohiva" %% "play-silhouette" % "4.0.0-BETA4",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0-BETA4",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0-BETA4",
  "com.iheart" %% "ficus" % "1.2.3",
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "com.github.seratch" %% "awscala" % "0.5.+",
  "com.itextpdf" % "itextpdf" % "5.5.9",
  "com.itextpdf.tool" % "xmlworker" % "5.5.9",
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars" % "bootstrap" % "4.0.0-alpha.2" exclude("org.webjars", "jquery"),
  "org.webjars" % "jquery" % "2.2.1",
  "org.webjars" % "font-awesome" % "4.5.0",
  "org.webjars" % "momentjs" % "2.12.0",
  "org.webjars" % "bootstrap-datepicker" % "1.5.0-1" exclude("org.webjars", "bootstrap"),
  "org.webjars" % "font-awesome" % "4.6.1",
  "org.webjars.bower" % "tether" % "1.1.1",
  "org.webjars.bower" % "bootstrap-validator" % "0.10.1",
  "org.webjars.bower" % "select2" % "4.0.2",
  "org.webjars.bower" % "select2-bootstrap-theme" % "0.1.0-beta.4",
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B4" exclude("org.webjars", "jquery")
    exclude("org.webjars", "bootstrap"),
  "joda-time" % "joda-time" % "2.9.2",
  // Test
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test,
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0-BETA2" % "test"
)

//scalacOptions ++= Seq(
//  "-encoding", "UTF-8",
//  "-deprecation",
//  "-feature",
//  "-unchecked",
//  "-Xfatal-warnings",
//  "-Xlint",
//  "-Ywarn-adapted-args",
//  "-Ywarn-dead-code",
//  "-Ywarn-inaccessible",
//  "-Ywarn-nullary-override",
//  "-Ywarn-value-discard",
//  "-language:reflectiveCalls"
//)
