name := "sessions-finalizer"
organization := "com.clicktale.pipeline"
version := "1.0.22"
scalaVersion := "2.12.2"

libraryDependencies += "com.google.code.gson" % "gson" % "2.8.1"
libraryDependencies += "com.newmotion" %% "akka-rabbitmq" % "4.0.0"
libraryDependencies += "com.aerospike" % "aerospike-client" % "3.2.5"
libraryDependencies += "net.codingwell" % "scala-guice_2.12" % "4.1.0"
libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.1.7"
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.12" % "2.5.3"
libraryDependencies += "com.typesafe.akka" % "akka-stream_2.12" % "2.5.3"
libraryDependencies += "com.typesafe.akka" % "akka-http_2.12" % "10.0.8"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.10.2.0"
libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.3" % "test"
libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_2.12" % "3.5.0"


// Resolvers for Maven2 repositories
resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "MVN Repository" at "http://mvnrepository.com/",
  "Typesafe Repository" at "http://dl.bintray.com/typesafe/maven-releases/",
  "The New Motion Public Repo" at "http://nexus.thenewmotion.com/content/groups/public/",
  "Pipeline Nexus Repository" at "http://rg-nexus:8080/nexus/content/repositories/nexus-pipeline/"
)

mainClass in Compile := Some("com.clicktale.pipeline.sessionsfinalizer.Boot")
mainClass in assembly := Some("com.clicktale.pipeline.sessionsfinalizer.Boot")
mainClass in packageBin := Some("com.clicktale.pipeline.sessionsfinalizer.Boot")

Revolver.settings: Seq[sbt.Def.Setting[_]]
enablePlugins(JavaServerAppPackaging, RpmPlugin, RpmDeployPlugin)
rpmVendor := "ClickTale"
rpmLicense := Some("ClickTale")
packageName in Rpm := s"${name.value}"
// we specify the name for our fat jar
assemblyJarName := s"${name.value}_${version.value}.jar"
// the bash scripts classpath only needs the fat jar
scriptClasspath := Seq(assemblyJarName.value)

packageArchitecture in Rpm := "noarch"
packageSummary in Rpm := "ct sessions-finalizer service"
packageDescription in Linux := "ct sessions-finalizer service"
daemonUser in Linux := "ec2-user"
maintainer in Linux := "Pipeline Team"

defaultLinuxInstallLocation := sys.props.getOrElse("clicktalefolder", default = "/opt/clicktale")
rpmPrefix := Some(defaultLinuxInstallLocation.value)
linuxPackageSymlinks := Seq.empty
defaultLinuxLogsLocation := "/var/log"

// removes all jar mappings in universal and appends the fat jar
mappings in Universal := {
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  // removing means filtering
  val filtered = universalMappings filter {
    case (file, fileName) => !fileName.endsWith(".jar")
  }
  filtered :+ fatJar -> ("lib/" + fatJar.getName)
}

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}


/*
mappings in Universal += {
  val resources = (resourceDirectory in Compile).value
  val conf = resources / "application-prod.conf"
  conf -> "conf/application.conf"
}

mappings in Universal += {
  val resources = (resourceDirectory in Compile).value
  val logback = resources / "logback-prod.xml"
  logback -> "conf/logback.xml"
}

// Pass the location of the log + app configuration files via javaOptions
javaOptions in Universal ++= Seq(
  s"-Dconfig.file=/${defaultLinuxInstallLocation.value}/${name.value}/conf/application.conf",
  s"-Dlogback.configurationFile=${defaultLinuxInstallLocation.value}/${name.value}/conf/logback.xml",
  "-Dcom.sun.management.jmxremote",
  "-Dcom.sun.management.jmxremote.port=9010",
  "-Dcom.sun.management.jmxremote.rmi.port=9010",
  "-Dcom.sun.management.jmxremote.authenticate=false",
  "-Dcom.sun.management.jmxremote.ssl=false"
)
*/

// Override the RPM Post install scriptlet so that it would just add it in init.d without starting it
maintainerScripts in Rpm := (maintainerScripts in Rpm).value ++ Map(RpmConstants.Post -> IO.readLines(sourceDirectory.value / "rpm" / "post-rpm"))

// Publish rpm to nexus repo
credentials += Credentials("Sonatype Nexus Repository Manager", "rg-nexus", "deployment", "dep123")
// disable publishing the java doc jar
publishArtifact in(Compile, packageDoc) := false
// publishMavenStyle is used to ensure POMs are generated and pushed
publishMavenStyle := true
publishTo := {
  val nexus = "http://rg-nexus:8080/nexus/"
  Some("releases" at nexus + "content/repositories/nexus-rpm/")
  //  if (version.value.trim.endsWith("SNAPSHOT"))
  //    Some("snapshots" at nexus + "content/repositories/snapshots")
  //  else
  //    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}