package cascii.domain;

// A pin belongs to 1 component and maximum 1 net
public class Pin {
    private final Component owner;
    private final int number;
    private final String name;
    private final boolean required;
    private final String alias;
    private Net net;

    Pin(Component owner, int number, String name, String alias, boolean required) {
        this.owner = owner;
        this.number = number;
        this.name = name;
        this.alias = alias;
        this.required = required;
    }

    public Component getOwner() {
        return owner;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    // A required pin is reported when left unconnected ("check" message)
    public boolean isRequired() {
        return required;
    }

    public Net getNet() {
        return net;
    }

    void setNet(Net net) {
        this.net = net;
    }

    // Full label name
    // example: U1.VIN = U1's VIN pin
    public String getLabel() {
        return owner.getRef() + "." + name;
    }

    // True if the text is the name or the number of this pin
    // A name like GND/EPAD can also be given as just GND
    public boolean matches(String id) {
        if (name.equalsIgnoreCase(id) || String.valueOf(number).equals(id)) {
            return true;
        }

        if (alias != null && alias.equalsIgnoreCase(id)) {
            return true;
        }

        int slash = name.indexOf('/');
        return slash > 0 && name.substring(0, slash).equalsIgnoreCase(id);
    }

    // What the pin is connected to
    // example: NC = not connected
    // example: <Net_Name> = Net's name
    public String getConnectionLabel() {
        if (net == null) {
            return "NC";
        }
        if (net.isNamed()) {
            return net.getName();
        }
        StringBuilder text = new StringBuilder();
        for (Pin other : net.getPins()) {
            if (other != this) {
                if (!text.isEmpty()) {
                    text.append(",");
                }
                text.append(other.getLabel());
            }
        }
        return text.toString();
    }
}