package com.abraxas.slothql.cypher

import cats.{ Bifunctor, Contravariant, Functor }
import cats.data.{ Ior, NonEmptyList }
import cats.syntax.bifunctor._
import cats.syntax.functor._

import scala.language.{ higherKinds, implicitConversions }

trait CypherFragment[-A] {
  // TODO: (String, Params)
  def toCypher(f: A): String
}

/** Cypher Query Language Fragments.
 *
 * From ''Cypher: An Evolving Query Language for Property Graphs''
 * [[http://homepages.inf.ed.ac.uk/pguaglia/papers/sigmod18.pdf]]:
 *
 * Syntax of expressions, queries, clauses and patterns of core Cypher.
 *
 *
 * `A` is a countable set of names.
 * `V` is the set of values, inductively defined as follows:
 *  - Identifiers (i.e., elements of `N` and `R`) are values;
 *  - Base types (elements of `Z` and `Σ∗`) are values;
 *  - `true`, `false` and `null `are values;
 *  - `list()` is a value (empty list), and if `v1, . . . ,vm` are values, for `m > 0`,
 *    then `list(v1, . . . ,vm)` is a value.
 *  - `map()` is a value (empty map), and if `k1, . . . , km` are distinct property keys and `v1, . . . ,vm` are values,
 *    for `m > 0`, then `map((k1,v1), . . . ,(km,vm))` is a value.
 *  - If `n` is a node identifier, then `path(n)` is a value. If `n1, . . . ,nm` are node ids and
 *    `r1, . . . ,rm−1` are relationship ids, for `m > 1`, then `path(n1,r1,n2, . . . ,nm−1,rm−1,nm)` is a value.
 * `F` are base functions.
 *
 * Expressions {{{
 * expr ::= v | a | f (expr_list)      where v ∈ V, a ∈ A, f ∈ F                                              values/variables
 *        | expr.k | {} | { prop_list }                                                                       maps
 *        | [ ] | [ expr_list ] | expr IN expr | expr[expr] | expr[expr..] | expr[..expr] | expr[expr..expr]  lists
 *        | expr STARTS WITH expr | expr ENDS WITH expr | expr CONTAINS expr                                  strings
 *        | expr OR expr | expr AND expr | expr XOR expr | NOT expr | expr IS NULL | expr IS NOT NULL         logic
 *        | expr < expr | expr <= expr | expr >= expr | expr > expr | expr = expr | expr <> expr              inequalities
 * }}}
 *
 * expr_list ::= expr | expr, expr_list                                    expression lists
 *
 * Queries {{{
 * query ::= query◦ | query UNION query | query UNION ALL query            unions
 * query◦ ::= RETURN ret | clause query◦                                   sequences of clauses
 * ret ::= ∗ | expr [AS a] | ret , expr [AS a]                             return lists
 * }}}
 * Clauses {{{
 * clause ::= [OPTIONAL] MATCH pattern_tuple [WHERE expr]                  matching clauses
 *          | WITH ret [WHERE expr] | UNWIND expr AS a     where a ∈ A     relational clauses
 * pattern_tuple ::= pattern | pattern, pattern_tuple                      tuples of patterns
 * }}}
 *
 * Pattern {{{
 * pattern ::= pattern◦ | a = pattern◦
 * pattern◦ ::= node_pattern | node_pattern rel_pattern pattern◦
 * node_pattern ::= (a? label_list? map? )
 * rel_pattern ::= -[a? type_list? len? map? ]->
 *               | <-[a? type_list? len? map? ]-
 *               | -[a? type_list? len? map? ]-
 * label_list ::= :ℓ | :ℓ label_list
 * map ::= { prop_list }
 * prop_list ::= k:expr | k:expr, prop_list
 * type_list ::= :t | type_list | t
 * len ::= ∗ | ∗d | ∗d1.. | ∗..d2 | ∗d1..d2       where d,d1,d2 ∈ N
 *}}}
 */
object CypherFragment {
  @inline def apply[A](f: A)(implicit fragment: CypherFragment[A]): String = fragment.toCypher(f)
  def define[A](toCypher0: A => String): CypherFragment[A] =
    new CypherFragment[A]{
      def toCypher(f: A): String = toCypher0(f)
    }

