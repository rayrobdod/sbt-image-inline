package com.rayrobdod

import sbt.File
import com.typesafe.sbt.web.PathMapping

package object sbtImageInline {
	
	/**
	 * Create a data uri using the given mime type and the contents of the specified file
	 */
	def toDataUri(f:File, mime:String):String = {
		val bytes:Array[Byte] = sbt.IO.readBytes(f)
		val encoded = java.util.Base64.getEncoder.encodeToString(bytes)
		s"data:${mime};base64,${encoded}"
	}
	
	/**
	 * Find the member of `fileToPathMapping` whose path is `path`
	 */
	def findFile(path:String, fileToPathMapping:Seq[PathMapping]):Option[File] = {
		fileToPathMapping.find{_._2 == path}.map{_._1}
	}
}
