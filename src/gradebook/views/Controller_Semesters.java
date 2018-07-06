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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import javax.swing.plaf.nimbus.State;
import java.io.IOException;
import java.sql.*;
import java.util.Optional;

public class Controller_Semesters {
    @FXML private Label semesterLabel;
    @FXML private Label emptyLabel;
    @FXML private TitledPane firstSemester;
    @FXML public Accordion semestersAccordion;
    @FXML public ToggleGroup currentSemesterToggle;
    static Accordion copySemesters;
    static Course clickedCourse;
    static int currentIndex = 9999;
    static String viewCourseName = "name";
    private int table_size = 0;

    /****************************************************************
     * Add to initialize                                            *
     * -- check database for current_home                           *
     *                                                              *
     * Add to setCurrentSemester()                                  *
     * -- set current_home and set current "true" value to false    *
     ****************************************************************/

    public void initialize() throws Exception {
        if (Main.gradebookDB != null) {
            copySemesters = semestersAccordion;
            ResultSet s = getSemesterNames(Main.gradebookDB);     // set of semesters

            if (semestersAccordion.getPanes().get(0) == firstSemester) {
                semestersAccordion.getPanes().remove(firstSemester);
            }

//          if set of semesters is not empty, initialize the page
            if (!s.isClosed()) {
                semestersAccordion.setVisible(true);
                emptyLabel.setVisible(false);

                while (s.next()) {
                    TitledPane tp = newSemester(s.getString("name"));
                    semestersAccordion.getPanes().addAll(tp);
                    semestersAccordion.setExpandedPane(tp);
                }
            }

            // if first time initialized, set index
            if (currentIndex == 9999) {
                initializeCurrentIndex();
            }

            // initialize size
            table_size = semestersAccordion.getPanes().size();

            // if accordion is not empty, set radio button
            if (currentIndex > -1 && semestersAccordion.getExpandedPane() != null) {
                semestersAccordion.setExpandedPane(semestersAccordion.getPanes().get(currentIndex));
                if (semestersAccordion.getPanes().indexOf(semestersAccordion.getExpandedPane()) == currentIndex) {
                    currentSemesterToggle.selectToggle(((RadioButton) ((((AnchorPane) (semestersAccordion.getExpandedPane().getContent()))).getChildren().get(0))));
                    semestersAccordion.setExpandedPane(semestersAccordion.getPanes().get(currentIndex));
                }
            }
        }
    }

    // Controls ----------------------------------------------------------------------------------
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
            try {
                if (table_size == 0) {
                    semestersAccordion.setVisible(true);
                    emptyLabel.setVisible(false);
                }

                insertSemester(Main.gradebookDB, popupTitle);
                TitledPane tp = newSemester(popupTitle);
                semestersAccordion.getPanes().add(tp);
                semestersAccordion.setExpandedPane(tp);

                // if table_size is 1, set as current semester
                if (table_size == 1) {
                    currentSemesterToggle.selectToggle(
                            ((RadioButton)((AnchorPane)(semestersAccordion.getExpandedPane().getContent()))
                                    .getChildren().get(0))
                    );

                    setCurrentSemester();
                    currentIndex--;
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        }
    }

