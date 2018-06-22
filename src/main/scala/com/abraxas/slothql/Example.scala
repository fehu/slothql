package com.abraxas.slothql

import shapeless._
import shapeless.syntax.singleton._

import com.abraxas.slothql.FragmentDefinitionHelper._
import com.abraxas.slothql.util._



sealed trait Expr[+T]
sealed trait Known[+T] {
  val t: T
  override def toString: String = s"Known($t)"
}
object LiftKnown extends Poly1 {
  implicit def impl[E <: Expr[_]]: Case.Aux[E, Known[E]] = at[E]{ x => new Known[E] {val t = x} }
}

trait Lit[A] extends Expr[A]{
  val value: A
  val m: Manifest[A]

  override def toString: String = s"Lit[$m]($value)"
}
object Lit{
  def apply[A: Manifest](a: A): Lit[A] =
    new Lit[A] {
      val value: A = a
      val m: Manifest[A] = manifest[A]
    }

  //  implicit def copy[A]: Copy.Aux[Lit[A], HNil, Lit[A]] = ???
  //  implicit def transform[A]: Transform.Children.Aux[Lit[A], _, Lit[A]] = ???
}


// // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //


/** ¡Template! */
trait Call0[A] extends Expr[A] {
  type Func <: String
  type Params <: HList

  val func: String fromNarrow Func
  val params: FromHList[Params, LiftKnown.type, List[Known[Expr[_]]]]
}

object Common {
  def undefined: Nothing = sys.error("undefined")
}

// Generated:

trait Call[A] extends Expr[A] {
  type Func <: String
  type Params <: HList

  val Func: Func
  val Params: Params

  val func: String
  val params: List[Known[Expr[_]]]

  // TODO:
  // - `toString`
  // - `equals`
  // - `hashCode`
  override def toString: String = s"Call($func, $params)"
}

object Call {
  type Aux[A, Func0 <: String, Params0 <: HList] = Call[A] { type Func = Func0; type Params = Params0 }
  /** Value-level constructor. */
  def apply[A](func: String, params: List[Known[Expr[_]]]): Call[A] = {
    @inline def func0 = func
    @inline def params0 = params
    new Call[A] {
      type Func = Nothing
      type Params = Nothing
      lazy val Func: Nothing = Common.undefined
      lazy val Params: Nothing = Common.undefined
      val func: String = func0
      val params: List[Known[Expr[_]]] = params0
    }
  }
  def unapply[A](arg: Expr[A]): Option[(String, List[Known[Expr[_]]])] =
    PartialFunction.condOpt(arg){ case c: Call[A @unchecked] => (c.func, c.params) }

  // Only if method `typed` isn't already defined in companion object
  /** Type-level constructor. */
  def typed[A]: Builder[A, Nothing, Nothing] = new Builder[A, Nothing, Nothing]

  // Only if method `typed` isn't already defined in companion object
  protected class Builder[A0, Func0 <: String, Params0 <: HList](
    func0: Option[String] = None,
    Func0: Option[Func0] = None,
    params0: Option[List[Known[Expr[_]]]] = None,
    Params0: Option[Params0] = None
  ) {

    // due to `Narrow` directive
    def withFunc[Func <: String](func: Func)(implicit w: Witness.Aux[Func]): Builder[A0, Func, Params0] =
      copy(func = Option(func), Func = Option(func))
    private[Call] def withFunc0[Func <: String](func: String): Builder[A0, Func, Params0] =
      copy(func = Option(func), Func = Option(func.asInstanceOf[Func]))

    // due to `FromHList` directive
    object withParams extends ProductArgs {
      def applyProduct[Params <: HList, MappedParams <: HList](params: Params)(
        implicit
        mapper: Cached[ops.hlist.Mapper.Aux[LiftKnown.type, Params, MappedParams]],
        toTraversable: Cached[ops.hlist.ToTraversable.Aux[MappedParams, List, Known[Expr[_]]]]
      ): Builder[A0, Func0, Params] =
        copy(params = Some(toTraversable.value(mapper.value(params))), Params = Option(params))
    }

    private[Call] def withParams0[Params <: HList](params: Params, asList: List[Known[Expr[_]]]): Builder[A0, Func0, Params] =
      copy(params = Option(asList), Params = Option(params))

    def build(
      implicit
      definedFunc: Func0 =:!= Nothing,
      definedParams: Params0 =:!= Nothing
    ): Call.Aux[A0, Func0, Params0] =
      new Call[A0] {
        type Func = Func0
        type Params = Params0
        val Func: Func0 = Func0 getOrElse Common.undefined
        val Params: Params0 = Params0 getOrElse Common.undefined
        val func: String = func0 getOrElse Common.undefined
        val params: List[Known[Expr[_]]] = params0 getOrElse Common.undefined
      }

    private def copy[A, Func <: String, Params <: HList](
      func: Option[String] = func0,
      Func: Option[Func] = Func0,
      params: Option[List[Known[Expr[_]]]] = params0,
      Params: Option[Params] = Params0
    ) = new Builder[A, Func, Params](func, Func, params, Params)
  }

