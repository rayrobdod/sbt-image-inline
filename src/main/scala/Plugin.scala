package com.rayrobdod.sbtImageInline

import sbt._
import sbt.Keys._
import java.nio.file.Files
import scala.collection.immutable.{Seq, Set, Map}
import scala.collection.{Seq => DSeq}
import com.typesafe.sbt.web.Import.WebKeys.webTarget
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping
import com.rayrobdod.sbtImageInline.Transformations.Transform

object Plugin extends AutoPlugin {
	object autoImport {
		val inlineImages = taskKey[Pipeline.Stage]("Convert links to resources to data uris")
		val documentsToInline = settingKey[Seq[(FileFilter, Transform)]]("")
		val imagesToInline = settingKey[Seq[(FileFilter, String)]]("")
	}
	import autoImport._
	
	private[this] val baseImageFilter = ExistsFileFilter && new SimpleFileFilter({f => Files.size(f.toPath) < (1024 * 2)})
	
	override lazy val projectSettings = Seq(
		imagesToInline in inlineImages := Seq(
				(baseImageFilter && "*.png") -> "image/png",
				(baseImageFilter && "*.jpe?g") -> "image/jpeg",
				(baseImageFilter && "*.gif") -> "image/gif"
		),
		documentsToInline in inlineImages := Seq(
			GlobFilter("*.svg") -> Transformations.Xlink,
			GlobFilter("*.x?html?") -> Transformations.Html,
			GlobFilter("*.css") -> Transformations.Css
		),
		target in inlineImages := webTarget.value / "inlineImages",
		
		inlineImages := { mappings:DSeq[PathMapping] =>
			val imgFilters = (imagesToInline in inlineImages).value
			val docFilters = (documentsToInline in inlineImages).value
			val outDir = (target in inlineImages).value
			val logger = (streams in inlineImages).value.log
			
			mappings.map{ab =>
				val (inputFile:File, inputRelStr:String) = ab
				
				docFilters.find{_._1.accept(inputFile)}.map{case (_, transform) =>
					transform.apply(inputFile, inputRelStr, imgFilters, mappings, outDir, logger) -> inputRelStr
				}.getOrElse(ab)
			}
		}
	)
	
	override def requires = com.typesafe.sbt.web.SbtWeb
	override def trigger = allRequirements
}