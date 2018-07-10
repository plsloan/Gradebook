package gradebook.views;

import gradebook.Category;
import gradebook.Course;
import gradebook.Main;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import static gradebook.views.Controller_Home.completedCredits;
import static gradebook.views.Controller_Home.completedGPA;

public class Controller_GPA {
    @FXML private Label gpaLabel;
    @FXML private TextField current_GPA;
    @FXML private TextField current_credits;
    @FXML private TextField calculated_GPA;
    @FXML private TextField calculated_credits;
    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> nameCol;
    @FXML private TableColumn<Course, String> pointsCol;
    @FXML private TableColumn<Course, String> creditsCol;
    @FXML private Button addButton;
    private ObservableList<Course> hypotheticalCourses = FXCollections.observableArrayList();

    public void initialize() {
        if (Integer.parseInt(completedCredits) != 0) {
            current_GPA.setText(completedGPA);
        } else {
            current_GPA.setText("N/A");
        }
        current_credits.setText(completedCredits);

        courseTable.setColumnResizePolicy(new Callback<TableView.ResizeFeatures, Boolean>() {
            @Override
            public Boolean call(TableView.ResizeFeatures p) {
                return true;
            }
        });

        addCourse();

        courseTable.setItems(hypotheticalCourses);
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(p.getValue().description));
        pointsCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pointsCol.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(Integer.toString(p.getValue().number)));
        creditsCol.setCellFactory(TextFieldTableCell.forTableColumn());
        creditsCol.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(Integer.toString(p.getValue().credit_hours)));

        nameCol.setOnEditCommit(t -> {
            t.getTableView().getItems().get(t.getTablePosition().getRow()).description = t.getNewValue();
        });
        pointsCol.setOnEditCommit(t -> {
            int new_points = Integer.parseInt(t.getNewValue());
            t.getTableView().getItems().get(t.getTablePosition().getRow()).number = new_points;
            calculateGPA();
        });
        creditsCol.setOnEditCommit(t -> {
            int new_credits = Integer.parseInt(t.getNewValue());
            t.getTableView().getItems().get(t.getTablePosition().getRow()).credit_hours = new_credits;
            calculateGPA();
        });

        courseTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent keyEvent)
            {
                final Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();

                if (selectedCourse != null)
                {
                    if (keyEvent.getCode().equals(KeyCode.DELETE))
                    {
                        courseTable.getItems().remove(selectedCourse);
                        calculateGPA();
                    }
                }
            }
        } );
    }

    // Controls ----------------------------------------------------------------------------------------------------
    @FXML private void addCourse() {
        Course course = new Course();
        course.description = "Course";
        course.number = 0;
        course.credit_hours = 3;

        hypotheticalCourses.addAll(course);
    }
    // -------------------------------------------------------------------------------------------------------------


    // Helpers -----------------------------------------------------------------------------------------------------
    private void calculateGPA() {
        double quality_points = 0, credits = 0;

        // get database quality_points and credits
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT received_points, possible_points, credit_hours FROM Courses;";
            ResultSet courses = statement.executeQuery(sql);

            while (courses.next()) {
                double received = courses.getInt("received_points");
                double possible = courses.getInt("possible_points");
                int ch = courses.getInt("credit_hours");
                credits += ch;

                if (received/possible >= 0.9) {
                    quality_points += 4*ch;
                } else if (received/possible >= 0.8) {
                    quality_points += 3*ch;
                } else if (received/possible >= 0.7) {
                    quality_points += 2*ch;
                } else if (received/possible >= 0.6) {
                    quality_points += ch;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        // get quality points and credits from table
        for (Course course : courseTable.getItems()) {
            int ch = course.credit_hours;
            credits += ch;
            if (course.number >= 900) {
                quality_points += 4*ch;
            } else if (course.number >= 800) {
                quality_points += 3*ch;
            } else if (course.number >= 700) {
                quality_points += 2*ch;
            } else if (course.number >= 600) {
                quality_points += ch;
            }
        }

        String gpa = Double.toString(quality_points/credits);
        int intCredits = (int) credits;
        if (gpa.length() > 4) {
            if (Character.getNumericValue(gpa.charAt(4)) >= 5) {
                gpa = gpa.substring(0, 3) + Integer.toString(Character.getNumericValue(gpa.charAt(3)) + 1);
            } else {
                gpa = gpa.substring(0, 4);
            }
        }

        if (gpa.equals("NaN") || gpa.equals("0.0")) {
            calculated_GPA.setText("N/A");
        } else {
            calculated_GPA.setText(gpa);
        }

        if (intCredits > 0) {
            calculated_credits.setText(Integer.toString(intCredits));
        } else {
            calculated_credits.setText("N/A");
        }
    }
    // -------------------------------------------------------------------------------------------------------------


    // Navigation --------------------------------------------------------------------------------------------------
    @FXML private void goToHome() throws IOException {
        Parent homeParent = FXMLLoader.load(getClass().getResource("Home.fxml"));
        Scene home = new Scene(homeParent);

        Stage window = (Stage) gpaLabel.getScene().getWindow();
        window.setScene(home);
        window.setTitle("Home");
        window.show();

        Controller_Home.titles.add("Home");
    }
    @FXML private void goToSemesters() throws IOException {
        Parent SemestersParent = FXMLLoader.load(getClass().getResource("Semesters.fxml"));
        Scene semesters = new Scene(SemestersParent);

        Stage window = (Stage) gpaLabel.getScene().getWindow();
        window.setScene(semesters);
        window.setTitle("Semesters");
        window.show();

        Controller_Home.titles.add("Semesters");
    }
    @FXML private void goBack() throws IOException {
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

        Stage window = (Stage) gpaLabel.getScene().getWindow();
        window.setScene(back);
        window.setTitle(Controller_Home.titles.get(n-1));
        window.show();
    }
}
