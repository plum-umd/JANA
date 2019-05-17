public class DelimSearch {

  // nested loops, disjunctions

  public int search(byte[] text, byte[] delims) {

    for(int i = 0; i < text.length; i++) {
      for(int j = 0; j < delims.length; j++) {
        if(text[i] == delims[j]) {
          return i;
        }
      }
    }
    //            text.length == delims.length ==> n^2
    // K <= pc <= text.length*delims.length + J
    return text.length;
  }
    
    public int test() {
        byte[] text = new byte[4];
        byte[] delims = new byte[8];
        return search(text, delims); //runtime: ret = 0; anticipated: 0 <= ret <= text.length
        //        return search(4, 8, 0, 0); //runtime: ret = 0; anticipated: ret = 0
    }
    
    public int search(int a, int b, int c, int d) {
        
        for(int i = 0; i < a; i++) {
            
            for(int j = 0; j < b; j++) {
                
                if(c == d) {
                    return i;
                }
                
            }
        }
        
        return a;
    }
}
