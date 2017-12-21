package pcc
package ir
package units

import forge._
import pcc.data.Effects
import pcc.ir.memories.SRAM

sealed abstract class BlackBox extends Op[Void] {
  override def effects: Effects = Effects.Simple
}
case class GEMMBox(a: SRAM[_], b: SRAM[_]) extends BlackBox
case class GEMVBox() extends BlackBox
case class CONVBox() extends BlackBox
case class SHIFTBox(validAfter: Int) extends BlackBox // Shift out registers, valid after X cycles

object BlackBox {
  @api def GEMM[T:Num](a: SRAM[T], b: SRAM[T]): Void = stage(GEMMBox(a,b))
  @api def GEMV: Void = stage(GEMVBox())
  @api def CONV: Void = stage(CONVBox())
  @api def SHIFT(validAfter: Int): Void = stage(SHIFTBox(validAfter))
}
