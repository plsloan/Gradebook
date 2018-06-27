package gradebook;

public class Category {
    public Category() {
        name = "";
        weight = 0;
    }

    public Category(String n, int w) {
        name = n;
        weight = w;
    }

    public void add(String n) {
        name = n;
    }

    public void add(int w) {
        weight = w;
    }


    public String name;
    public Integer weight;  // points
}
