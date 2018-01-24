package pcc.node

import forge._
import pcc.core._
import pcc.data._
import pcc.lang._

abstract class StructAlloc[S:Struct] extends Primitive[S] {
  def elems: Seq[(String,Sym[_])]

  override def inputs = syms(elems.map(_._2))
  override def reads  = Nil
  override def aliases = Nil
  override def contains = syms(elems.map(_._2))
  override val isStateless: Boolean = true
}

@op case class SimpleStruct[S:Struct](elems: Seq[(String,Sym[_])]) extends StructAlloc[S]

@op case class FieldApply[S:Struct,A:Sym](struct: S, name: String) extends Primitive[A] {
  override val isStateless: Boolean = true
}

@op case class FieldUpdate[S:Struct,A:Sym](struct: S, name: String, data: A) extends Primitive[Void] {
  override def effects: Effects = Effects.Writes(struct.asSym)
  override val debugOnly: Boolean = true
}