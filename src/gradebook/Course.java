package gradebook;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Course {
    public Course() {
        prefix = "";
        number = 0;
        description = "";
        letter_grade = "";
        credit_hours = 3;
        categories = FXCollections.observableArrayList();
    }

    public Course(String p, int n, String d, String lg, int ch) {
        prefix = p;
        number = n;
        description = d;
        letter_grade = lg;
        credit_hours = ch;

        categories = FXCollections.observableArrayList();
    }

    public Course(String p, int n, String d, String lg, int ch, ObservableList<Category> list) {
        prefix = p;
        number = n;
        description = d;
        letter_grade = lg;
        credit_hours = ch;
        categories = list;
    }

    public void add(int index, String value) {
        if (index == 0) {
            prefix = value;
        } else if (index == 2) {
            description = value;
        } else if (index == 3) {
            letter_grade = value;
        } else {
            System.out.println("Invalid add... String did not go to prefix, description, or letter grade");
        }
    }

    public void add(int index, int value) {
        if (index == 1) {
            number = value;
        } else if (index == 5) {
            credit_hours = value;
        } else {
            System.out.println("Invalid add... String did not go to number or credit_hours");
        }
    }

    public void add(Category cat) {
        categories.addAll(cat);
    }

    public void add(ObservableList<Category> list) {
        categories = list;
    }

    public String prefix;
    public Integer number;
    public String description;
    public String letter_grade;
    public Integer credit_hours;
    public ObservableList<Category> categories;
}
