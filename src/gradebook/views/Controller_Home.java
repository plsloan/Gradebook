package gradebook.views;

import gradebook.Course;
import gradebook.Main;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static gradebook.views.Controller_Semesters.*;

public class Controller_Home {
    public static List<String> titles = new ArrayList<>();
    @FXML private Button semesterBtn;
    @FXML private Button back_home;
    @FXML private TextField current_GPA;
    @FXML private TextField overall_GPA;
    @FXML private TableView<Course> homeTable;
    @FXML private TableColumn<Course, String> prefix;
    @FXML private TableColumn<Course, Integer> number;
    @FXML private TableColumn<Course, String> section;
    @FXML private TableColumn<Course, String> description;
    @FXML private TableColumn<Course, String> grade;
    @FXML private TableColumn<Course, Integer> ch;
    static int semesterID;

    public void initialize() {
        back_home.visibleProperty().bind(new SimpleBooleanProperty(titles.size() > 1));

        // get Current Semester table
        // also sets semesterID
        initializeCurrentIndex();
        getCurrentTable();
        setCurrentGPA();
        setOverallGPA();
    }

    // Helpers -----------------------------------------------------
    // initialize currentIndex
    private void initializeCurrentIndex() {
        int index = 0;
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT name, current_home FROM Semesters;";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                if (rs.getBoolean("current_home")) {
                    currentIndex = index;
                }
                else {
                    index++;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void getCurrentTable() {
        try {
            ObservableList<Course> data = FXCollections.observableArrayList();
            int index = getCurrentIndex();

            Statement statement = Main.gradebookDB.createStatement();
            String sql_getRecentTable = "SELECT id_semester, prefix, number, section, description, received_points, possible_points, " +
                    "credit_hours FROM Courses WHERE id_semester=" + Integer.toString(index) + ";";
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
                        rs.getString("section"),
                        rs.getString("description"),
                        letter_grade,
                        rs.getInt("credit_hours")
                ));
            }

            homeTable.setItems(data);
            homeTable.setOnMousePressed(new EventHandler<>() {
                @Override
                public void handle(MouseEvent click) {
                    if (click.isPrimaryButtonDown() && click.getClickCount() == 2) {
                        clickedCourse = homeTable.getSelectionModel().getSelectedItem();

                        try {
                            goToViewCourse();
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }
            });

            prefix.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                    new SimpleStringProperty(p.getValue().prefix));
            number.setCellValueFactory((TableColumn.CellDataFeatures<Course, Integer> p) ->
                    new SimpleIntegerProperty(p.getValue().number).asObject());
            section.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                    new SimpleStringProperty(p.getValue().section));
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

    private void setCurrentGPA() {
        try {
            String gpa;
            Statement statement = Main.gradebookDB.createStatement();
            String get_semester = "SELECT id, name, printf(\"%.2f\", gpa) as gpa " +
                    "FROM Semesters " +
                    "WHERE id=" + Integer.toString(semesterID) + ";";
            ResultSet rs = statement.executeQuery(get_semester);

            while(rs.next()) {
                gpa = Double.toString(rs.getDouble("gpa"));
                if (gpa.length() >= 4) {
                    gpa = gpa.substring(0, 4);
                }

                current_GPA.setText(gpa);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void setOverallGPA() {
        double quality_points = 0;
        double credits = 0.0;

        try {
            String gpa;
            Statement statement = Main.gradebookDB.createStatement();
            String get_overall = "SELECT id, name, printf(\"%.2f\", gpa) as gpa, credits " +
                    "FROM Semesters;";
            ResultSet rs = statement.executeQuery(get_overall);

            while (rs.next()) {
                quality_points += rs.getDouble("gpa") * rs.getInt("credits");
                credits += rs.getInt("credits");
            }

            gpa = Double.toString(quality_points/credits);

            // check for rounding
            if (gpa.length() >= 4) {
                if (Character.getNumericValue(gpa.charAt(4)) >= 5) {
                    gpa = gpa.substring(0, 3) + Integer.toString(Character.getNumericValue(gpa.charAt(3)) + 1);
                } else {
                    gpa = gpa.substring(0, 4);
                }
            }

            if (gpa.equals("NaN")) {
                overall_GPA.setText("N/A");
            } else {
                overall_GPA.setText(gpa);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private int getCurrentIndex() {
        int s = -1;
        int index = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT * FROM Semesters;";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                s++;
                if (s == currentIndex) {
                    index = rs.getInt("id");
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }

        return index;
    }

//    private int getCurrentIndex(int index) {
//        try {
//            Statement statement = Main.gradebookDB.createStatement();
//            String sql = "SELECT * FROM Semesters;";
//            ResultSet rs = statement.executeQuery(sql);
//
//            for (int i = 0; i < index; i++) {
//                rs.next();
//            }
//
//            return rs.getInt("id");
//
//        } catch (Exception e) {
//            System.out.println(e);
//        }
//
//        return -1;
//    }
    // -------------------------------------------------------------


    // Navigation --------------------------------------------------
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
    private void goToViewCourse() throws IOException {
        Parent viewCourseParent = FXMLLoader.load(getClass().getResource("Course_View.fxml"));
        Scene viewCourse = new Scene(viewCourseParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(viewCourse);
        window.setTitle(clickedCourse.description);
        window.show();

        Controller_Home.titles.add("View Course");
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
