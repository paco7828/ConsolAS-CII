package cascii.domain;

import java.util.ArrayList;

// A part with two pins (drawn as one line)
public abstract class TwoPinComponent extends Component {
    protected TwoPinComponent(String prefix, String firstPin, String secondPin) {
        super(prefix);
        addPin(firstPin, true);
        addPin(secondPin, true);
    }

    // The schematic symbol of the part
    // example: [ 50k ]
    protected abstract String symbol();

    @Override
    public String draw() {
        ArrayList<Pin> pins = getPins();
        return getRef() + "  " + describe() + "\n"
                + "    " + pins.get(0).getConnectionLabel()
                + " ───" + symbol() + "─── "
                + pins.get(1).getConnectionLabel();
    }
}