  implicit def constructCall[A, Func0 <: String, Params0 <: HList, MappedParams <: HList](
    implicit
    w: Witness.Aux[Func0],                                                                 // TODO
    mapper: Cached[ops.hlist.Mapper.Aux[LiftKnown.type, Params0, MappedParams]],           // TODO
    toTraversable: Cached[ops.hlist.ToTraversable.Aux[MappedParams, List, Known[Expr[_]]]] // TODO
  ): Construct.Aux[Call[A], Func0 :: Params0 :: HNil, Call.Aux[A, Func0, Params0]] =
    new Construct[Call[A], Func0 :: Params0 :: HNil] {
      type Out = Call.Aux[A, Func0, Params0]
      def apply(args: Func0 :: Params0 :: HNil): Call.Aux[A, Func0, Params0] =
        Call.typed[A]
          .withFunc(args(0))
          .withParams.applyProduct(args(1))
          .build
    }

  implicit def constructCall2[A, Func0 <: String, Params0 <: HList]
  : Construct.Aux[Call[A], Func0 :: Params0 :: List[Known[Expr[_]]] :: HNil, Call.Aux[A, Func0, Params0]] =
    new Construct[Call[A], Func0 :: Params0 :: List[Known[Expr[_]]] :: HNil] {
      type Out = Call.Aux[A, Func0, Params0]
      def apply(args: Func0 :: Params0 :: List[Known[Expr[_]]] :: HNil): Call.Aux[A, Func0, Params0] =
        Call.typed[A]
        .withFunc0[Func0](args(0))
        .withParams0(args(1), args(2))
        .build
    }

  trait MapAndToList[HF, In, Lub] extends DepFn1[In] { type Out = List[Known[Lub]] }
  object MapAndToList {
    implicit def impl[HF, In <: HList, Lub, Mapped <: HList](
      implicit
      mapper: Cached[ops.hlist.Mapper.Aux[HF, In, Mapped]],
      toTraversable: Cached[ops.hlist.ToTraversable.Aux[Mapped, List, Known[Lub]]]
    ): MapAndToList[HF, In, Lub] =
      new MapAndToList[HF, In, Lub] {
        def apply(t: In): List[Known[Lub]] = toTraversable.value(mapper.value(t))
      }
  }

  type LiftKnownAndToList[In] = MapAndToList[LiftKnown.type, In, Expr[_]]

  implicit def copyCall[
    A, Args <: HList,
    Func0 <: String, Params0 <: HList,
    Func1 <: String, Params1 <: HList,
    Func  <: String, Params  <: HList,
    FuncI,           ParamsI
  ](
     implicit
     funcRec: ops.record.Selector.Aux[Args, Witness.`'func`.T, Func1] = null,
     funcIev: MappedOrElse.Aux[Witness.Aux, Func1, DummyImplicit, FuncI],
     funcI: FuncI,
     func0: com.abraxas.slothql.util.OrElse.Aux[Func1, Func0, Func],
     paramsRec: ops.record.Selector.Aux[Args, Witness.`'params`.T, Params1] = null,
     paramsIev: MappedOrElse.Aux[LiftKnownAndToList, Params1, DummyImplicit, ParamsI],
     paramsI: ParamsI,
     params0: com.abraxas.slothql.util.OrElse.Aux[Params1, Params0, Params],
     construct: Construct.Aux[Call[A], Func :: Params :: List[Known[Expr[_]]] :: HNil, Call.Aux[A, Func, Params]]
    ): Copy.Aux[Call.Aux[A, Func0, Params0], Args, Call.Aux[A, Func, Params]] =
    new Copy[Call.Aux[A, Func0, Params0], Args] {
      type Out = Call.Aux[A, Func, Params]
      def apply(call: Call.Aux[A, Func0, Params0], args: Args): Call.Aux[A, Func, Params] = {
        val func = Option(funcRec).map(_(args)).getOrElse(call.Func).asInstanceOf[Func]
        val (params, paramsKnown) = paramsRec match {
          case null => call.Params.asInstanceOf[Params]  -> call.params
          case sel =>
            val p = sel(args)
            val l = paramsI.asInstanceOf[LiftKnownAndToList[Params1]].apply(p)
            p.asInstanceOf[Params] -> l
        }
        construct(func :: params :: paramsKnown :: HNil)
      }
    }



  object CallTagFuncPoly extends Poly1 {
    implicit def impl[A]: Case.Aux[A, Witness.`'func`.Field[A] :: HNil] = at[A](a => labelled.field[Witness.`'func`.T][A](a) :: HNil)
  }
  object CallTagParamsPoly extends Poly1 {
    implicit def impl[A]: Case.Aux[A, Witness.`'params`.Field[A] :: HNil] = at[A](a => labelled.field[Witness.`'params`.T][A](a) :: HNil)
  }

