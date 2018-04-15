import Core._

import java.io._
import collection.JavaConversions._

object OutPrint {

  def printResult(time: Long) : Unit = {
  
    val out = new PrintWriter(new BufferedWriter(new FileWriter("./files/" + ConfigManager.outName + "/" +ConfigManager.outName + ".txt", true)));
    var config = ConfigManager.isHybrid
    
    if(ConfigManager.checkArrayBound) {
      val bound_results = Clients.bound_check
      val (success, fail, notExist) = bound_results.foldLeft((0, 0, 0)) {
        case ((succ, f, not), (key, result)) =>
          if(result.equals("SUCCESS")) {
            (succ + 1, f, not)
          } else if(result.equals("FAIL")) {
            (succ, f+1, not)
          } else if(result.equals("NotExist")) {
            (succ, f, not+1)
          } else {
            throw NotImplemented("case not possible")
          }
      }
      val total = success + fail + notExist
      val percent = success.toDouble/total.toDouble
      
      out.write("array bound\t" + success + "\t" + (fail + notExist) + "\t" + f"$percent%1.2f" + "\t")
    }
    
    if(ConfigManager.checkDivideByZero) {
      val divided_by_zero_results = Clients.divided_by_zero_check
      val (success, fail) = divided_by_zero_results.foldLeft((0, 0)) {
        case ((succ, f), (key, result)) =>
          if(result.equals("SUCCESS")) {
            (succ + 1, f)
          } else if(result.equals("FAIL")) {
            (succ, f+1)
          } else {
            throw NotImplemented("case not possible")
          }
      }
      val total = success + fail
      val percent = success.toDouble/total.toDouble
      
      out.write("divide by zero\t" + success + "\t" + fail + "\t" + f"$percent%1.2f" + "\t")
    }
    
    if(ConfigManager.checkFieldAccess) {
      val field_access_results = Clients.field_access_check
      val (success, fail) = field_access_results.foldLeft((0, 0)) {
        case ((succ, f), (key, result)) =>
          if(result.equals("SUCCESS")) {
            (succ + 1, f)
          } else if(result.equals("FAIL")) {
            (succ, f+1)
          } else {
            throw NotImplemented("case not possible")
          }
      }
      val total = success + fail
      val percent = success.toDouble/total.toDouble
      
      out.write("field access\t" + success + "\t" + fail + "\t" + f"$percent%1.2f" + "\t")
    }
    
    if(ConfigManager.checkReturn) {
      val return_results = Clients.return_check
      val (success, fail) = return_results.foldLeft((0, 0)) {
        case ((succ, f), (key, result)) =>
          if(result.equals("SUCCESS")) {
            (succ + 1, f)
          } else if(result.equals("FAIL")) {
            (succ, f+1)
          } else {
            throw NotImplemented("case not possible")
          }
      }
      val total = success + fail
      val percent = success.toDouble/total.toDouble
      
      out.write("return check\t" + success + "\t" + fail + "\t" + f"$percent%1.2f" + "\t")
    }
    
    out.write("reachable_methods:\t" + AInterp.reachable_methods.size + "\t")
    
    out.write("numerical analysis time(ms):\t" + time)
    out.close()
    
    val out_1 = new PrintWriter(new BufferedWriter(new FileWriter("./files/"+ ConfigManager.outName + "/"+ ConfigManager.index + "-rm-" + ConfigManager.outName+ ".txt", true)));
    AInterp.reachable_methods.foreach { x => out_1.write(x + "\n") }
    out_1.close()
    
    if(ConfigManager.checkArrayBound) {
      val out_2 = new PrintWriter(new BufferedWriter(new FileWriter("./files/"+ ConfigManager.outName + "/"+ ConfigManager.index + "-array-" + ConfigManager.outName+ ".txt", true)));
      Clients.bound_check.keySet.foreach { key => out_2.write(key + "\t" + Clients.bound_check.get(key).get + "\n") }
      out_2.close()
    }
    
    if(ConfigManager.checkDivideByZero) {
      val out_3 = new PrintWriter(new BufferedWriter(new FileWriter("./files/"+ ConfigManager.outName + "/"+ ConfigManager.index + "-divide-" + ConfigManager.outName+ ".txt", true)));
      Clients.divided_by_zero_check.keySet.foreach { key => out_3.write(key + "\t" + Clients.divided_by_zero_check.get(key).get + "\n") }
      out_3.close()
    }
    
    if(ConfigManager.checkFieldAccess) {
      val out_4 = new PrintWriter(new BufferedWriter(new FileWriter("./files/"+ ConfigManager.outName + "/"+ ConfigManager.index + "-field-" + ConfigManager.outName+ ".txt", true)));
      Clients.field_access_check.keySet.foreach { key => out_4.write(key + "\t" + Clients.field_access_check.get(key).get + "\n") }
      out_4.close()
    }
  
  }
}
