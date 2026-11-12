package cascii.domain;

public class Resistor extends TwoPinComponent {
    private final double ohms;

    public Resistor(double ohms) {
        super("R", "1", "2");
        if (ohms < 0) {
            throw new ConfigurationException("Resistance must be greater than or equal to 0");
        }
        this.ohms = ohms;
    }

    @Override
    public String getTypeName() {
        return "resistor";
    }

    @Override
    public String getTypeKey() {
        return "R";
    }

    @Override
    public double getValue() {
        return ohms;
    }

    @Override
    public String getValueText() {
        return Units.format(ohms);
    }

    @Override
    protected String symbol() {
        return "[ " + getValueText() + " ]";
    }
}