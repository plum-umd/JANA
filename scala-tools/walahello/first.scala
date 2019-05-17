// remember that 
// type N = CGNode // call graph nodes
// type PutI = SSAPutInstruction
// type LocalP = LocalPointerKey

import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector
import com.ibm.wala.util.graph.traverse.DFS

import com.ibm.wala.ssa.IR

import com.ibm.wala.shrikeBT.IBinaryOpInstruction
import com.ibm.wala.shrikeBT.IShiftInstruction
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction

import com.ibm.wala.types.TypeReference

import edu.illinois.wala.ipa.callgraph.FlexibleCallGraphBuilder
import edu.illinois.wala.ipa.callgraph.propagation.P

// convenience object that activates all implicit converters
import edu.illinois.wala.Facade._

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map, HashMap, Stack=>BStack, ListBuffer, Set}
import scala.collection.immutable.{List}
import scala.sys.exit

object Test extends App {

  //a current actiation record
  class Frame(i:IR,c:CFG) {
    val cfg : CFG = c
    val ir : IR = i
    var regs : Map[Int, Value] = new HashMap[Int, Value]
    var savedPC : Int = 0
    var returnVal : Option[Value] = None
  }

  class Heap() { 
    var mem: Map[Reference, Set[Value]] = new HashMap[Reference, Set[Value]]
    
    var lowaddr: Int = 0

    //concrete allocator
    def allocate(v:Value) : Reference = { 
      val curaddr = this.lowaddr
      this.lowaddr = this.lowaddr + 1 

      val r = new Reference(curaddr.toString)

      this.mem += (r -> Set(v))

      return r
    }
  }

  //the concrete machine
  class Machine() {
    //the heap
    var heap: Heap = new Heap()

    //some abstractions of the heap

    //the stack, in terms of procedures called
    var stack: BStack[Frame] = new BStack[Frame]

    //last value produced by the machine
    var rvalue : Value = new Reference("null")
  }

  //need to get the next non-null instruction specified by
  //the index
  def getinst(index:Int, cfg:CFG) : Tuple2[I,Int] = {
    val instructions = cfg.getInstructions()
    var i = index
  
    while(instructions(i) == null) { i = i + 1 }

    return (instructions(i),i)
  }

  //get a value from the frame
  //value could be a constant! have to look up from symbol table
  def readvalue(idx:Int, f:Frame) : Option[Value] = { 
    var v = try {
      Some(f.regs(idx))
    } catch {
      case _:Throwable => None
    }

    if(v == None) {
      val symboltable = f.ir.getSymbolTable()
      //miss in the local value table, what about the 
      //constant table? 
      if(symboltable.isConstant(idx)) {
        //euuuugh...
        if(symboltable.isIntegerConstant(idx)) {
          v = Some(CIV(symboltable.getIntValue(idx)))
        }
      }
    }

    return v
  }

  //put a value in the frame
  def writevalue(idx:Int, f:Frame, v:Value) : Unit = {
    f.regs += (idx -> v)
    return
  }

  def createvalue(ty:TypeReference,newparms:List[Int]) : Value = { 
    //case split on the type of value that we are creating 
    println(ty.toString())

    return new Object(List[Value]())
  }

