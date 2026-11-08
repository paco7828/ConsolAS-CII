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
            "add <R|C|L|D|LED|AMS1117-x> [value]     e.g. add R 50k | add C 100n | add AMS1117-3.3 | add AMS1117-ADJ\n"
                    + "connect <pin|net> <pin|net>              e.g. connect R1.1 U1.VIN | connect C1.2 GND\n"
                    + "disconnect <pin>                         e.g. disconnect R1.1\n"
                    + "remove <ref>                             e.g. remove R1\n"
                    + "show [ref]                               draw the whole schematic or one part\n"
                    + "list [sort ref|type|value]               list the parts\n"
                    + "nets                                     list the nets\n"
                    + "info <ref>                               pins of a part and what they are connected to\n"
                    + "check                                    look for problems in the circuit\n"
                    + "analyze <ref> <args>                     e.g. analyze U1 5 200m (input voltage, load current)\n"
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
            case "show" -> show(parts);
            case "list" -> list(parts);
            case "nets" -> nets();
            case "info" -> info(parts);
            case "check" -> check();
            case "analyze" -> analyze(parts);
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
            default -> throw new ConfigurationException("Unknown command: " + parts[0] + " (type 'help')");
        };
    }

    private String add(String[] parts) {
        if (parts.length < 2 || parts.length > 3) {
            throw new ConfigurationException("Usage: add <R|C|L|D|LED|AMS1117-x> [value]");
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

    private String show(String[] parts) {
        if (parts.length == 2) {
            return circuit.find(parts[1]).draw();
        }

        if (parts.length != 1) {
            throw new ConfigurationException("Usage: show [ref]");
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

    private String list(String[] parts) {
        Comparator<Component> comparator;

        if (parts.length == 1) {
            comparator = comparatorFor("ref");
        } else if (parts.length == 3 && parts[1].equalsIgnoreCase("sort")) {
            comparator = comparatorFor(parts[2]);
        } else {
            throw new ConfigurationException("Usage: list [sort ref|type|value]");
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

    private String info(String[] parts) {
        if (parts.length != 2) {
            throw new ConfigurationException("Usage: info <ref>");
        }

        Component component = circuit.find(parts[1]);
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