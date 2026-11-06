package cascii.domain;

import java.util.ArrayList;

// Common parent of every schematic part
public abstract class Component implements Comparable<Component> {
    private final String prefix;
    private int number;
    private final ArrayList<Pin> pins = new ArrayList<>();

    protected Component(String prefix) {
        this.prefix = prefix;
    }

    // Subclasses call this in their constructor
    protected final void addPin(String name, boolean required) {
        pins.add(new Pin(this, pins.size() + 1, name, required));
    }

    public String getPrefix() {
        return prefix;
    }

    public int getNumber() {
        return number;
    }

    // The circuit gives the number when the part is added
    void setNumber(int number) {
        this.number = number;
    }

    // Reference designator (example: R1, C2, U1, ...)
    public String getRef() {
        return number == 0 ? prefix + "?" : prefix + number;
    }

    public ArrayList<Pin> getPins() {
        return new ArrayList<>(pins);
    }

    // Finds a pin by its name or number
    public Pin getPin(String id) {
        for (Pin pin : pins) {
            if (pin.matches(id)) {
                return pin;
            }
        }
        StringBuilder names = new StringBuilder();
        for (Pin pin : pins) {
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(pin.getName());
        }
        throw new ConfigurationException(getRef() + " has no pin '" + id + "' (pins: " + names + ")");
    }

    // Part's kind as text
    // example: "resistor"
    public abstract String getTypeName();

    // Short key used by the add command
    public abstract String getTypeKey();

    // Numeric value (ohm, farad, henry) or 0 if the part has no value
    public double getValue() {
        return 0;
    }

    // The value as text (example: "50k") or null if the part has no value
    public String getValueText() {
        return null;
    }

    public String describe() {
        String valueText = getValueText();
        if (valueText == null) {
            return getTypeName();
        }
        return getTypeName() + " " + valueText;
    }

    // ASCII drawing of the part, showing where it's pins are connected to
    public abstract String draw();

    public ArrayList<String> check() {
        ArrayList<String> problems = new ArrayList<>();
        for (Pin pin : pins) {
            if (pin.isRequired() && pin.getNet() == null) {
                problems.add(pin.getLabel() + " is not connected");
            }
        }
        return problems;
    }

    // Calculation for the part
    // Only some parts (ICs) can do this
    public String analyze(String[] args) {
        throw new ConfigurationException(getRef() + " (" + getTypeName() + ") has nothing to analyze");
    }

    // Natural order: by prefix, then by number (R1, R2, R10, C1, ...)
    @Override
    public int compareTo(Component other) {
        int result = prefix.compareTo(other.prefix);
        if (result != 0) {
            return result;
        }
        return Integer.compare(number, other.number);
    }
}