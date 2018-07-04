package gradebook;

public class Grade {
    public Grade() {
        name = "";
        received = 0;
        possible = 0;
    }

    public Grade(String n, int r, int p) {
        name = n;
        received = r;
        possible = p;
    }

    public String name;
    public int received;
    public int possible;
}
