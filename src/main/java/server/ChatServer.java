package server;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ChatServer extends Application {

    public static final int SERVER_PORT = 8000;
    public static final String SERVER_IP = "localhost";

    public static ArrayList<MyFile> files = new ArrayList<>();
    /*
    GUI labels
     */
    Label lbLog = new Label("Log");
    Label lbUserList = new Label("Active User in system");

    /*
     Store information for display purpose
     */
    private ArrayList<String> logList = new ArrayList<>();
    private ArrayList<String> userList = new ArrayList<>();

    /*
     Display information to GUI
     */
    ListView<String> logListView = new ListView<String>();
    ListView<String> userListView = new ListView<String>();

    ObservableList<String> logItems =
            FXCollections.observableArrayList (logList);
    ObservableList<String> userItems =
            FXCollections.observableArrayList (userList);

    /*
     Mapping of sockets to output streams
     */
    private Hashtable outputStreams = new Hashtable();
    /*
    All open Socket.
     */
    private ArrayList<Socket> socketList = new ArrayList<>();

    private ServerSocket serverSocket;

    @Override
    public void start(Stage primaryStage) {

        /*
        Setting content to display for the ListVIew
         */
        userListView.setItems(userItems);
        logListView.setItems(logItems);
        logListView.setMinWidth(430);

        /*
        GUI part
         */
        // Creating GridPane to arrange all the node.
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setStyle("-fx-background-color: #f207fa");
        //All the nodes are added to the gridPane.
        gridPane.add(lbLog,0,0);
        gridPane.add(logListView,0,1);
        gridPane.add(lbUserList,0,2);
        gridPane.add(userListView,0,3);

        // Create a scene and place it in the stage
        Scene scene = new Scene(gridPane, 450, 400);
        primaryStage.setTitle("Server GUI");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        /*
        Font
         */
        lbLog.setFont(Font.font("Times New Roman", FontWeight.BOLD, FontPosture.ITALIC, 20));
        lbLog.setStyle("-fx-text-fill: white");
        lbUserList.setFont(Font.font("Times New Roman", FontWeight.BOLD, FontPosture.ITALIC, 20));
        lbUserList.setStyle("-fx-text-fill: white");
        /*
        Ensure to close all socket when close GUI
         */
        primaryStage.setOnCloseRequest(t -> closeSocketExit());

        // Start a new thread to listen for connection.
        new Thread(() -> listen()).start();
    }

    /*
    Close all existing sockets
     */
    private void closeSocketExit() {
        try {
            for(Socket socket : socketList){
                if(socket != null) {
                    socket.close();
                }
            }
            Platform.exit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
   Server started and wait for connection request from clients
   Then accept request, add that socket connection store it in the arraylist
   Then create a new thread to handle that socket connection
     */
    private void listen() {
        try {
            /*
             Create a server socket, starts listening
             */
            serverSocket = new ServerSocket(SERVER_PORT);
            Platform.runLater(() ->
                    logItems.add("MultiThreadServer started at " + new Date()));
            while (true) {
                Socket socket = serverSocket.accept();
                /*
                Add accepted socket to the socketList.
                 */
                socketList.add(socket);
                Platform.runLater(() -> logItems.add("Connection from " + socket + " at " + new Date()));
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                // Save output stream to hashtable
                outputStreams.put(socket, dataOutputStream);
                new ServerThread(this, socket);
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     Update userlist to all user in the server.
     */
    private void updateUserlist() {
        this.sendToAll(userList.toString());
    }

    // Used to get the output streams
    Enumeration getOutputStreams(){
        return outputStreams.elements();
    }

    /*
    Send message to all clients
     */
    void sendToAll(String message){
        /*
         Go through hashtable and send message to each output stream
         */
        for (Enumeration e = getOutputStreams(); e.hasMoreElements();){
            DataOutputStream dout = (DataOutputStream)e.nextElement();
            try {
                dout.writeUTF(message);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
     This method send all people in the chatroom to all the user excluding self.
     */
    void sendUsersInSystem(Socket socket, String message){
        for (Enumeration e = getOutputStreams(); e.hasMoreElements();){
            DataOutputStream dataOutputStream = (DataOutputStream)e.nextElement();
            try {
                if(!(outputStreams.get(socket) == dataOutputStream)){
                    // Write message
                    dataOutputStream.writeUTF(message);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
    Class Thread handle each connection from client
     */
    public class ServerThread extends Thread {
        private ChatServer server;
        private Socket socket;
        String userName;
        boolean userJoined;

        public ServerThread(ChatServer server, Socket socket) {
            this.socket = socket;
            this.server = server;
            start();
        }

        @Override
        public void run() {
            try {
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    /*
                    Check if already joined
                    1. if not then add them
                    2. if already joined then broadcast the message from server to all users
                    */
                    if(!userJoined){
                        userName = dataInputStream.readUTF();
                        if(userList.contains(userName)){
                            dataOutputStream.writeUTF(userName);
                            System.out.println(userName + " already exist.");
                        }
                        else{
                            userList.add(userName);
                            dataOutputStream.writeUTF("Accepted");
                            server.updateUserlist();
                            System.out.println(userName +" joined the chat room");
                            userJoined = true;
                            String userNotification = userName + " joined the chat room.";
                            Platform.runLater(() ->
                                    logItems.add(userName + " joined the chat room."));
                            server.sendUsersInSystem(socket, userNotification);
                            userItems.clear();
                            userItems.addAll(userList);
                        }
                    }
                     /*
                    Handle the already joined scenario, including broadcasting the message to all clients
                    */
                    else if(userJoined){
                        String string = dataInputStream.readUTF();
                        System.out.println(string);

                        server.sendToAll(string);
                        server.updateUserlist();

                        Platform.runLater(() -> logItems.add(string));

                    }
                }
            }
            /*
           Avoid any problem, when socket related problem occured, this program
           will close the connection to avoid affecting the entire system.
           This code include removing to avoid system corrupt, and procedure to properly
           close connection like exit function
             */
            catch(IOException ex) {
                System.out.println("Connection Closed for " + userName);
                ex.printStackTrace();
                Platform.runLater(() ->
                        logItems.add("Connection Closed for " + userName));

                if( !userName.equals(null)){
                    userList.remove(userName);
                }
                outputStreams.remove(socket);
                server.updateUserlist();
                if ( !userName.equals(null)){
                    server.sendToAll(userName + " has left the chat room.");
                }
                Platform.runLater(() ->{
                    userItems.clear();
                    userItems.addAll(userList);
                });
            }
        }
    }
}
