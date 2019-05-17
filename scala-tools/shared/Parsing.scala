import scala.util.parsing.combinator._
import scala.util.parsing.combinator.lexical._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.input._

object VidLex extends StdLexical {
  override def token: Parser[Token] = vid | super.token

  case class VID(vid: String) extends Token() {
    val chars = vid
  }

  val vid: Parser[Token] = char('v') ~> rep1(digit) ^^ { chars => VID(chars.mkString("")) }

  def char(c:Char) = elem("", ch => ch==c )
}

trait Parsing[T] extends StandardTokenParsers with PackratParsers {
  type PT[E] = PackratParser[E]

  def getRes[T](r: ParseResult[T]): T = r match {
    case Success(res, _) => res
    case fail: NoSuccess => scala.sys.error(fail.msg)
  }

  def int: PT[Int] = numericLit ^^ { _.toInt }
  lazy val vid: PT[Int] = acceptMatch("vid", {
    case lexical.VID(x) => x.toInt
  })

  def optIter[T](lopts: List[PT[T]]): PT[T] = lopts match {
    case Nil => failure("")
    case a :: Nil => a
    case a :: rest => a ||| optIter[T](rest)
  }

  def logOrNot[E](p: PT[E])(msg: String): PT[E] = {
    if (Core.debug) log(p)(msg) else p
  }

  val parser: PT[T]

  override val lexical = VidLex

  def parseString(instring: String): T = {
    val tokens = new lexical.Scanner(instring)
    getRes(phrase(parser)(new PackratReader(tokens)))
  }

  def castp[T](p: Parsing[T]): PT[T] = p.parser.asInstanceOf[PT[T]]
}
