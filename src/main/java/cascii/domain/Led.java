package cascii.domain;

// Pin 1 = Anode, pin 2 = Cathode
public class Led extends TwoPinComponent {
    public Led() {
        super("LED", "ANODE", "A", "CATHODE", "K");
    }

    @Override
    public String getTypeName() {
        return "LED";
    }

    @Override
    public String getTypeKey() {
        return "LED";
    }

    @Override
    protected String symbol() {
        return "|>|»";
    }
}