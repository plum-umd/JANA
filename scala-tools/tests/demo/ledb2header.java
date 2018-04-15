/** ledb2 - the nested loops without exceptions */

class ledb2 {

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
 
      if (!contains(restricted, nextkey)) {
        ind++;
      } else {
        int getkey = range[ind];
 
	boolean b = contains(restricted, getkey);
            
        while (b && (ind < len)) {
 
          ind++;
          if (ind < len) {
            getkey = range[ind];
          }
	  b = contains(restricted, getkey);
        }
      }
    }
    return 0;
  }

  public static int ledb_noconditions(int x) {
    int i = 0;
    while (i < x) {
      if (i > 10) {
        i++;
      } else {
        while (i < 20) {
          i++;
        }
      }
    }
    return 0;
  }

  public static int ledb_noconditions_sym(int x, int y) {
    int i = 0;
    while (i < x) {
      if (i > y) {
        i++;
      } else {
        while (i < x) {
          i++;
        }
      }
    }
    return 0;
  }
}
