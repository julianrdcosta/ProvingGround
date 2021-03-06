package provingground.induction

import provingground._, HoTT._
import shapeless._
import scala.language.existentials

/**
  * term level inductive structure for runtime contexts
  */
trait ExstInducStruc {
  def subs(x: Term, y: Term): ExstInducStruc

  def recOpt[C <: Term with Subs[C]](dom: Term, cod: Typ[C]): Option[Term]

  def inducOpt(dom: Term, cod: Term): Option[Term]

}

object ExstInducStruc {
  import translation.TermPatterns.fm

  case class OrElse(first: ExstInducStruc, second: ExstInducStruc)
      extends ExstInducStruc {
    def subs(x: Term, y: Term) = OrElse(first.subs(x, y), second.subs(x, y))

    def recOpt[C <: Term with Subs[C]](dom: Term, cod: Typ[C]): Option[Term] =
      first.recOpt(dom, cod).orElse(second.recOpt(dom, cod))

    def inducOpt(dom: Term, cod: Term): Option[Term] =
      first.inducOpt(dom, cod).orElse(second.inducOpt(cod, cod))

  }

  case class LambdaInduc(variable: Term, struct: ExstInducStruc)
      extends ExstInducStruc {
    def subs(x: Term, y: Term) =
      LambdaInduc(variable.replace(x, y), struct.subs(x, y))

    def recOpt[C <: Term with Subs[C]](dom: Term, cod: Typ[C]): Option[Term] =
      dom match {
        case FormalAppln(fmly, value) if value.typ == variable.typ =>
          struct.subs(variable, value).recOpt(dom, cod)
        case _ => None
      }

    def inducOpt(dom: Term, cod: Term): Option[Term] =
      dom match {
        case FormalAppln(fmly, value) if value.typ == variable.typ =>
          struct.subs(variable, value).inducOpt(dom, cod)
        case _ => None
      }
  }

  case class ConsSeqExst[SS <: HList, H <: Term with Subs[H], Intros <: HList](
      cs: ConstructorSeqTL[SS, H, Intros]
  ) extends ExstInducStruc {
    def subs(x: Term, y: Term) = ConsSeqExst(cs.subs(x, y))

    def recOpt[C <: Term with Subs[C]](dom: Term, cod: Typ[C]) =
      if (dom == cs.typ) Some(cs.recE(cod)) else None

    def inducOpt(dom: Term, cod: Term): Option[Term] =
      if (dom == cs.typ) Some(cs.inducE(fm[H](cs.typ, cod))) else None
  }

  case class IndConsSeqExst[SS <: HList, H <: Term with Subs[H],
  F <: Term with Subs[F], Index <: HList: TermList, Intros <: HList](
      cs: IndexedConstructorSeqDom[SS, H, F, Index, Intros])
      extends ExstInducStruc {
    def subs(x: Term, y: Term): ExstInducStruc = ???

    def recOpt[C <: Term with Subs[C]](dom: Term, cod: Typ[C]) =
      if (dom == cs.W) Some(cs.recE(cod)) else None

    def inducOpt(dom: Term, cod: Term): Option[Term] =
      if (dom == cs.W) Some(cs.inducE(cod)) else None
  }

  case object Base extends ExstInducStruc {
    def subs(x: Term, y: Term) = this

    def recOpt[C <: Term with Subs[C]](dom: Term, cod: Typ[C]): Option[Term] =
      cod match {
        case pt: ProdTyp[u, v] =>
          Some(pt.rec(cod))
        case Zero =>
          Some(Zero.rec(cod))
        case Unit =>
          Some(Unit.rec(cod))
        case pt: PlusTyp[u, v] =>
          Some(pt.rec(cod))
        case pt: SigmaTyp[u, v] =>
          Some(pt.rec(cod))
        case idt: IdentityTyp[u] =>
          Some(IdentityTyp.rec(idt.dom, cod))
        case _ =>
          None
      }

    def inducOpt(dom: Term, cod: Term): Option[Term] =
      cod match {
        case pt: ProdTyp[u, v] =>
          val x    = pt.first.Var
          val y    = pt.second.Var
          val tp   = fold(fold(cod)(x))(y).asInstanceOf[Typ[Term]]
          val fmly = x :-> (y :-> tp)
          Some(pt.induc(fmly))
        case Zero =>
          Some(Zero.induc(fm(Zero, cod)))
        case Unit =>
          Some(Unit.induc(fm(Unit, cod)))
        case pt: PlusTyp[u, v] =>
          Some(pt.induc(fm(pt, cod)))
        case pt: SigmaTyp[u, v] =>
          val x    = pt.fibers.dom.Var
          val y    = pt.fibers(x).Var
          val tp   = fold(fold(cod)(x))(y).asInstanceOf[Typ[Term]]
          val fmly = x :-> (y :-> tp)
          Some(pt.induc(fmly))
        case idt: IdentityTyp[u] =>
          Some(
            IdentityTyp.induc(
              idt.dom,
              cod
                .asInstanceOf[
                  FuncLike[u, FuncLike[u, FuncLike[Equality[u], Typ[Term]]]]])
          )
        case _ =>
          None
      }

  }
}
