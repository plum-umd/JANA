/** ledb2b - nested loops with a flag for contains */

class ledbfixed {
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
        b = contains(restricted, getkey);
             
        while (b && (ind < len)) {
 
          ind++;
          if (ind < len) {
            getkey = range[ind];
            b = contains(restricted, getkey);
          }
        }
      }
    }
    return 0;
  }

  public static int ledbsimple(int[] range, int[] restricted) {
    int ind = 0;
    int len = range.length;
    while (ind < len) {
      int nextkey = range[ind];
      boolean b = contains(restricted, nextkey);
 
      if (!b) {
        ind++;
      } else {
        int getkey = range[ind];
        b = contains(restricted, getkey);
             
        ind++;
        while (b && (ind < len)) {
          getkey = range[ind];
          b = contains(restricted, getkey);
          ind++;
        }
      }
    }
    return 0;
  }
}
