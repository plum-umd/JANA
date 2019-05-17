public class DelimSearch2 {
  //methods, disjunction 

  public boolean check(byte a, byte d1, byte d2, byte d3, byte d4) {
    if(a == d1) {
      return true;
    }
    if(a == d2) {
      return true;
    }
    if(a == d3) {
      return true;
    }
    if(a == d4) {
      return true;
    }

    return false;
  }
  // attack because assumption is delims is a set, but it is not
  //
  public int search(byte t1, byte t2, byte t3, byte t4, byte d1, byte d2, byte d3, byte d4) {

    if(check(t1, d1, d2, d3, d4) == true) {
      return 1;
    }
    if(check(t2, d1, d2, d3, d4) == true) {
      return 2;
    }
    if(check(t3, d1, d2, d3, d4) == true) {
      return 3;
    }
    if(check(t4, d1, d2, d3, d4) == true) {
      return 4;
    }

    return 5;
  }
    
    public int test() {
        //        byte t1 = 'a';
        //        byte t2 = 'b';
        //        byte t3 = 'c';
        //        byte t4 = 'd';
        //        byte d1 = 'e';
        //        byte d2 = 'f';
        //        byte d3 = 'g';
        //        byte d4 = 'h';
        //        return search(t1, t2, t3, t4, d1, d2, d3, d4); //anticipated: ret =5; actual: 0 <= ret <=5
        byte t1 = 'a';
        byte t2 = 'a';
        byte t3 = 'a';
        byte t4 = 'a';
        byte d1 = 'a';
        byte d2 = 'a';
        byte d3 = 'a';
        byte d4 = 'a';
        return search(t1, t2, t3, t4, d1, d2, d3, d4); //anticipated: ret = 1; actual: ret = 1
        
    }
}
