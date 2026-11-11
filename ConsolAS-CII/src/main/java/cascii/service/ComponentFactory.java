package cascii.service;

import cascii.domain.*;

// Factory pattern: creates the right Component subclass from a short key like "R"
public class ComponentFactory {
    // Diode, LED, IC => value is null
    public Component create(String type, String value) {
        String key = type.toUpperCase();
        switch (key) {
            case "R":
            case "RESISTOR":
                return new Resistor(Units.parse(requireValue(key, value)));
            case "C":
            case "CAPACITOR":
                return new Capacitor(Units.parse(requireValue(key, value)));
            case "L":
            case "INDUCTOR":
                return new Inductor(Units.parse(requireValue(key, value)));
            case "D":
            case "DIODE":
                requireNoValue(key, value);
                return new Diode();
            case "LED":
                requireNoValue(key, value);
                return new Led();
            case "TP4056":
                requireNoValue(key, value);
                return new Tp4056();
            case "MT3608":
                requireNoValue(key, value);
                return new Mt3608();
            default:
                if (key.startsWith("AMS1117-")) {
                    requireNoValue(key, value);
                    return new Ams1117(key.substring("AMS1117-".length()));
                }
                throw new ConfigurationException("Unknown component type: " + type
                        + " (R, C, L, D, LED, TP4056, MT3608, AMS1117-<1.2|1.5|1.8|2.5|2.85|3.3|5.0|ADJ>)");
        }
    }

    private String requireValue(String type, String value) {
        if (value == null) {
            throw new ConfigurationException(type + " needs a value, e.g. add " + type + " 10k");
        }
        return value;
    }

    private void requireNoValue(String type, String value) {
        if (value != null) {
            throw new ConfigurationException(type + " has no value, use: add " + type);
        }
    }
}