  implicit lazy val CypherFragmentIsContravariant: Contravariant[CypherFragment] =
    new Contravariant[CypherFragment] {
      def contramap[A, B](fa: CypherFragment[A])(f: B => A): CypherFragment[B] =
        define(fa.toCypher _ compose f)
    }

  sealed trait Known[+A] {
    val fragment: A
    def toCypher: String
  }
  object Known {
    implicit def apply[A](f: A)(implicit cypherFragment: CypherFragment[A]): Known[A] =
      new Known[A] {
        val fragment: A = f
        lazy val toCypher: String = cypherFragment.toCypher(f)
      }
    implicit def functor[F[_]: Functor, A: CypherFragment](fa: F[A]): F[Known[A]] = fa.map(apply[A])
    implicit def bifunctor[F[_, _]: Bifunctor, A: CypherFragment, B: CypherFragment](fab: F[A, B]): F[Known[A], Known[B]] =
      fab.bimap(apply[A], apply[B])
  }
  // Explicit analog of implicit `Known.apply`
  implicit class KnownOps[A: CypherFragment](f: A) {
    @inline def known: Known[A] = Known(f)
  }


  sealed trait Expr[+T]
  object Expr {
    // // // Values and Variables // // //
    case class Lit[A](value: A)(implicit val m: Manifest[A]) extends Expr[A]
    case class Var[A](name: String)(implicit val m: Manifest[A]) extends Expr[A]
    case class Call[A](func: String, params: NonEmptyList[Known[Expr[_]]]) extends Expr[A]

    object Lit {
      implicit lazy val literalStringFragment: CypherFragment[Lit[String]] = define {
        case Lit(str) => "\"" + str.replaceAll("\"", "\\\"") + "\""
      }
      /** Warning: it does nothing to check whether the number can be represented in cypher (~Long~ equivalent). */
      implicit def literalNumericFragment[N: Numeric]: CypherFragment[Lit[N]] =
        literalToString.asInstanceOf[CypherFragment[Lit[N]]]
      implicit lazy val literalBooleanFragment: CypherFragment[Lit[Boolean]] =
        literalToString.asInstanceOf[CypherFragment[Lit[Boolean]]]

      private lazy val literalToString = define[Lit[_]](_.value.toString)
    }
    object Var {
      implicit def fragment[A]: CypherFragment[Var[A]] = instance.asInstanceOf[CypherFragment[Var[A]]]
      private lazy val instance = define[Var[_]](_.name)
    }
    object Call {
      implicit def fragment[A]: CypherFragment[Call[A]] = instance.asInstanceOf[CypherFragment[Call[A]]]
      private lazy val instance = define[Call[_]] {
        case Call(func, params) => s"${escapeName(func)}(${params.toList.map(_.toCypher).mkString(", ")})"
      }
    }

    // // // Maps // // //
    case class Map(get: Predef.Map[String, Known[Expr[_]]]) extends Expr[Predef.Map[String, Expr[_]]]
    type MapExpr = Expr[Predef.Map[String, Known[Expr[_]]]]
    case class Key[A](map: Known[MapExpr], key: String)(implicit val m: Manifest[A]) extends Expr[A]

    object Map {
      implicit lazy val fragment: CypherFragment[Map] = define {
        _.get.map{ case (k, v) => s"${escapeName(k)}: ${v.toCypher}" }.mkString("{ ", ", ", " }")
      }
    }
    object Key {
      implicit def fragment[A]: CypherFragment[Key[A]] = instance.asInstanceOf[CypherFragment[Key[A]]]
      implicit lazy val instance: CypherFragment[Key[_]] = define {
        case Key(m, k) => s"${m.toCypher}.${escapeName(k)}"
      }
    }

    // // // Lists // // //
    case class List(get: scala.List[Known[Expr[_]]]) extends Expr[scala.List[Expr[_]]]
    type ListExpr = Expr[scala.List[_]] // TODO
    type IndexExpr = Expr[Long]         // TODO
    case class In(elem: Known[Expr[_]], list: Known[ListExpr]) extends Expr[Boolean]
    case class AtIndex[A](list: Known[ListExpr], index: Known[IndexExpr])(implicit val m: Manifest[A]) extends Expr[A]
    case class AtRange[A](list: Known[ListExpr], limits: Ior[Known[IndexExpr], Known[IndexExpr]])(implicit val m: Manifest[A]) extends Expr[scala.List[A]]

