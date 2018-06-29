package gradebook.views;

import gradebook.Category;
import gradebook.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class Controller_ViewCourse {
    @FXML private Label viewLabel;
    @FXML private TextField prefix;
    @FXML private TextField number;
    @FXML private TextField description;
    @FXML private TextField credit_hours;
    @FXML private Accordion categoryAccordion;
    @FXML private TitledPane firstCategory;

    /********************************************
     *                                          *
     * Add label when categories table is empty *
     * Add initialization and template          *
     *                                          *
     *******************************************/



    public void initialize() {

    }

    // Helpers ----------------------------------------------
    // make popup, get info, and return as Category
    private Dialog<Pair<String, Integer>> categoryPopup() {

        // make popup
        Dialog<Pair<String, Integer>> popup = new Dialog<>();
        popup.setTitle("Add Category");
        popup.setHeaderText("Enter name and weight of the new category below...");
        popup.setContentText("Name: ");
        popup.showAndWait();



        // make pane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField name_field = new TextField();
        TextField weight_field = new TextField();
        name_field.setPromptText("Tests");
        weight_field.setPromptText("100");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(name_field, 1, 0);
        grid.add(new Label("Weight:"), 0, 1);
        grid.add(weight_field, 1, 1);



        // set content and format results
        popup.getDialogPane().setContent(grid);
        Platform.runLater(() -> name_field.requestFocus());
        popup.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Pair<>(name_field.getText(), Integer.parseInt(weight_field.getText()));
            }
            return null;
        });

        return popup;
    }

    // adds category to database
    private void insertCategory(Category category) {
        Statement statement;

        try {
            statement = Main.gradebookDB.createStatement();
            String sql = "INSERT INTO Course_Categories (id, id_course, name, weight) " +
                    "VALUES (" + Integer.toString(getCategoryID()) + ", " + Integer.toString(getCourseID()) +
                    ", " + category.name +"\", " + Integer.toString(category.weight) + ");";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // get new ID for insertCategory
    private int getCategoryID() {
        ResultSet rs;
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id FROM Course_Categories;";
            rs = statement.executeQuery(sql);

            if (!rs.isClosed()) {
                rs.last();
                id = rs.getInt("id") + 1;
            } else {
                id = 1;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }

    private int getCourseID() {
        ResultSet rs;
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();

            /************************
             * Fix SQL statement    *
             ***********************/

            String sql = "SELECT id FROM Courses;";
            rs = statement.executeQuery(sql);

            while (rs.next()) {
                id = rs.getInt("id");
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }

    // template for Categories (TitledPane)
    private TitledPane newCategory(Category category) {
        TitledPane template = new TitledPane();
        return template;
    }

    // ------------------------------------------------------


    // Controls ---------------------------------------------
    @FXML
    private void addCategory() {
        // popup for category, return popup with result
        Dialog<Pair<String, Integer>> popup = categoryPopup();
        Optional<Pair<String, Integer>> result = popup.showAndWait();

        // get results
        result.ifPresent(nameWeight -> {
            final String name = nameWeight.getKey();
            final Integer weight = nameWeight.getValue();
            Category category = new Category(name, weight);

            // add to category then to accordion
            insertCategory(category);
            TitledPane newCategory = newCategory(category);
            categoryAccordion.getPanes().addAll(newCategory);
            categoryAccordion.setExpandedPane(newCategory);
        });
    }

    @FXML
    private void deleteCategory() {
        String name = categoryAccordion.getExpandedPane().getText();

        /************************
         * Fix SQL statement    *
         ***********************/

        // remove from database
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "DELETE FROM Course_Categories WHERE ;";

            // remove from accordion
            categoryAccordion.getPanes().remove(categoryAccordion.getExpandedPane());

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @FXML
    private void addGrade() {

    }

    @FXML
    private void submitChanges() {


        // finished... return to previous page
        try {
            goBack();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    // ------------------------------------------------------


    // Navigation -------------------------------------------
    @FXML
    private void goToHome() throws IOException {
        Parent homeParent = FXMLLoader.load(getClass().getResource("Home.fxml"));
        Scene home = new Scene(homeParent);

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(home);
        window.setTitle("Home");
        window.show();

        Controller_Home.titles.add("Home");
    }

    @FXML
    private void goToSemesters() throws IOException {
        Parent SemestersParent = FXMLLoader.load(getClass().getResource("Semesters.fxml"));
        Scene semesters = new Scene(SemestersParent);

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(semesters);
        window.setTitle("Semesters");
        window.show();

        Controller_Home.titles.add("Semesters");
    }

    @FXML
    private void goToGPA() throws IOException {
        Parent gpaParent = FXMLLoader.load(getClass().getResource("GPA_Calculator.fxml"));
        Scene gpa = new Scene(gpaParent);

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(gpa);
        window.setTitle("Calculate GPA");
        window.show();

        Controller_Home.titles.add("Calculate GPA");
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

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(back);
        window.setTitle(Controller_Home.titles.get(n-1));
        window.show();
    }
}
