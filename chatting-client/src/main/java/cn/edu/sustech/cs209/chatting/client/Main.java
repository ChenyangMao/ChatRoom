package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.IOException;
import java.net.Socket;


// --module-path "C:\Users\cheny\Desktop\卷纸\大二下\java2\javafx-sdk-17.0.1\lib" --add-modules javafx.controls,javafx.fxml
public class Main extends Application {

    public static void main(String[] args) throws IOException {
        launch();
    }


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = fxmlLoader.load();
        try {
            Socket socket = new Socket("localhost", 1234);
            Controller controller = fxmlLoader.getController();
            controller.socket = socket;
            controller.setStage(stage);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Chatting Client");
            stage.show();
            stage.setOnCloseRequest(event -> controller.handleClose());
        } catch (IOException e) {
            System.out.println("Fail connect server");
        }
    }
}
