public class E3 {

  public boolean check(int a, int d1, int d2, int d3, int d4) {
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

  public int search(int t1, int t2, int t3, int t4, int d1, int d2, int d3, int d4) {

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
}

