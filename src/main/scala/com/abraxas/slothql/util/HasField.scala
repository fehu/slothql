package com.abraxas.slothql.util

import shapeless.tag.@@
import shapeless._

/** Evidence that `Obj` has field `K` of type `V`. */
@annotation.implicitNotFound(msg = "${Obj} doesn't have field ${K}")
trait HasField[Obj, K <: String] { type Value }
object HasField {
  @annotation.implicitNotFound(msg = "${Obj} doesn't have field ${K} of type ${V}")
  type Aux[Obj, K <: String, V] = HasField[Obj, K] { type Value = V }
  implicit def evidence[Obj, K <: String, V, Repr <: HList](
    implicit
    generic: Cached[LabelledGeneric.Aux[Obj, Repr]],
    select:  Cached[ops.record.Selector.Aux[Repr, Symbol @@ K, V]]
  ): HasField.Aux[Obj, K, V] = instance.asInstanceOf[HasField.Aux[Obj, K, V]]
  private lazy val instance = new HasField[Any, String]{}
}
