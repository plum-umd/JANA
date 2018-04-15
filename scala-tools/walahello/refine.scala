import parma_polyhedra_library._
import scala.collection.immutable.TreeSet._
import collection.JavaConversions._

import PPL.LExp._
import PPL.LinearArray
import Core._
import CommonLoopBounds._

import Lemmas._
import MyLanguage._

case class Procedure()
case class Invariant()

class AbstractElement()
class TrueAbstractElement() extends AbstractElement

object RefineProcedure {
    def getInvariant(s: Language, p: ProgramLocation, e: AbstractElement) : AbstractElement = {
        return new AbstractElement()
    }

    def replaceLoop(p: Procedure, sres: Language, loop: Language) : Procedure = {
        return p
    }

    def getLanguageFromProcedure(p: Procedure) : Language = {
        return new Skip()
    }

    def refine(p: Procedure, loop: Language) : Procedure = {
        if (loop.isRepeat()) {
            var E = getInvariant(getLanguageFromProcedure(p), ProgramLocation(), new TrueAbstractElement())
            var s : Language = loop.flatten()
            var q : List[AbstractElement] = List(E)

            var (sres, newZ) = auxFunctionR(s, q)

            return replaceLoop(p, sres, loop)
        }

        return p
    }

    def auxFunctionR(flatS: Language, q: List[AbstractElement]): (Language, List[AbstractElement]) = {
        if (flatS.isChoose() && q.length > 0) {
        // if (flatS.isChoose() && q.length >= 0) {
            var E = q.head

            var SiList : List[Language] = List()
            var ZiList : List[List[AbstractElement]] = List()
            var Z : List[AbstractElement] = List()

            for (i <- 0 to flatS.l.length-1) {
                var rhoi = flatS.l(i)

                var newList = flatS.l diff List(rhoi)

                var newSi = new Sequence(
                    new RepeatPlus(rhoi),
                    new Choose(newList)
                )

                var newE = getInvariant(newSi, ProgramLocation(), E)

                var Zi : List[AbstractElement] = List()

                if (newE == null) {
                    newSi = null
                } else if (q.contains(newE)) {
                    Zi = newE::Zi
                } else {
                    var (newS, newZi) = auxFunctionR(flatS, newE::q)
                    newSi = new Sequence(
                        newSi,
                        newS
                    )
                    Zi = newZi
                }

                SiList = SiList:::List(newSi)
                ZiList = ZiList:::List(Zi)
            }

            var Sif : List[Language] = List(new Skip())
            var Swh : List[Language] = List()

            for (i <- 0 to flatS.l.length-1) {
                var rhoi = flatS.l(i)

                Sif = Sif:::List(new RepeatPlus(rhoi))

                if (SiList(i) != null) {
                    if (q.contains(E)) {
                        Swh = Swh:::List(SiList(i))
                    } else {
                        Sif = Sif:::List(SiList(i))
                    }

                    Z = Z:::ZiList(i) diff List(E)
                }
            }

            return (new Choose(Sif:::List(new Repeat(new Choose(Swh)))), Z)
        }

        return (new Skip(), List())
    }

    def refineT() = {
        var h = new Sequence(
            new Choose(
                List(
                    new Assume("x", "<", "N"),
                    new Assign("x", "3"),
                    new Assign("y", "4"),
                    new Assign("z", "5")
                )
            ),
            new Choose(
                List(
                    new Assume("y", ">=", "M"),
                    new Assign("x", "6"),
                    new Assign("y", "7"),
                    new Assign("z", "8")
                )
            )
        )

        println(h.flatten().toString())

        auxFunctionR(h.flatten(), List())

    }
}
