package org.omp4j.extractor

import org.antlr.v4.runtime.atn._
import org.antlr.v4.runtime.tree._
import org.antlr.v4.runtime._

import org.omp4j.preprocessor.grammar._

/** Extracts first-level classes from ANTLR4 ParseTree */
class ClassExtractor extends Java8BaseVisitor[List[Java8Parser.ClassDeclarationContext]] {

	/** Java8Parser.ClassDeclarationContext typedef */
	type CDC = Java8Parser.ClassDeclarationContext

	/** Do not continue, so no nested classes included */
	override def visitClassDeclaration(classCtx: CDC) = List[CDC](classCtx)
	override def defaultResult() = List[CDC]()
	override def aggregateResult(a: List[CDC], b: List[CDC]) = a ::: b
}