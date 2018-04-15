import parma_polyhedra_library._
import scala.collection.immutable.TreeSet._
import collection.JavaConversions._

import PPL.LExp._
import PPL._
import PPL.LinearArray
import Core._
import CommonLoopBounds._

import Lemmas._
import BoundExpressions._

object ComputeBound {
    def printBounds(bounds: List[BoundExpression]) = {
        if (!bounds.isEmpty()) {
            var i = 1
            for (bound <- bounds) {
                ifDebug {
                    println("bound " + i.toString() + ": " + bound.toString())
                }
                i += 1
            }
        } else {
            ifDebug {
                println("no bounds could be calculated")
            }
        }
    }

    def getListsFromTuples(tuples: List[(DimIndex, DimIndex)]) : List[List[DimIndex]] = {
        def combineLists(list1: List[DimIndex], list2: List[DimIndex]) : List[DimIndex] = {
            if ((list1.toSet & list2.toSet).size >= 1) {
                return (list1.toSet | list2.toSet).toList
            } else {
                return list1
            }
        }

        if (tuples.length <= 0) {
            return List()
        } else if (tuples.length == 1) {
            return List(List(tuples.head._1, tuples.head._2).sorted)
        } else {
            var tailLists = getListsFromTuples(tuples.tail)
            var headList = List(tuples.head._1, tuples.head._2).sorted
            if (tailLists.filter{ x => (x.toSet & headList.toSet).size >= 1 }.length >= 1) {
                return tailLists.map{ x => combineLists(x, headList).sorted}
            } else {
                return tailLists:::List(headList)
            }
        }

        return List()
    }

    def getLoopSummaryFromPolyhedra(loopSummaryPolyhedra: LoopSummaryPolyhedra) : LoopSummary = {
        ifDebug {
            // println("getLoopSummaryFromPolyhedra")
            // println(loopSummaryPolyhedra)
        }

        var variableDims : List[DimIndex] = loopSummaryPolyhedra.primedVars.map{
            case (d1, d2) => d2
        }
        var variableDimsMap : Map[DimIndex, DimIndex] = loopSummaryPolyhedra.primedVars.map{
            case (d1, d2) => d2 -> d1
        }.toMap

        ifDebug {
            // println("variableDims")
            // println(variableDims)
            // println("variableDimsMap")
            // println(variableDimsMap)
            //
            // println("pre")
            // println(loopSummaryPolyhedra.pre)
            //
            // println("trans")
            // println(loopSummaryPolyhedra.trans)
            //
            // println("post")
            // println(loopSummaryPolyhedra.post)
        }

        // println("START PPL.transSpecOfPoly(variableDimsMap(d), d, loopSummaryPolyhedra.trans)")

        // variableDims.map{ d => println(PPL.transSpecOfPoly(variableDimsMap(d), d, loopSummaryPolyhedra.trans))}

        // println("END PPL.transSpecOfPoly(variableDimsMap(d), d, loopSummaryPolyhedra.trans)")

        LoopSummary(
            variableDims.map{ d => PPL.edgeSpecOfPoly(variableDimsMap(d), loopSummaryPolyhedra.pre)},
            variableDims.map{ d => PPL.transSpecOfPoly(variableDimsMap(d), d, loopSummaryPolyhedra.trans)},
            variableDims.map{ d => PPL.edgeSpecOfPoly(variableDimsMap(d), loopSummaryPolyhedra.post)}
        )
    }

    def computeBounds(loopSummaryPolyhedra: LoopSummaryPolyhedra, upper: Boolean = true) : List[BoundExpression] = {
        var varEquiv = getListsFromTuples(loopSummaryPolyhedra.primedVars)
        var bounds : List[BoundExpression] = computeBoundsWithVarEquivalences(
            getLoopSummaryFromPolyhedra(loopSummaryPolyhedra),
            varEquiv,
            upper
        )

        ifDebug {
            // println("Bounds:")
            // println(bounds)
        }
        bounds = bounds.map{ bound => bound.renameWithBinding(convertBimapWithEquivalence(loopSummaryPolyhedra.transBindings, varEquiv))}

        ifDebug {
            // println("Bounds:")
            // println(bounds)
        }

        ifDebug {
            // println("Bindings:")
            // println(loopSummaryPolyhedra.transBindings)
            // println(convertBimapWithEquivalence(loopSummaryPolyhedra.transBindings, varEquiv))
        }

        // if (bounds.length == 0) {
        //     if (upper) {
        //         return List(new BoundExpressionInfinity())
        //     }
        // }

        return bounds
    }

