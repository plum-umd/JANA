import Config._
import Core._

import scalaz._
import Scalaz._

import collection.JavaConversions._

object Joiner {
  type Machine = AbstractMachine
  val Machine = AbstractMachine

  // A joiner used for joining machines at backedges, allows widening.
  def joinerBack
    (history_ms: List[Machine])
    (current_m: Option[Machine])
    (input_next_m: Machine)
    (implicit conf: AInterpConfig): (Option[Machine]) = {

    val num_preds = input_next_m.cfg.getPredNodes(input_next_m.blockTop).size
    val visit_max = ConfigManager.widen * conf.joinAfterNumIterations * num_preds * 2 + 1
    val history_count = history_ms.length + 1
    val next_m = current_m match {
      case None => input_next_m
      case Some(prior_m) => input_next_m.join(prior_m)
    }
    if (history_ms.isEmpty) {
      Some(next_m)

    }  else if (history_count >= ConfigManager.widen) {
      if (history_ms.any(next_m.widen_leq(_))) {
        None
      } else {
        val older_m = Machine.join(history_ms)
        val labels = next_m.diff(older_m)
        val newer_m =
          if(!labels.isEmpty){
            older_m.join(next_m.filter({ l => !labels.contains(l) }))
          } else {
            older_m.join(next_m)
          }
        
        val wider_m = older_m.widen(newer_m)
        Some(wider_m)
      }

    } else if (history_ms.any(next_m.leq(_))) {
      None
    } else {
      Some(next_m)

    }
  }

  // Joiner for non-back edges, does not allow widening.
  def joinerForward
    (history_ms: List[Machine])
    (current_m: Option[Machine])
    (input_next_m: Machine)
    (implicit conf: AInterpConfig): (Option[Machine]) = {

    val num_preds = input_next_m.cfg.getPredNodes(input_next_m.blockTop).size
    val visit_max = ConfigManager.widen * conf.joinAfterNumIterations * num_preds * 2 + 1
    val history_count = history_ms.length + 1
    val next_m = current_m match {
      case None => input_next_m
      case Some(prior_m) => input_next_m.join(prior_m)
    }
    if (history_ms.isEmpty) {
      Some(next_m)
//    } else if (history_ms.any(next_m.leq(_))) {
    } else if (next_m.leq(history_ms.head)) {
      None
    } else {
      Some(next_m)
    }
  }


}
