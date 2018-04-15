import parma_polyhedra_library._

import Core._
import WALA._
import PPL.LExp._
import PPL.LinearArray
import CommonLoopBounds._

import scala.collection.immutable.TreeSet._
import collection.JavaConversions._

import BoundExpressions.{BoundExpression,BoundExpressionDiv,BoundExpressionDiff,BoundExpressionLog,BoundExpressionConst, BoundExpressionConstC}

object Lemmas {
    abstract class Lemma {
        def apply(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : List[BoundExpression] = {
            List()
        }

        def apply(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec], upper: Boolean = true) : List[BoundExpression] = {
            return this.apply(preCond, transCond, postCond)
        }

        def matches(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : Boolean = {
            false
        }

        def matches(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec], upper: Boolean = true) : Boolean = {
            return this.matches(preCond, transCond, postCond)
        }
    }

    class LemmaLinear extends Lemma{
        def booleanVarCoefTest(x: BigInt, y: BigInt) : Boolean = {
            return (x.abs > 0 && y.abs > 0 && x.abs == y.abs)
        }

        def isImportantPreRelation(s: SolvedLinearConstraint) : Boolean = {
            return false
        }

        def isImportantPostRelation(s: SolvedLinearConstraint) : Boolean = {
            return false
        }

        def isRelatingPrimedVars(constraint : SolvedLinearConstraint, v : DimIndex, vp : DimIndex) :Boolean = {
            if (constraint.lhs_coefficient != 0) {
                if (constraint.lhs_varindex == v) {
                    return constraint.rhs_coefficients.linearCoefficients(vp.toInt) != 0
                } else if (constraint.lhs_varindex == vp) {
                    return constraint.rhs_coefficients.linearCoefficients(v.toInt) != 0
                }
            }

            return false
        }

        def isTransitionConstantOnPrimedVars(constraint : SolvedLinearConstraint, v : DimIndex, vp : DimIndex) :Boolean = {
            return (constraint.rhs_coefficients.constantCoefficient == 0 && constraint.rhs_coefficients.linearCoefficients.zipWithIndex.filter{x => x._1 != 0}.map{y=>y._2}.toSet.diff(Set(vp.toInt)).isEmpty)
        }

