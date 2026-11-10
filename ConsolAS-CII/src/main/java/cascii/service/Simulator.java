package cascii.service;

import cascii.domain.Circuit;
import cascii.domain.Component;
import cascii.domain.ConfigurationException;
import cascii.domain.Net;
import cascii.domain.Pin;
import cascii.domain.TextUtil;
import cascii.persistence.CircuitStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

// Handles console commands
public class Simulator {
    private static final String HELP =
            "add <R|C|L> <value>                     e.g. add R 50k | add resistor 50k | add C 100n\n"
                    + "add <D|LED|TP4056|MT3608|AMS1117-x>      no value, e.g. add LED | add TP4056 | add AMS1117-3.3\n"
                    + "                                         (AMS1117-x: 1.2 1.5 1.8 2.5 2.85 3.3 5.0 ADJ)\n"
                    + "connect <pin|net> <pin|net>              e.g. connect R1.1 U1.VIN | connect C1.2 GND | connect LED1.ANODE VCC(5V)\n"
                    + "disconnect <pin>                         e.g. disconnect R1.1\n"
                    + "remove <ref>                             e.g. remove R1\n"
                    + "rename net <old-name> <new-name>         e.g. rename net N1 VIN\n"
                    + "draw [ref]                               draw the whole schematic, or only one part (draw U1)\n"
                    + "show pins <ref>                          pins of a part and what they are connected to\n"
                    + "components [sort ref|type|value]         list the parts\n"
                    + "nets                                     list the nets\n"
                    + "run drc                                  design rule check: look for problems in the circuit\n"
                    + "analyze <ref> <args>                     e.g. analyze U1 5 200m (input voltage, load current)\n"
                    + "clear | cls                               clear the screen\n"
                    + "rename circuit <new-name>                 e.g. rename circuit My charger\n"
                    + "circuits                                  list saved circuits\n"
                    + "new <name> | save | load <name> | help | exit";

    private final ComponentFactory factory = new ComponentFactory();
    private final CircuitStorage storage = new CircuitStorage("circuits");
    private Circuit circuit = new Circuit("untitled");

    public String execute(String line) throws IOException {
        String[] parts = line.trim().split("\\s+");
        return switch (parts[0].toLowerCase()) {
            case "add" -> add(parts);
            case "connect" -> connect(parts);
            case "disconnect" -> disconnect(parts);
            case "remove" -> remove(parts);
            case "draw" -> draw(parts);
            case "components" -> listComponents(parts);
            case "nets" -> nets();
            case "show" -> showPins(parts);
            case "run" -> run(parts);
            case "analyze" -> analyze(parts);
            case "rename" -> rename(parts);
            case "new" -> {
                circuit = new Circuit(rest(line, "new <name>"));
                yield "Circuit '" + circuit.getName() + "' created";
            }
            case "save" -> {
                storage.save(circuit);
                yield "Circuit '" + circuit.getName() + "' saved";
            }
            case "load" -> {
                circuit = storage.load(rest(line, "load <name>"));
                yield "Circuit '" + circuit.getName() + "' loaded";
            }
            case "help" -> HELP;
            case "circuits" -> storage.listSavedCircuits();
            default -> throw new ConfigurationException("Unknown command: " + parts[0] + " (type 'help')");
        };
    }

    private String add(String[] parts) {
        if (parts.length < 2 || parts.length > 3) {
            throw new ConfigurationException("Usage: add <R|C|L> <value> | add <D|LED|TP4056|MT3608|AMS1117-x>");
        }

        String value = parts.length == 3 ? parts[2] : null;
        Component component = factory.create(parts[1], value);
        circuit.add(component);
        return component.getRef() + " added: " + component.describe();
    }

    private String connect(String[] parts) {
        if (parts.length != 3) {
            throw new ConfigurationException("Usage: connect <pin|net> <pin|net>");
        }

        Net net = circuit.connect(parts[1], parts[2]);
        return parts[1].toUpperCase() + " -- " + parts[2].toUpperCase() + "  (net " + net.getName() + ")";
    }

    private String disconnect(String[] parts) {
        if (parts.length != 2) {
            throw new ConfigurationException("Usage: disconnect <pin>");
        }

        circuit.disconnect(parts[1]);
        return parts[1].toUpperCase() + " disconnected";
    }

    private String remove(String[] parts) {
        if (parts.length != 2) {
            throw new ConfigurationException("Usage: remove <ref>");
        }

        circuit.remove(parts[1]);
        return parts[1].toUpperCase() + " removed";
    }

