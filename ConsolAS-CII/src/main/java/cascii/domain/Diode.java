package cascii.domain;

// Pin 1 = A (anode), pin 2 = K (cathode)
public class Diode extends TwoPinComponent {
    public Diode() {
        super("D", "A", "K");
    }

    @Override
    public String getTypeName() {
        return "diode";
    }

    @Override
    public String getTypeKey() {
        return "D";
    }

    @Override
    protected String symbol() {
        return "|>|";
    }
}