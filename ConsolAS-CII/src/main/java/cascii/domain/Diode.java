package cascii.domain;

// Pin 1 = Anode, pin 2 = Cathode
public class Diode extends TwoPinComponent {
    public Diode() {
        super("D", "ANODE", "A", "CATHODE", "K");
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