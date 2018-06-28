package gradebook.views;

import gradebook.Course;
import gradebook.Main;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Controller_Home {
    public static List<String> titles = new ArrayList<>();
    @FXML private Button semesterBtn;
    @FXML private Button back_home;
    @FXML private TableView<Course> homeTable;
    @FXML private TableColumn<Course, String> prefix;
    @FXML private TableColumn<Course, Integer> number;
    @FXML private TableColumn<Course, String> description;
    @FXML private TableColumn<Course, String> grade;
    @FXML private TableColumn<Course, Integer> ch;
    public static int semesterID;

    public void initialize() {
        back_home.visibleProperty().bind(new SimpleBooleanProperty(titles.size() > 1));

        // get Current Semester table
        // also sets semesterID
        getCurrentTable();
    }

    private void getCurrentTable() {
        try {
            ObservableList<Course> data = FXCollections.observableArrayList();
            int s = getCurrentSize();
            if (Controller_Semesters.copySemesters != null && Controller_Semesters.currentIndex != -1) {
                TitledPane titledPane = Controller_Semesters.copySemesters.getPanes().get(Controller_Semesters.currentIndex);
                s = Controller_Semesters.copySemesters.getPanes().indexOf(titledPane) + 1;
            } else if (Controller_Semesters.currentIndex != 9999) {
                s = Controller_Semesters.currentIndex + 1;
            }

            Statement statement = Main.gradebookDB.createStatement();
            String sql_getRecentTable = "SELECT id_semester, prefix, number, description, received_points, possible_points, " +
                    "credit_hours FROM Courses WHERE id_semester=" + Integer.toString(s) + ";";
            ResultSet rs = statement.executeQuery(sql_getRecentTable);

            // gets letter grade string
            // adds data
            while (rs.next()) {
                String letter_grade;
                int received = rs.getInt("received_points");
                int possible = rs.getInt("possible_points");

                if (possible - received <= 100) {
                    letter_grade = "A";
                } else if (possible - received <= 200) {
                    letter_grade = "B";
                } else if (possible - received <= 300) {
                    letter_grade = "C";
                } else if (possible - received <= 400) {
                    letter_grade = "D";
                } else if(received == 0 && possible == 1000) {
                    letter_grade = "N/A";
                } else {
                    letter_grade = "F";
                }

                semesterID = rs.getInt("id_semester");

                data.addAll(new Course(
                        rs.getString("prefix"),
                        rs.getInt("number"),
                        rs.getString("description"),
                        letter_grade,
                        rs.getInt("credit_hours")
                ));
            }

            homeTable.setItems(data);

            prefix.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                    new SimpleStringProperty(p.getValue().prefix));
            number.setCellValueFactory((TableColumn.CellDataFeatures<Course, Integer> p) ->
                    new SimpleIntegerProperty(p.getValue().number).asObject());
            description.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                    new SimpleStringProperty(p.getValue().description));
            grade.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                    new SimpleStringProperty(p.getValue().letter_grade));
            ch.setCellValueFactory((TableColumn.CellDataFeatures<Course, Integer> p) ->
                    new SimpleIntegerProperty(p.getValue().credit_hours).asObject());

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private int getCurrentSize() {
        int s = 0;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT * FROM Semesters;";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                s++;
            }

        } catch (Exception e) {
            System.out.println(e);
        }

        return s;
    }

    @FXML
    private void goToSemesters() throws IOException {
        Parent SemestersParent = FXMLLoader.load(getClass().getResource("Semesters.fxml"));
        Scene semesters = new Scene(SemestersParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(semesters);
        window.setTitle("Semesters");
        window.show();

        titles.add("Semesters");
    }

    @FXML
    private void goToGPA() throws IOException {
        Parent gpaParent = FXMLLoader.load(getClass().getResource("GPA_Calculator.fxml"));
        Scene gpa = new Scene(gpaParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(gpa);
        window.setTitle("Calculate GPA");
        window.show();

        titles.add("Calculate GPA");
    }

    @FXML
    private void goToAddCourse() throws IOException {
        Parent addCourseParent = FXMLLoader.load(getClass().getResource("Course_Add.fxml"));
        Scene addCourse = new Scene(addCourseParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(addCourse);
        window.setTitle("Add Course");
        window.show();

        titles.add("Add Course");
    }

    @FXML
    private void goBack() throws IOException {
        int n = Controller_Home.titles.size() - 1;
        Controller_Home.titles.remove(n);

        Parent backParent = null;
        switch (Controller_Home.titles.get(n-1)) {
            case "Home":
                backParent = FXMLLoader.load(getClass().getResource("Home.fxml"));
                break;
            case "Add Course":
                backParent = FXMLLoader.load(getClass().getResource("Course_Add.fxml"));
                break;
            case "Calculate GPA":
                backParent = FXMLLoader.load(getClass().getResource("GPA_Calculator.fxml"));
                break;
            case "Semesters":
                backParent = FXMLLoader.load(getClass().getResource("Semesters.fxml"));
                break;
            case "View Course":
                backParent = FXMLLoader.load(getClass().getResource("Course_View.fxml"));
                break;
        }

        Scene back = new Scene(backParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(back);
        window.setTitle(Controller_Home.titles.get(n-1));
        window.show();
    }
}
