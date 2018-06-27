package gradebook.views;

import gradebook.Course;
import gradebook.Main;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.Optional;

public class Controller_Semesters {
    @FXML private Label semesterLabel;
    @FXML private Label emptyLabel;
    @FXML private TitledPane firstSemester;
    @FXML public Accordion semestersAccordion;
    @FXML public ToggleGroup currentSemesterToggle;
    public static Accordion copySemesters;
    public static int currentIndex = 9999;
    private int table_size = 0;
    private ResultSet s = null;

    public void initialize() throws Exception {
        if (Main.gradebookDB != null) {
            copySemesters = semestersAccordion;
            s = getSemesterNames(Main.gradebookDB);     // set of semesters

//          if set of semesters is not empty, initialize the page
            if (!s.isClosed()) {
                semestersAccordion.setVisible(true);
                emptyLabel.setVisible(false);
                if (semestersAccordion.getPanes().get(0) == firstSemester) {
                    semestersAccordion.getPanes().remove(firstSemester);
                }

                while (s.next()) {

                    TitledPane tp = newSemester(s.getString("name"));
                    semestersAccordion.getPanes().addAll(tp);
                    semestersAccordion.setExpandedPane(tp);
                    table_size++;
                }
            }

            // if not first time initialized
            if (currentIndex == 9999) {
                currentIndex = semestersAccordion.getPanes().size()-1;
            }

//            can't remember which is current semester
//            // if accordion is not empty, set radio button
            if (currentIndex > -1 && semestersAccordion.getExpandedPane() != null) {
                semestersAccordion.setExpandedPane(semestersAccordion.getPanes().get(currentIndex));
                if (semestersAccordion.getPanes().indexOf(semestersAccordion.getExpandedPane()) == currentIndex) {
                    currentSemesterToggle.selectToggle(((RadioButton) ((((AnchorPane) (semestersAccordion.getExpandedPane().getContent()))).getChildren().get(0))));
                    semestersAccordion.setExpandedPane(semestersAccordion.getPanes().get(currentIndex));
                }
            }

        }
    }

