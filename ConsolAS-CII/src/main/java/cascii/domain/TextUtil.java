package cascii.domain;

public class TextUtil {
    private TextUtil() {
    }

    // Returns true if text is null or contains ONLY space/tab/newline/cr
    public static boolean isBlank(String text) {
        if (text == null) {
            return true;
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }

        return true;
    }

    // Repeat text for <count> times
    // StringBuilder modifies the existing object's content => uses less memory
    public static String repeat(String text, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(text);
        }
        return sb.toString();
    }

    // Adds <count> spaces
    public static String spaces(int count) {
        return repeat(" ", count);
    }

    // Adds spaces BEFORE the text until it is <width> characters long
    public static String padLeft(String text, int width) {
        return spaces(width - text.length()) + text;
    }

    // Adds spaces AFTER the text until it is <width> characters long
    public static String padRight(String text, int width) {
        return text + spaces(width - text.length());
    }
}