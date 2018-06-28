package gradebook;

import static gradebook.views.Controller_Home.titles;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.*;


public class Main extends Application {

    public static Connection gradebookDB = null;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("views/Home.fxml"));
        Scene home = new Scene(root);

        primaryStage.setTitle("Home");
        primaryStage.setScene(home);
        primaryStage.show();

        titles.add("Home");
    }

    public static void main(String[] args) {
        try {
            java.lang.Class.forName("org.sqlite.JDBC");
            gradebookDB = DriverManager.getConnection("jdbc:sqlite:gradebook.db");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Database connected...");
        launch(args);
    }
}