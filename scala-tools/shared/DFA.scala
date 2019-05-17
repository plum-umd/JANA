object DFA {

  // Automaton
  trait Automaton[Q, Σ]{
    //def accept(input: Seq[Σ]): (Boolean, Q)
    def getStartState() : Option[Q] = { return None }
  }

  // Deterministic Finite Automata
  // σ: transition function
  // q0: initial state
  // f: member ship function of accepted states
  case class DFA[Q, Σ](σ: (Q, Σ) => Q, q0: Q, f: Q => Boolean) extends Automaton[Q, Σ]{
    //def accept(input: Seq[Σ]): (Boolean, Q) = {
    //return (false, q0)
    //}

    override def getStartState : Option[Q] = { return None }
    //def accept(input: Seq[Σ]): (Boolean, Q) = _accept(input)(q0)
  }


}
