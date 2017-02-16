package com.rayrobdod.sbtImageInline

import org.scalatest.FunSpec
import sbt.File

final class PackageTest extends FunSpec {
	
	describe ("findFile") {
		it ("does find a _1 with the given _2 if there is one") {
			val dut = Seq(
				new File("abc/def") -> "abc",
				new File("ghi/jkl") -> "jkl"
			)
			assertResult(Option(new File("ghi/jkl"))){findFile("jkl", dut)}
		}
		it ("None if there isn't one") {
			val dut = Seq(
				new File("abc/def") -> "abc",
				new File("ghi/jkl") -> "jkl"
			)
			assertResult(None){findFile("zzz", dut)}
		}
		it ("None if there isn't one (Nil)") {
			assertResult(None){findFile("aaa", Nil)}
		}
	}
}