    object List {
      implicit lazy val fragment: CypherFragment[List] = define {
        _.get.map(_.toCypher).mkString("[ ", ", ", " ]")
      }
    }
    object In {
      implicit lazy val fragment: CypherFragment[In] = define {
        case In(elem, list) => s"${elem.toCypher} IN ${list.toCypher}"
      }
    }
    private def atIndex(list: Known[ListExpr], index: String) = s"${list.toCypher}[$index]"
    object AtIndex {
      implicit def fragment[A]: CypherFragment[AtIndex[A]] = instance.asInstanceOf[CypherFragment[AtIndex[A]]]
      private lazy val instance = define[AtIndex[_]] {
        case AtIndex(list, index) => atIndex(list, index.toCypher)
      }
    }
    object AtRange {
      implicit def fragment[A]: CypherFragment[AtRange[A]] = instance.asInstanceOf[CypherFragment[AtRange[A]]]
      private lazy val instance = define[AtRange[_]] {
        case AtRange(list, Ior.Left(min))      => atIndex(list, s"${min.toCypher}..")
        case AtRange(list, Ior.Right(max))     => atIndex(list, s"..${max.toCypher}")
        case AtRange(list, Ior.Both(min, max)) => atIndex(list, s"${min.toCypher}..${max.toCypher}")
      }
    }

    // // // Strings // // //
    case class StringExpr(left: Known[Expr[String]], right: Known[Expr[String]], op: StringExpr.Op) extends Expr[Boolean]
    object StringExpr {
      sealed trait Op
      case object StartsWith extends Op
      case object EndsWith   extends Op
      case object Contains   extends Op

      implicit lazy val fragment: CypherFragment[StringExpr] = define {
        case StringExpr(left, right, op) =>
          val opStr = op match {
            case StartsWith => "STARTS WITH"
            case EndsWith   => "ENDS WITH"
            case Contains   => "CONTAINS"
          }
          s"${left.toCypher} $opStr ${right.toCypher}"
      }
    }

    // // // Logic // // //
    sealed trait LogicExpr extends Expr[Boolean]
    case class LogicBinaryExpr(left: Known[Expr[Boolean]], right: Known[Expr[Boolean]], op: LogicExpr.BinaryOp) extends LogicExpr
    case class LogicUnaryExpr(expr: Known[Expr[Boolean]], op: LogicExpr.UnaryOp) extends LogicExpr
    object LogicExpr {
      sealed trait BinaryOp
      case object Or  extends BinaryOp
      case object And extends BinaryOp
      case object Xor extends BinaryOp

      sealed trait UnaryOp
      case object Not     extends UnaryOp
      case object IsNull  extends UnaryOp
      case object NotNull extends UnaryOp

      implicit lazy val fragment: CypherFragment[LogicExpr] = define {
        case LogicBinaryExpr(left, right, op) =>
          val opStr = op match {
            case Or  => "OR"
            case And => "AND"
            case Xor => "XOR"
          }
          s"${left.toCypher} $opStr ${right.toCypher}"
        case LogicUnaryExpr(expr, Not) =>
          s"NOT ${expr.toCypher}"
        case LogicUnaryExpr(expr, op) =>
          val opStr = (op: @unchecked) match {
            case IsNull  => "IS NULL"
            case NotNull => "IS NOT NULL"
          }
          s"${expr.toCypher} $opStr"
      }
    }

    // // // Compare // // //
    case class CompareExpr(left: Known[Expr[Any]], right: Known[Expr[Any]], op: CompareExpr.Op) extends Expr[Boolean]
    object CompareExpr {
      sealed trait Op
      case object Lt  extends Op
      case object Lte extends Op
      case object Gte extends Op
      case object Gt  extends Op
      case object Eq  extends Op
      case object Neq extends Op

      implicit lazy val fragment: CypherFragment[CompareExpr] = define {
        case CompareExpr(left, right, op) =>
          val opStr = op match {
            case Lt  => "<"
            case Lte => "<="
            case Gte => ">="
            case Gt  => ">"
            case Eq  => "="
            case Neq => "<>"
          }
          s"${left.toCypher} $opStr ${right.toCypher}"
      }
    }
  }

  private def escapeName(name: String) = "`" + name.replaceAll("`", "``") + "`"
}
