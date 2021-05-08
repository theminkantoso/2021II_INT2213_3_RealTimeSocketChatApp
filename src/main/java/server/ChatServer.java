package server;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A multithreaded chat room server. When a client connects the server requests
 * a screen name by sending the client the text "SUBMITNAME", and keeps
 * requesting a name until a unique one is received. After a client submits a
 * unique name, the server acknowledges with "NAMEACCEPTED". Then all messages
 * from that client will be broadcast to all other clients that have submitted a
 * unique screen name. The broadcast messages are prefixed with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g.,
 * better logging. Another is to accept a lot of fun commands, like Slack.
 */
public class ChatServer {

    public static final int SERVER_PORT = 59001;
    public static final String SERVER_ADDR = "127.0.0.1";
    private static final int MAX_CLI = 500;
    private static ExecutorService pool = Executors.newFixedThreadPool(MAX_CLI);
    public static List<String> names = new ArrayList<>();;
    public static void main(String[] args) throws Exception {

        System.out.println("The chat server is running...");
        ServerSocket listener = new ServerSocket(SERVER_PORT);
        while (true) {
            System.out.println("SERVER | Waiting for client connection ...");
            System.out.println(names.toArray().length);
            for(int i = 0; i < names.toArray().length; i ++) {
                System.out.println(names.toArray()[i] + "__");
            }
            Socket clientConnection = listener.accept();
            System.out.println("SERVER | Connected to client!");
            Handler clientThread = new Handler(clientConnection);
            pool.execute(clientThread);

        }
    }


}