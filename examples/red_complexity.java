import java.net.Socket;

public class red_complexity {    
    public void false_positive_1(int a) {
        // This example should run in constant time (1000 loop
        // iterations) but hides this fact via the java api
        // (specifically socket). Determining that it is constant time
        // requires understanding of the API sufficiently to know that
        // a socket created with a port X then returns that same
        // port via getPort.
        
        assert(1 <= a && a <= 1000);
        
        Socket s = hidden_int_create(a);
        
        for (int i = 0; i <= a; i++) {
            // some noop
        }

        int makeup = hidden_int_read(s);
        
        for (int i = 0; i <= makeup; i++) {
            // some noop
        }

        return;
    }

    public void false_positive_2(int a) {
        // Same point but now it is hiding the constant running time
        // in a boolean.
        
        assert(1 <= a && a <= 1000);
        
        Socket s = hidden_bool_create(true);
        
        for (int i = 0; i <= a; i++) {
            // some noop
        }

        if (hidden_bool_read(s)) {
            for (int i = 0; i <= 1000-a; i++) {
                // some noop
            }
        }
        
        return;
    }

    // The below are examples of hiding integers and booleans in the
    // java api and hopefully the underlying system calls. Some of
    // these might be analysable if the java api replicates some of
    // the data by itself instead of relying on the OS to keep the
    // datas (socket port, socket flags, etc.).
    
    public Socket hidden_int_create(int port) {
        // Hide an integer in the socket's port.
        try {
            Socket socket = new Socket("localhost", port);
            return socket;
        } finally {
            return null;
        }
    }
    public int hidden_int_read(Socket s) {
        return s.getPort();
    }

    public Socket hidden_bool_create(boolean initial) {
        // Hide a boolean in a socket's keepalive flag.
        try {
            Socket socket = new Socket("localhost", 2000);
            socket.setKeepAlive(initial);
            return socket;
        } finally {
            return null;
        }
    }
    public boolean hidden_bool_read(Socket s) {
        try {
            return s.getKeepAlive();
        } finally {
            return false;
        }
    }
}