    @FXML // create TitledPane
    public void addSemester() {
//      create popup for input
        TextInputDialog popup = new TextInputDialog();
        popup.setTitle("Enter Semester Name...");
        popup.setHeaderText("Enter name of the new semester below...");
        popup.setContentText("Name: ");
        popup.showAndWait();
        String popupTitle = popup.getResult();

//      if input was given
        if (popupTitle != null && !popupTitle.equals("")) {

//          check if table is empty and first has been deleted
            if (table_size == 0) {
                semestersAccordion.setVisible(true);
                semestersAccordion.setExpandedPane(firstSemester);
                emptyLabel.setVisible(false);

                try {
                    insertSemester(Main.gradebookDB, popupTitle);
                    firstSemester.setText(popupTitle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
//            table is not empty
            } else {
                try {
                    insertSemester(Main.gradebookDB, popupTitle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

//          add to accordion
            TitledPane tp = newSemester(popupTitle);
            if (!tp.getText().equals(semestersAccordion.getPanes().get(0).getText())) {
                semestersAccordion.getPanes().add(tp);
                semestersAccordion.setExpandedPane(tp);
            } else {
                semestersAccordion.setExpandedPane(semestersAccordion.getPanes().get(0));
                ((RadioButton)(((AnchorPane)(semestersAccordion.getPanes().get(0).getContent()))
                        .getChildren().get(0))).setSelected(true);
            }

//          if table is only one item, set as current semester
            if (table_size == 1) {
                RadioButton r = (RadioButton)((AnchorPane)tp.getContent()).getChildren().get(0);
                r.setSelected(true);
                currentIndex = semestersAccordion.getPanes().indexOf(semestersAccordion.getExpandedPane()) - 1;
            }
        }
    }

    // add to database & increment table size
    private void insertSemester(Connection c, String name) {
        Statement statement;

        try {
            statement = c.createStatement();
            String sql = "INSERT INTO semesters (id, name) VALUES (\"" + Integer.toString(table_size + 1) + "\", " +
                    "\"" + name +"\");";
            statement.executeUpdate(sql);
            table_size++;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // get results from query of names from semesters
    private ResultSet getSemesterNames(Connection c) throws Exception {
        Statement statement;
        ResultSet rs = null;

        try {
            statement = c.createStatement();
            String sql = "SELECT name FROM semesters ORDER BY id";
            rs = statement.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rs;
    }

    // template for semester accordion items
    private TitledPane newSemester(String title) {
        TitledPane template = new TitledPane();
        AnchorPane pane = new AnchorPane();
        RadioButton rb = new RadioButton();
        Button delete = new Button();
        TableView<Course> tv = new TableView<>();
        Button add = new Button();

        template.setId(title.trim());
        template.setText(title);
        template.setAnimated(true);
        template.setCollapsible(true);

        rb.setText("Current Semester");
        rb.setToggleGroup(currentSemesterToggle);
        rb.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    setCurrentSemester();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        });

        delete.setText("Delete Semester");
        delete.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    deleteSemester();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        });

        // set table column data
        try {
            final ObservableList<Course> data = getCourses(title);
            tv.setItems(data);
        }
        catch (Exception e) {
            System.out.println(e);
        }

        TableColumn<Course, String> prefix = new TableColumn<>("Prefix");
        prefix.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(p.getValue().prefix));

        TableColumn<Course, Integer> number = new TableColumn<>("Number");
        number.setCellValueFactory((TableColumn.CellDataFeatures<Course, Integer> p) ->
                new SimpleIntegerProperty(p.getValue().number).asObject());

        TableColumn<Course, String> description = new TableColumn<>("Description");
        description.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(p.getValue().description));

        TableColumn<Course, String> grade = new TableColumn<>("Grade");
        grade.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(p.getValue().letter_grade));

        TableColumn<Course, Integer> credit_hours = new TableColumn<>("Credit Hours");
        credit_hours.setCellValueFactory((TableColumn.CellDataFeatures<Course, Integer> p) ->
                new SimpleIntegerProperty(p.getValue().credit_hours).asObject());


        tv.getColumns().addAll(prefix, number, description, grade, credit_hours);

        add.setText("Add Course");
        add.setOnAction(new EventHandler<>() {
            @Override public void handle(ActionEvent event) {
                try {
                    goToAddCourse();
                } catch (IOException ioe) {
                    System.out.println(ioe);
                }
            }
        });

        AnchorPane.setTopAnchor(rb, 10.0);
        AnchorPane.setLeftAnchor(rb, 10.0);

        AnchorPane.setTopAnchor(delete, 5.0);
        AnchorPane.setRightAnchor(delete, 10.0);

        AnchorPane.setTopAnchor(tv, 40.0);
        AnchorPane.setLeftAnchor(tv, 10.0);
        AnchorPane.setRightAnchor(tv, 10.0);
        AnchorPane.setBottomAnchor(tv, 40.0);

        AnchorPane.setLeftAnchor(add, 40.0);
        AnchorPane.setRightAnchor(add, 40.0);
        AnchorPane.setBottomAnchor(add, 0.0);

        pane.getChildren().addAll(rb, tv, add, delete);
        template.setContent(pane);
        template.setExpanded(true);

        return template;
    }

    // change for multiple courses
    private ObservableList<Course> getCourses(String semesterName) throws Exception {
        ObservableList<Course> courses = FXCollections.observableArrayList();
        ResultSet rs;
        TitledPane tp = null;
        int id = -1;

        // find semester
        for (int i = 0; i < semestersAccordion.getPanes().size(); i++) {
            if (semesterName.equals(semestersAccordion.getPanes().get(i).getText()) || table_size == 1) {
                tp = semestersAccordion.getPanes().get(i);
                id = i + 1;
            }
        }


        // create sql statement
        Statement statement = Main.gradebookDB.createStatement();
        String sql = "SELECT prefix, number, description, received_points, possible_points, credit_hours " +
                "FROM courses JOIN semesters ON courses.id_semester = semesters.id " +
                "WHERE courses.id_semester = " + Integer.toString(id) + ";";
        rs = statement.executeQuery(sql);

        // go from ResultSet to Course
        int received = 0, possible = 0;
        while(rs.next()) {
            Course c = new Course();
            for (int i = 0; i < 6; i++) {
                if (i == 0 || i == 2) {         // if prefix or description (strings)
                    c.add(i, rs.getString(i+1));
                } else if (i == 3) {             // if received_points
                    received = rs.getInt(i+1);    // \too be calculated\
                } else if (i == 4) {            // if possible_points
                    possible = rs.getInt(i+1);
                } else {                        // if number or credit_hours (int)
                    c.add(i, rs.getInt(i+1));
                }
            }

            // get the letter grade
            if ((received-possible) <= 100) {
                c.add(3, "A");
            } else if ((received-possible) <= 200) {
                c.add(3, "B");
            } else if ((received-possible) <= 300) {
                c.add(3, "C");
            } else if ((received-possible) <= 400) {
                c.add(3, "D");
            } else {
                c.add(3, "F");
            }
            courses.addAll(c);
        }

        return courses;
    }

    @FXML
    public void deleteSemester() throws Exception {
        int id = semestersAccordion.getPanes().indexOf(semestersAccordion.getExpandedPane()) + 1;
        String name = semestersAccordion.getExpandedPane().getText();

        Statement statement = Main.gradebookDB.createStatement();
        String sql_semesters = "DELETE FROM semesters WHERE name=" + "\"" + name + "\";";
        String sql_courses = "DELETE FROM courses WHERE id_semester = " + id + ";";

        Alert warn = new Alert(Alert.AlertType.CONFIRMATION);
        warn.setTitle("Are you sure?");
        warn.setHeaderText(null);
        warn.setContentText("Are you sure you would like to delete this semester?\n" +
                            "This will delete all grades, courses, and other information.");

        Optional<ButtonType> result = warn.showAndWait();

        // semester is deleted
        if (result.get() == ButtonType.OK) {
            try {
                semestersAccordion.getPanes().remove(id-1);
                statement.execute(sql_semesters);
                statement.execute(sql_courses);
                if (currentIndex >= 0) {
                    currentIndex--;
                }
                table_size--;
            } catch (Exception e) {
                System.out.println(e);
            }

        }

        // was firstSemester deleted?
        if (semestersAccordion.getPanes().size() == 0) {
            semestersAccordion.setVisible(false);
            emptyLabel.setVisible(true);
        } else {
            TitledPane titledPane = semestersAccordion.getPanes().get(table_size - 1);

            RadioButton radio = (RadioButton) ((AnchorPane)titledPane.getContent()).getChildren().get(0);
            radio.setSelected(true);
            semestersAccordion.setExpandedPane(titledPane);
        }

//        shiftDatabase();
    }

    @FXML
    public void setCurrentSemester() {
        currentIndex = semestersAccordion.getPanes().indexOf(semestersAccordion.getExpandedPane());
    }

    @FXML
    private void goToHome() throws IOException {
        Parent homeParent = FXMLLoader.load(getClass().getResource("Home.fxml"));
        Scene home = new Scene(homeParent);

        Stage window = (Stage) semesterLabel.getScene().getWindow();
        window.setScene(home);
        window.setTitle("Home");
        window.show();

        Controller_Home.titles.add("Home");
    }

    @FXML
    private void goToGPA() throws IOException {
        Parent gpaParent = FXMLLoader.load(getClass().getResource("GPA_Calculator.fxml"));
        Scene gpa = new Scene(gpaParent);

        Stage window = (Stage) semesterLabel.getScene().getWindow();
        window.setScene(gpa);
        window.setTitle("Calculate GPA");
        window.show();

        Controller_Home.titles.add("Calculate GPA");
    }

    @FXML
    private void goToAddCourse() throws IOException {
        Parent addCourseParent = FXMLLoader.load(getClass().getResource("Course_Add.fxml"));
        Scene addCourse = new Scene(addCourseParent);

        Stage window = (Stage) semesterLabel.getScene().getWindow();
        window.setScene(addCourse);
        window.setTitle("Add Course");
        window.show();

        Controller_Home.titles.add("Add Course");
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

        Stage window = (Stage) semesterLabel.getScene().getWindow();
        window.setScene(back);
        window.setTitle(Controller_Home.titles.get(n-1));
        window.show();
    }
}
