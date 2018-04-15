/** ledb1 - the inner loop */

class ledb1 {
  public static boolean contains(int[] a, int v) {
    for (int i = 0; i < a.length; i++) {
      if (v == a[i]) return true;
    }
    return false;
  }

  public static int ledb(int[] range, int[] restricted, int getkey) {
    int ind = 0;
    int len = range.length;
    while ((contains(restricted, getkey)) && (ind < len)) {
 
      ind++;
      if (ind < len) {
        getkey = range[ind];
      }
    }

    return 0;
  }
}
