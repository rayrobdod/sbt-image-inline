version := "0.1"

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

pipelineStages := Seq(inlineImages)

TaskKey[Seq[File]]("genImage") := {
	import java.nio.file.StandardOpenOption.CREATE
	import java.nio.charset.StandardCharsets.UTF_8
	val outputFile = new File((sourceManaged in Assets).value, "arrow.png")
	val output = new Array[Byte](1024 * 4)
	
	IO.createDirectory(outputFile.getParentFile)
	java.nio.file.Files.write(outputFile.toPath, output, CREATE)
	Seq(outputFile)
}

sourceGenerators in Assets <+= TaskKey[Seq[File]]("genImage")

TaskKey[Unit]("verify") in WebKeys.stage := {
	import java.io._
	import com.typesafe.sbt.web.Import.Assets
	
	val res = sbt.IO.readBytes((resourceDirectory in Assets).value / "index.svg")
	val exp = sbt.IO.readBytes(WebKeys.stagingDirectory.value / "index.svg")
	assert(res.size == exp.size)
	assert(res.zip(exp).forall{x => x._1 == x._2})
}
