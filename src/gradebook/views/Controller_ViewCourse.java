package gradebook.views;

import gradebook.Category;
import gradebook.Course;
import gradebook.Grade;
import gradebook.Main;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.swing.plaf.nimbus.State;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Optional;

import static gradebook.views.Controller_Semesters.clickedCourse;

public class Controller_ViewCourse {
    @FXML private Label viewLabel;
    @FXML private Label emptyLabel;
    @FXML private TextField prefix;
    @FXML private TextField number;
    @FXML private TextField section;
    @FXML private TextField description;
    @FXML private TextField credit_hours;
    @FXML private TextField points_field;
    @FXML private TextField letter_grade;
    @FXML private Accordion categoryAccordion;
    @FXML private TitledPane firstCategory;

    public void initialize() {
        // initialize course info
        prefix.setText(clickedCourse.prefix);
        number.setText(Integer.toString(clickedCourse.number));
        section.setText(clickedCourse.section);
        description.setText(clickedCourse.description);
        credit_hours.setText(Integer.toString(clickedCourse.credit_hours));
        points_field.setText(getPoints());
        letter_grade.setText(clickedCourse.letter_grade);

        // initialize accordion and grade tables
        initializeAccordion();
        initializeGrades();

        // if accordion is empty
        int accordionSize = categoryAccordion.getPanes().size();
        categoryAccordion.visibleProperty().bind(new SimpleBooleanProperty(accordionSize > 0));
        emptyLabel.visibleProperty().bind(new SimpleBooleanProperty(accordionSize == 0));
    }

