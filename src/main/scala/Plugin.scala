package com.rayrobdod.sbtImageInline

import sbt._
import sbt.Keys._
import java.nio.file.Files
import scala.collection.immutable.{Seq, Set, Map}
import scala.collection.{Seq => DSeq}
import com.typesafe.sbt.web.Import.WebKeys.webTarget
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping

object Plugin extends AutoPlugin {
	object autoImport {
		val inlineImages = taskKey[Pipeline.Stage]("Convert links to resources to data uris")
		val documentsToInline = settingKey[Seq[(FileFilter, Transformation)]]("A set of filters for document files to inline into, mapped to the transformation of how to modify those files")
		val imagesToInline = settingKey[Seq[(FileFilter, String)]]("A set of filters for image files to place inline, mapped to the Content-Type of those files")
	}
	import autoImport._
	
	override lazy val projectSettings:Seq[Def.Setting[_]] = Seq(
		includeFilter in imagesToInline := ExistsFileFilter && new SimpleFileFilter({f => Files.size(f.toPath) < (1024 * 2)}),
		imagesToInline in inlineImages := Seq(
				((includeFilter in imagesToInline).value && "*.png") -> "image/png",
				((includeFilter in imagesToInline).value && "*.jpg") -> "image/jpeg",
				((includeFilter in imagesToInline).value && "*.jpeg") -> "image/jpeg",
				((includeFilter in imagesToInline).value && "*.gif") -> "image/gif"
		),
		documentsToInline in inlineImages := Seq(
			GlobFilter("*.svg") -> Transformation.Xlink,
			GlobFilter("*.htm") -> Transformation.Html,
			GlobFilter("*.html") -> Transformation.Html,
			GlobFilter("*.xhtml") -> Transformation.Html,
			GlobFilter("*.css") -> Transformation.Css
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
