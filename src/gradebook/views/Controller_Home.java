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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.plaf.nimbus.State;
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
    @FXML private TextField completed_GPA;
    @FXML private TextField potential_GPA;
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
        homeTable.setColumnResizePolicy(new Callback<TableView.ResizeFeatures, Boolean>() {
            @Override
            public Boolean call(TableView.ResizeFeatures p) {
                return true;
            }
        });

        initializeCurrentIndex();
        getCurrentTable();
        setCurrentGPA();
        setCompletedGPA();
        setPotentialGPA();
    }

    // Helpers -----------------------------------------------------------------------------------------------------
    // initialization
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
            String sql_getRecentTable = "SELECT * FROM Courses WHERE id_semester=" + Integer.toString(index) + ";";
            ResultSet rs = statement.executeQuery(sql_getRecentTable);

            // gets letter grade string
            // adds data
            while (rs.next()) {
                String letter_grade;
                double received = rs.getInt("received_points");
                double possible = rs.getInt("possible_points");

                if (received/possible >= 0.9) {
                    letter_grade = "A";
                } else if (received/possible >= 0.8) {
                    letter_grade = "B";
                } else if (received/possible >= 0.7) {
                    letter_grade = "C";
                } else if (received/possible >= 0.6) {
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
            homeTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(final KeyEvent keyEvent)
                {
                    final Course selectedCourse = homeTable.getSelectionModel().getSelectedItem();

                    if (selectedCourse != null)
                    {
                        if (keyEvent.getCode().equals(KeyCode.DELETE))
                        {
                            int idCourse = deleteCourse(selectedCourse);
                            ArrayList<Integer> idsCategories = deleteCategory(idCourse);
                            deleteGrade(idsCategories);
                            homeTable.getItems().remove(selectedCourse);
                        }
                    }
                }
            } );

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
            double quality_points = 0, credits = 0;
            String gpa;
            Statement statement = Main.gradebookDB.createStatement();
            String get_semester =
                    "SELECT id_semester, received_points, possible_points, credit_hours FROM Courses " +
                    "WHERE id_semester=" + semesterID + ";";
            ResultSet currentSemester = statement.executeQuery(get_semester);

            while (currentSemester.next()) {
                double received = currentSemester.getInt("received_points");
                double possible = currentSemester.getInt("possible_points");
                int credit_hours = currentSemester.getInt("credit_hours");
                int currentID = currentSemester.getInt("id_semester");

                if (currentID == semesterID) {
                    if (received != 0) {
                        credits += credit_hours;
                    }

                    if (received/possible >= 0.9) {
                        quality_points += 4*credit_hours;
                    } else if (received/possible >= 0.8) {
                        quality_points += 3*credit_hours;
                    } else if (received/possible >= 0.7) {
                        quality_points += 2*credit_hours;
                    } else if (received/possible >= 0.6) {
                        quality_points += credit_hours;
                    }
                }
            }

            gpa = Double.toString(quality_points/credits);
            if (gpa.length() > 4) {
                if (Character.getNumericValue(gpa.charAt(4)) >= 5) {
                    gpa = gpa.substring(0, 3) + Integer.toString(Character.getNumericValue(gpa.charAt(3)) + 1);
                } else {
                    gpa = gpa.substring(0, 4);
                }
            }

            if (gpa.equals("NaN") || gpa.equals("0.0")) {
                current_GPA.setText("N/A");
            } else {
                current_GPA.setText(gpa);
            }


        } catch (Exception e) {
            System.out.println(e);
        }
    }
    private void setCompletedGPA() {
        double quality_points = 0, credits = 0;

        try {
            String gpa;

            Statement statement = Main.gradebookDB.createStatement();
            String sql_completedGPA =
                    "SELECT id, id_semester, received_points, possible_points, credit_hours FROM Courses;";
            ResultSet grades = statement.executeQuery(sql_completedGPA);

            while (grades.next()) {
                double received = grades.getInt("received_points");
                double possible = grades.getInt("possible_points");
                int credit_hours = grades.getInt("credit_hours");
                int currentID = grades.getInt("id_semester");

                if (currentID != semesterID) {
                    if (received != 0) {
                        credits += credit_hours;
                    }

                    if (received/possible >= 0.9) {
                        quality_points += 4*credit_hours;
                    } else if (received/possible >= 0.8) {
                        quality_points += 3*credit_hours;
                    } else if (received/possible >= 0.7) {
                        quality_points += 2*credit_hours;
                    } else if (received/possible >= 0.6) {
                        quality_points += credit_hours;
                    }
                }
            }

            gpa = Double.toString(quality_points/credits);

            // check for rounding
            if (gpa.length() > 4) {
                if (Character.getNumericValue(gpa.charAt(4)) >= 5) {
                    gpa = gpa.substring(0, 3) + Integer.toString(Character.getNumericValue(gpa.charAt(3)) + 1);
                } else {
                    gpa = gpa.substring(0, 4);
                }
            }

            if (gpa.equals("NaN") || gpa.equals("0.0")) {
                completed_GPA.setText("N/A");
            } else {
                completed_GPA.setText(gpa);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
    private void setPotentialGPA() {
        double quality_points = 0.0, credits = 0.0;

        try {
            String gpa;
            Statement statement = Main.gradebookDB.createStatement();
            String get_overall =
                    "SELECT id, name, printf(\"%.2f\", gpa) as gpa, credits " +
                    "FROM Semesters;";
            ResultSet rs = statement.executeQuery(get_overall);

            while (rs.next()) {
                quality_points += rs.getDouble("gpa") * rs.getInt("credits");
                if (rs.getDouble("gpa") != 0) {
                    credits += rs.getInt("credits");
                }
            }

            gpa = Double.toString(quality_points/credits);

            // check for rounding
            if (gpa.length() > 4) {
                if (Character.getNumericValue(gpa.charAt(4)) >= 5) {
                    gpa = gpa.substring(0, 3) + Integer.toString(Character.getNumericValue(gpa.charAt(3)) + 1);
                } else {
                    gpa = gpa.substring(0, 4);
                }
            }

            if (gpa.equals("NaN") || gpa.equals("0.0")) {
                potential_GPA.setText("N/A");
            } else {
                potential_GPA.setText(gpa);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // used to delete Course, Categories, and Grades with delete keystroke
    private int deleteCourse(Course course) {
        int id = -1;
        ResultSet rs;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id FROM Courses WHERE prefix=\"" + course.prefix + "\" AND number=" + course.number + ";";
            rs = statement.executeQuery(sql);
            id = rs.getInt("id");

            sql = "DELETE FROM Courses WHERE prefix=\"" + course.prefix + "\" AND number=" + course.number + ";";
            statement.executeUpdate(sql);

            // update gpa
            sql = "SELECT received_points, possible_points, credit_hours FROM Courses " +
                    "WHERE id_semester=" + semesterID + ";";
            ResultSet newGPA = statement.executeQuery(sql);
            double received = newGPA.getInt("received_points"),
                    possible = newGPA.getInt("possible_points"),
                    qualityPoints = 0, credits = 0;
            int credit_hours = newGPA.getInt("credit_hours");
            while (newGPA.next()) {
                if (received != 0) {
                    credits += credit_hours;
                }

                if (received/possible >= 0.9) {
                    qualityPoints += 4*credit_hours;
                } else if (received/possible >= 0.8) {
                    qualityPoints += 3*credit_hours;
                } else if (received/possible >= 0.7) {
                    qualityPoints += 2*credit_hours;
                } else if (received/possible >= 0.6) {
                    qualityPoints += credit_hours;
                }
            }

            String gpa = Double.toString(qualityPoints/credits);
            if (gpa.length() > 4) {
                if (Character.getNumericValue(gpa.charAt(4)) >= 5) {
                    gpa = gpa.substring(0, 3) + Integer.toString(Character.getNumericValue(gpa.charAt(3)) + 1);
                } else {
                    gpa = gpa.substring(0, 4);
                }
            }


            sql = "UPDATE Semesters SET credits=credits-" + course.credit_hours + ", gpa=" + gpa + " " +
                    "WHERE id=" + semesterID + ";";

            statement.executeUpdate(sql);

            setCurrentGPA();
            setCompletedGPA();
            setPotentialGPA();
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }
    private ArrayList<Integer> deleteCategory(int idCourse) {
        ArrayList<Integer> IDs = null;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id FROM Course_Categories WHERE id_course=" + idCourse + ";";
            ResultSet rs = statement.executeQuery(sql);

            IDs = new ArrayList<>();
            while (rs.next()) {
                IDs.add(rs.getInt("id"));
            }

            sql = "DELETE FROM Course_Categories WHERE id_course=" + idCourse + ";";
            statement.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println(e);
        }

        return IDs;
    }
    private void deleteGrade(ArrayList<Integer> idsCategories) {
        try {
            Statement statement = Main.gradebookDB.createStatement();
            for (int i = 0; i < idsCategories.size(); i++) {
                String sql = "DELETE FROM Grades WHERE id_category=" + idsCategories.get(i) + ";";
                statement.executeUpdate(sql);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // used in getCurrentTable()
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
    // -------------------------------------------------------------------------------------------------------------


    // Navigation --------------------------------------------------------------------------------------------------
    @FXML private void goToSemesters() throws IOException {
        Parent SemestersParent = FXMLLoader.load(getClass().getResource("Semesters.fxml"));
        Scene semesters = new Scene(SemestersParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(semesters);
        window.setTitle("Semesters");
        window.show();

        titles.add("Semesters");
    }
    @FXML private void goToGPA() throws IOException {
        Parent gpaParent = FXMLLoader.load(getClass().getResource("GPA_Calculator.fxml"));
        Scene gpa = new Scene(gpaParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(gpa);
        window.setTitle("Calculate GPA");
        window.show();

        titles.add("Calculate GPA");
    }
    @FXML private void goToAddCourse() throws IOException {
        Parent addCourseParent = FXMLLoader.load(getClass().getResource("Course_Add.fxml"));
        Scene addCourse = new Scene(addCourseParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(addCourse);
        window.setTitle("Add Course");
        window.show();

        titles.add("Add Course");
    }
    @FXML private void goToViewCourse() throws IOException {
        Parent viewCourseParent = FXMLLoader.load(getClass().getResource("Course_View.fxml"));
        Scene viewCourse = new Scene(viewCourseParent);

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(viewCourse);
        window.setTitle(clickedCourse.description);
        window.show();

        Controller_Home.titles.add("View Course");
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

        Stage window = (Stage) semesterBtn.getScene().getWindow();
        window.setScene(back);
        window.setTitle(Controller_Home.titles.get(n-1));
        window.show();
    }
}
