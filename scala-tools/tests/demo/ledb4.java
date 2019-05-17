/** ledb4 - nested loops, exceptions, and java lib data structures */

import java.util.ArrayList;
import java.util.List;

class ledb4 {

  /******************* Law enforcement database **************/
  
  public static class RestrictedAccessException extends Exception {
  }
  
  public static void ledb(List<Integer> range, ArrayList<Integer> restricted) {
    /* 245 */         int ind = 0;
    /* 246 */         while (ind < range.size()) {
      /*     */           try {
        /* 248 */             Integer nextkey = (Integer)range.get(ind);
        /*     */ 
        /* 257 */             if (!restricted.contains(nextkey.intValue()))
          /*     */             {
          System.out.println("UNRESTRICTED: " + nextkey);
          /* 271 */               ind++;
          /*     */             }
        /*     */             else
          /*     */             {
          /* 277 */               System.out.println("RESTRICTED:" + nextkey);
          /* 278 */               throw new RestrictedAccessException();
          /*     */             }

        /*     */           }
      /*     */           catch (RestrictedAccessException rae)
        /*     */           {
        /* 284 */             Integer getkey = (Integer)range.get(ind);
        /*     */             
        /* 286 */             while ((restricted.contains(getkey.intValue())) && (ind < range.size()))
          /*     */             {
          /*     */ 
          /* 333 */               ind++;
          /* 334 */               if (ind < range.size()) {
            System.out.println("STRING OF RESTRICTED: " + ind);
            /* 335 */                 getkey = (Integer)range.get(ind);
            /*     */               }
          /*     */             }
        /*     */           }
      /*     */         }
    /*     */         
  }

  // public static boolean contains(int[] a, int v) {
  //   for (int i = 0; i < a.length; i++) {
  //     if (v == a[i]) return true;
  //   }
  //   return false;
  // }
}
