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
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import server.ChatServer;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatClient extends Application {

    /*
    UI Labels
     */
    Label l_usernameInput = new Label("Nickname");
    Label l_messageArea = new Label("Messages");
    Label l_labelTitle = new Label();
    Label l_labelMessages = new Label("User Message ");
    Label l_activeUserList = new Label("Active User In Chatroom");
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
    Button b_join = new Button("Join");
    Button b_send = new Button("Send");
    Button b_exit = new Button("Exit and Disconnect");
    Button b_sendFile = new Button("File txt");

    /*
     IntputStream and OutputStream to communicate with server
     Output: Send to server
     Input: Receive from server
     */
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;

    /*
     Check whether user has joined the chatroom
     By default it set to false, and will be modify later when the b_join action completed
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

        /*
        These all serving GUI purpose
         */
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10));
        borderPane.setStyle("-fx-background-color: #a791ff");

        l_labelTitle.setText("Chatroom Application");
        l_labelTitle.setFont(Font.font("Times New Roman", FontWeight.BOLD, 35));
        Color titleColor = Color.web("#FF0000");
        l_labelTitle.setTextFill(titleColor);

        // Setting Prompt for user text field and area.
        nameInput.setPromptText("Enter your Nickname");
        messageInput.setPromptText("Enter your message");


        messageInput.setPrefHeight(2 * (nameInput.getHeight()));
        messageInput.setPrefWidth(250);

        // Creating GridPane for the Center part of BorderPane. This servers GUI purpose
        GridPane centreGridPane = new GridPane();
        centreGridPane.setPadding(new Insets(10));
        centreGridPane.setHgap(20);
        centreGridPane.setVgap(10);

        //Change font
        Font labelFonts = Font.font("Times New Roman", FontWeight.BOLD, 15);
        l_usernameInput.setFont(labelFonts);
        l_messageArea.setFont(labelFonts);
        l_labelMessages.setFont(labelFonts);
        l_activeUserList.setFont(labelFonts);

        // Adding item to the centreGridPane
        centreGridPane.add(l_usernameInput,0,0);
        centreGridPane.add(nameInput,1,0);
        centreGridPane.add(b_join,2,0);
        centreGridPane.add(l_messageArea,0,2);
        centreGridPane.add(errorLabel,1,1,2,1);
        centreGridPane.add(messageListView,1,2,2,1);

        //Setting content to display for the ListVIew
        messageListView.setItems(messageItem);
        userListView.setStyle("-fx-font-weight: bold;" +
                "-fx-background-color : #113ff5;" +
                "-fx-text-fill: #ffffff;"
                );
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
        rightVBox.getChildren().addAll(l_activeUserList, userListView);
        borderPane.setRight(rightVBox);


        //Creating and adding note to bottomGridPane.
        GridPane bottomGridPane = new GridPane();
        bottomGridPane.add(l_labelMessages,0,0);
        bottomGridPane.add(messageInput,1,0);
        bottomGridPane.add(b_send,4,0);
        bottomGridPane.add(b_exit,7,0);
        bottomGridPane.add(b_sendFile,5,0);
        bottomGridPane.setHgap(20);
        bottomGridPane.setPadding(new Insets(10,0,10,10));
        b_send.setAlignment(Pos.BASELINE_RIGHT);

        //Adding item to the Top of BorderPane
        borderPane.setTop(l_labelTitle);
        borderPane.setAlignment(l_labelTitle, Pos.CENTER);

        //Adding item to the Center of BorderPane
        borderPane.setCenter(centreGridPane);

        //Adding item to the Bottom of BorderPane.
        borderPane.setBottom(bottomGridPane);

        //Creating new scene and placing borderPane.
        Scene scene = new Scene(borderPane,750,400);
        primaryStage.setScene(scene); //Setting scene.
        primaryStage.setTitle("Realtime Chat App"); //Setting title.
        primaryStage.setResizable(false);
        primaryStage.show();    //Display Stage.

        /*
         Make sure that socket is closed when user close the
         application.
         */
        primaryStage.setOnCloseRequest(t -> closeSocketExit());

        /*
        Disable sending anything until user input his or her username
         */
        b_send.setDisable(true);
        b_sendFile.setDisable(true);

        // Setting listener for the buttons.
        b_join.setOnAction(event -> joinChatUsername());
        b_send.setOnAction(e -> process());
        b_exit.setOnAction(event -> closeSocketExit());
        b_sendFile.setOnAction(event-> {
            try {
                selectFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        try {
            socket = new Socket(ChatServer.SERVER_IP, ChatServer.SERVER_PORT);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> receiveMessages()).start();
        }
        // Providing feedback to user to notify connection issues.
        catch (IOException ex) {
            errorLabel.setTextFill(Color.RED);
            errorLabel.setText("Unable to establish connection");
            System.err.println("Connection refused");
        }
        /*
        Set color for buttons
         */
        b_join.setStyle("-fx-text-fill: black; -fx-background-color: #38ff2e;");
        b_join.setOnMouseEntered(e-> b_join.setStyle("-fx-text-fill: #38ff2e;"));
        b_join.setOnMouseExited(e-> b_join.setStyle("-fx-text-fill: black; -fx-background-color: #38ff2e;"));

        b_send.setStyle("-fx-text-fill: black; -fx-background-color: #38ff2e;");
        b_send.setOnMouseEntered(e-> b_send.setStyle("-fx-text-fill: #38ff2e; -fx-background-color: #d5ff2e"));
        b_send.setOnMouseExited(e-> b_send.setStyle("-fx-text-fill: black; -fx-background-color: #38ff2e;"));

        b_sendFile.setStyle("-fx-text-fill: #2e5bff");
        b_sendFile.setOnMouseEntered(e-> b_sendFile.setStyle("-fx-text-fill: white; -fx-background-color: #2e5bff"));
        b_sendFile.setOnMouseExited(e-> b_sendFile.setStyle("-fx-text-fill: #2e5bff"));

        b_exit.setStyle("-fx-text-fill: red");
        b_exit.setOnMouseEntered(e-> b_exit.setStyle("-fx-text-fill: white; -fx-background-color: red;"));
        b_exit.setOnMouseExited(e-> b_exit.setStyle("-fx-text-fill: red"));
    }

    private void selectFile() throws IOException {
        FileChooser fileChoose = new FileChooser();
        File file;
        Stage stage = new Stage();
        while(true) {
            /*
            assing the selected file to the file variable
             */
            file = fileChoose.showOpenDialog(stage);
            String fileName = file.getName();

            /*
            make sure only txt files are accepted
             */
            String fileType = fileName.substring(fileName.length() - 3);
            if(fileType.equals("txt")) {
                break;
            }
            //alert user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Wrong File");
            alert.setHeaderText("Only txt text file!");
            alert.setContentText("Please choose again!");

            alert.showAndWait();
        }
        try {
            FileInputStream fileInputStreamFile = new FileInputStream(file.getAbsolutePath());
            //
            /*
            Convert the name of the file into an array of bytes to be sent to the server.
             */
            byte[] fileNameBytes = file.getName().getBytes();
            /*
             Create a byte array the size of the file so don't b_send too little or too much data to the server.
             */
            byte[] fileBytes = new byte[(int) file.length()];
            /*
             Put the contents of the file into the array of bytes to be sent so these bytes can be sent to the server.
             */
            fileInputStreamFile.read(fileBytes);
            /*
             Send the length of the name of the file so server knows when to stop reading.
             */
            dataOutputStream.writeInt(fileNameBytes.length);
            /*
             Send the file name.
             */
            dataOutputStream.write(fileNameBytes);
            /*
             Send the length of the byte array so the server knows when to stop reading.
             */
            dataOutputStream.writeInt(fileBytes.length);
            /*
             Send the actual file.
             */
            dataOutputStream.write(fileBytes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Ensure closing all sockets
     */
    private void closeSocketExit() {
        try {
            //If socket doesn't exist, no need to close.
            if(socket != null){
                socket.close();
            }
            Platform.exit();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Receive string from server to inputStream, then display the message
     */
    public void receiveMessages(){
        try{
            while(connection){
                String message;

                if(!joined){
                    addUserName();
                }
                else{
                    /*
                    If message start with "[" that is
                    arrayList of user and this is
                    added to user List view.
                    So we are using "[" as a mark
                     */
                    message = dataInputStream.readUTF();
                    if(message.startsWith("[")){
                        addMessageToUserListView(message);
                    } else if(message.contains(" joined the chat room.")) {
                        Platform.runLater(() -> {
                            messageItem.add("***** "+ message + " *****");
                        });
                    } else if(message.contains(" has left the chat room")) {
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
    Checks username in put, avoid duplication and syntax problems
    If OK send the userName to the server
     */
    private void joinChatUsername(){
        userName = nameInput.getText();
        if(userName.contains(",")){
            Platform.runLater(() -> {
                // Update UI here.
                errorLabel.setTextFill(Color.RED);
                errorLabel.setText("Cannot contain ','.");
            });
        }
        else if(!userName.equals("")){
            try {
                dataOutputStream.writeUTF(userName);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    Create Userlist
     */
    private void addMessageToUserListView(String s) {
        List<String> list =
                Arrays.asList(
                        s.substring(1, s.length() - 1).split(", ")
                );
        Platform.runLater(() -> {
            userItems.clear();
            for(int i = 0; i < list.size(); i++){
                if(! (list.get(i).equals(userName))){
                    userItems.add(list.get(i));
                } else {
                    userItems.add(list.get(i) + " (You)");
                }
            }
        });
    }


    /*
    Check username input, assure no duplication
    If OK send a green message and allow chat action
     */
    private void addUserName()  {
        String response;
        try {
            response = dataInputStream.readUTF();
            if (response.startsWith("Accepted")){
                joined = true;
                Platform.runLater(() -> {
                    System.out.println("User Connected as "+ userName);
                    b_send.setDisable(false);
                    b_join.setDisable(true);
                    b_sendFile.setDisable(false);
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
                errorLabel.setText("Unable to establish connection");
                connection = false;
            });
        }
    }


    /*
    Receives message from server and display it
     */
    private void process() {
        try {
            String string = nameInput.getText().trim() + ": " +
                    messageInput.getText().trim();
            // Send the text to the server
            if(!messageInput.getText().trim().equals("")) {
                dataOutputStream.writeUTF(string);
            }
            messageInput.setText("");
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }
}


