version := "0.1"

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

includeFilter in filter := (includeFilter in filter).value || (imagesToInline in inlineImages).value.foldLeft[sbt.FileFilter](sbt.NothingFilter){_ || _._1}

TaskKey[Unit]("verify") in WebKeys.stage := {
	import java.io._
	import com.typesafe.sbt.web.Import.Assets
	
	val res = sbt.IO.readLines(sourceDirectory.value / "expected" / "stage" / "index.svg")
	val exp = sbt.IO.readLines(WebKeys.stagingDirectory.value / "index.svg")
	assert(res.size == exp.size)
	assert(res.zip(exp).forall{x => x._1 == x._2})
}

pipelineStages := Seq(inlineImages, filter)
