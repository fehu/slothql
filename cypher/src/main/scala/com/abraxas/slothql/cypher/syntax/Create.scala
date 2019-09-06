package com.abraxas.slothql.cypher.syntax

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

import cats.data.NonEmptyList

import com.abraxas.slothql.cypher.CypherFragment.{ Clause, Query }
import com.abraxas.slothql.cypher.syntax.Match.{ Result => MResult }

object Create {
  def apply[R](f: Graph => MResult[R]): Query[R] = macro Internal.impl[R]

  object Internal {
    def impl[R: c.WeakTypeTag](c: whitebox.Context)(f: c.Expr[Graph => MResult[R]]): c.Expr[Query[R]] = {
      val m = new Match.InternalImpl[c.type](c)
      import c.universe._
      m.mkClause(f) {
        case (_, Some(guardPos), _) => c.abort(guardPos, "No `if` guard is allowed at Create")
        case (pattern, _, _) =>
          reify {
            Clause.Create(NonEmptyList(pattern.splice, Nil))
          }
      }
    }
  }
}
