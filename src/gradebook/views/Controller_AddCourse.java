package gradebook.views;

import gradebook.Category;
import gradebook.Course;
import gradebook.Main;
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

import static gradebook.views.Controller_Home.titles;
import static gradebook.views.Controller_Semesters.copySemesters;


public class Controller_AddCourse {
    @FXML private Button submitCourseBtn;
    @FXML private Button addCategoryBtn;
    @FXML private TextField prefix;
    @FXML private TextField number;
    @FXML private TextField description;
    @FXML private TextField credit_hours;
    @FXML private TextField name_field;
    @FXML private TextField weight_field;
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, String> name_col;
    @FXML private TableColumn<Category, Integer> weight_col;
    public TitledPane currentSemester;
    private ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<Category> data = categories;
    private int id_course = 1;
    public static int id_semester = 1;



    public void initialize() {
        if (Main.gradebookDB != null) {

            if (titles.get(titles.size()-1).equals("Home")) {

            } else if (titles.get(titles.size()-1).equals("Semesters")) {
                if (copySemesters.getPanes().size() != 0) {
                    currentSemester = copySemesters.getExpandedPane();
                }
            }

            // get data from database
            categoriesTable.setItems(data);

            name_col.setCellValueFactory((TableColumn.CellDataFeatures<Category, String> p) ->
                    new SimpleStringProperty(p.getValue().name));
            weight_col.setCellValueFactory((TableColumn.CellDataFeatures<Category, Integer> p) ->
                    new SimpleIntegerProperty(p.getValue().weight).asObject());
        }
    }

    @FXML
    private void addCategory() {
        // create and add category
        Category category = new Category(name_field.getText(), Integer.parseInt(weight_field.getText()));
        categories.addAll(category);

        // add to database
        try {
            Statement statement = Main.gradebookDB.createStatement();

            // get data
            int id = getCategoryId();
            id_course = getCourseID();
            String sql_insertCategory = "INSERT INTO Course_Categories (id, id_course, name, weight) " +
                    "VALUES (" + Integer.toString(id) + ", " + Integer.toString(id_course) + ", \"" + category.name +
                    "\", " + Integer.toString(category.weight) + ")";

            statement.execute(sql_insertCategory);
        } catch (Exception e) {
            System.out.println(e);
        }


        // reset fields
        name_field.setText("");
        weight_field.setText("");
    }

    private int getCategoryId() {
        int id = 1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT * FROM Course_Categories;";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                id++;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }

    private int getCourseID() {
        int id = 1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT * FROM Courses;";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                id++;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }

    @FXML
    private void submitCourse() {
        // create course
        Course course = new Course();

        // add data
        course.add(0, prefix.getText());
        course.add(1, Integer.parseInt(number.getText()));
        course.add(2, description.getText());
        course.add(5, Integer.parseInt(credit_hours.getText()));
        course.add(categories);

        id_semester = copySemesters.getPanes().indexOf(currentSemester) + 1;
        id_course = getCourseID();

        // sql statement
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql_insertCourse = "INSERT INTO Courses (id, id_semester, prefix, number, description, credit_hours) " +
                    "VALUES (" + Integer.toString(id_course) + ", " + Integer.toString(id_semester) + ", \"" + course.prefix + "\", " +
                    Integer.toString(course.number) + ", \"" + course.description + "\", " +
                    Integer.toString(course.credit_hours) + ");";
            statement.execute(sql_insertCourse);
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            goBack();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @FXML
    private void goToHome() throws IOException {
        Parent homeParent = FXMLLoader.load(getClass().getResource("Home.fxml"));
        Scene home = new Scene(homeParent);

        Stage window = (Stage) submitCourseBtn.getScene().getWindow();
        window.setScene(home);
        window.setTitle("Home");
        window.show();

        titles.add("Home");
    }

    @FXML
    private void goToSemesters() throws IOException {
        Parent SemestersParent = FXMLLoader.load(getClass().getResource("Semesters.fxml"));
        Scene semesters = new Scene(SemestersParent);

        Stage window = (Stage) submitCourseBtn.getScene().getWindow();
        window.setScene(semesters);
        window.setTitle("Semesters");
        window.show();

        titles.add("Semesters");
    }

    @FXML
    private void goToGPA() throws IOException {
        Parent gpaParent = FXMLLoader.load(getClass().getResource("GPA_Calculator.fxml"));
        Scene gpa = new Scene(gpaParent);

        Stage window = (Stage) submitCourseBtn.getScene().getWindow();
        window.setScene(gpa);
        window.setTitle("Calculate GPA");
        window.show();

        titles.add("Calculate GPA");
    }

    @FXML
    private void goBack() throws IOException {
        int n = titles.size() - 1;
        titles.remove(n);

        Parent backParent = null;
        switch (titles.get(n-1)) {
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

        Stage window = (Stage) submitCourseBtn.getScene().getWindow();
        window.setScene(back);
        window.setTitle(titles.get(n-1));
        window.show();
    }
}
