name := "Lift Demo"

version := "1.0"

scalaVersion := "2.11.8"

organization := "com.dotography"

enablePlugins(JettyPlugin)

libraryDependencies ++= Seq(
  "net.liftweb"           %% "lift-webkit"            % "2.6.3"   % "compile",
  "net.liftweb"           %% "lift-mapper"            % "2.6.3",
  "com.h2database"        % "h2"                      % "1.4.191",
  "org.specs2"            %% "specs2-core"            % "3.8.5"   % "test",
  "com.typesafe.akka"     %% "akka-actor"             % "2.4.4",
  "com.typesafe.akka"     %% "akka-http-experimental" % "2.4.4"
)

scalacOptions in Test ++= Seq("-Yrangepos")
