public class E2 {
  //this represents password checking logic on an unrolled
  //array of characters-as-integers, short-circuit evaluation
  //of the if-statement produces an early return on first non-match
  public boolean checkPw(int a, int b, int c, int d, int e) {
    if(a == 1 && b == 2 && c == 3 && d == 4 && e == 5) {
      return true;
    } else {
      return false;
    }
  }

  //here, we thread a counter c through the checking logic and the
  //timing dependence should be visible in the interpreter
  public int checkPwI(int a, int b, int c, int d, int e) {
    int ct = 0;
    if(a == 1) {
      ct++;
      if(b == 2) {
        ct++;
        if(c == 3) {
          ct++;
          if(d == 4) {
            ct++;
            if(e == 5) {
              ct++;
              //assert(ct == 5)
              return ct;
            }
          }
        }
      }
    }

    //assert(ct >= 0 && ct < 5)
    return ct;
  }
}
