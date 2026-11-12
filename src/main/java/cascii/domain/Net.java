package cascii.domain;

import java.util.ArrayList;

/**
 * A net is a wire that joins two or more pins.
 * Named nets (GND, VIN...) are created by the user, unnamed ones (N1, N2...) are made automatically.
 */
// A net is basically a named connection(-junction)
// Named nets (VIN, GND, ...) = Created by the user
// Unnamed nets (N1, N2) = Created automatically
public class Net {
    private String name;
    private boolean named;
    private final ArrayList<Pin> pins = new ArrayList<>();

    Net(String name, boolean named) {
        this.name = name;
        this.named = named;
    }

    public String getName() {
        return name;
    }

    public boolean isNamed() {
        return named;
    }

    public ArrayList<Pin> getPins() {
        return new ArrayList<>(pins);
    }

    void add(Pin pin) {
        pins.add(pin);
        pin.setNet(this);
    }

    void remove(Pin pin) {
        pins.remove(pin);
        pin.setNet(null);
    }

    // True if any pin on this net belongs to a component of the given type, for example: "capacitor"
    public boolean hasPinOfType(String typeName) {
        for (Pin pin : pins) {
            if (pin.getOwner().getTypeName().equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    // Rename a net to a custom one
    void rename(String newName) {
        name = newName;
        named = true;
    }
}