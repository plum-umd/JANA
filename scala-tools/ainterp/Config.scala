object Config {
  
  case class AInterpConfig(
      val widenAfterNumIterations: Int ,
      val joinAfterNumIterations: Int, // PRESENTLY UNUSED
      val interProc: Boolean, //TRUE: inter-procedural analysis; FALSE: intra-procedural analysis
      val bottomUp: Boolean, //TRUE: bottom-up analysis; FALSE: top-down analysis
      val collapse: Boolean, //TRUE: collapse the points-to set into one object (naive implementation and unsound)
      val joinPolicy: Option[String => Boolean]
  ) {
    
    def this(interProc: Boolean, bottomUp: Boolean) = this(
      widenAfterNumIterations = 3,
      joinAfterNumIterations = 2,
      interProc = interProc,
      bottomUp = bottomUp,
      collapse = false,
      joinPolicy = Some(Label.all)
    )
        
    def this() = this(
      widenAfterNumIterations = 3,
      joinAfterNumIterations = 2,
      interProc = true,
      bottomUp = true,
      collapse = false,
      joinPolicy = Some(Label.all)
    )
    
  }
  
  implicit object defaultConfig extends AInterpConfig { }

}