    // Controls ------------------------------------------------------------
    @FXML private void addCategory() {
        // popup for category, return popup with result
        Dialog<Pair<String, Integer>> popup = categoryPopup();
        Optional<Pair<String, Integer>> result = popup.showAndWait();

        result.ifPresent(nameWeight -> {
            // get results
            final String name = nameWeight.getKey();
            final Integer weight = nameWeight.getValue();
            Category category = new Category(name, weight);

            // add to category then to accordion
            insertCategory(category);
            categoryAccordion.getPanes().addAll(newCategory(category));

            // show accordion
            int accordionSize = categoryAccordion.getPanes().size();
            categoryAccordion.visibleProperty().bind(new SimpleBooleanProperty(accordionSize > 0));
            emptyLabel.visibleProperty().bind(new SimpleBooleanProperty(accordionSize == 0));
        });
    }
    @FXML private void deleteCategory() {
        String name = categoryAccordion.getExpandedPane().getText();

        Alert warn = new Alert(Alert.AlertType.CONFIRMATION);
        warn.setTitle("Are you sure?");
        warn.setHeaderText(null);
        warn.setContentText("Are you sure you would like to delete this semester?\n" +
                "This will delete all grades, courses, and other information.");

        Optional<ButtonType> result = warn.showAndWait();

        if (result.get() == ButtonType.OK) {
            // remove from database
            try {
                Statement statement = Main.gradebookDB.createStatement();
                String sql = "DELETE FROM Course_Categories " +
                        "WHERE name=\"" + name + "\" AND id_course=" + getCourseID() + ";\n" +
                        "DELETE FROM Grades " +
                        "WHERE id_category=" + getCategoryID(name) + ";";
                statement.executeUpdate(sql);

                // remove from accordion
                categoryAccordion.getPanes().remove(categoryAccordion.getExpandedPane());

            } catch (Exception e) {
                System.out.println(e);
            }

            // if accordion is empty
            int accordionSize = categoryAccordion.getPanes().size();
            categoryAccordion.visibleProperty().bind(new SimpleBooleanProperty(accordionSize > 0));
            emptyLabel.visibleProperty().bind(new SimpleBooleanProperty(accordionSize == 0));
        }
    }
    @FXML private void addGrade() {
        String name = categoryAccordion.getExpandedPane().getText();
        TableView<Grade> table = (TableView<Grade>) (((AnchorPane)categoryAccordion.getExpandedPane().getContent()).getChildren().get(0));

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "INSERT INTO Grades (id, id_category, name) " +
                    "VALUES (" + getNewGradeID() + ", " + getCategoryID(name) + ", \"" + name + "\");";
            statement.executeUpdate(sql);

            // set table
            ObservableList<Grade> grades = getGrades(name);
            table.setItems(grades);

        } catch (Exception e) {
            System.out.println(e);
        }
    }
    @FXML private void submitChanges() {
        // save course info
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "UPDATE Courses " +
                    "SET prefix=\"" + prefix.getText() + "\", " +
                    "number=" + Integer.parseInt(number.getText()) + ", " +
                    "section=\"" + section.getText() + "\", " +
                    "description=\"" + description.getText() + "\", " +
                    "credit_hours=" + Integer.parseInt(credit_hours.getText()) + " " +
                    "WHERE id=" + Integer.toString(getCourseID()) + ";";
            statement.executeUpdate(sql);

            goBack();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    // ---------------------------------------------------------------------


    // Helpers -------------------------------------------------------------
    // make popup, get info, and return as Dialog
    private Dialog<Pair<String, Integer>> categoryPopup() {

        // make popup
        Dialog<Pair<String, Integer>> popup = new Dialog<>();
        popup.setTitle("Add Category");
        popup.setHeaderText("Enter name and weight of the new category below...");

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
        popup.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

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
                    "VALUES (" + Integer.toString(getNewCategoryID()) + ", " + Integer.toString(getCourseID()) +
                    ", \"" + category.name +"\", " + Integer.toString(category.weight) + ");";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // get current IDs
    private int getCourseID() {
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id, prefix, number " +
                    "FROM Courses " +
                    "WHERE prefix=\"" + clickedCourse.prefix + "\" AND number=" + clickedCourse.number + ";";
            ResultSet rs = statement.executeQuery(sql);

            if (rs.next()) {
                id = rs.getInt("id");
            } else {
                id = 1;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }
    private int getCategoryID(String name) {
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id, id_course, name FROM Course_Categories " +
                    "WHERE id_course=" + getCourseID() + " AND name=\"" + name + "\";";
            ResultSet rs = statement.executeQuery(sql);

            if (rs.next()) {
                id = rs.getInt("id");
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }

    // get new IDs
    private int getNewCategoryID() {
        ResultSet rs;
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id FROM Course_Categories;";
            rs = statement.executeQuery(sql);

            if (!rs.isClosed()) {
                while (rs.next()) {
                    id = rs.getInt("id") + 1;
                }
            } else {
                id = 1;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }
    private int getNewGradeID() {
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id FROM Grades;";
            ResultSet rs = statement.executeQuery(sql);

            // if no grades
            if (rs.isClosed()) {
                id = 1;
            }

            while (rs.next()) {
                id = rs.getInt("id") + 1;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }

    // template for Categories
    private TitledPane newCategory(Category category) {
        TitledPane template = new TitledPane();
        AnchorPane content = new AnchorPane();
        TableView<Grade> categoryTableView = new TableView<>();
        Button addGrade = new Button("Add Grade");
        Button deleteCategory = new Button("Delete Category");

        template.setId(category.name.trim());
        template.setText(category.name);
        template.setAnimated(true);
        template.setCollapsible(true);

        addGrade.setOnAction(new EventHandler<>() {
            @Override public void handle(ActionEvent event) {
                try {
                    addGrade();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        });
        deleteCategory.setOnAction(new EventHandler<>() {
            @Override public void handle(ActionEvent event) {
                try {
                    deleteCategory();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        });


        // add data
        try {
            final ObservableList<Grade> data = getGrades(category.name);
            categoryTableView.setItems(data);

        } catch (Exception e) {
            System.out.println(e);
        }


        // add tableView columns
        TableColumn<Grade, String> name = new TableColumn<>("Name");
        name.setCellFactory(TextFieldTableCell.forTableColumn());
        name.setCellValueFactory((TableColumn.CellDataFeatures<Grade, String> p) ->
                new SimpleStringProperty(p.getValue().name));

        TableColumn<Grade, String> points = new TableColumn<>("Points");
        points.setCellFactory(TextFieldTableCell.forTableColumn());
        points.setCellValueFactory((TableColumn.CellDataFeatures<Grade, String> p) ->
                new SimpleStringProperty(Integer.toString(p.getValue().received)));

        TableColumn<Grade, String> outOf = new TableColumn<>("Out Of");
        outOf.setCellFactory(TextFieldTableCell.forTableColumn());
        outOf.setCellValueFactory((TableColumn.CellDataFeatures<Grade, String> p) ->
                new SimpleStringProperty(Integer.toString(p.getValue().possible)));

        // when value is edited
        name.setOnEditCommit(t -> {
            String old_name = t.getTableView().getItems().get(t.getTablePosition().getRow()).name;
            String category_name = categoryAccordion.getExpandedPane().getText();

            // change in database
            try {
                Statement statement = Main.gradebookDB.createStatement();
                String sql = "UPDATE Grades " +
                        "SET name=\"" + t.getNewValue() + "\" " +
                        "WHERE name=\"" + old_name + "\" " +
                        "AND id_category=" + getCategoryID(category_name) + ";";
                statement.executeUpdate(sql);
            } catch (Exception e) {
                System.out.println(e);
            }

            // change in TableView
            t.getTableView().getItems().get(t.getTablePosition().getRow()).name = t.getNewValue();
        });
        points.setOnEditCommit(t -> {
            int old_received = t.getTableView().getItems().get(t.getTablePosition().getRow()).received;
            String grade_name = t.getTableView().getItems().get(t.getTablePosition().getRow()).name;
            String category_name = categoryAccordion.getExpandedPane().getText();

            // change in database
            try {
                Statement statement = Main.gradebookDB.createStatement();
                String sql = "UPDATE Grades " +
                        "SET received_points=" + t.getNewValue() + " " +
                        "WHERE received_points=" + old_received +
                        " AND id_category=" + getCategoryID(category_name) + " " +
                        " AND name=\"" + grade_name + "\";";
                statement.executeUpdate(sql);
                getNewPoints();

                String getPoints = "SELECT id, received_points, possible_points FROM Courses " +
                        "WHERE id=" + getCourseID() + ";";
                ResultSet coursePoints = statement.executeQuery(getPoints);

                if (coursePoints.next()) {
                    int received = coursePoints.getInt("received_points");
                    int possible = coursePoints.getInt("possible_points");
                    points_field.setText(Integer.toString(received));

                    if (possible - received <= 100) {
                        letter_grade.setText("A");
                    } else if (possible - received <= 200) {
                        letter_grade.setText("B");
                    } else if (possible - received <= 300) {
                        letter_grade.setText("C");
                    } else if (possible - received <= 400) {
                        letter_grade.setText("D");
                    } else {
                        letter_grade.setText("F");
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            // change in TableView
            t.getTableView().getItems().get(t.getTablePosition().getRow()).received = Integer.parseInt(t.getNewValue());
        });
        outOf.setOnEditCommit(t -> {
            int old_possible = t.getTableView().getItems().get(t.getTablePosition().getRow()).possible;
            String category_name = categoryAccordion.getExpandedPane().getText();

            // change in database
            try {
                Statement statement = Main.gradebookDB.createStatement();
                String sql = "UPDATE Grades " +
                        "SET possible_points=" + t.getNewValue() + " " +
                        "WHERE possible_points=" + old_possible +
                        " AND id_category=" + getCategoryID(category_name) + ";";
                statement.executeUpdate(sql);
            } catch (Exception e) {
                System.out.println(e);
            }

            // change in TableView
            t.getTableView().getItems().get(t.getTablePosition().getRow()).possible = Integer.parseInt(t.getNewValue());
        });

        categoryTableView.getColumns().addAll(name, points, outOf);
        categoryTableView.setEditable(true);
        categoryTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        categoryTableView.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent keyEvent) {
                final Grade selectedGrade = categoryTableView.getSelectionModel().getSelectedItem();

                if (selectedGrade != null)
                {
                    if (keyEvent.getCode().equals(KeyCode.DELETE))
                    {
                        int idCategory = getCategoryID(categoryAccordion.getExpandedPane().getText());
                        deleteGrade(idCategory, selectedGrade.name);
                        categoryTableView.getItems().remove(selectedGrade);
                    }
                }
            }
        } );


        // set positions
        AnchorPane.setTopAnchor(categoryTableView, 45.0);
        AnchorPane.setBottomAnchor(categoryTableView, 60.0);
        AnchorPane.setLeftAnchor(categoryTableView, 20.0);
        AnchorPane.setRightAnchor(categoryTableView, 20.0);

        AnchorPane.setBottomAnchor(addGrade, 20.0);
        AnchorPane.setLeftAnchor(addGrade, 100.0);
        AnchorPane.setRightAnchor(addGrade, 100.0);

        AnchorPane.setTopAnchor(deleteCategory, 10.0);
        AnchorPane.setRightAnchor(deleteCategory, 20.0);

        content.getChildren().addAll(categoryTableView, addGrade, deleteCategory);
        template.setContent(content);

        return template;
    }
    private void deleteGrade(int idCategory, String name) {
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "DELETE FROM Grades " +
                    "WHERE id_category=" + idCategory + " AND name=\"" + name + "\";";
            statement.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // used to get grades for newCategory() and initializing grade table
    private ObservableList<Grade> getGrades(String categoryName) {
        ObservableList<Grade> grades = FXCollections.observableArrayList();
        ResultSet rs;

        // get grades for category id
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql =
                    "SELECT name, received_points, possible_points FROM Grades " +
                    "WHERE id_category=" + getCategoryID(categoryName) + ";";
            rs = statement.executeQuery(sql);

            while (rs.next()) {
                Grade grade = new Grade(
                        rs.getString("name"),
                        rs.getInt("received_points"),
                        rs.getInt("possible_points")
                );

                grades.addAll(grade);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return grades;
    }

    // calculate Category received_points, Course received_points & possible_points, and Semester gpa
    private void getNewPoints() {
        try {
            // grade -> course points ----------------------------------------------------------------------------
            // get grade points
            int received_points = 0, possible_points = 0,
                    idCategory,
                    idCourse = getCourseID(),
                    idSemester = -1;
            Statement statement = Main.gradebookDB.createStatement();

            String getCategoryIDs =
                    "SELECT id, id_course FROM Course_Categories " +
                    "WHERE id_course=" + idCourse + ";";
            ResultSet idCategoryList = statement.executeQuery(getCategoryIDs);

            while (idCategoryList.next()) {
                idCategory = idCategoryList.getInt("id");

                Statement gradeStatement = Main.gradebookDB.createStatement();
                String getGradePoints =
                        "SELECT id_category, received_points FROM Grades " +
                        "WHERE id_category=" + idCategory + ";";
                ResultSet gradePoints = gradeStatement.executeQuery(getGradePoints);

                // calculate received_points
                while (gradePoints.next()) {
                    received_points += gradePoints.getInt("received_points");
                }
            }

            // get category weight
            String getCategoryPoints =
                    "SELECT id, id_course, weight FROM Course_Categories " +
                    "WHERE id_course=" + idCourse + ";";
            ResultSet categoryPoints = statement.executeQuery(getCategoryPoints);

            // calculate possible_points
            while (categoryPoints.next()) {
                possible_points += categoryPoints.getInt("weight");
            }

            // set course points
            String setCoursePoints =
                    "UPDATE Courses " +
                    "SET received_points=" + received_points + ", possible_points=" + possible_points + " " +
                    "WHERE id=" + idCourse + ";";
            statement.executeUpdate(setCoursePoints);
            // -----------------------------------------------------------------------------------------------------


            // course -> semester gpa ------------------------------------------------------------------------------
            // calculate
            double quality_points = 0, credits = 0;
            String gpa;

            // get idSemester
            Statement statement_SemesterID = Main.gradebookDB.createStatement();
            String getSemesterID =
                    "SELECT id_semester FROM Courses " +
                    "WHERE id=" + getCourseID() + ";";
            ResultSet SemesterID = statement_SemesterID.executeQuery(getSemesterID);

            if(SemesterID.next()) {
                idSemester = SemesterID.getInt("id_semester");
            }

            // get quality points and credits
            String getQualityCredits =
                    "SELECT id, id_semester, received_points, possible_points, credit_hours FROM Courses " +
                    "WHERE id_semester=" + idSemester + ";";
            ResultSet quality_credits = statement.executeQuery(getQualityCredits);

            while (quality_credits.next()) {
                received_points = quality_credits.getInt("received_points");
                possible_points = quality_credits.getInt("possible_points");
                credits += quality_credits.getInt("credit_hours");
                idSemester = quality_credits.getInt("id_semester");

                if (possible_points - received_points <= 100) {
                    quality_points += 4*(quality_credits.getInt("credit_hours"));
                } else if (possible_points - received_points <= 200) {
                    quality_points += 3*(quality_credits.getInt("credit_hours"));
                } else if (possible_points - received_points <= 300) {
                    quality_points += 2*(quality_credits.getInt("credit_hours"));
                } else if (possible_points - received_points <= 400) {
                    quality_points += (quality_credits.getInt("credit_hours"));
                }
            }

            // check for rounding
            gpa = Double.toString(quality_points/credits);
            if (gpa.length() >= 4) {
                if (Character.getNumericValue(gpa.charAt(4)) >= 5) {
                    gpa = gpa.substring(0, 3) + Integer.toString(Character.getNumericValue(gpa.charAt(3)) + 1);
                } else {
                    gpa = gpa.substring(0, 4);
                }
            }


            // set semester GPA
            if (!gpa.equals("NaN")) {
                String setGPA =
                        "UPDATE Semesters " +
                        "SET gpa=" + gpa + " " +
                        "WHERE id=" + idSemester + ";";
                statement.executeUpdate(setGPA);
            }
            // -----------------------------------------------------------------------------------------------------


        } catch (Exception e) {
            System.out.println(e);
        }
    }
    // ---------------------------------------------------------------------


    // Initialization -------------------------------------------------------
    private void initializeAccordion() {
        TitledPane titledPane;
        categoryAccordion.getPanes().remove(firstCategory);

        // get categories for this course
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT name, weight FROM Course_Categories " +
                    "WHERE id_course=" + getCourseID() + ";";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                Category category = new Category(rs.getString("name"), rs.getInt("weight"));;
                categoryAccordion.getPanes().addAll(newCategory(category));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    private void initializeGrades() {
        for (int i = 0; i < categoryAccordion.getPanes().size(); i++) {
            String name = categoryAccordion.getPanes().get(i).getText();
            TableView<Grade> gradeTableView = (TableView<Grade>)
                    ((AnchorPane)(categoryAccordion.getPanes().get(i).getContent())).getChildren().get(0);

            gradeTableView.setItems(getGrades(name));
            gradeTableView.refresh();
        }
    }
    private String getPoints() {
        String points = "N/A";
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id, received_points FROM Courses WHERE id=" + getCourseID() + ";";
            ResultSet rs = statement.executeQuery(sql);

            if (rs.next()) {
                points = Integer.toString(rs.getInt("received_points"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return points;
    }
    // ---------------------------------------------------------------------

    // Navigation -------------------------------------------
    @FXML private void goToHome() throws IOException {
        Parent homeParent = FXMLLoader.load(getClass().getResource("Home.fxml"));
        Scene home = new Scene(homeParent);

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(home);
        window.setTitle("Home");
        window.show();

        Controller_Home.titles.add("Home");
    }
    @FXML private void goToSemesters() throws IOException {
        Parent SemestersParent = FXMLLoader.load(getClass().getResource("Semesters.fxml"));
        Scene semesters = new Scene(SemestersParent);

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(semesters);
        window.setTitle("Semesters");
        window.show();

        Controller_Home.titles.add("Semesters");
    }
    @FXML private void goToGPA() throws IOException {
        Parent gpaParent = FXMLLoader.load(getClass().getResource("GPA_Calculator.fxml"));
        Scene gpa = new Scene(gpaParent);

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(gpa);
        window.setTitle("Calculate GPA");
        window.show();

        Controller_Home.titles.add("Calculate GPA");
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

        Stage window = (Stage) viewLabel.getScene().getWindow();
        window.setScene(back);
        window.setTitle(Controller_Home.titles.get(n-1));
        window.show();
    }
}
