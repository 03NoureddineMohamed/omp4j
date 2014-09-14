package org.omp4j.tree

import org.antlr.v4.runtime.atn._
import org.antlr.v4.runtime.tree._
import org.antlr.v4.runtime._

import scala.collection.JavaConverters._

import org.omp4j.Config
import org.omp4j.exception._
import org.omp4j.extractor._
import org.omp4j.grammar._

/** The nonreflextable class representation. This class can't be reflected. */
trait Nonreflectable extends ClassTrait {

	/** Placeholder for debug purposes */
	override lazy val FQN: String = s"[LOCAL] $name"

	/** Inner classes of type InnerInLocalClass */
	val innerClasses: List[OMPClass] = (new InnerClassExtractor ).visit(ctx.normalClassDeclaration.classBody).map(new InnerInLocalClass(_, THIS, parser)(conf, classMap))

	/** Find all fields syntactically (use only for allFields initialization)
	  * @return Array of OMPVariable
	  */
	def findAllFields: Array[OMPVariable] = {

		var res = Array[OMPVariable]()
		var inheritedFields = Array[OMPVariable]()
		try {
			res = ctx.normalClassDeclaration.classBody.classBodyDeclaration.asScala
				.filter(d => d.classMemberDeclaration != null)
				.map(_.classMemberDeclaration)
				.filter(f => f.fieldDeclaration != null)
				.map(_.fieldDeclaration)	// fields
				.map(f =>
					f.variableDeclaratorList.variableDeclarator.asScala
					 .map(g => new OMPVariable(g.variableDeclaratorId.Identifier.getText, f.unannType.getText, OMPVariableType.Class, ! f.fieldModifier.asScala.exists(m => m.getText == "public" || m.getText == "protected")))
				)
				.flatten
				.toArray

			try {	// try to load superclass
				val superName = ctx.normalClassDeclaration.superclass.classType.getText
				val visibleLocalClasses = Inheritor.getVisibleLocalClasses(ctx)
				val filteredClasses = visibleLocalClasses.filter(_.normalClassDeclaration != null).filter(_.normalClassDeclaration.Identifier.getText == superName)

				inheritedFields = filteredClasses.size match {
					case 0 =>	// is not local
						try {
							val cls = conf.loader.load(superName, cunit)
							findAllFieldsRecursively(cls, false)
						} catch {
							case e: Exception => throw new ParseException(s"Class '$name' ($FQN) was not found in generated JAR even though it was found by ANTLR", e)
						}

					case _ =>	// is local, taking the first one
						classMap.get(filteredClasses.head) match {
							case Some(x) => x.allFields.toArray
							case None    => throw new ParseException(s"Local class '$superName' not cached in OMPTree")
						}
				}
			} catch {
				case e: NullPointerException => ;	// no superclass
			}

		} catch {
			case e: Exception => throw new ParseException(s"Unexpected exception during finding all fields in '$FQN'", e)
		}

		res ++ inheritedFields.filter(! _.isPrivate)
	}
}
