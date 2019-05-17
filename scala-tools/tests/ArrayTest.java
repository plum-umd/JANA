public class ArrayTest {
    
    public static void testArrayInit1() {
        //initialization with constant values
        
        //result: B.length = 10; 0 <= B.elements <= 'c'(99)
        
        byte[] b =new byte[10];
        b[2] = 'a';
        b[3] = 'c';
    }
    
    public static void testArrayInit2(int l, byte i) {
        //initialization with input parameters
        
        //result: B.length = l; 0 <= B.elements <= 'a'(97) V B.elements = i;
        
        byte[] b =new byte[l];
        b[2] = i;
        b[1] = 'a';
    }
    
    public static void testArrayInit3() {
        //initialization of two arrays with constant values
        //result: B1.length = 5; 0 <= B1.elements <= 'a'; B2.length = 10; 0 <= B2.elements <= 'b'
        
        byte[] b1 = new byte[5];
        b1[3] ='a';
        byte[] b2 = new byte[10];
        b2[2] = 'b';
    }
    
    public static void testArrayInit4(byte[] b1, byte b) {
        //initialization of two arrays with constant values and parameters
        //result: B2.length = 10; B1.elements = B1.elemments[in] V B1.elements = 97; B2.elements = 0 V B2.elements = b
        
        b1[3] = 'a';
        byte[] b2 = new byte[10];
        b2[2] = b;
    }
    
    public static void testArrayWBranch1(byte[] a, int b) {
        //WRONG: to debug, joining that initialize array element with 0...
        if(b > 0) {
            a[10] = 'a';
        }
    }
    
//    public static void testArrayWBranch2(byte[] a, int b) {
//        if(b == 0) {
//            a[10] = 'a';
//        } else {
//            a[8] = 'b';
//        }
//    }
//    
//    public static int test(byte[]a ,byte[] b, int c){
//        if(c == 0) {
//            a[2] = 'a';
//        } else {
//            b[3] = 'b';
//        }
//        return a.length;
//    }
    
}

