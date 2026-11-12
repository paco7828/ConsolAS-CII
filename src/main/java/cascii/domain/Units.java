package cascii.domain;

public class Units {
    /*
     * p  = pico  = 10^(-12)
     * n  = nano  = 10^(-9)
     * u  = micro = 10^(-6)
     * m  = milli = 10^(-3)
     * "" = base  = 10^1
     * k  = kilo  = 10^3
     * M  = Mega  = 10^6
     * G  = Giga  = 10^9
     */
    private static final String[] SUFFIX = {"p", "n", "u", "m", "", "k", "M", "G"};
    private static final int[] EXPONENT = {-12, -9, -6, -3, 0, 3, 6, 9};

    private Units() {
    }

    // Converts values with prefixes to whole numbers
    // example: 50k -> 50000
    public static double parse(String text) {
        // Empty value
        if (TextUtil.isBlank(text)) {
            throw new ConfigurationException("Value is missing");
        }

        // Remove whitespaces surrounding the text
        String number = text.trim();

        // Optional unit letter at the end: 5V, 100mA, 10uF, 1mH
        char unit = number.charAt(number.length() - 1);
        if (number.length() > 1 && "VAFHRvafhr\u03a9".indexOf(unit) >= 0) {
            number = number.substring(0, number.length() - 1);
        }

        int exponent = 0;

        // Checks for prefix at the end of the value
        int index = suffixIndex(number.charAt(number.length() - 1));
        if (index >= 0) {
            exponent = EXPONENT[index];
            number = number.substring(0, number.length() - 1);
        }

        // Wrong value param
        if (!isNumber(number)) {
            throw new ConfigurationException("Invalid value: '" + text + "' (examples: 50k, 4.7k, 100n, 10u, 220)");
        }
        // "4.7" + "e3" = 4700
        return Double.parseDouble(number + "e" + exponent);
    }

    // Converts whole numbers to values with prefixes
    // example: 50000 -> 50k
    public static String format(double value) {
        // Accepted value: more than 0
        if (value <= 0) {
            return "0";
        }

        // Calculates needed prefix
        int index = 0;
        for (int i = EXPONENT.length - 1; i >= 0; i--) {
            if (value >= Math.pow(10, EXPONENT[i]) * 0.9999999) {
                index = i;
                break;
            }
        }

        // Rounds value to 2 decimal points
        double scaled = round(value / Math.pow(10, EXPONENT[index]), 2);
        String number;

        // If number is integer => return WITHOUT decimal places
        // If number is double => return WITH decimal places
        if (scaled == Math.floor(scaled)) {
            number = String.valueOf((long) scaled);
        } else {
            number = String.valueOf(scaled);
        }

        return number + SUFFIX[index];
    }

    // Rounds to the given number of decimals
    // example: round(23.8472, 1) -> 23.8
    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    // Finds the index of the given prefix
    private static int suffixIndex(char c) {
        if (c == 'K') {
            c = 'k';
        }

        // 'µ' check
        if (c == '\u00b5') {
            c = 'u';
        }

        for (int i = 0; i < SUFFIX.length; i++) {
            if (SUFFIX[i].length() == 1 && SUFFIX[i].charAt(0) == c) {
                return i;
            }
        }

        return -1;
    }

    // Checks if the text contains only numbers or one dot (decimal indicator)
    private static boolean isNumber(String text) {
        boolean hasDigit = false;
        boolean hasDot = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else if (c == '.' && !hasDot) {
                hasDot = true;
            } else {
                return false;
            }
        }

        return hasDigit;
    }
}