    // def computeBounds(loopSummaryPolyhedra: LoopSummaryPolyhedra) : List[BoundExpression] = {
    //     computeBounds(loopSummaryPolyhedra, true)
    // }

    def computeBound(loopSummary: LoopSummary) :BoundExpression = {

        var bounds = computeBounds(loopSummary, null);

        var bestBound = new BoundExpression()
        if (bounds != null) {
            bestBound = bounds.head;
        }

        // for (bound <- bounds) {
        //     if bound.isBetterThan(bestBound);
        //     bestBound = bound;
        // }

        return bestBound;
    }

    def computeBoundsWithVarEquivalences(loopSummary: LoopSummary, varsEquiv: List[List[DimIndex]], upper: Boolean = true) : List[BoundExpression] = {
        ifDebug {
            println("\n\n\ncompute bound\n\n\n")
            // println(upper)
        }

        var bounds = List[BoundExpression]()

        ifDebug {
            // println(loopSummary)
        }

        // var (preCondMap, transCondMap, postCondMap) = createMapFromSummary(loopSummary, primedVars)
        var (preCondMap, transCondMap, postCondMap) = createEquivalenceMapFromSummary(loopSummary, varsEquiv)

        ifDebug {
            // loopSummary.trans.map {cond => println(getEquivalenceElementOfVar(cond.loopvarOld, varsEquiv)); println(convertTransConditionWithEquivalence(cond, varsEquiv))}
        }

        ifDebug {
            // println("pre, trans, post maps")
            // println(preCondMap)
            // println(transCondMap)
            // println(postCondMap)
        }

        var listOfMatchedLemmas = matchLemmas(preCondMap, transCondMap, postCondMap, upper)
        ifDebug {
            // println("APPLYING MATCHED LEMMAS")
            // println(listOfMatchedLemmas)
        }
        if (!listOfMatchedLemmas.isEmpty()) {
            for (lemma <- listOfMatchedLemmas) {
                bounds = lemma.apply(preCondMap, transCondMap, postCondMap, upper) ++ bounds
            }

            return bounds
        }

        return bounds
    }

    def computeBounds(loopSummary: LoopSummary, primedVars: List[(DimIndex, DimIndex)]) : List[BoundExpression] = {
        ifDebug {
            // println("\n\n\ncompute bound \n\n\n")
        }

        var bounds = List[BoundExpression]()

        ifDebug {
            // println("loopSummary")
            // println(loopSummary)
        }

        var (preCondMap, transCondMap, postCondMap) = createMapFromSummary(loopSummary, primedVars)
        var listOfMatchedLemmas = matchLemmas(preCondMap, transCondMap, postCondMap)
        ifDebug {
            // println("APPLYING MATCHED LEMMAS")
            // println(listOfMatchedLemmas)
        }
        if (!listOfMatchedLemmas.isEmpty()) {
            for (lemma <- listOfMatchedLemmas) {
                bounds = lemma.apply(preCondMap, transCondMap, postCondMap) ++ bounds
            }

            return bounds
        }

        return bounds
    }

    def matchLemmas(preCond: Map[DimIndex, LoopEdgeSpec], transCond: Map[DimIndex, LoopTransSpec], postCond: Map[DimIndex, LoopEdgeSpec], upper: Boolean = true) : List[Lemma] = {
        // var lemmas = List(new LemmaLinearInc(), new LemmaMultInc(), new LemmaLinearIncMultipleVars(), new LemmaMultipleVars())

        var lemmas = List(new LemmaMultipleVars())
        // var lemmas = List(new LemmaLinearIncMultipleVars(), new LemmaMultipleVars())
        // var lemmas = List(new LemmaLinearDecMultipleVars())
        var matchedLemmas = lemmas.filter { _.matches(preCond, transCond, postCond, upper)}

        //only one lemma returned right now
        if (matchedLemmas.isEmpty()) {
            return List()
        } else {
            return List(matchedLemmas.reverse.head)
        }
    }

