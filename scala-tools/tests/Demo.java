import java.util.ArrayList;
import java.util.List;

class Demo {

  /******************* Law enforcement database **************/
  
  public static class RestrictedAccessException extends Exception {
  }
  
  // public static void ledb(List<Integer> range, ArrayList<Integer> restricted) {
  //   /* 245 */         int ind = 0;
  //   /* 246 */         while (ind < range.size()) {
  //     /*     */           try {
  //       /* 248 */             Integer nextkey = (Integer)range.get(ind);
  //       /*     */ 
  //       /* 257 */             if (!restricted.contains(nextkey.intValue()))
  //         /*     */             {
  //         System.out.println("UNRESTRICTED: " + nextkey);
  //         /* 271 */               ind++;
  //         /*     */             }
  //       /*     */             else
  //         /*     */             {
  //         /* 277 */               System.out.println("RESTRICTED:" + nextkey);
  //         /* 278 */               throw new RestrictedAccessException();
  //         /*     */             }

  //       /*     */           }
  //     /*     */           catch (RestrictedAccessException rae)
  //       /*     */           {
  //       /* 284 */             Integer getkey = (Integer)range.get(ind);
  //       /*     */             
  //       /* 286 */             while ((restricted.contains(getkey.intValue())) && (ind < range.size()))
  //         /*     */             {
  //         /*     */ 
  //         /* 333 */               ind++;
  //         /* 334 */               if (ind < range.size()) {
  //           System.out.println("STRING OF RESTRICTED: " + ind);
  //           /* 335 */                 getkey = (Integer)range.get(ind);
  //           /*     */               }
  //         /*     */             }
  //       /*     */           }
  //     /*     */         }
  //   /*     */         
  // }

  public static boolean contains(int[] a, int v) {
    for (int i = 0; i < a.length; i++) {
      if (v == a[i]) return true;
    }
    return false;
  }

  public static int ledb1(int[] range, int[] restricted) {
    int ind = 0;
    int len = range.length;
    while (ind < len) {
      int nextkey = range[ind];
 
      if (!contains(restricted, nextkey)) {
        ind++;
      } else {
        int getkey = range[ind];
             
        while ((contains(restricted, getkey)) && (ind < len)) {
 
          ind++;
          if (ind < len) {
            getkey = range[ind];
          }
        }
      }
    }
    return 0;
  }

  public static int ledb2(int[] range, int[] restricted) {
    int ind = 0;
    while (ind < range.length) {
      try {
        int nextkey = range[ind];
 
        if (!contains(restricted, nextkey)) {
          ind++;
        } else {
          throw new RestrictedAccessException();
        }
      } catch (RestrictedAccessException e) {
        int getkey = range[ind];
             
        while ((contains(restricted, getkey)) && (ind < range.length)) {
 
          ind++;
          if (ind < range.length) {
            getkey = range[ind];
          }
        }
      }
    }
    
    return 0;
  }

  public static int inner(int[] range, int[] restricted, int getkey) {
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


  /********************** Blogger ************************/
  
  // public boolean verify(String string)
  // {
  //   LinkedList<Tuple<Integer, URIElement>> tuples = new LinkedList();
  //   tuples.push(new Tuple(Integer.valueOf(0), this.verifierElements));
  //   Tuple<Integer, URIElement> peek;
  //   while ((!tuples.isEmpty()) && ((peek = (Tuple)tuples.pop()) != null)) {
  //     if ((((URIElement)peek.second).isFinal) && (((Integer)peek.first).intValue() == string.length())) {
  //       return true;
  //     }
       
  //     if (string.length() > ((Integer)peek.first).intValue()) {
  //       for (URIElement URIElement : ((URIElement)peek.second).get(string.charAt(((Integer)peek.first).intValue()))) {
  //         tuples.push(new Tuple(Integer.valueOf(((Integer)peek.first).intValue() + 1), URIElement));
  //       }
  //     }
       
  //     for (URIElement child : ((URIElement)peek.second).get(-1)) {
  //       tuples.push(new Tuple(peek.first, child));
  //     }
  //   }
     
  //   return false;
  // }

  public static int runtimeException1(int n) {
    int x = 10 / n;
    return x;
  }
  
  public static void runtimeException2(int n) {
    int x = 10 / n;
  }
  
  public static void runtimeException3(int n) {
    int x = 10 / 1;
  }
}
