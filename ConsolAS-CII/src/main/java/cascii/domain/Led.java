package cascii.domain;

// Pin 1 = A (anode), pin 2 = K (cathode)
public class Led extends TwoPinComponent {
    public Led() {
        super("LED", "A", "K");
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