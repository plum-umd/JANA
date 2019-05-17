
public class Blogger {
    public static void m() {
  	int i = 0;
	int n = 10;	
	int[] A = {1,1,1,1,1,0,0,0,0,0};
	while (i<n) {
	    if (A[i] == 0) { i++; }
	    else {
		while (A[i] == 1 && i<n)
		    {  i++; }
	    }
	}
	// outer loop should be n times, not n^2 times
    }
}
