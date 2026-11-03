package cascii.domain;

// When the circuit builder's rule is broken
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }
}
