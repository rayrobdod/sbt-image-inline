package com.rayrobdod.sbtImageInline

import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.immutable.{Seq, Set, Map}
import scala.collection.{Seq => DSeq}
import sbt._
import com.typesafe.sbt.web.PathMapping

/**
 * A function that takes an input document and, using the information provided
 * by the other parameters, writes an altered output file.
 */
trait Transformation {
	/**
	* Take the contents of `inputFile`, transform it, then write the output to a file and return that file
	* @param inputFile the file containing the contents of the document
	* @param path the logical path relative to the website root
	* @param inlineFilters the filters indicating which files to inline to inline 
	* @param allPaths All files in the current classpath, including a file and a logical path
	* @param outDir The directory to write files to
	* @param logger a logger
	* @return the file that was written
	*/
	def apply(inputFile:File, path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], outDir:File, logger:sbt.Logger):File
}
	
/** Implementations of [[Transformation]] */
object Transformation {
	/** A transform that affects xlink:href attributes in XML documents */
	object Xlink extends Transformation {
		import com.codecommit.antixml.{Selector, XML, QName, Node, Elem}
		private[this] val uri = """http://www.w3.org/1999/xlink"""
		private[this] val relPart = "href"
		
		def apply(inputFile:File, path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], outDir:File, logger:sbt.Logger):File = {
			val inputXml = sbt.IO.reader(inputFile, UTF_8){XML.fromReader}
			val replacableElems = inputXml \\ ElemWithXlinkTargetSelector
			logger.debug(replacableElems.toString)
			
			if (replacableElems.isEmpty) {
				inputFile
			} else {
				val newElems = replacableElems.map{e => e match {
					case Elem(prefix, local, attrs, namespaces, relative) => {
						namespaces.findByUri(uri).map{ns =>
							val imgStr = attrs(QName(ns.prefix, relPart))
							val imgAbs = (new File(path).getParentFile / imgStr).toString
							logger.debug(s"$imgStr -> $imgAbs")
							
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
				logger.debug((outDir / path).toString)
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
	
	/** A transform that affects `img[src]` attributes in HTML documents */
	object Html extends Transformation {
		import org.jsoup.nodes.Node
		import org.jsoup.select.NodeVisitor
		val relPart = "src"
		
		def apply(inputFile:File, path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], outDir:File, logger:sbt.Logger):File = {
			val doc = org.jsoup.Jsoup.parse(inputFile, "UTF-8", path)
			val replaceableElems = doc select "img[src]"
			logger.debug(replaceableElems.toString)
			
			if (replaceableElems.isEmpty) {
				inputFile
			} else {
				replaceableElems.traverse(new InlineSrcVisitor(path, inlineFilters, allPaths, logger))
				val newString = doc.toString
				sbt.IO.write(outDir / path, newString)
				logger.debug((outDir / path).toString)
				outDir / path
			}
		}
		
		private[this] class InlineSrcVisitor(path:String, inlineFilters:Seq[(sbt.FileFilter, String)], allPaths:DSeq[PathMapping], logger:sbt.Logger) extends NodeVisitor {
			override def head(node:Node, depth:Int):Unit = {
				val imgStr = node.attributes.get(relPart)
				val imgAbs = (new File(path).getParentFile / imgStr).toString
				logger.debug(s"$imgStr -> $imgAbs")
				
				val updated = findFile(imgAbs, allPaths).map{imgFile =>
					inlineFilters.find{_._1.accept(imgFile)}.map{case (_, mime) =>
						node.attr(relPart, toDataUri(imgFile, mime))
						true
					}.getOrElse(false)
				}.getOrElse(false)
			}
			
			override def tail(n:Node, depth:Int):Unit = {
				// do nothing
			}
		}
	}
	
	/** A transform that affects `url(relativeurl)` text */
	object Css extends Transformation {
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