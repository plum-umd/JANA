import parma_polyhedra_library._
import scala.collection.immutable.TreeSet._
import collection.JavaConversions._

import PPL._
import PPL.LExp._
import CommonLoopBounds._
import WALA._

object MyLanguage {
    abstract class Language() {
        val l: List[Language] = List()

        def auxFunctionF() : List[Language] = {
            return List(this)
        }

        def flatten() : Language = {
            return new Choose(
                this.auxFunctionF()
            )
        }

        def isRepeat(): Boolean = {
            return false;
        }

        def isChoose(): Boolean = {
            return false;
        }
    }

    class Skip() extends Language {

        override def toString(): String = {
            return "skip"
        }
    }

    class Assign(x: String, e: String) extends Language {
        override def toString(): String = {
            return x + ":=" + e
        }
    }

    class Assume(x: String, c : String, e: String) extends Language {
        override def toString(): String = {
            return x + " " + c + " " + e
        }
    }

    class Sequence(c1: Language, c2: Language) extends Language {
        override def toString(): String = {
            return c1.toString() + ";" + c2.toString()
        }

        override def auxFunctionF() : List[Language] = {
            var l1 = c1.auxFunctionF()
            var l2 = c2.auxFunctionF()

            var retList : List[Language] = List()

            for (e1 <- l1) {
                for (e2 <- l2) {
                    retList = retList ++ List(new Sequence(e1, e2))
                }
            }

            return retList
        }
    }

    class Choose(override val l: List[Language]) extends Language {

        override def toString(): String = {
            var retString = "("

            retString += "("+l.head.toString()+")"

            for (c <- l.tail) {
                retString += " | "+"("+c.toString()+")"
            }

            retString += ")"
            return retString
        }

        override def auxFunctionF() : List[Language] = {
            return l.map(x => x.auxFunctionF()).reduceLeft(_ ++ _)
        }

        override def isChoose(): Boolean = {
            return true;
        }
    }

    class Repeat(l: Language) extends Language {

        override def toString(): String = {
            return "(" + l.toString() + ")*"
        }

        override def isRepeat(): Boolean = {
            return true;
        }
    }

    class RepeatPlus(l: Language) extends Language {

        override def toString(): String = {
            return "(" + l.toString() + ")+"
        }
    }
}
