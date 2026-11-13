package cascii.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitTest {
    private Circuit circuit;

    @BeforeEach
    void setUp() {
        circuit = new Circuit("test");
        circuit.add(new Resistor(100)); // R1
        circuit.add(new Resistor(200)); // R2
    }

    @Test
    void numbersPartsPerPrefix() {
        circuit.add(new Capacitor(1e-6));
        circuit.add(new Led());

        assertEquals("R1", circuit.find("R1").getRef());
        assertEquals("R2", circuit.find("r2").getRef());
        assertEquals("C1", circuit.find("C1").getRef());
        assertEquals("LED1", circuit.find("LED1").getRef());
    }

    @Test
    void numbersAreNotReusedWhileHigherOnesExist() {
        circuit.remove("R1");
        circuit.add(new Resistor(300));

        assertEquals("R3", circuit.find("R3").getRef());
    }

    @Test
    void unknownPartAndBadPinAreRejected() {
        assertThrows(ConfigurationException.class, () -> circuit.find("R9"));
        assertThrows(ConfigurationException.class, () -> circuit.findPin("R1"));
        assertThrows(ConfigurationException.class, () -> circuit.findPin("R1.7"));
    }

    @Test
    void connectingTwoPinsCreatesAnAutomaticNet() {
        Net net = circuit.connect("R1.1", "R2.1");

        assertEquals("N1", net.getName());
        assertFalse(net.isNamed());
        assertEquals(2, net.getPins().size());
    }

    @Test
    void connectingToANameCreatesANamedNet() {
        Net net = circuit.connect("R1.1", "gnd");

        assertEquals("GND", net.getName());
        assertTrue(net.isNamed());
    }

    @Test
    void pinsOnTheSameNameShareOneNet() {
        circuit.connect("R1.2", "GND");
        circuit.connect("R2.2", "GND");

        assertEquals(1, circuit.getNets().size());
        assertEquals(2, circuit.getNets().get(0).getPins().size());
    }

    @Test
    void automaticNetMergesIntoNamedNet() {
        circuit.connect("R1.1", "R2.1");
        circuit.connect("R1.1", "GND");

        assertEquals(1, circuit.getNets().size());
        assertEquals("GND", circuit.getNets().get(0).getName());
        assertEquals(2, circuit.getNets().get(0).getPins().size());
    }

    @Test
    void joiningTwoNamedNetsIsAShortCircuit() {
        circuit.connect("R1.1", "GND");
        circuit.connect("R2.1", "VCC");

        assertThrows(ConfigurationException.class, () -> circuit.connect("R1.1", "R2.1"));
    }

    @Test
    void sameConnectionTwiceIsRejected() {
        circuit.connect("R1.1", "GND");

        assertThrows(ConfigurationException.class, () -> circuit.connect("R1.1", "GND"));
    }

    @Test
    void connectingOnlyNamesIsRejected() {
        assertThrows(ConfigurationException.class, () -> circuit.connect("GND", "VCC"));
    }

    @Test
    void disconnectRemovesAnAutomaticNetWithOnePinLeft() {
        circuit.connect("R1.1", "R2.1");
        circuit.disconnect("R1.1");

        assertTrue(circuit.getNets().isEmpty());
        assertThrows(ConfigurationException.class, () -> circuit.disconnect("R2.1"));
    }

    @Test
    void removingAPartRemovesItsConnections() {
        circuit.connect("R1.1", "GND");
        circuit.connect("R1.2", "VCC");
        circuit.remove("R1");

        assertTrue(circuit.getNets().isEmpty());
        assertThrows(ConfigurationException.class, () -> circuit.find("R1"));
    }

    @Test
    void acceptsValidNetNames() {
        assertEquals("VCC(5V)", circuit.connect("R1.1", "vcc(5v)").getName());
        assertEquals("VCC(3.3V)", circuit.connect("R2.1", "VCC(3.3V)").getName());
        assertEquals("5V", circuit.connect("R1.2", "5v").getName());
    }

    @Test
    void rejectsInvalidNetNames() {
        assertThrows(ConfigurationException.class, () -> circuit.connect("R1.1", "N1"));
        assertThrows(ConfigurationException.class, () -> circuit.connect("R1.1", "NC"));
        assertThrows(ConfigurationException.class, () -> circuit.connect("R1.1", "bad-name"));
        assertThrows(ConfigurationException.class, () -> circuit.connect("R1.1", "VCC(5)"));
        assertThrows(ConfigurationException.class, () -> circuit.connect("R1.1", "R2"));
    }

    @Test
    void renamesNets() {
        circuit.connect("R1.1", "R2.1");
        circuit.renameNet("N1", "vin");

        Net net = circuit.getNets().get(0);
        assertEquals("VIN", net.getName());
        assertTrue(net.isNamed());
    }

    @Test
    void renameRejectsUnknownAndExistingNames() {
        circuit.connect("R1.1", "GND");
        circuit.connect("R2.1", "VCC");

        assertThrows(ConfigurationException.class, () -> circuit.renameNet("XYZ", "ABC"));
        assertThrows(ConfigurationException.class, () -> circuit.renameNet("GND", "VCC"));
        assertThrows(ConfigurationException.class, () -> circuit.renameNet("GND", "GND"));
    }

    @Test
    void diodePinsCanBeUsedByNameOrShortName() {
        circuit.add(new Led());

        circuit.connect("LED1.anode", "VCC");
        circuit.connect("LED1.K", "GND");

        assertEquals("VCC", circuit.findPin("LED1.A").getNet().getName());
        assertEquals("GND", circuit.findPin("LED1.CATHODE").getNet().getName());
    }

    @Test
    void circuitNameIsValidated() {
        assertThrows(ConfigurationException.class, () -> new Circuit("  "));
        assertThrows(ConfigurationException.class, () -> new Circuit("a;b"));

        circuit.setName("  My circuit ");
        assertEquals("My circuit", circuit.getName());
    }
}
