object SpecialLibHandling {
  
  final val STRING_LENGTH = "java.lang.String.length()I"
  
  final val STRING_CHARAT = "java.lang.String.charAt(I)C"
  
  final val READER_READ = "java.io.BufferedReader.read()I"
  
  final val BUFFER_INDEXOF = "java.lang.StringBuffer.indexOf(Ljava/lang/String;)I"
  
  final val BUFFER_LENGTH = "java.lang.StringBuffer.length()I"
  
  final val LIST_SIZE = "java.util.ArrayList.size()I"
    
  final val LIST_CONTAINS = "java.util.ArrayList.contains(Ljava/lang/Object;)Z"
  
  final val BIG_INT_LENGTH = "java.math.BigInteger.bitLength()I"
  
  final val BIG_INT_VALUEOF = "java.math.BigInteger.valueOf(J)Ljava/math/BigInteger;"
  
  final val BIG_INT_TESTBIT = "java.math.BigInteger.testBit(I)Z"

  final val libs:Set[String] = Set(STRING_LENGTH, STRING_CHARAT, READER_READ, BUFFER_INDEXOF, BUFFER_LENGTH, LIST_SIZE, LIST_CONTAINS, BIG_INT_LENGTH, BIG_INT_VALUEOF, BIG_INT_TESTBIT)

}
