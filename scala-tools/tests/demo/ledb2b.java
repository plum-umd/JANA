/** ledb2b - nested loops with a flag for contains */

class ledb2b {
  public static boolean contains(int[] a, int v) {
    for (int i = 0; i < a.length; i++) {
      if (v == a[i]) return true;
    }
    return false;
  }

  public static int ledb(int[] range, int[] restricted) {
    int ind = 0;
    int len = range.length;
    while (ind < len) {
      int nextkey = range[ind];
      boolean b = contains(restricted, nextkey);
 
      if (!b) {
        ind++;
      } else {
        int getkey = range[ind];
             
        while (b && (ind < len)) {
 
          ind++;
          if (ind < len) {
            getkey = range[ind];
          }
        }
      }
    }
    return 0;
  }
}