        def getImportantConditions(preCond: LoopEdgeSpec, transCond: LoopTransSpec, postCond: LoopEdgeSpec) : (LoopEdgeSpec, LoopTransSpec, LoopEdgeSpec) = {
            if (preCond.loopvar != transCond.loopvarOld || transCond.loopvarOld != postCond.loopvar) {
                return (null, null, null)
            }

            if (transCond.loopvarRelations.length <= 0) {
                return (null,null,null)
            }

            var varIndex = transCond.loopvarOld

            var impPreConstraints = preCond.loopvarRelations.filter { j => isImportantPreRelation(j) }.filter { k => !isRelatingPrimedVars(k, transCond.loopvarOld, transCond.loopvarNew)}

            if (impPreConstraints.isEmpty()) {
                ifDebug {
                    println("Empty pre:" + preCond.loopvarRelations)
                }

                impPreConstraints = List(
                    SolvedLinearConstraint(
                        1,
                        transCond.loopvarNew,
                        Relation_Symbol.EQUAL,
                        new LinearArray(transCond.loopvarRelations.head.rhs_coefficients.size, transCond.loopvarNew, 1)
                    )
                )
            }

            var tempImpTransConstraints = transCond.loopvarRelations.filter { j => j.relation match {
                    case Relation_Symbol.EQUAL => true
                    case _ => false
                }
            }.filter{ k =>
                !isTransitionConstantOnPrimedVars(k, transCond.loopvarOld, transCond.loopvarNew)
            }
            // .filter{ m =>
            //     isTransitionDirectionConsistent(k, transCond.loopvarOld, transCond.loopvarNew)
            // }

            var impTransConstraints = tempImpTransConstraints.filter { j => (j.rhs_coefficients.linearCoefficients(transCond.loopvarOld.toInt),j.lhs_coefficient) match {
                    case (x,y) if booleanVarCoefTest(x,y) => true
                    case _ => false
                }
            }

            var impPostConstraints = postCond.loopvarRelations.filter { j => isImportantPostRelation(j) }.filter { k => !isRelatingPrimedVars(k, transCond.loopvarOld, transCond.loopvarNew)}

            // if (impPostConstraints.isEmpty()) {
            //     ifDebug {
            //         println("Empty post:" + postCond.loopvarRelations)
            //     }
            //
            //     println("Empty post 2")
            //     transCond.otherRelations.filter{ x => x.coefficients.linearCoefficients(transCond.loopvarOld.toInt) != 0}.map{x => println(x)}
            //     impPostConstraints = transCond.otherRelations.filter{ x => x.coefficients.linearCoefficients(transCond.loopvarOld.toInt) != 0}.map{ x=>
            //         var coef = -x.coefficients.linearCoefficients(transCond.loopvarOld.toInt)
            //         var coefs = x.coefficients.clone()
            //         coefs.linearCoefficients(transCond.loopvarOld.toInt) = 0
            //
            //         var newRelation = x.relation
            //
            //         if (coef < 0) {
            //             coefs = new LinearArray(-coefs.constantCoefficient, coefs.linearCoefficients.map{x => -x}.clone())
            //             // coefs.linearCoefficients = coefs.linearCoefficients.map(x => -x).clone()
            //             coef = -coef
            //         } else {
            //             newRelation match {
            //                 case Relation_Symbol.EQUAL => newRelation = Relation_Symbol.EQUAL
            //                 case Relation_Symbol.NOT_EQUAL => newRelation = Relation_Symbol.NOT_EQUAL
            //                 case Relation_Symbol.GREATER_THAN => newRelation = Relation_Symbol.LESS_THAN
            //                 case Relation_Symbol.GREATER_OR_EQUAL => newRelation = Relation_Symbol.LESS_OR_EQUAL
            //                 case Relation_Symbol.LESS_THAN => newRelation = Relation_Symbol.GREATER_THAN
            //                 case Relation_Symbol.LESS_OR_EQUAL => newRelation = Relation_Symbol.GREATER_OR_EQUAL
            //             }
            //         }
            //
            //         println("newRelation")
            //         println(newRelation)
            //
            //         SolvedLinearConstraint(
            //             coef,
            //             transCond.loopvarNew,
            //             newRelation,
            //             coefs
            //         )
            //     }
            //
            //     println("new post constraints:")
            //
            //     impPostConstraints.map{x=> println(x)}
            // }

            if (!impPreConstraints.isEmpty() && !impTransConstraints.isEmpty() && !impPostConstraints.isEmpty()) {
                var impPreConditions = LoopEdgeSpec(
                  loopvar = preCond.loopvar,
                  loopvarRelations = impPreConstraints,
                  otherRelations = preCond.otherRelations
                )
                var impTransConditions = LoopTransSpec(
                  loopvarOld = transCond.loopvarOld,
                  loopvarNew = transCond.loopvarNew,
                  loopvarRelations = impTransConstraints,
                  otherRelations = transCond.otherRelations
                )
                var impPostConditions = LoopEdgeSpec(
                  loopvar = postCond.loopvar,
                  loopvarRelations = impPostConstraints,
                  otherRelations = postCond.otherRelations
                )

                ifDebug {
                    // println("NON-EMPTY CONSTRAINTS: " + impPreConstraints + " " + impTransConstraints + " " + impPostConstraints)
                }
                return (impPreConditions, impTransConditions, impPostConditions)
            } else {
                ifDebug {
                    // println("EMPTY CONSTRAINTS: " + impPreConstraints.isEmpty() + " " + impTransConstraints.isEmpty() + " " + impPostConstraints.isEmpty())
                }
                return (null, null, null)
            }
        }

        def getBoundFromSolvedLinearConstraints(preLinearConstraint: SolvedLinearConstraint, transLinearConstraint: SolvedLinearConstraint, postLinearConstraint: SolvedLinearConstraint, loopVar: DimIndex) : BoundExpression = {
            var negate = shouldNegateConstraints()
            var vInitBoundExpression = new BoundExpression().fromSolvedLinearConstraint(false)(preLinearConstraint)

            var solvedLinearTransConstraint = SolvedLinearConstraint(
                lhs_coefficient = transLinearConstraint.lhs_coefficient,
                lhs_varindex = transLinearConstraint.lhs_varindex,
                relation = transLinearConstraint.relation,
                rhs_coefficients = transLinearConstraint.rhs_coefficients - new LinearArray(transLinearConstraint.rhs_coefficients.size, loopVar, transLinearConstraint.rhs_coefficients.linearCoefficients(loopVar.toInt))
                //###
            )

            ifDebug {
                println(loopVar)
                println(transLinearConstraint)
                println(solvedLinearTransConstraint)
            }

            var vIncBoundExpression = new BoundExpression().fromSolvedLinearConstraint(negate)(solvedLinearTransConstraint)

            var vFinalBoundExpression = new BoundExpression().fromSolvedLinearConstraint(false)(postLinearConstraint)

            ifDebug {
                println("getBoundFromSolvedLinearConstraints")
                println(vInitBoundExpression)
                println(vIncBoundExpression)
                println(vFinalBoundExpression)
            }

            var bound = getBoundFrom(vInitBoundExpression, vIncBoundExpression, vFinalBoundExpression)

            return bound
        }