    private String draw(String[] parts) {
        if (parts.length == 2) {
            return circuit.find(parts[1]).draw();
        }

        if (parts.length != 1) {
            throw new ConfigurationException("Usage: draw [ref]");
        }

        ArrayList<Component> components = circuit.getComponentsSorted(comparatorFor("ref"));
        if (components.isEmpty()) {
            return "The circuit is empty";
        }

        StringBuilder sb = new StringBuilder();
        for (Component component : components) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }

            sb.append(component.draw());
        }

        return sb.toString();
    }

    private String listComponents(String[] parts) {
        Comparator<Component> comparator;

        if (parts.length == 1) {
            comparator = comparatorFor("ref");
        } else if (parts.length == 3 && parts[1].equalsIgnoreCase("sort")) {
            comparator = comparatorFor(parts[2]);
        } else {
            throw new ConfigurationException("Usage: components [sort ref|type|value]");
        }

        StringBuilder sb = new StringBuilder("Circuit: " + circuit.getName());
        ArrayList<Component> components = circuit.getComponentsSorted(comparator);

        if (components.isEmpty()) {
            sb.append("\n  (no components)");
        }

        for (Component component : components) {
            sb.append("\n  ").append(TextUtil.padRight(component.getRef(), 6)).append(component.describe());
        }

        return sb.toString();
    }

    // Sorting rules for the list command
    private Comparator<Component> comparatorFor(String key) {
        return switch (key.toLowerCase()) {
            case "ref" -> (a, b) -> a.compareTo(b);
            case "type" -> (a, b) -> {
                int result = a.getTypeName().compareToIgnoreCase(b.getTypeName());
                return result != 0 ? result : a.compareTo(b);
            };
            case "value" -> (a, b) -> {
                int result = Double.compare(a.getValue(), b.getValue());
                return result != 0 ? result : a.compareTo(b);
            };
            default -> throw new ConfigurationException("Unknown sort key: " + key + " (ref, type, value)");
        };
    }

    private String nets() {
        ArrayList<Net> nets = circuit.getNets();

        if (nets.isEmpty()) {
            return "There are no nets yet";
        }

        nets.sort((a, b) -> a.getName().compareTo(b.getName()));
        StringBuilder sb = new StringBuilder();

        for (Net net : nets) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }

            sb.append(TextUtil.padRight(net.getName() + ":", 6));
            ArrayList<Pin> pins = net.getPins();
            for (int i = 0; i < pins.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(pins.get(i).getLabel());
            }
        }

        return sb.toString();
    }

    private String showPins(String[] parts) {
        if (parts.length != 3 || !parts[1].equalsIgnoreCase("pins")) {
            throw new ConfigurationException("Usage: show pins <ref>");
        }

        Component component = circuit.find(parts[2]);
        StringBuilder sb = new StringBuilder(component.getRef() + "  " + component.describe());

        for (Pin pin : component.getPins()) {
            sb.append("\n  pin ").append(pin.getNumber()).append("  ")
                    .append(TextUtil.padRight(pin.getLabel(), 10))
                    .append(" -> ").append(pin.getConnectionLabel());
        }

        return sb.toString();
    }

    private String check() {
        ArrayList<String> problems = circuit.check();
        if (problems.isEmpty()) {
            return "OK, no problems found";
        }

        StringBuilder sb = new StringBuilder(problems.size() + " problem(s) found:");
        for (String problem : problems) {
            sb.append("\n  - ").append(problem);
        }

        return sb.toString();
    }

    private String run(String[] parts) {
        if (parts.length != 2 || !parts[1].equalsIgnoreCase("drc")) {
            throw new ConfigurationException("Usage: run drc");
        }

        return check();
    }

    private String rename(String[] parts) {
        if (parts.length == 4 && parts[1].equalsIgnoreCase("net")) {
            circuit.renameNet(parts[2], parts[3]);
            return "Net " + parts[2].toUpperCase() + " renamed to " + parts[3].toUpperCase();
        }

        if (parts.length >= 3 && parts[1].equalsIgnoreCase("circuit")) {
            circuit.setName(String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)));
            return "Circuit renamed to '" + circuit.getName() + "'";
        }

        throw new ConfigurationException("Usage: rename net <old-name> <new-name> | rename circuit <new-name>");
    }

    private String analyze(String[] parts) {
        if (parts.length < 2) {
            throw new ConfigurationException("Usage: analyze <ref> <args>");
        }

        Component component = circuit.find(parts[1]);
        return component.analyze(Arrays.copyOfRange(parts, 2, parts.length));
    }

    private static String rest(String line, String usage) {
        String[] split = line.trim().split("\\s+", 2);
        if (split.length < 2) {
            throw new ConfigurationException("Usage: " + usage);
        }

        return split[1].replace("\"", "").trim();
    }
}