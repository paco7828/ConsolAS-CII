package cascii.domain;

public class Capacitor extends TwoPinComponent {
    private final double farads;

    public Capacitor(double farads) {
        super("C", "1", "2");
        if (farads <= 0) {
            throw new ConfigurationException("Capacitance must be greater than 0");
        }
        this.farads = farads;
    }

    @Override
    public String getTypeName() {
        return "capacitor";
    }

    @Override
    public String getTypeKey() {
        return "C";
    }

    @Override
    public double getValue() {
        return farads;
    }

    @Override
    public String getValueText() {
        return Units.format(farads);
    }

    @Override
    protected String symbol() {
        return "┤├";
    }
}