    @FXML
    public void deleteSemester() throws Exception {
        String name = semestersAccordion.getExpandedPane().getText();
        int id = getSemesterID(name);

        Statement statement = Main.gradebookDB.createStatement();
        String sql_semesters = "DELETE FROM semesters WHERE name=" + "\"" + name + "\";";
        String sql_courses = "DELETE FROM courses WHERE id_semester=" + id + ";";

        Alert warn = new Alert(Alert.AlertType.CONFIRMATION);
        warn.setTitle("Are you sure?");
        warn.setHeaderText(null);
        warn.setContentText("Are you sure you would like to delete this semester?\n" +
                            "This will delete all grades, courses, and other information.");

        Optional<ButtonType> result = warn.showAndWait();

        // semester is deleted
        if (result.get() == ButtonType.OK) {
            try {
                semestersAccordion.getPanes().remove(semestersAccordion.getExpandedPane());
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
            setCurrentSemester();
        }
    }

    @FXML
    public void setCurrentSemester() {
        String name = semestersAccordion.getExpandedPane().getText();
        currentIndex = semestersAccordion.getPanes().indexOf(semestersAccordion.getExpandedPane());

        // alter database
        try {
            Statement statement = Main.gradebookDB.createStatement();
            String all_false = "UPDATE Semesters SET current_home=0;";
            String set_current = "UPDATE Semesters SET current_home=1 WHERE name=\"" + name + "\";";
            statement.executeUpdate(all_false);
            statement.executeUpdate(set_current);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    // -------------------------------------------------------------------------------------------


    // Helpers -----------------------------------------------------------------------------------
    // get results from query of names from semesters
    private ResultSet getSemesterNames(Connection c) {
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

    // add to database & increment table size
    private void insertSemester(Connection c, String name) {
        Statement statement;

        try {
            statement = c.createStatement();
            String sql = "INSERT INTO semesters (id, name) VALUES (" + Integer.toString(getSemesterID()) + ", " +
                    "\"" + name +"\");";
            statement.executeUpdate(sql);
            table_size++;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // used to set insertSemester's ID
    private int getSemesterID() {
        ResultSet rs;
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id FROM Semesters;";
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

    private int getSemesterID(String name) {
        ResultSet rs;
        int id = -1;

        try {
            Statement statement = Main.gradebookDB.createStatement();
            String sql = "SELECT id, name FROM Semesters;";
            rs = statement.executeQuery(sql);

            if (!rs.isClosed()) {
                while (rs.next()) {
                    if (semestersAccordion.getExpandedPane().getText().equals(rs.getString("name"))) {
                        id = rs.getInt("id");
                    }
                }
            } else {
                id = 1;
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return id;
    }

    // template for semester accordion items
    private TitledPane newSemester(String title) {
        TitledPane template = new TitledPane();
        AnchorPane pane = new AnchorPane();
        RadioButton rb = new RadioButton();
        Button delete = new Button("Delete Semester");
        TableView<Course> tv = new TableView<>();
        Button add = new Button("Add Course");

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
        add.setOnAction(new EventHandler<>() {
            @Override public void handle(ActionEvent event) {
                try {
                    goToAddCourse();
                } catch (IOException ioe) {
                    System.out.println(ioe);
                }
            }
        });


        // set table column data ----------
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

        TableColumn<Course, String> section = new TableColumn<>("Section");
        section.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(p.getValue().section));

        TableColumn<Course, String> description = new TableColumn<>("Description");
        description.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(p.getValue().description));

        TableColumn<Course, String> grade = new TableColumn<>("Grade");
        grade.setCellValueFactory((TableColumn.CellDataFeatures<Course, String> p) ->
                new SimpleStringProperty(p.getValue().letter_grade));

        TableColumn<Course, Integer> credit_hours = new TableColumn<>("Credit Hours");
        credit_hours.setCellValueFactory((TableColumn.CellDataFeatures<Course, Integer> p) ->
                new SimpleIntegerProperty(p.getValue().credit_hours).asObject());


        tv.getColumns().addAll(prefix, number, section, description, grade, credit_hours);
        tv.setOnMousePressed(new EventHandler<>() {
            @Override
            public void handle(MouseEvent click) {
                if (click.isPrimaryButtonDown() && click.getClickCount() == 2) {
                    clickedCourse = tv.getSelectionModel().getSelectedItem();

                    try {
                        goToViewCourse();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        });

        // --------------------------------


        // set constraints ---------------
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

        // -------------------------------


        pane.getChildren().addAll(rb, tv, add, delete);
        template.setContent(pane);
        template.setExpanded(true);

        return template;
    }

    // help for newSemester
    // change for multiple courses
    private ObservableList<Course> getCourses(String semesterName) throws Exception {
        ObservableList<Course> courses = FXCollections.observableArrayList();
        ResultSet rs;
        int id, num;

        Statement s = Main.gradebookDB.createStatement();
        String sql_num = "SELECT * FROM Semesters;";
        ResultSet resultSet = s.executeQuery(sql_num);

        // find current semester id
        num = semestersAccordion.getPanes().size() + 1;
        for (int i = 0; i < num; i++) {
            resultSet.next();
        }

        id = resultSet.getInt("id");




        // create sql statement
        Statement statement = Main.gradebookDB.createStatement();
        String sql = "SELECT prefix, number, section, description, received_points, possible_points, credit_hours " +
                "FROM courses JOIN semesters ON courses.id_semester = semesters.id " +
                "WHERE courses.id_semester = " + Integer.toString(id) + ";";
        rs = statement.executeQuery(sql);

        // go from ResultSet to Course
        int received = 0, possible = 0;
        while(rs.next()) {
            Course c = new Course();
            for (int i = 0; i < 7; i++) {
                if (i == 0 || i == 3) {         // if prefix or description (strings)
                    c.add(i, rs.getString(i+1));
                } else if (i == 2) {            // if section
                    c.add(i, rs.getString(i+1));
                } else if (i == 4) {            // if received_points
                    received = rs.getInt(i+1);    // \too be calculated\
                } else if (i == 5) {            // if possible_points
                    possible = rs.getInt(i+1);
                } else {                        // if number or credit_hours (int)
                    c.add(i, rs.getInt(i+1));
                }
            }

            // get the letter grade
            if ((possible - received) <= 100) {
                c.add(4, "A");
            } else if ((possible - received) <= 200) {
                c.add(4, "B");
            } else if ((possible - received) <= 300) {
                c.add(4, "C");
            } else if ((possible - received) <= 400) {
                c.add(4, "D");
            } else if (received == 0 && possible == 1000) {
                c.add(4, "N/A");
            } else {
                c.add(4, "F");
            }
            courses.addAll(c);
        }

        return courses;
    }

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
    // -------------------------------------------------------------------------------------------


    // Navigation --------------------------------------------------------------------------------
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
    private void goToViewCourse() throws IOException {
        Parent viewCourseParent = FXMLLoader.load(getClass().getResource("Course_View.fxml"));
        Scene viewCourse = new Scene(viewCourseParent);

        Stage window = (Stage) semesterLabel.getScene().getWindow();
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

        Stage window = (Stage) semesterLabel.getScene().getWindow();
        window.setScene(back);
        window.setTitle(Controller_Home.titles.get(n-1));
        window.show();
    }
}
