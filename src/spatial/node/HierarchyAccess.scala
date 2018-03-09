package spatial.node

import core._
import spatial.lang._

/** Memory accesses */
abstract class Access {
  def mem:  Sym[_]
  def addr: Seq[Idx]
  def ens:  Set[Bit]
}
case class Read(mem: Sym[_], addr: Seq[Idx], ens: Set[Bit]) extends Access
case class Write(mem: Sym[_], data: Sym[_], addr: Seq[Idx], ens: Set[Bit]) extends Access

abstract class BankedAccess {
  def mem:  Sym[_]
  def bank: Seq[Seq[Idx]]
  def ofs:  Seq[Idx]
  def ens:  Seq[Set[Bit]]
}
case class BankedRead(mem: Sym[_], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]) extends BankedAccess
case class BankedWrite(mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]) extends BankedAccess


/** Status read of a memory */
abstract class StatusRead[R:Type] extends EnPrimitive[R] {
  def mem: Sym[_]
}

abstract class Resetter[A:Type] extends EnPrimitive[Void] {
  val tA: Type[A] = Type[A]
  def mem: Sym[_]
}

object StatusRead {
  def unapply(x: Op[_]): Option[(Sym[_],Set[Bit])] = x match {
    case a: StatusRead[_] => Some((a.mem,a.ens))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Set[Bit])] = x.op.flatMap(StatusRead.unapply)
}


/** Any access of a memory */
abstract class Accessor[A:Type,R:Type] extends EnPrimitive[R] {
  val tA: Type[A] = Type[A]
  def mem:  Sym[_]
  def addr: Seq[Idx]
  def localRead: Option[Read]
  def localWrite: Option[Write]
  def localAccesses: Set[Access] = (localRead ++ localWrite).toSet
}

object Accessor {
  def unapply(x: Op[_]): Option[(Option[Write],Option[Read])] = x match {
    case a: Accessor[_,_] if a.localWrite.nonEmpty || a.localRead.nonEmpty =>
      Some((a.localWrite,a.localRead))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Option[Write],Option[Read])] = x.op.flatMap(Accessor.unapply)
}

/** Any read of a memory */
abstract class Reader[A:Type,R:Type] extends Accessor[A,R] {
  def localRead = Some(Read(mem,addr,ens))
  def localWrite: Option[Write] = None
}

object Reader {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Accessor[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Reader.unapply)
}

/** Any dequeue-like operation from a memory */
abstract class DequeuerLike[A:Type,R:Type] extends Reader[A,R]

/** An address-less dequeue operation. */
abstract class Dequeuer[A:Type,R:Type] extends DequeuerLike[A,R] {
  def addr: Seq[Idx] = Nil
}

object Dequeuer {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Dequeuer[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Dequeuer.unapply)
}


/** Any write to a memory */
abstract class Writer[A:Type] extends Accessor[A,Void] {
  override def effects: Effects = Effects.Writes(mem)

  def data: Sym[_]
  def localRead: Option[Read] = None
  def localWrite = Some(Write(mem,data,addr,ens))
}

object Writer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Accessor[_,_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Writer.unapply)
}

/** Any enqueue-like operation to a memory */
abstract class EnqueuerLike[A:Type] extends Writer[A]

/** An address-less enqueue operation. */
abstract class Enqueuer[A:Type] extends Writer[A] {
  def addr: Seq[Idx] = Nil
}

object Enqueuer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Enqueuer[_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Enqueuer.unapply)
}



/** Banked accessors */
abstract class BankedAccessor[A:Type,R:Type] extends EnPrimitive[R] {
  val tA: Type[A] = Type[A]
  def bankedRead: Option[BankedRead]
  def bankedWrite: Option[BankedWrite]
  final var ens: Set[Bit] = Set.empty

  def mem: Sym[_]
  def bank: Seq[Seq[Idx]]
  def ofs: Seq[Idx]
  var enss: Seq[Set[Bit]]

  override def mirrorEn(f: Tx, addEns: Set[Bit]): Op[R] = {
    enss = enss.map{ens => ens ++ addEns}
    this.mirror(f)
  }
  override def updateEn(f: Tx, addEns: Set[Bit]): Unit = {
    enss = enss.map{ens => ens ++ addEns}
    this.update(f)
  }
}

abstract class BankedReader[T:Type](implicit vT: Type[Vec[T]]) extends BankedAccessor[T,Vec[T]] {
  def bankedRead = Some(BankedRead(mem,bank,ofs,enss))
  def bankedWrite: Option[BankedWrite] = None
}

abstract class BankedDequeue[T:Type](implicit vT: Type[Vec[T]]) extends BankedReader[T] {
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}


abstract class BankedWriter[T:Type] extends BankedAccessor[T,Void] {
  def data: Seq[Sym[_]]
  def bankedRead: Option[BankedRead] = None
  def bankedWrite = Some(BankedWrite(mem,data,bank,ofs,enss))
}

abstract class BankedEnqueue[T:Type] extends BankedWriter[T] {
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}




