sbtPlugin := true

name := "sbt-image-inline"

organization := "com.rayrobdod"

organizationHomepage := Some(new URL("http://rayrobdod.name/"))

version := "0.1"

libraryDependencies += ("com.rayrobdod" %% "anti-xml" % "0.7-SNAPSHOT-20150909")

addSbtPlugin("com.typesafe.sbt" %% "sbt-web" % "1.4.0")


ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false
