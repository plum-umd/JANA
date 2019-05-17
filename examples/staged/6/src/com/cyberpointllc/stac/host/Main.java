/*    */ package com.cyberpointllc.stac.host;
/*    */ 
/*    */ import com.cyberpointllc.stac.delimsearch.DelimSearch;
/*    */ import java.io.IOException;
/*    */ import java.io.PrintStream;
/*    */ import java.nio.charset.Charset;
/*    */ import java.nio.file.Files;
/*    */ import java.nio.file.Paths;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class Main
/*    */ {
/*    */   public static String readFile(String path, Charset encoding)
/*    */     throws IOException
/*    */   {
/* 17 */     byte[] encoded = Files.readAllBytes(Paths.get(path, new String[0]));
/* 18 */     return new String(encoded, encoding);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static void main(String[] args)
/*    */   {
/*    */     try
/*    */     {
/* 29 */       String text = readFile(args[0], Charset.defaultCharset()).trim();
/* 30 */       String delims = readFile(args[1], Charset.defaultCharset()).trim();
/*    */       
/* 32 */       DelimSearch dsearch = new DelimSearch();
/* 33 */       int index = dsearch.search(text, delims);
               // K <= pc <= text.length()*delims.length()
               // 0 <= index <= text.length()
/* 34 */       if (index < text.length()) {
/* 35 */         System.out.println("Delimiter [" + text.charAt(index) + "] found at " + index);
/*    */       } else {
/* 37 */         System.out.println("No delimiters found");
/*    */       }
/*    */     } catch (IOException ioe) {
/* 40 */       ioe.printStackTrace();
/*    */     }
/*    */   }
/*    */ }


/* Location:              /home/andrew/code/soucis/information/sample_4/challenge_program/delimsearch_1.jar!/com/cyberpointllc/stac/host/Main.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */
