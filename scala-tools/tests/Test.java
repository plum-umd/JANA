public class Test {
  private static final String  secret = "password";

  public static int foo(int a, int b) {
    //int c[] = new int[50];
    int i;

    //for(i = 0; i < c.length; i++ ) {
    //  c[i] = 0;
    //}

    if(a < 0) { 
      return a+b;
    } else {
      return 0;
    }
  }

  public static void main(String  []args) {
    boolean auth = false;

    if(args.length > 0) {
      String  fromCmd = args[0];
      if(fromCmd.length() >= 8) {
        auth = (  fromCmd.charAt(0) == Test.secret.charAt(0) &&
                  fromCmd.charAt(1) == Test.secret.charAt(1) &&
                  fromCmd.charAt(2) == Test.secret.charAt(2) &&
                  fromCmd.charAt(3) == Test.secret.charAt(3) &&
                  fromCmd.charAt(4) == Test.secret.charAt(4) &&
                  fromCmd.charAt(5) == Test.secret.charAt(5) &&
                  fromCmd.charAt(6) == Test.secret.charAt(6) &&
                  fromCmd.charAt(7) == Test.secret.charAt(7));
      }
    }

    if(auth) {
      System.out.printf("authenticated!\n");
    } else {
      System.out.printf("not authenticated\n");
    }

    return;
  }
}
