package cascii.service;

import cascii.domain.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatorTest {
    private Simulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new Simulator();
    }

    @Test
    void addsPartsWithDesignators() throws IOException {
        assertEquals("U1 added: AMS1117-3.3 (3.3 V LDO voltage regulator)", simulator.execute("add AMS1117-3.3"));
        assertEquals("R1 added: resistor 220", simulator.execute("add resistor 220R"));
        assertEquals("R2 added: resistor 50k", simulator.execute("ADD r 50k"));
    }

    @Test
    void connectsAndDisconnectsPins() throws IOException {
        simulator.execute("add R 1k");

        assertEquals("R1.1 -- GND  (net GND)", simulator.execute("connect R1.1 gnd"));
        assertEquals("R1.1 disconnected", simulator.execute("disconnect R1.1"));
    }

    @Test
    void drawsAPartWithItsConnections() throws IOException {
        simulator.execute("add R 220");
        simulator.execute("connect R1.1 GND");
        simulator.execute("connect R1.2 VCC(5V)");

        assertEquals("R1  resistor 220\n    GND ───[ 220 ]─── VCC(5V)", simulator.execute("draw R1"));
    }

    @Test
    void listsTheNetsWithAlignedPins() throws IOException {
        simulator.execute("add R 220");
        simulator.execute("connect R1.1 GND");
        simulator.execute("connect R1.2 VCC(5V)");

        assertEquals("GND:     R1.1\nVCC(5V): R1.2", simulator.execute("nets"));
    }

    @Test
    void showsThePinsOfAPart() throws IOException {
        simulator.execute("add AMS1117-3.3");

        String result = simulator.execute("show pins U1");

        assertTrue(result.contains("pin 1  U1.GND"));
        assertTrue(result.contains("U1.TAB/VOUT -> NC"));
    }

    @Test
    void listsPartsInDifferentOrders() throws IOException {
        simulator.execute("add R 1k");
        simulator.execute("add R 100");
        simulator.execute("add C 1u");
        simulator.execute("add LED");

        String byRef = simulator.execute("components");
        String byValue = simulator.execute("components sort value");
        String byType = simulator.execute("components sort type");

        assertTrue(byRef.indexOf("C1") < byRef.indexOf("LED1"));
        assertTrue(byRef.indexOf("LED1") < byRef.indexOf("R1"));
        assertTrue(byValue.indexOf("C1") < byValue.indexOf("R2"));
        assertTrue(byValue.indexOf("R2") < byValue.indexOf("R1"));
        assertTrue(byType.indexOf("C1") < byType.indexOf("LED1"));
        assertTrue(byType.indexOf("LED1") < byType.indexOf("R1"));
    }

    @Test
    void designRuleCheckFindsProblems() throws IOException {
        assertEquals("OK, no problems found", simulator.execute("run drc"));

        simulator.execute("add R 1k");

        String result = simulator.execute("run drc");
        assertTrue(result.startsWith("2 problem(s) found:"));
        assertTrue(result.contains("R1.1 is not connected"));
    }

    @Test
    void analyzesARegulator() throws IOException {
        simulator.execute("add AMS1117-3.3");

        assertTrue(simulator.execute("analyze U1 5V 100mA").contains("Vout  = 3.3 V (regulating)"));
    }

    @Test
    void renamesNetsAndTheCircuit() throws IOException {
        simulator.execute("add R 1k");
        simulator.execute("add R 1k");
        simulator.execute("connect R1.1 R2.1");

        assertEquals("Net N1 renamed to VIN", simulator.execute("rename net N1 vin"));
        assertEquals("Circuit renamed to 'My test'", simulator.execute("rename circuit My test"));
    }

    @Test
    void removesParts() throws IOException {
        simulator.execute("add R 1k");

        assertEquals("R1 removed", simulator.execute("remove R1"));
        assertThrows(ConfigurationException.class, () -> simulator.execute("remove R1"));
    }

    @Test
    void wrongCommandsAreRejected() {
        assertThrows(ConfigurationException.class, () -> simulator.execute("foo"));
        assertThrows(ConfigurationException.class, () -> simulator.execute("add"));
        assertThrows(ConfigurationException.class, () -> simulator.execute("connect R1.1"));
        assertThrows(ConfigurationException.class, () -> simulator.execute("components sort foo"));
        assertThrows(ConfigurationException.class, () -> simulator.execute("run test"));
    }

    @Test
    void helpListsTheCommands() throws IOException {
        String help = simulator.execute("help");

        assertTrue(help.contains("draw"));
        assertTrue(help.contains("run drc"));
    }
}
