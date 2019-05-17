import Label._

//The global configuration object that tunes analysis options
object ConfigManager {
  
  //top-down analysis uses global heap labels; bottom-up analysis uses method-based labels
  var globalHeapLabel = false
  def setGlobalHeapLabel(isGlobal: Boolean) = {globalHeapLabel = isGlobal}
  
  var hasSummarizedDimensions = true
  def setHasSummarizedDimensions(summarizedDimensions: Boolean) = {hasSummarizedDimensions = summarizedDimensions}
  
  var hasAccessPath = true
  def setHasAccessPath(accessPath: Boolean) = {hasAccessPath = accessPath}
  
  //join policy. None: always join.
  var joinPolicy: Option[String => Boolean] = None
  def setJoinPolicy(policy: Option[String => Boolean]) = {joinPolicy = policy}
  
  //if the analysis is top-down or bottom-up
  var isTopDown = true
  def setIsTopDown(topdown: Boolean) = {isTopDown = topdown}
  
  var isHybrid = false
  def setIsHybrid(hybrid: Boolean) = {isHybrid = hybrid}
  
  //if an analysis is inter-procedural
  var isInterProc = true
  def setIsInterProc(interproc: Boolean) = {isInterProc = interproc}  
  
  //join or not
  //TODO: may be redundant; use joinPolicy
  var join = true
  def setJoin(b: Boolean) = {join = b}
  
  //check array bounds
  var checkArrayBound = true
  def setCheckArrayBound(b: Boolean) = {checkArrayBound = b}
  
  var outName = "results.txt"
  def setOutName(s: String) = {outName = s}
  
  var index = "0"
  def setIndex(i: String) = {index = i}
  
  //check divide by zero
  var checkDivideByZero = true
  def setCheckDivideByZero(b: Boolean) = {checkDivideByZero = b}
  
  var inline = false
  def setInline(b: Boolean) = {inline = b}
  
  //check return values
  var checkReturn = false
  def setCheckReturn(b: Boolean) = {checkReturn = b}
  
  //check field access
  var checkFieldAccess = true
  def setCheckFieldAccess(b: Boolean) = {checkFieldAccess = b}
  
  //use library model; currently implemented only for bottom-up analysis
  var useLibraryModel = false
  def setUseLibraryModel(b: Boolean) = {useLibraryModel = b}
  
  //unification of points-to set
  var unification = false
  def setUnification(b: Boolean) = {unification = b}
  
  var context: Option[String] = None
  def setContext(c: Option[String]) = {context = c}
  
  var obj_rep: String = "ALLOCATION"
  def setObjRep(s: String) = {obj_rep = s}
  
//  var packing: (Option[String], Option[Integer]) = (None, None)
//  def setPacking(opt: (Option[String], Option[Integer])) = {packing = opt}
  
  var domain: String = "POLY"
  def setDomain(d:String) = {domain = d}
  
  var widen: Integer = 3
  def setWiden(i: Integer) = {widen = i}
  
  var base_domain: Option[String] = None
  def setBaseDomain(base:Option[String]) = {base_domain = base}
  
  var widen_alg: Option[String] = None
  def setWidenAlg(alg:Option[String]) = {widen_alg = alg}
}
