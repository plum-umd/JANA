/*    */ package com.cyberpointllc.stac.delimsearch;
/*    */ 
/*    */ public class DelimSearch
/*    */ {
           //                       imm           imm
/*    */   public int search(String text, String delims) {
             int pc = 0;

/*  6 */     for (int i = 0; i < text.length(); i++) {
               // pc++;

               // 0 <= i <= text.length()
/*  7 */       for (int j = 0; j < delims.length(); j++)
/*    */       {
                 // pc++;

                 // 0 <= j <= delims.length()
/*  9 */         if (text.charAt(i) == delims.charAt(j)) {
/* 10 */           return i;
/*    */         }
/*    */       }

               pc = pc + delims.length();
/*    */     }

             //pc = pc + text.length();

             //pc = text.length()*delims.lenght();
/*    */     
/* 15 */     return text.length();
/*    */   }
/*    */ }


/* Location:              /home/andrew/code/soucis/information/sample_4/challenge_program/delimsearch_1.jar!/com/cyberpointllc/stac/delimsearch/DelimSearch.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */
