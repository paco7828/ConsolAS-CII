package cascii.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ams1117Test {
    private Circuit circuit;

    @BeforeEach
    void setUp() {
        circuit = new Circuit("test");
    }

    // U1 with input and output capacitors, TAB on VOUT
    private void buildRegulator(String version) {
        circuit.add(new Ams1117(version));
        circuit.add(new Capacitor(10e-6)); // C1
        circuit.add(new Capacitor(10e-6)); // C2
        circuit.connect("U1.VIN", "VIN");
        circuit.connect("U1.VOUT", "VOUT");
        circuit.connect("U1.TAB", "VOUT");
        circuit.connect("C1.1", "VIN");
        circuit.connect("C1.2", "GND");
        circuit.connect("C2.1", "VOUT");
        circuit.connect("C2.2", "GND");
    }

    @Test
    void hasAllVersions() {
        String[] versions = {"1.2", "1.5", "1.8", "2.5", "2.85", "3.3", "5.0", "ADJ"};

        for (String version : versions) {
            assertEquals("AMS1117-" + version, new Ams1117(version).getTypeKey());
        }
        assertEquals("AMS1117-ADJ", new Ams1117("adj").getTypeKey());
    }

    @Test
    void rejectsUnknownVersion() {
        assertThrows(ConfigurationException.class, () -> new Ams1117("9"));
    }

    @Test
    void firstPinIsGndOrAdj() {
        assertEquals("GND", new Ams1117("3.3").getPin("1").getName());
        assertEquals("ADJ", new Ams1117("ADJ").getPin("1").getName());
    }

    @Test
    void describesItsOutputVoltage() {
        assertEquals("AMS1117-3.3 (3.3 V LDO voltage regulator)", new Ams1117("3.3").describe());
        assertEquals("AMS1117-2.85 (2.85 V LDO voltage regulator)", new Ams1117("2.85").describe());
    }

    @Test
    void correctlyWiredRegulatorHasNoProblems() {
        buildRegulator("3.3");
        circuit.connect("U1.GND", "GND");

        assertTrue(circuit.check().isEmpty());
    }

    @Test
    void reportsUnconnectedRequiredPins() {
        circuit.add(new Ams1117("3.3"));

        assertEquals(3, circuit.check().size());
    }

    @Test
    void reportsMissingCapacitors() {
        circuit.add(new Ams1117("3.3"));
        circuit.connect("U1.GND", "GND");
        circuit.connect("U1.VIN", "VIN");
        circuit.connect("U1.VOUT", "VOUT");

        ArrayList<String> problems = circuit.check();

        assertTrue(problems.contains("U1: no input capacitor on the VIN net"));
        assertTrue(problems.contains("U1: no output capacitor on the VOUT net"));
    }

    @Test
    void reportsShortedInputAndOutput() {
        circuit.add(new Ams1117("3.3"));
        circuit.connect("U1.GND", "GND");
        circuit.connect("U1.VIN", "X");
        circuit.connect("U1.VOUT", "X");

        assertTrue(circuit.check().contains("U1: VIN and VOUT are on the same net (X)"));
    }

    @Test
    void reportsTabOnTheWrongNet() {
        circuit.add(new Ams1117("3.3"));
        circuit.connect("U1.GND", "GND");
        circuit.connect("U1.VIN", "VIN");
        circuit.connect("U1.VOUT", "VOUT");
        circuit.connect("U1.TAB", "OTHER");

        assertTrue(circuit.check().contains(
                "U1: the TAB is internally connected to VOUT, it should be on the VOUT net"));
    }

    @Test
    void regulatesWhenTheInputIsHighEnough() {
        circuit.add(new Ams1117("3.3"));

        String result = circuit.find("U1").analyze(new String[]{"5", "100m"});

        assertTrue(result.contains("Vout  = 3.3 V (regulating)"));
        assertTrue(result.contains("Power = 0.17 W"));
    }

    @Test
    void dropsOutWhenTheInputIsTooLow() {
        circuit.add(new Ams1117("3.3"));

        String result = circuit.find("U1").analyze(new String[]{"4"});

        assertTrue(result.contains("Vout  = 2.7 V (dropout"));
    }

    @Test
    void warnsAboutVoltageAndCurrentLimits() {
        circuit.add(new Ams1117("3.3"));

        String result = circuit.find("U1").analyze(new String[]{"20", "2"});

        assertTrue(result.contains("WARNING: the input voltage is above the maximum (15.0 V)"));
        assertTrue(result.contains("WARNING: the load current is above the maximum (1.0 A)"));
    }

    @Test
    void analyzeNeedsTheRightNumberOfArguments() {
        circuit.add(new Ams1117("3.3"));

        assertThrows(ConfigurationException.class, () -> circuit.find("U1").analyze(new String[0]));
    }

    @Test
    void adjustableVersionUsesTheResistorDivider() {
        circuit.add(new Ams1117("ADJ"));
        circuit.add(new Resistor(240)); // R1: VOUT - ADJ
        circuit.add(new Resistor(390)); // R2: ADJ - GND
        circuit.connect("U1.VOUT", "VOUT");
        circuit.connect("R1.1", "VOUT");
        circuit.connect("R1.2", "U1.ADJ");
        circuit.connect("R2.1", "U1.ADJ");
        circuit.connect("R2.2", "GND");

        String result = circuit.find("U1").analyze(new String[]{"5"});

        assertTrue(result.contains("Vout  = 3.303 V (regulating)"));
    }

    @Test
    void adjustableVersionNeedsItsResistors() {
        circuit.add(new Ams1117("ADJ"));

        assertThrows(ConfigurationException.class, () -> circuit.find("U1").analyze(new String[]{"5"}));
    }
}