  implicit def transformCallChildren[A, HF <: Poly1, Func0 <: String, Func1 <: String, FuncChange <: HList, Params0 <: HList, Params1 <: HList, ParamsChange <: HList, Changes <: HList](
    implicit
    noTransform: Not[poly.Case1[HF, Call.Aux[A, Func0, Params0]]],
    transformFuncOpt: poly.Case1.Aux[HF, Func0, Func1] = null,
    transformParamsOpt: poly.Case1.Aux[HF, Params0, Params1] = null,
    func: MapOrElse.Aux[CallTagFuncPoly.type, Func1, HNil, FuncChange],
    params: MapOrElse.Aux[CallTagParamsPoly.type, Params1, HNil, ParamsChange],
    concat0: ops.hlist.Prepend.Aux[FuncChange, ParamsChange, Changes]
//    copy: Copy[Call.Aux[A, Func0, Params0], Changes]
  ): Transform.Children.Aux[Call.Aux[A, Func0, Params0], HF, Call.Aux[A, Func0, Params0] /*copy.Out*/] = ???
//    new Transform.Children[Call.Aux[A, Func0, Params0], HF] {
//      type Out = copy.Out
//      def apply(call: Aux[A, Func0, Params0]): copy.Out = {
//        val func0 =
//          if (transformFuncOpt != null) func(transformFuncOpt(call.Func), HNil) // TODO
//          else HNil.asInstanceOf[FuncChange]
//        val params0 =
//          if (transformParamsOpt != null) params(transformParamsOpt(call.Params), HNil)
//          else HNil.asInstanceOf[ParamsChange]
//        val changes = concat0(func0, params0)
//        copy(call, changes)
//      }
//    }
}




/*
SlothQL>
val call = Call.typed[String].withFunc[Witness.`"foo"`.T]("foo").withParams(Lit("bar"), Lit("baz")).build
call: Call.Aux[String, foo, Lit[String] :: Lit[String] :: HNil] = com.abraxas.slothql.Call$Builder$$anon$9@59f03482

SlothQL> object F extends Poly1{
           implicit def impl[C <: Call[_]]: Case.Aux[C, C] = at[C](identity)
         }
defined object F

SlothQL> Transform[call.type, F.type](call, F)
res6: Transform[call.type, F.type]{type Out = com.abraxas.slothql.Call.<refinement>.type} = com.abraxas.slothql.Transform$$anon$6@5fbeb267



SlothQL> trait X
defined trait X

SlothQL>
object F extends Poly1{
  implicit def impl[C <: Call[_]]: Case.Aux[C, C with X] = at[C](_.asInstanceOf[C with X])
}
defined object F

SlothQL> Transform[call.type, F.type](call, F)
res9: Transform[call.type, F.type]{type Out = com.abraxas.slothql.Call.<refinement>.type with ammonite.$sess.cmd7.X} = com.abraxas.slothql.Transform$$anon$6@572098bb

 */

object TestCopy{
  import com.abraxas.slothql.util.WitnessAlternative._

  val call = Call.typed[String].withFunc[Witness.`"foo"`.T]("foo").withParams(Lit("bar"), Lit("baz")).build
  // Call.Aux[String, foo, ::[Lit[String], ::[Lit[String], HNil]]]
  // = Call(foo, List(Known(Lit[String](bar)), Known(Lit[String](baz))))
  def copy = Copy(call)
  // Copy.Builder[Call.Aux[String, foo, ::[Lit[String], ::[Lit[String], HNil]]]] = Copy$Builder@303a4fd

  val copy1 = copy(func = "abc".narrow)
  // Call[String]{type Func = String("abc");type Params = ::[Lit[String],::[Lit[String],HNil]]}
  // = Call(abc, List(Known(Lit[String](bar)), Known(Lit[String](baz))))

  val copy2 = copy(params = Lit("A") :: Lit(1) :: HNil)
  // Call[String]{type Func = String("foo");type Params = ::[Lit[String],::[Lit[Int],HNil]]}
  // = Call(foo, List(Known(Lit[String](A)), Known(Lit[Int](1))))

  val copy3 = copy()
  // Call[String]{type Func = String("foo");type Params = ::[Lit[String],::[Lit[String],HNil]]}
  // = Call(foo, List(Known(Lit[String](bar)), Known(Lit[String](baz))))

  val copy4 = copy(func = "abc".narrow, params = Lit("A") :: Lit(1) :: HNil)
  // Call[String]{type Func = String("abc");type Params = ::[Lit[String],::[Lit[Int],HNil]]}
  // = Call(abc, List(Known(Lit[String](A)), Known(Lit[Int](1))))
}


object TestTransform {
  val call = Call.typed[String].withFunc[Witness.`"foo"`.T]("foo").withParams(Lit("bar"), Lit("baz")).build

  trait X
  object F extends Poly1{
    implicit def impl[C]: Case.Aux[C, C with X] = at[C](_.asInstanceOf[C with X])
  }

}