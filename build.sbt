sbtPlugin := true

name := "sbt-image-inline"

organization := "com.rayrobdod"

organizationHomepage := Some(new URL("http://rayrobdod.name/"))

version := "1.0-SNAPSHOT"

resolvers += ("rayrobdod" at "https://ivy.rayrobdod.name/")
libraryDependencies ++= Seq(
	  "com.rayrobdod" %% "anti-xml" % "0.7-SNAPSHOT-20150909"
	, "org.jsoup" % "jsoup" % "1.10.2"
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-web" % "1.4.0")


ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false

// scalaTest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"
testOptions in Test += Tests.Argument("-oS", "-u", s"${crossTarget.value}/test-results-junit")