  //takes an instruction and apply the instruction to the machine
  //XXX we want to make this function abstract and parameterized around
  //    some different things:
  //      - the machine type
  //      - the return PC type
  def iinterpret(pc:Int, inst:I, m:Machine) : Tuple2[Option[Int],Machine] = {
    println(inst.toString());
    val frame = m.stack.top
    val nextinst : Option[Int] = inst match {
      case i:NewI => 
        //new could have some parameters, what are they
        var k = 0
        var tyl = new ListBuffer[Int]()
        while(k < i.getNumberOfUses()) {
          tyl += k
          k = k + 1
        }
        val newv = createvalue(i.getConcreteType(), tyl.toList)

        //allocate a value for this value
        val ref = m.heap.allocate(newv)

        writevalue(i.getDef(), frame, ref)
        None
        //Some(pc+1)

      case i:ArrayLengthI =>
        //we will read a reference to an array!
        val v = readvalue(i.getUse(0), frame)
        val arrv = v match {
          case Some(a:Reference) => 
            //look up a in the heap
            try {
              val fromheap = m.heap.mem(a)
              if(fromheap.size == 1) { 
                fromheap.head match {
                  case Array(l) => l
                  case _ => 
                    println("E: did not read value of type array")
                    exit()
                }
              } else {
                if(fromheap.size > 1) {
                  println("E: nondestructive heap update in concrete mode!")
                  exit()
                } else {
                  println("E: heap read miss")
                  exit()
                }
              }
            } catch { 
              case _:Throwable => 
                println("invalid array reference")
                exit()
            }
          case _ => 
            println("did not read value of type reference")
            exit()
        }

        writevalue(i.getDef(), frame, arrv)
        Some(pc+1)

      case i:ArrayStoreI =>
        None

      case i:ArrayLoadI =>
        None

      case i:GotoI =>
        //our PC changes unconditionally
        Some(i.getTarget())

      case i:BranchI =>
        //read some values
        val v1 = readvalue(i.getUse(0), frame)
        val v2 = readvalue(i.getUse(1), frame)

        //XXX operations on types of branches
        val (i1,i2) = (v1,v2) match {
          case (Some(a:CIV),Some(b:CIV)) => (a,b)
          case _ => 
            println("invalid values")
            exit()
        }

        //perform some comparison
        val res = i.getOperator() match {
          case IConditionalBranchInstruction.Operator.EQ =>
            i1.eq(i2)
          case IConditionalBranchInstruction.Operator.NE =>
            i1.ne(i2)
          case IConditionalBranchInstruction.Operator.LT =>
            i1.lt(i2)
          case IConditionalBranchInstruction.Operator.LE =>
            i1.le(i2)
          case IConditionalBranchInstruction.Operator.GT =>
            i1.gt(i2)
          case IConditionalBranchInstruction.Operator.GE =>
            i1.ge(i2)
        }

        //find the next PC, either + 1 or target (in concrete semantics)
        res match { 
          case CBV(true) => Some(i.getTarget())
          case CBV(false) => Some(pc+1)
        }

      case i:ReturnI =>
        //see if we have a value that we are producing
        if(i.getNumberOfUses() == 1) {
          readvalue(i.getUse(0), frame) match {
            case Some(i:Value) =>
              frame.returnVal = Some(i)
            case _ => 
              //error case, we are trying to return a nonexistant value
              println("should have val but don't!")
              exit()
          }
        }

        //our current control flow stops
        None

      case i:PutI => 
        //write a value into a field of an object

        //the PC advances
        Some(pc + 1)

      case i:GetI =>
        //read a value from a field of an object

        //the PC advances
        Some(pc + 1)

      case i:BinopI => 
        //read out the values that are operands to binop
        val v1 = readvalue(i.getUse(0), frame)
        val v2 = readvalue(i.getUse(1), frame)

        //check that the values are both IntegerValues, abort otherwise
        val (iv1,iv2) = (v1,v2) match {
          case (Some(a:CIV),Some(b:CIV)) => (a,b)
          case _ => 
            //anything else, abort the program
            println("invalid operands to binop")
            exit()
        }

        //switch on the operator
        val result = i.getOperator() match {
          case IBinaryOpInstruction.Operator.ADD => 
            iv1.add(iv2)
          case IBinaryOpInstruction.Operator.SUB => 
            iv1.sub(iv2)
          case IBinaryOpInstruction.Operator.MUL => 
            iv1.mul(iv2)
          case IBinaryOpInstruction.Operator.DIV => 
            iv1.div(iv2)
          case IBinaryOpInstruction.Operator.REM => 
            iv1.rem(iv2)
          case IBinaryOpInstruction.Operator.AND => 
            iv1.and(iv2)
          case IBinaryOpInstruction.Operator.OR => 
            iv1.or(iv2)
          case IBinaryOpInstruction.Operator.XOR => 
            iv1.xor(iv2)
        }

        //store the new value in the stack frame
        writevalue(i.getDef(), frame, result)

        //advance the pc
        Some(pc + 1)

      case _ => 
        println("unhandled instruction!")
        None
    }

    return new Tuple2[Option[Int],Machine](nextinst,m)
  }

  //iteratively applies iinterpret to a machine until there is nothing
  //left to do
  def concreteinterpret(m:Machine) : Machine = { 
    var pc = 0
   
    while(m.stack.size > 0) {
      var f = m.stack.top
      val i = getinst(pc, f.cfg)
      pc = i._2
      var pcnew = iinterpret(pc, i._1, m)
      
      pc = pcnew._1 match { 
        case Some(x) => 
          //easy, x is the new pc
          x
        case None => 
          //we stopped executing, so set the pc to the saved
          //pc on the stack frame
          val oldframe = m.stack.pop()
          if(m.stack.size > 0) {
            val savedf = m.stack.top
            savedf.savedPC
          } else {
            oldframe.returnVal match {
              case Some(v:Value) => 
                m.rvalue = v
              case None => 
                ()
            }
            //doesn't matter
            0
          }
      }
    }

    return m
  }

  implicit val config = CommandConfig.config("../tests", "Test.foo.*", "java/awt/.* javax/.* com/sun/.* com/apple/.* sun/.* apple/.*  org/eclipse/.*  apache/.*")

  // creates a new pointer analysis with a special context selector
  // implicitly uses the above config file
  val pa = new FlexibleCallGraphBuilder() {
    override def cs = new nCFAContextSelector(2, new ContextInsensitiveSelector());
  }

  // make cg, heap, etc. available in scope
  import pa._

  //get cfg for procedure we want to start with, for now, main
  val entrypoints = pa.cg.getEntrypointNodes()
  for(proc <- entrypoints) { 
    println("interpreting %s", proc.toString())

    val ir = proc.getIR()
    println(ir.toString())

    val c = ir.getControlFlowGraph()
    //make a machine
    var m = new Machine()
    //make a stack frame 
    var f = new Frame(ir,c)

    //add two integer parameters as arguments
    //f.regs = f.regs + (1 -> IntegerValue(8)) + (2 -> IntegerValue(12))
    writevalue(1, f, CIV(8))
    writevalue(2, f, CIV(12))
    m.stack.push(f)

    //call the interpreter
    val mprime = concreteinterpret(m)

    //print the results
    println("interpreter finished with result ", mprime.rvalue)
  } 

  //for( n <- proc) {
  //  println(n.toString())
  //}

  // more verbose to each understanding
  //val startNodes = pa.cg filter { n: N => n.m.name == "main" }
  //val reachableNodes = DFS.getReachableNodes(pa.cg, startNodes)
  /*val foo = reachableNodes flatMap { n =>
    n.instructions collect {
      case i: PutI =>
        val p: LocalP = P(n, i.v)
        val variableNames: Iterable[String] = p.variableNames()
        val fieldName: F = i.f.get
        (fieldName, variableNames)
    }
  }*/

  // and a 3-liner doing exactly the name thing
  /*DFS
    .getReachableNodes(cg, cg filter { _.m.name == "main" })
   .flatMap { n => n.instructions collect { case i: PutI => (i.f.get, P(n, i.v).variableNames()) } }*/  
}