    def getPrimedVar(d: DimIndex, primedVars: List[(DimIndex, DimIndex)]) : DimIndex = {
        var primedVarsMap : Map[DimIndex, DimIndex] = primedVars.map{ j => j match {
            case (d, y) => d -> y
            case (x,_) => d -> x
            }
        }.toMap

        return primedVarsMap(d)
    }

    def getEquivalenceClassOfVar(d: DimIndex, primedVars: List[List[DimIndex]]) : List[DimIndex] = {
        var intersects = primedVars.filter{ x => (x.toSet & Set(d)).size >= 1}
        if (intersects.length >= 1) {
            return intersects(0)
        }

        return List()
    }

    def getEquivalenceElementOfVar(d: DimIndex, primedVars: List[List[DimIndex]]) : DimIndex = {
        var equivClass = getEquivalenceClassOfVar(d, primedVars)
        if (equivClass.length >= 1) {
            return equivClass(0)
        }

        return d
    }

    def areVarsEquivalent(d: DimIndex, e: DimIndex, varsEquiv: List[List[DimIndex]]) : Boolean = {
        var intersectsWithD = varsEquiv.filter{ x => (x.toSet & Set(d)).size >= 1}
        var intersectsWithE = intersectsWithD.filter{ x => (x.toSet & Set(e)).size >= 1}

        return intersectsWithE.length >= 1
    }

    def createMapFromSummary(loopSummary: LoopSummary, primedVars: List[(DimIndex, DimIndex)]) : (Map[DimIndex, LoopEdgeSpec], Map[DimIndex, LoopTransSpec], Map[DimIndex, LoopEdgeSpec]) = {
        var preCondMap = loopSummary.pre.map (cond => getPrimedVar(cond.loopvar, primedVars) -> cond).toMap
        var transCondMap = loopSummary.trans.map (cond => cond.loopvarOld -> cond).toMap
        var postCondMap = loopSummary.post.map (cond => getPrimedVar(cond.loopvar, primedVars) -> cond).toMap

        return (preCondMap, transCondMap, postCondMap)
    }

    def convertPrePostConditionWithEquivalence(edgeCond: LoopEdgeSpec, varsEquiv: List[List[DimIndex]]) : LoopEdgeSpec = {
        return LoopEdgeSpec(
            getEquivalenceElementOfVar(edgeCond.loopvar, varsEquiv),
            edgeCond.loopvarRelations.map{x => convertSolvedConstraintWithEquivalence(x, varsEquiv)},
            edgeCond.otherRelations.map{x=> convertLinearConstraintWithEquivalence(x, varsEquiv)}
        )
    }

    def convertTransConditionWithEquivalence(transCond: LoopTransSpec, varsEquiv: List[List[DimIndex]]) : LoopTransSpec = {
        return LoopTransSpec(
            getEquivalenceElementOfVar(transCond.loopvarOld, varsEquiv),
            getEquivalenceElementOfVar(transCond.loopvarNew, varsEquiv),
            transCond.loopvarRelations.map{x => convertSolvedConstraintWithEquivalence(x, varsEquiv)},
            transCond.otherRelations.map{x=> convertLinearConstraintWithEquivalence(x, varsEquiv)}
        )
    }

    def convertSolvedConstraintWithEquivalence(constraint: SolvedLinearConstraint, varsEquiv: List[List[DimIndex]]) : SolvedLinearConstraint = {
        return SolvedLinearConstraint(
            constraint.lhs_coefficient,
            getEquivalenceElementOfVar(constraint.lhs_varindex, varsEquiv),
            constraint.relation,
            constraint.rhs_coefficients
        )
    }

    def convertLinearConstraintWithEquivalence(constraint: LinearConstraint, varsEquiv: List[List[DimIndex]]) : LinearConstraint = {
        var allIndices = List.range(0, constraint.coefficients.size, 1)

        var retLinearArray = constraint.coefficients.clone()
        allIndices.foreach{
            x => retLinearArray = convertLinearArrayWithEquivalence(x.asInstanceOf[DimIndex], retLinearArray, varsEquiv)
        }
        return LinearConstraint(
            constraint.relation,
            retLinearArray.clone()
        )
    }

