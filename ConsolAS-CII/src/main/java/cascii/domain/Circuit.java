package cascii.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// A schematic has components (with pins) and nets that join pins together.
public class Circuit {
    private final String name;
    private final ArrayList<Component> components = new ArrayList<>();
    private final ArrayList<Net> nets = new ArrayList<>();
    private int autoNetCounter = 0;

    public Circuit(String name) {
        if (TextUtil.isBlank(name)) {
            throw new ConfigurationException("Circuit name must not be empty");
        }

        if (name.indexOf(';') >= 0) {
            throw new ConfigurationException("Circuit name must not contain ';'");
        }

        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    // Adds the component with a designator (R1, R2...)
    public void add(Component component) {
        component.setNumber(nextNumber(component.getPrefix()));
        components.add(component);
    }

    // Adds the component with a given number (used when importing a circuit)
    public void add(Component component, int number) {
        if (number <= 0) {
            throw new ConfigurationException("Component number must be positive");
        }

        component.setNumber(number);

        for (Component existing : components) {
            if (existing.getRef().equalsIgnoreCase(component.getRef())) {
                throw new ConfigurationException("Duplicate component: " + component.getRef());
            }
        }

        components.add(component);
    }

    private int nextNumber(String prefix) {
        int max = 0;

        for (Component component : components) {
            if (component.getPrefix().equals(prefix) && component.getNumber() > max) {
                max = component.getNumber();
            }
        }

        return max + 1;
    }

    public Component find(String ref) {
        for (Component component : components) {
            if (component.getRef().equalsIgnoreCase(ref)) {
                return component;
            }
        }

        throw new ConfigurationException("No such component: " + ref);
    }

    // Finds a pin from text
    // example: R1.1, U1.VIN
    public Pin findPin(String reference) {
        int dot = reference.indexOf('.');

        if (dot <= 0 || dot == reference.length() - 1) {
            throw new ConfigurationException("Not a pin: '" + reference + "' (examples: R1.1, U1.VIN)");
        }

        return find(reference.substring(0, dot)).getPin(reference.substring(dot + 1));
    }

    // Removes the component and related connections
    public void remove(String ref) {
        Component component = find(ref);

        for (Pin pin : component.getPins()) {
            if (pin.getNet() != null) {
                detach(pin);
            }
        }

        components.remove(component);
    }

    public ArrayList<Component> getComponents() {
        return new ArrayList<>(components);
    }

    // Sorted components based on their designators
    public ArrayList<Component> getComponentsSorted(Comparator<Component> comparator) {
        ArrayList<Component> copy = new ArrayList<>(components);
        Collections.sort(copy, comparator);
        return copy;
    }

    public ArrayList<Net> getNets() {
        return new ArrayList<>(nets);
    }

    // Connects 2 things
    // At least one side must be a pin
    // Returns the net
    public Net connect(String a, String b) {
        Pin pinA = isPin(a) ? findPin(a) : null;
        Pin pinB = isPin(b) ? findPin(b) : null;

        if (pinA == null && pinB == null) {
            throw new ConfigurationException("At least one side must be a pin (e.g. R1.1), not only net names");
        }

        if (pinA != null && pinB != null) {
            joinPins(pinA, pinB);
            return pinA.getNet();
        }

        if (pinA != null) {
            joinPinToNet(pinA, b);
            return pinA.getNet();
        }

        joinPinToNet(pinB, a);
        return pinB.getNet();
    }

    // Removes a pin from its net
    public void disconnect(String pinReference) {
        Pin pin = findPin(pinReference);

        if (pin.getNet() == null) {
            throw new ConfigurationException(pin.getLabel() + " is not connected");
        }

        detach(pin);
    }

    private boolean isPin(String text) {
        return text.indexOf('.') >= 0;
    }

    private void joinPins(Pin a, Pin b) {
        if (a == b) {
            throw new ConfigurationException("A pin cannot be connected to itself");
        }

        if (a.getNet() == null && b.getNet() == null) {
            autoNetCounter++;
            Net net = new Net("N" + autoNetCounter, false);
            nets.add(net);
            net.add(a);
            net.add(b);
        } else if (a.getNet() == null) {
            b.getNet().add(a);
        } else if (b.getNet() == null) {
            a.getNet().add(b);
        } else if (a.getNet() == b.getNet()) {
            throw new ConfigurationException(a.getLabel() + " and " + b.getLabel() + " are already connected");
        } else {
            merge(a.getNet(), b.getNet());
        }
    }

    private void joinPinToNet(Pin pin, String netName) {
        String name = checkNetName(netName);
        Net current = pin.getNet();

        if (current != null && current.isNamed()) {
            if (current.getName().equals(name)) {
                throw new ConfigurationException(pin.getLabel() + " is already on net " + name);
            }
            throw new ConfigurationException(pin.getLabel() + " is on net " + current.getName()
                    + ", connecting it to " + name + " would short the two nets");
        }

        Net target = findNamedNet(name);

        if (target == null) {
            target = new Net(name, true);
            nets.add(target);
        }

        if (current == null) {
            target.add(pin);
        } else {
            merge(current, target);
        }
    }

    // Moves all pins into 1 net
    private void merge(Net a, Net b) {
        if (a.isNamed() && b.isNamed()) {
            throw new ConfigurationException("Short circuit: this would connect net " + a.getName()
                    + " to net " + b.getName());
        }

        Net keep = b.isNamed() ? b : a;
        Net drop = (keep == a) ? b : a;

        for (Pin pin : drop.getPins()) {
            drop.remove(pin);
            keep.add(pin);
        }

        nets.remove(drop);
    }

    private void detach(Pin pin) {
        Net net = pin.getNet();
        net.remove(pin);

        if (net.isNamed()) {
            if (net.getPins().isEmpty()) {
                nets.remove(net);
            }
        } else if (net.getPins().size() < 2) {
            for (Pin rest : net.getPins()) {
                net.remove(rest);
            }
            nets.remove(net);
        }
    }

    private Net findNamedNet(String name) {
        for (Net net : nets) {
            if (net.isNamed() && net.getName().equals(name)) {
                return net;
            }
        }
        return null;
    }

    // Net names: letters, digits, _, +
    // Returns name in uppercase
    private String checkNetName(String text) {
        String name = text.toUpperCase();

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '+';
            if (!ok) {
                throw new ConfigurationException("Invalid net name: '" + text + "' (letters, digits, _, +)");
            }
        }

        if (name.matches("N[0-9]+") || name.equals("NC")) {
            throw new ConfigurationException("The net name " + name + " is reserved");
        }

        for (Component component : components) {
            if (component.getRef().equalsIgnoreCase(name)) {
                throw new ConfigurationException(name + " is a component, use one of its pins (e.g. " + name + ".1)");
            }
        }

        return name;
    }

    // All problems of all components in reference order
    public ArrayList<String> check() {
        ArrayList<Component> sorted = getComponents();
        Collections.sort(sorted);
        ArrayList<String> problems = new ArrayList<>();

        for (Component component : sorted) {
            problems.addAll(component.check());
        }

        return problems;
    }
}