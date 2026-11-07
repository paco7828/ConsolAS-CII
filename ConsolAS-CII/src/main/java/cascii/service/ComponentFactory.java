package cascii.service;

import cascii.domain.Ams1117;
import cascii.domain.Capacitor;
import cascii.domain.Component;
import cascii.domain.ConfigurationException;
import cascii.domain.Diode;
import cascii.domain.Inductor;
import cascii.domain.Led;
import cascii.domain.Resistor;
import cascii.domain.Units;

// Factory pattern: creates the right Component subclass from a short key like "R"
public class ComponentFactory {
    // Diode, LED, IC => value is null
    public Component create(String type, String value) {
        String key = type.toUpperCase();
        switch (key) {
            case "R":
                return new Resistor(Units.parse(requireValue(key, value)));
            case "C":
                return new Capacitor(Units.parse(requireValue(key, value)));
            case "L":
                return new Inductor(Units.parse(requireValue(key, value)));
            case "D":
                requireNoValue(key, value);
                return new Diode();
            case "LED":
                requireNoValue(key, value);
                return new Led();
            case "AMS1117-3.3":
                requireNoValue(key, value);
                return new Ams1117();
            default:
                throw new ConfigurationException("Unknown component type: " + type
                        + " (R, C, L, D, LED, AMS1117-3.3)");
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