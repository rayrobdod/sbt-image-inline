package com.rayrobdod.sbtImageInline

import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.immutable.{Seq, Set, Map}
import scala.collection.{Seq => DSeq}
import sbt._
import com.typesafe.sbt.web.PathMapping

object Transformations {
	trait Transform {
		def apply(inputFile:File, path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], outDir:File, logger:sbt.Logger):File
	}
	
	
	
	object Xlink extends Transform {
		import com.codecommit.antixml.{Selector, XML, QName, Node, Elem}
		private[this] val uri = """http://www.w3.org/1999/xlink"""
		private[this] val relPart = "href"
		
		def apply(inputFile:File, path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], outDir:File, logger:sbt.Logger):File = {
			val inputXml = sbt.IO.reader(inputFile, UTF_8){XML.fromReader}
			val replacableElems = inputXml \\ ElemWithXlinkTargetSelector
			logger.info(replacableElems.toString)
			
			if (replacableElems.isEmpty) {
				inputFile
			} else {
				val newElems = replacableElems.map{e => e match {
					case Elem(prefix, local, attrs, namespaces, relative) => {
						namespaces.findByUri(uri).map{ns =>
							val imgStr = attrs(QName(ns.prefix, relPart))
							val imgAbs = (new File(path).getParentFile / imgStr).toString
							logger.info(s"$imgStr -> $imgAbs")
							
							findFile(imgAbs, allPaths).map{imgFile =>
								inlineFilters.find{_._1.accept(imgFile)}.map{case (_, mime) =>
									val newAttrs = attrs + ((QName(ns.prefix, relPart), toDataUri(imgFile, mime)))
									Elem(prefix, local, newAttrs, namespaces, relative)
								}.getOrElse(e)
							}.getOrElse(e)
						}.getOrElse(e)
					}
					case _ => ??? // shouldn't happen
				}}
				val newXml = newElems.unselect
				val newString = newXml.toString
				sbt.IO.write(outDir / path, newString)
				logger.info((outDir / path).toString)
				outDir / path
			}
			
		}
		
		private[this] object ElemWithXlinkTargetSelector extends Selector[Node] {
			def apply(x:Node):Node = x
			def isDefinedAt(x:Node):Boolean = x match {
				case Elem(_, _, attrs, namespaces, _) => {
					namespaces.findByUri(uri).map{ns =>
						val prefix = ns.prefix
						attrs.contains(QName(ns.prefix, relPart))
					}.getOrElse(false)
				}
				case _ => false
			}
		}
	}
	
	object Html extends Transform {
		def apply(inputFile:File, path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], outDir:File, logger:sbt.Logger):File = {
			inputFile
		}
	}
	
	object Css extends Transform {
		private[this] val pattern = java.util.regex.Pattern.compile("""url\("([^"]*)"\)""")
		
		def apply(inputFile:File, path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], outDir:File, logger:sbt.Logger):File = {
			val inputStr = sbt.IO.readLines(inputFile, UTF_8).mkString("\n")
			val inputMatcher = pattern.matcher(inputStr)
			
			val outputBuf = new java.lang.StringBuffer()
			while (inputMatcher.find()) {
				val imgStr = inputMatcher.group(1)
				val imgAbs = (new File(path).getParentFile / imgStr).toString
				
				val newUri = findFile(imgAbs, allPaths).map{imgFile =>
					inlineFilters.find{_._1.accept(imgFile)}.map{case (_, mime) =>
						toDataUri(imgFile, mime)
					}.getOrElse(imgStr)
				}.getOrElse(imgStr)
				val replacement = "url(\"" + newUri + "\")"
				
				inputMatcher.appendReplacement(outputBuf, replacement);
			}
			inputMatcher.appendTail(outputBuf);
			
			sbt.IO.writeLines(outDir / path, outputBuf.toString.split("\n"), UTF_8, false)
			outDir / path
		}
	}
	
}