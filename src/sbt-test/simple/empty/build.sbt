version := "0.1"

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

pipelineStages := Seq(inlineImages)
