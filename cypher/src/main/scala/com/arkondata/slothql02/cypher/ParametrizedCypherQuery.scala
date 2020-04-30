package com.arkondata.slothql02.cypher

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.macros.whitebox

import shapeless._


final class ParametrizedCypherQuery[Params <: HList, T] protected(val query: CypherFragment.Query.Query0[T])
                                                                 (implicit
                                                                  val gen: CypherStatement.Gen,
                                                                  val toMap: ops.record.ToMap.Aux[Params, Symbol, Any]) {
  override def hashCode(): Int = query.hashCode()
  override def equals(obj: Any): Boolean = PartialFunction.cond(obj){
    case pcq: ParametrizedCypherQuery[_, _] => this.query == pcq.query
  }

  override def toString: String = s"Parametrized[$query]"
}

object ParametrizedCypherQuery {

  /** Interface for building [[ParametrizedCypherQuery]] from function (with macro). */
  class Build {
    def apply(f: Any): ParametrizedCypherQuery[_, _] = macro ParametrizedCypherStatementMacros.buildImpl
  }

  /** Interface for applying parameters to [[ParametrizedCypherQuery]]. */
  class Apply[Params <: HList, T, Out](pcq: ParametrizedCypherQuery[Params, T], out: CypherStatement.Prepared[T] => Out) extends RecordArgs {
    final def withParamsRecord(params: Params): Out =
      out {
        pcq.query.toCypher(pcq.gen)._1.withParamsUnchecked {
          pcq.toMap(params).map{ case (k, v) => k.name -> v }
        }
      }
  }

  implicit class ParametrizedCypherQueryToPreparedOps[Params <: HList, T](pcq: ParametrizedCypherQuery[Params, T]) {
    object prepared extends Apply[Params, T, CypherStatement.Prepared[T]](pcq, identity)
  }

  object Internal {
    def create[Params <: HList, T](query: CypherFragment.Query.Query0[T])
                                  (implicit toMap: ops.record.ToMap.Aux[Params, Symbol, Any]): ParametrizedCypherQuery[Params, T] =
      new ParametrizedCypherQuery(query)
  }
}

class ParametrizedCypherStatementMacros(val c: whitebox.Context) { outer =>
  import c.universe._

  private val helper = new CaseClassMacros { lazy val c: outer.c.type = outer.c }

  def buildImpl(f: Tree): Tree =
    f match {
      case Function(params, body) =>
        val (paramTrees, recTpes) = params.map{ p =>
          val tpe = p.tpt.tpe match {
            case p if p <:< typeOf[CypherFragment.Expr.Param[_]] =>
              val List(t) = p.baseType(symbolOf[CypherFragment.Expr.Param[_]]).typeArgs
              t
            case other => c.abort(p.pos, s"`parameterized` arguments must be of type `Param[?]`, got $other")
          }
          val argTree =
            q"""
              _root_.com.arkondata.slothql02.cypher.CypherFragment.Expr.Param[$tpe](
                ${p.name.decodedName.toString},
                _root_.com.arkondata.slothql02.cypher.CypherStatement.LiftValue[$tpe]
              )
            """
          val recEntry = helper.mkFieldTpe(p.name, tpe)
          argTree -> recEntry
        }.unzip
        val recTpe = helper.mkHListTpe(recTpes)
        val List(retType) = body.tpe.baseType(symbolOf[CypherFragment.Query[_]]).typeArgs

        q"""
          _root_.com.arkondata.slothql02.cypher.ParametrizedCypherQuery.Internal.create[$recTpe, $retType](
            $f(..$paramTrees)
          ): _root_.com.arkondata.slothql02.cypher.ParametrizedCypherQuery[$recTpe, $retType]
         """
      case _ =>
        c.abort(c.enclosingPosition, "Expecting function (Expr.Param[A1], Expr.Param[A2], ...) => Query.Query0[R]")
    }

}
