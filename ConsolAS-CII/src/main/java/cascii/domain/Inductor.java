package cascii.domain;

public class Inductor extends TwoPinComponent {
    private final double henries;

    public Inductor(double henries) {
        super("L", "1", "2");
        if (henries <= 0) {
            throw new ConfigurationException("Inductance must be greater than 0");
        }
        this.henries = henries;
    }

    @Override
    public String getTypeName() {
        return "inductor";
    }

    @Override
    public String getTypeKey() {
        return "L";
    }

    @Override
    public double getValue() {
        return henries;
    }

    @Override
    public String getValueText() {
        return Units.format(henries);
    }

    @Override
    protected String symbol() {
        return "((((((";
    }
}