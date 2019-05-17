import java.util.*;

public class SwapList {

    public static <E> void swap(List<E> a, int i, int j) {
	E tmp = a.get(i);
	a.set(i, a.get(j));
	a.set(j, tmp);
    }

    public static void main(String[] args) {
	List<String> list = new ArrayList<String>();
        for (String a : args)
            list.add(a);
	if(args.length > 1) {
	    swap(list,0,1);
	}
        //System.out.println(list);
	return;
    }
}