    def convertLinearArrayWithEquivalence(d: DimIndex, linearArray: LinearArray, varsEquiv: List[List[DimIndex]]) : LinearArray = {
        var equivClass = getEquivalenceClassOfVar(d, varsEquiv)
        var equivElem = getEquivalenceElementOfVar(d, varsEquiv)

        ifDebug {
            // println("convertLinearArrayWithEquivalence")
            // println(d)
            // println(equivElem)
            // println(equivClass)
        }

        var remEquivClass = equivClass.filter{ x => x != equivElem}

        var retLinearArray = new LinearArray(linearArray)
        remEquivClass.foreach{
            (x: DimIndex) => retLinearArray = replaceVarsInLinearArray(x, equivElem, retLinearArray)
        }

        return retLinearArray
    }

    def replaceVarsInLinearArray(d: DimIndex, e: DimIndex, linearArray: LinearArray) : LinearArray = {
        var retLinearArray = linearArray.clone

        var dCoef = retLinearArray.lins(d.toInt)
        var eCoef = retLinearArray.lins(e.toInt)
        if (eCoef == 0) {
            retLinearArray.lins(e.toInt) = dCoef
            retLinearArray.lins(d.toInt) = 0

            return retLinearArray
        }

        return retLinearArray
    }

    def convertBimapWithEquivalence(bimap: Bimap[Label.Label, DimIndex], varsEquiv: List[List[DimIndex]]) : Bimap[Label.Label, DimIndex] = {
        var forw = bimap.forward.map{ case (key,value) => (key, getEquivalenceElementOfVar(value, varsEquiv))}
        var back  = bimap.backward.map { case (key,value) => (getEquivalenceElementOfVar(key, varsEquiv), value)}

        return new Bimap(forw, back)
    }

    def combineEdgeSpec(edgeSpecs: List[LoopEdgeSpec]) : LoopEdgeSpec = {
        var allLoopVarRelations: List[SolvedLinearConstraint] = edgeSpecs.map(_.loopvarRelations).foldLeft(List[SolvedLinearConstraint]())((b,a) => b ::: a)
        var allOtherRelations: List[LinearConstraint] = edgeSpecs.map(_.otherRelations).foldLeft(List[LinearConstraint]())((b,a) => b ::: a)

        if (edgeSpecs.length >= 1) {
            return LoopEdgeSpec(
                edgeSpecs(0).loopvar,
                allLoopVarRelations,
                allOtherRelations
            )
        } else {
            return null
        }
    }

    def combineTransSpec(transSpecs: List[LoopTransSpec]) : LoopTransSpec = {
        var allLoopVarRelations: List[SolvedLinearConstraint] = transSpecs.map(_.loopvarRelations).foldLeft(List[SolvedLinearConstraint]())((b,a) => b ::: a)
        var allOtherRelations: List[LinearConstraint] = transSpecs.map(_.otherRelations).foldLeft(List[LinearConstraint]())((b,a) => b ::: a)

        if (transSpecs.length >= 1) {
            return LoopTransSpec(
                transSpecs(0).loopvarOld,
                transSpecs(0).loopvarNew,
                allLoopVarRelations,
                allOtherRelations
            )
        } else {
            return null
        }
    }

    def createEquivalenceMapFromSummary(loopSummary: LoopSummary, varsEquiv: List[List[DimIndex]]) : (Map[DimIndex, LoopEdgeSpec], Map[DimIndex, LoopTransSpec], Map[DimIndex, LoopEdgeSpec]) = {
        ifDebug {
            // println("createEquivalenceMapFromSummary")
            // println(loopSummary)
        }
        var preCondMap = loopSummary.pre.map(
            cond =>
                getEquivalenceElementOfVar(cond.loopvar, varsEquiv) -> convertPrePostConditionWithEquivalence(cond, varsEquiv)
            ).groupBy(_._1).mapValues(_.map(_._2)).mapValues(combineEdgeSpec(_))
        var transCondMap = loopSummary.trans.map(
            cond =>
                getEquivalenceElementOfVar(cond.loopvarOld, varsEquiv) -> convertTransConditionWithEquivalence(cond, varsEquiv)
            ).groupBy(_._1).mapValues(_.map(_._2)).mapValues(combineTransSpec(_))
        var postCondMap = loopSummary.post.map(
            cond =>
                getEquivalenceElementOfVar(cond.loopvar, varsEquiv) -> convertPrePostConditionWithEquivalence(cond, varsEquiv)
            ).groupBy(_._1).mapValues(_.map(_._2)).mapValues(combineEdgeSpec(_))

        return (preCondMap, transCondMap, postCondMap)
    }
}
