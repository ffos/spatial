package pcc.spade.node

import pcc.core._
import pcc.lang.Box

abstract class Module[B:Box] extends Op[B] {
  //def wires: Seq[Sym[_]] = inputs
  //def names: Seq[String]

  //override def elems: Seq[(String,Sym[_])] = names.zip(wires)
}

//case class In[B:Box,A:Bits](box: B, name: String) extends Op[A]