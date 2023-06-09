package regularproject;

public class Main {
    private String str = "str";

    public String method(String input, int pos) {
        return input.charAt(pos + 1) + str;
    }
}
