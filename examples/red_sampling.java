public class red_sampling {
    int maxint = 1000000;

    public void hide_1(int a, int b) {
        // This program is has a different running time when a == 1234
        // and b == 42 than otherwise. Straight up sampling input
        // values, however, would be hard-pressed to discover this
        // given the large input space of two integers.

        // On the other hand, a sampling scheme that forces some form
        // of line coverage (tries to create a sample that executes
        // the "big noop" line), might be able to find this issue.
        
        if (a == 1234 && b == 42) {
            // big noop
        } else {
            // small noop
        }
        return;
    }    
    
    public void hide_2(int a, int b) {
        // A revision of the above, except now the line coverage might
        // be harder to figure out. The condition is the same as in
        // the above, except a and b are first copied over to x and y
        // by adding 1 repeatedly inside two nested loops. The nested
        // loops ensure that it is harder to figure out the condition
        // on a, b that results in the line coverage that includes
        // "big noop".

        int x = 0;
        int y = 0;
        int useless = 0;

        // Copy over a,b into x,y.
        for (int i = 0; i < a; i++) {
            x += 1;
            for (int j = 0; j < b; j++) {
                y += 1;
            }
            for (int j = b; j <= maxint; j++) {
                // This is just to make the running time of the first
                // part of the program constant.
                useless += 1;
            }
        }
        for (int i = a; i <= maxint; i++) {
            // Making runtime constant again.
            for (int j = 0; j <= maxint; j++) {
                useless += 1;
            }
        }

        if (x == 1234 && y == 42) {
            // big noop
        } else {
            // small noop
        }

        return;
    }

    public void hide_3(int a) {
        // How to make line coverage insufficient to sample the bad
        // event? Make sure the bad event requires an interaction
        // between multiple lines that none on their own will exhibit
        // the problem.
    }

    public void hide_4(int a) {
        // How to hide the bad event in a sampling scheme that tries
        // to achieve high path coverage (as opposed to line
        // coverage).
    }
}
