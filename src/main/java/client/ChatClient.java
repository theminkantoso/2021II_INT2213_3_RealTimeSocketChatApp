package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import server.ChatServer;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ChatClient extends Application {

    /*
    UI Labels
     */
    Label UsernameInput = new Label("Username");
    Label MessageArea = new Label("Chat Messages");
    Label labelTitle = new Label();
    Label labelMessages = new Label("User Message ");
    Label activeUserList = new Label("Active User In System");
    Label errorLabel = new Label("");

    /*
     List Active Users and messages
     */
    ArrayList<String> userList = new ArrayList<>();
    ArrayList<String> chatMessages = new ArrayList<>();

    /*
    List Active Users and messages for GUI purpose
     */
    ListView<String> userListView = new ListView<String>();
    ListView<String> messageListView = new ListView<String>();
    ObservableList<String> userItems =
            FXCollections.observableArrayList (userList);

    ObservableList<String> messageItem =
            FXCollections.observableArrayList (chatMessages);


    /*
    Text Field for input username and message
     */
    TextField nameInput = new TextField();
    TextArea messageInput = new TextArea();

    /*
    Buttons on the GUI
     */
    Button join = new Button("Join");
    Button send = new Button("Send");
    Button exit = new Button("Exit and Disconnect");
    Button sendFile = new Button("File txt");

    /*
     IntputStream and OutputStream to communicate with server
     Output: Send to server
     Input: Receive from server
     */
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;

    /*
     Check whether user has joined the chatroom
     By default it set to false, and will be modify later when the join action completed
     */
    boolean joined = false;

    /*
    Client socket init connection
     */
    private Socket socket;

    private String userName;

    /*
    Check server connection
     */
    private boolean connection = true;


    @Override
    public void start(Stage primaryStage)  {

        // Creating BorderPane to arrange all the node.
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10));

        //Setting Title of the application
        Font titleFont = new Font("Times New Roman",20);
        labelTitle.setText("Welcome to Anonymous Chat Application");
        labelTitle.setFont(titleFont);
        Color titleColor = new Color(0.1, 0, 0.5,1);
        labelTitle.setTextFill(titleColor);


        // Setting Prompt for user text field and area.
        nameInput.setPromptText("Enter your username");
        messageInput.setPromptText("Enter your message");

        // Setting size of the compose text area. So, user can send
        // multiline messages.
        messageInput.setPrefHeight(2 * (nameInput.getHeight()));
        messageInput.setPrefWidth(250);

        // Creating GridPane for the Center part of BorderPane.
        GridPane centreGridPane = new GridPane();
        centreGridPane.setPadding(new Insets(10));
        centreGridPane.setHgap(20);
        centreGridPane.setVgap(10);

        // Adding item to the centreGridPane
        centreGridPane.add(UsernameInput,0,0);
        centreGridPane.add(nameInput,1,0);
        centreGridPane.add(join,2,0);
        centreGridPane.add(MessageArea,0,2);
        centreGridPane.add(errorLabel,1,1,2,1);
        centreGridPane.add(messageListView,1,2,2,1);

        //Setting content to display for the ListVIew
        messageListView.setItems(messageItem);
        userListView.setItems(userItems);

        // user and message list view is made uneditable.
        userListView.setEditable(false);
        messageListView.setEditable(false);
        messageInput.setEditable(false);
        // Setting size of user ListView.
        messageListView.setMinWidth(400);
        userListView.setMaxWidth(180);
        userListView.setMaxHeight(250);


        //Creating and adding item to right of BorderPane
        VBox rightVBox = new VBox();
        rightVBox.setPadding(new Insets(20,0,10,0));
        rightVBox.setSpacing(10);
        rightVBox.getChildren().addAll(activeUserList,userListView);
        borderPane.setRight(rightVBox);


        //Creating and adding note to bottomGridPane.
        GridPane bottomGridPane = new GridPane();
        bottomGridPane.add(labelMessages,0,0);
        bottomGridPane.add(messageInput,1,0);
        bottomGridPane.add(send,4,0);
        bottomGridPane.add(exit,7,0);
        bottomGridPane.add(sendFile,5,0);
        bottomGridPane.setHgap(20);
        bottomGridPane.setPadding(new Insets(10,0,10,10));
        send.setAlignment(Pos.BASELINE_RIGHT);

        //Adding item to the Top of BorderPane
        borderPane.setTop(labelTitle);
        borderPane.setAlignment(labelTitle,Pos.CENTER);

        //Adding item to the Center of BorderPane
        borderPane.setCenter(centreGridPane);

        //Adding item to the Bottom of BorderPane.
        borderPane.setBottom(bottomGridPane);

        //Creating new scene and placing borderPane.
        Scene scene = new Scene(borderPane,750,400);
        primaryStage.setScene(scene); //Setting scene.
        primaryStage.setTitle("Anonymous Chat"); //Setting title.
        primaryStage.setResizable(false);
        primaryStage.show();    //Display Stage.

        /*
         As socket need to be closed properly for the best
         user experience of the application, it is made
         sure that socket is closed when user close the
         application.
         */
        primaryStage.setOnCloseRequest(t -> closeSocketExit());
        //Send is disable until username is accepted.
        send.setDisable(true);
        sendFile.setDisable(true);

        // Setting listener for the buttons.
        join.setOnAction(event -> joinChat());
        send.setOnAction(e -> process());
        exit.setOnAction(event -> closeSocketExit());
        sendFile.setOnAction(event-> {
            try {
                selectFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        try {
            // Create a socket to connect to the server
            socket = new Socket(ChatServer.SERVER_IP, ChatServer.SERVER_PORT);

            // Create an input stream to receive data from server.
            dataInputStream =
                    new DataInputStream(socket.getInputStream());

            // Create an output stream to send data to the server
            dataOutputStream =
                    new DataOutputStream(socket.getOutputStream());

            // Start a new thread for receiving messages
            new Thread(() -> receiveMessages()).start();
        }
        // Providing feedback to user to notify connection issues.
        catch (IOException ex) {
            errorLabel.setTextFill(Color.RED);
            errorLabel.setText("Unable to establish connection.");
            System.err.println("Connection refused.");
        }
    }

    private void selectFile() throws IOException {
        FileChooser fileChoose = new FileChooser();
        File file;
        Stage stage = new Stage();
        while(true) {
            file = fileChoose.showOpenDialog(stage);
            String fileName = file.getName();
            String fileType = fileName.substring(fileName.length() - 3);
            if(fileType.equals("txt")) {
                break;
            }
        }
        try {

            FileInputStream fileInputStreamFile = new FileInputStream(file.getAbsolutePath());

            // Get the name of the file you want to send and store it in filename.

            // Convert the name of the file into an array of bytes to be sent to the server.
            byte[] fileNameBytes = file.getName().getBytes();
            // Create a byte array the size of the file so don't send too little or too much data to the server.
            byte[] fileBytes = new byte[(int) file.length()];
            // Put the contents of the file into the array of bytes to be sent so these bytes can be sent to the server.
            fileInputStreamFile.read(fileBytes);
            //Send mark message
//            dataOutputStream.writeUTF("!@#$%^&*()");
            // Send the length of the name of the file so server knows when to stop reading.
            dataOutputStream.writeInt(fileNameBytes.length);
            // Send the file name.
            dataOutputStream.write(fileNameBytes);
            // Send the length of the byte array so the server knows when to stop reading.
            dataOutputStream.writeInt(fileBytes.length);
            // Send the actual file.
            dataOutputStream.write(fileBytes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
    As socket need to be closed properly for the best user
    experience of the application, this method is created to
    make sure that the socket is closed and stage is closed
    when this method is called.
     */
    private void closeSocketExit() {
        try {
            //If socket doesn't exit, no need to close.
            if(socket != null){
                socket.close();
            }
            Platform.exit();    // Close UI.
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
    This method receive message for server and read the
    message to be displayed in proper place and relevant
    information is shown to user. It can be related to
    showing in errorLabel whether username has been
    successfully added to server or whether the message
    is to be displayed in user or chat list view.
     */
    public void receiveMessages(){
        try{
            while(connection){
                String message;
                /*If user has not joined the server,
                only addUserName() is allowed to
                perform and other information is
                not shared with user.
                 */

                if(!joined){
                    addUserName();
                }
                /*
                Once userName has been accepted, other
                information like active userList and
                messages is transmitted.
                 */
                else{
                    /*
                    If message start with "[" that is
                    arrayList of user and this is
                    added to user List view.
                     */
                    message = dataInputStream.readUTF();
                    if(message.startsWith("[")){
                        addMessageToUserListView(message);
                    } else if(message.contains(" joined the chat room.")) {
                        Platform.runLater(() -> {
                            messageItem.add("***** "+ message + " *****");
                        });
                    } else if(message.contains(" has left the chat room.")) {
                        Platform.runLater(() -> {
                            messageItem.add("xxxxx "+ message + " xxxxx");
                        });
                    }
                    else if(!message.equals("")){
                        // Display to the message list view.
                        Platform.runLater(() -> {
                            messageItem.add(message);
                        });
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Socket is closed");
            Platform.runLater(() -> {
                errorLabel.setTextFill(Color.RED);
                errorLabel.setText("Unable to establish connection to server");
            });
            connection = false;
        }
    }


    /*
    joinChat method allow user to send the userName to
    be approved to the server, as "," is being processed
    in other code to convert arrayList.toString back to
    arrayList, this is not allowed as userName. Else, the
    userName is send to the server and error message is
    handled as so.
     */
    private void joinChat(){
        userName = nameInput.getText();
        if(userName.contains(",")){
            Platform.runLater(() -> {
                // Update UI here.
                errorLabel.setTextFill(Color.RED);
                errorLabel.setText("Cannot contain ','.");
            });
        }
        else{
            try {
                dataOutputStream.writeUTF(userName);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /*
    This method recreate an arrayList from the message and
    add the name to the userListView excluding its own name
    as it is not useful information.
     */
    private void addMessageToUserListView(String s) {
        List<String> list =
                Arrays.asList(
                        s.substring(1, s.length() - 1).split(", ")
                );
        Platform.runLater(() -> {
            // Update UI here.
            userItems.clear();
            for(int i = 0; i < list.size(); i++){
                if(!(list.get(i).equals(userName))){
                    userItems.add(list.get(i));
                }
            }
        });
    }


    /*
    If the server send response to the user and it says accepted,
    then the status of boolean joined is set to be true and this
    is updated in errorLabel to show that the user has joined
    the conversation and the join button is disabled and send
    message button is enabled.
    If it is not accepted, that mean there is userName is in the
    server arrayList so error message is shown letting user
    that the user name exist.
     */
    private void addUserName()  {
        String response;
        try {
            response = dataInputStream.readUTF();
            if (response.startsWith("Accepted")){
                joined = true;
                Platform.runLater(() -> {
                    System.out.println("User Connected as "+ userName);
                    send.setDisable(false);
                    join.setDisable(true);
                    sendFile.setDisable(false);
                    nameInput.setEditable(false);
                    messageInput.setEditable(true);
                    errorLabel.setTextFill(Color.GREEN);
                    errorLabel.setText("Joined as " + userName);
                });
            }
            else if(response.equals(userName)){
                Platform.runLater(() -> {
                    // Update UI here.
                    nameInput.clear();
                    errorLabel.setTextFill(Color.RED);
                    errorLabel.setText("User with same name exist.");
                });
            }
        } catch (IOException e) {
            System.out.println("Socket is closed.add");
            Platform.runLater(() -> {
                errorLabel.setTextFill(Color.RED);
                errorLabel.setText("Unable to establish connection.");
                connection = false;
            });
        }
    }


    /*
    This method send message to server by adding name to the message, so
    that the message can be send to all the user in the chat group.
    Special care has been taken to make sure that the formatting of the
    multiline text is preserved.
     */
    private void process() {
        try {
            // Get the text from the text field
            String string = nameInput.getText().trim() + ": " +
                    messageInput.getText().trim();

            // Send the text to the server
            dataOutputStream.writeUTF(string);

            // Clear text area.
            messageInput.setText("");
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }
}