        def getBoundsFromPreTransPostConstraints(preSpec: LoopEdgeSpec, transSpec: LoopTransSpec, postSpec: LoopEdgeSpec) : List[BoundExpression] = {
            var preConstraints = preSpec.loopvarRelations
            var transConstraints = transSpec.loopvarRelations
            var postConstraints = postSpec.loopvarRelations

            var allCombinations = preConstraints.map{
                x => transConstraints.map{
                    y => postConstraints.map(z => (x,y,z))
                }
            }.foldLeft(List[List[Any]]())(_ ++ _).foldLeft(List[Any]())(_ ++ _)

            return allCombinations.map{ case (x,y,z) => getBoundFromSolvedLinearConstraints(x.asInstanceOf[SolvedLinearConstraint],y.asInstanceOf[SolvedLinearConstraint],z.asInstanceOf[SolvedLinearConstraint], transSpec.loopvarOld)}
        }

        override def apply(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : List[BoundExpression] = {
            if (preCond.size() == 1 && transCond.size() == 1 && postCond.size() == 1) {
                var (impPreConditions, impTransConditions, impPostConditions) = getImportantConditions(preCond.head._2, transCond.head._2, postCond.head._2)

                if (impPreConditions != null && impTransConditions != null && impPreConditions != null) {
                    if (!(impPreConditions.loopvarRelations.isEmpty() || impTransConditions.loopvarRelations.isEmpty() || impPostConditions.loopvarRelations.isEmpty())) {

                        return getBoundsFromPreTransPostConstraints(impPreConditions, impTransConditions, impPostConditions)
                    }
                }
            }

            return List()
        }

        override def matches(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : Boolean = {
            if (preCond.size() == 1 && transCond.size() == 1 && postCond.size() == 1) {
                var (impPreConditions, impTransConditions, impPostConditions) = getImportantConditions(preCond.head._2, transCond.head._2, postCond.head._2)

                if (impPreConditions != null && impTransConditions != null && impPostConditions != null) {
                    if (!(impPreConditions.loopvarRelations.isEmpty() || impTransConditions.loopvarRelations.isEmpty() || impPostConditions.loopvarRelations.isEmpty())) {
                        return true
                    }
                }
            }

            ifDebug {
                // println("SIZES: " + preCond.size() + " " + transCond.size() + " " + postCond.size())
            }

            return false
        }

        def getBoundFrom(initBound: BoundExpression, incBound: BoundExpression, finalBound: BoundExpression) : BoundExpression = {
            return null
        }

        def shouldNegateConstraints() : Boolean = {
            return true
        }
    }

    class LemmaLinearIncConstant extends LemmaLinear {
        override def booleanVarCoefTest(x: BigInt, y: BigInt) : Boolean = {
            return (x.abs > 0 && y.abs > 0 && x.abs == y.abs)
        }

        override def isImportantPreRelation(s: SolvedLinearConstraint) : Boolean = {
            s.relation match {
                case Relation_Symbol.GREATER_THAN => true
                case Relation_Symbol.GREATER_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def isImportantPostRelation(s: SolvedLinearConstraint) : Boolean = {
            ifDebug {
                println("isImportantPostRelation: INC CONSTANT")
            }
            s.relation match {
                case Relation_Symbol.LESS_THAN => true
                case Relation_Symbol.LESS_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def getBoundFrom(initBound: BoundExpression, incBound: BoundExpression, finalBound: BoundExpression) : BoundExpression = {
            // return new BoundExpressionDiv(new BoundExpressionDiff(finalBound, initBound), incBound)
            // return new BoundExpressionConstC()
            return new BoundExpressionConst(1)
        }

        override def shouldNegateConstraints() : Boolean = {
            return false
        }
    }

    class LemmaLinearDecConstant extends LemmaLinear {
        override def booleanVarCoefTest(x: BigInt, y: BigInt) : Boolean = {
            return (x.abs > 0 && y.abs > 0 && x.abs == y.abs)
        }

        override def isImportantPreRelation(s: SolvedLinearConstraint) : Boolean = {
            s.relation match {
                case Relation_Symbol.LESS_THAN => true
                case Relation_Symbol.LESS_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def isImportantPostRelation(s: SolvedLinearConstraint) : Boolean = {
            ifDebug {
                println("isImportantPostRelation: DEC CONSTANT")
            }
            s.relation match {
                case Relation_Symbol.GREATER_THAN => true
                case Relation_Symbol.GREATER_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def getBoundFrom(initBound: BoundExpression, incBound: BoundExpression, finalBound: BoundExpression) : BoundExpression = {
            // return new BoundExpressionDiv(new BoundExpressionDiff(initBound, finalBound), incBound)
            // return new BoundExpressionConstC()
            return new BoundExpressionConst(1)
        }

        override def shouldNegateConstraints() : Boolean = {
            return true
        }
    }

    class LemmaLinearInc extends LemmaLinear {
        override def booleanVarCoefTest(x: BigInt, y: BigInt) : Boolean = {
            return (x.abs > 0 && y.abs > 0 && x.abs == y.abs)
        }

        override def isImportantPreRelation(s: SolvedLinearConstraint) : Boolean = {
            s.relation match {
                case Relation_Symbol.GREATER_THAN => true
                case Relation_Symbol.GREATER_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def isImportantPostRelation(s: SolvedLinearConstraint) : Boolean = {
            ifDebug {
                println("isImportantPostRelation: INC")
            }
            s.relation match {
                case Relation_Symbol.GREATER_THAN => true
                case Relation_Symbol.GREATER_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def getBoundFrom(initBound: BoundExpression, incBound: BoundExpression, finalBound: BoundExpression) : BoundExpression = {
            return new BoundExpressionDiv(new BoundExpressionDiff(finalBound, initBound), incBound)
        }

        override def shouldNegateConstraints() : Boolean = {
            return false
        }
    }

    class LemmaLinearDec extends LemmaLinear {
        override def booleanVarCoefTest(x: BigInt, y: BigInt) : Boolean = {
            return (x.abs > 0 && y.abs > 0 && x.abs == y.abs)
        }

        override def isImportantPreRelation(s: SolvedLinearConstraint) : Boolean = {
            s.relation match {
                case Relation_Symbol.LESS_THAN => true
                case Relation_Symbol.LESS_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def isImportantPostRelation(s: SolvedLinearConstraint) : Boolean = {
            ifDebug {
                println("isImportantPostRelation: DEC")
            }
            s.relation match {
                case Relation_Symbol.LESS_THAN => true
                case Relation_Symbol.LESS_OR_EQUAL => true
                case Relation_Symbol.EQUAL => true
                case _ => false
            }
        }

        override def getBoundFrom(initBound: BoundExpression, incBound: BoundExpression, finalBound: BoundExpression) : BoundExpression = {
            return new BoundExpressionDiv(new BoundExpressionDiff(initBound, finalBound), incBound)
        }

        override def shouldNegateConstraints() : Boolean = {
            return true
        }
    }


    class LemmaMultInc extends LemmaLinearInc{
        // aiming for changes of the form v' = c*v+d where c,d constants, c > 1
        override def booleanVarCoefTest(x: BigInt, y: BigInt) : Boolean = {
            return (x.abs > 0 && y.abs > 0 && x.abs != y.abs)
        }

        override def apply(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : List[BoundExpression] = {
            if (preCond.size() == 1 && transCond.size() == 1 && postCond.size() == 1) {
                var (impPreConditions, impTransConditions, impPostConditions) = getImportantConditions(preCond.head._2, transCond.head._2, postCond.head._2)

                if (impPreConditions != null && impTransConditions != null && impPostConditions != null) {
                    if (!(impPreConditions.loopvarRelations.isEmpty() || impTransConditions.loopvarRelations.isEmpty() || impPostConditions.loopvarRelations.isEmpty())) {
                        var vInitBoundExpression = new BoundExpression().fromSolvedLinearConstraint(false)(impPreConditions.loopvarRelations.head)
                        // var solvedLinearTransConstraint = SolvedLinearConstraint(
                        //     lhs_coefficient = impTransConditions.loopvarRelations.head.lhs_coefficient,
                        //     lhs_varindex = impTransConditions.loopvarRelations.head.lhs_varindex,
                        //     relation = impTransConditions.loopvarRelations.head.relation,
                        //     rhs_coefficients = impTransConditions.loopvarRelations.head.rhs_coefficients - new LinearArray(impTransConditions.loopvarRelations.head.rhs_coefficients.size, impTransConditions.loopvarOld, impTransConditions.loopvarRelations.head.rhs_coefficients.linearCoefficients(impTransConditions.loopvarOld.toInt))
                        // )

                        // var vIncBoundExpression = new BoundExpression().fromSolvedLinearConstraint(solvedLinearTransConstraint)
                        var vFinalBoundExpression = new BoundExpression().fromSolvedLinearConstraint(false)(impPostConditions.loopvarRelations.head)

                        ifDebug {
                            println("LemmaMultInc returns bound")
                            // println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
                            // println(impPreConditions)
                            // println(impTransConditions)
                            // println(impPostConditions)
                            // println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
                            // println(vInitBoundExpression)
                            // // println(vIncBoundExpression)
                            // println(new BoundExpressionConst(impTransConditions.loopvarRelations.head.rhs_coefficients.linearCoefficients(impTransConditions.loopvarOld.toInt)))
                            // println(vFinalBoundExpression)
                            // println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
                        }
                        var bound = new BoundExpressionLog(new BoundExpressionConst(impTransConditions.loopvarRelations.head.rhs_coefficients.linearCoefficients(impTransConditions.loopvarOld.toInt)), new BoundExpressionDiff(vFinalBoundExpression, vInitBoundExpression))

                        return List(bound)
                    }
                }
            }

            return List()
        }
    }

    class LemmaLinearMultipleVars extends LemmaLinearInc{
        // aiming for changes of the form v' = c*v+d where c,d constants, c > 1

        def getLemma() : Lemma = {
            return null
        }

        override def apply(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : List[BoundExpression] = {
            var lemmaLinear = getLemma()
            var firstDimIndex: DimIndex = 0
            var bounds = List[BoundExpression]()

            if (preCond.size() == 1 && transCond.size() == 1 && postCond.size() == 1) {
                firstDimIndex = preCond.head._1
                bounds = lemmaLinear.apply(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))
                if (bounds.size() >= 1) {
                    ifDebug {
                        println("LemmaLinearMultipleVars returns bounds")
                    }
                }
                return bounds
            } else if (preCond.size() >= 1 && transCond.size() >= 1 && postCond.size() >= 1) {
                firstDimIndex = preCond.head._1
                bounds = this.apply(preCond - firstDimIndex, transCond - firstDimIndex, postCond - firstDimIndex) ++ lemmaLinear.apply(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))
                if (bounds.size() >= 1) {
                    ifDebug {
                        println("LemmaLinearMultipleVars returns bounds")
                    }
                }
                return bounds
            }

            return List()
        }

        override def matches(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : Boolean = {
            var lemmaLinear = getLemma()
            var firstDimIndex: DimIndex = 0

            if (preCond.size() == 1 && transCond.size() == 1 && postCond.size() == 1) {
                ifDebug {
                    // println("ONE VAR REMAINING for LemmaLinearMultipleVars: " + preCond.head._1)
                }
                firstDimIndex = preCond.head._1
                return lemmaLinear.matches(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))
            } else if (preCond.size() >= 1 && transCond.size() >= 1 && postCond.size() >= 1) {
                ifDebug {
                    // println("MORE THAN ONE VAR for LemmaLinearMultipleVars: " + preCond.head._1)
                }
                firstDimIndex = preCond.head._1
                return this.matches(preCond - firstDimIndex, transCond - firstDimIndex, postCond - firstDimIndex) || lemmaLinear.matches(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))
            }

            return false
        }
    }

    class LemmaLinearIncConstantMultipleVars extends LemmaLinearMultipleVars{
        override def getLemma() : Lemma = {
            return new LemmaLinearIncConstant()
        }
    }

    class LemmaLinearDecConstantMultipleVars extends LemmaLinearMultipleVars{
        override def getLemma() : Lemma = {
            return new LemmaLinearDecConstant()
        }
    }

    class LemmaLinearIncMultipleVars extends LemmaLinearMultipleVars{
        override def getLemma() : Lemma = {
            return new LemmaLinearInc()
        }
    }

    class LemmaLinearDecMultipleVars extends LemmaLinearMultipleVars{
        override def getLemma() : Lemma = {
            return new LemmaLinearDec()
        }
    }

    class LemmaMultipleVars extends Lemma{
        // aiming for changes of the form v' = c*v+d where c,d constants, c > 1
        override def apply(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : List[BoundExpression] = {
            return this.apply(preCond, transCond, postCond, true)
        }

        override def apply(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec], upper: Boolean = true) : List[BoundExpression] = {
            var lemmasList = List[Lemma]()

            if (upper) {
                ifDebug {
                    println("upper bounds")
                }
                lemmasList = List(new LemmaLinearIncMultipleVars(), new LemmaLinearDecMultipleVars(), new LemmaMultInc())
            } else {
                ifDebug {
                    println("lower bounds apply")
                }
                lemmasList = List(new LemmaLinearIncConstantMultipleVars(), new LemmaLinearDecConstantMultipleVars())
                // lemmasList = List(new LemmaLinearIncConstantMultipleVars())
            }

            // var lemmasList = List(new LemmaLinearDecMultipleVars())
            var firstDimIndex : DimIndex = 0
            var bounds = List[BoundExpression]()

            def append(x: List[BoundExpression], y: List[BoundExpression]) : List[BoundExpression] = {
                return x ++ y
            }

            if (preCond.size() == 1 && transCond.size() == 1 && postCond.size() == 1) {
                firstDimIndex = preCond.head._1

                bounds = lemmasList.map{ x => x.apply(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))}.reduce{append}
                if (bounds.size() >= 1) {
                    ifDebug {
                        println("LemmaMultipleVars returns bounds")
                    }
                }
                return bounds
            } else if (preCond.size() >= 1 && transCond.size() >= 1 && postCond.size() >= 1) {
                firstDimIndex = preCond.head._1
                bounds = this.apply(preCond - firstDimIndex, transCond - firstDimIndex, postCond - firstDimIndex) ++ lemmasList.map{ x => x.apply(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))}.reduce{append}
                if (bounds.size() >= 1) {
                    ifDebug {
                        println("LemmaMultipleVars returns bounds")
                    }
                }
                return bounds
            }

            return List()
        }

        override def matches(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec]) : Boolean = {
            return this.matches(preCond, transCond, postCond, true)
        }

        override def matches(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec], upper: Boolean = true) : Boolean = {
            var firstDimIndex : DimIndex = 0
            var lemmasList = List[Lemma]()

            if (upper) {
                ifDebug {
                    println("upper bounds")
                }
                lemmasList = List(new LemmaLinearIncMultipleVars(), new LemmaLinearDecMultipleVars(), new LemmaMultInc())
            } else {
                ifDebug {
                    println("lower bounds matches")
                }
                lemmasList = List(new LemmaLinearIncConstantMultipleVars(), new LemmaLinearDecConstantMultipleVars())
                // lemmasList = List(new LemmaLinearIncConstantMultipleVars())
            }
            // var lemmasList = List(new LemmaLinearDecMultipleVars())

            def or(x: Boolean, y: Boolean) : Boolean = {
                x || y
            }

            if (preCond.size() == 1 && transCond.size() == 1 && postCond.size() == 1) {
                ifDebug {
                    // println("ONE VAR REMAINING: " + preCond.head._1)
                    // println(lemmasList.length)
                }
                firstDimIndex = preCond.head._1
                return lemmasList.map{ x => x.matches(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))}.foldLeft(false)(_ || _)
            } else if (preCond.size() >= 1 && transCond.size() >= 1 && postCond.size() >= 1) {
                ifDebug {
                    // println("MORE THAN ONE VAR: " + preCond.head._1)
                }
                firstDimIndex = preCond.head._1
                return this.matches(preCond - firstDimIndex, transCond - firstDimIndex, postCond - firstDimIndex) || lemmasList.map{ x => x.matches(Map() + (firstDimIndex -> preCond(firstDimIndex)),Map() + (firstDimIndex -> transCond(firstDimIndex)),Map() + (firstDimIndex -> postCond(firstDimIndex)))}.foldLeft(false)(_ || _)
            }

            return false
        }
    }
}
