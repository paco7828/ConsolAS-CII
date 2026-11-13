package cascii.persistence;

import cascii.domain.Circuit;
import cascii.domain.Capacitor;
import cascii.domain.Component;
import cascii.domain.Led;
import cascii.domain.Resistor;
import cascii.domain.Ams1117;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitStorageTest {
    @TempDir
    Path directory;

    private CircuitStorage storage;

    @BeforeEach
    void setUp() {
        storage = new CircuitStorage(directory.toString());
    }

    private Circuit buildCircuit(String name) {
        Circuit circuit = new Circuit(name);
        circuit.add(new Ams1117("3.3")); // U1
        circuit.add(new Capacitor(10e-6)); // C1
        circuit.add(new Capacitor(10e-6)); // C2
        circuit.add(new Resistor(330)); // R1
        circuit.add(new Led()); // LED1

        circuit.connect("U1.GND", "GND");
        circuit.connect("U1.VIN", "VCC(5V)");
        circuit.connect("U1.VOUT", "VOUT");
        circuit.connect("U1.TAB", "VOUT");
        circuit.connect("C1.1", "VCC(5V)");
        circuit.connect("C1.2", "GND");
        circuit.connect("C2.1", "VOUT");
        circuit.connect("C2.2", "GND");
        circuit.connect("R1.1", "VOUT");
        circuit.connect("R1.2", "LED1.ANODE");
        circuit.connect("LED1.CATHODE", "GND");
        return circuit;
    }

    @Test
    void savedCircuitIsLoadedBackTheSame() throws IOException {
        Circuit original = buildCircuit("Test circuit");

        storage.save(original);
        Circuit loaded = storage.load("Test circuit");

        assertEquals("Test circuit", loaded.getName());
        assertEquals(original.getComponents().size(), loaded.getComponents().size());
        assertEquals(original.getNets().size(), loaded.getNets().size());
        for (Component component : original.getComponents()) {
            assertEquals(component.draw(), loaded.find(component.getRef()).draw());
        }
    }

    @Test
    void saveCreatesTheFileWithASafeName() throws IOException {
        storage.save(new Circuit("My circuit: v2"));

        assertTrue(Files.exists(directory.resolve("My_circuit__v2.txt")));
    }

    @Test
    void loadingAMissingCircuitFails() {
        assertThrows(IOException.class, () -> storage.load("nothing"));
    }

    @Test
    void corruptFilesAreReportedAsErrors() throws IOException {
        Files.writeString(directory.resolve("missing.txt"), "CIRCUIT;x\nPART;R\n");
        Files.writeString(directory.resolve("badvalue.txt"), "CIRCUIT;x\nPART;R;1;abc\n");
        Files.writeString(directory.resolve("noheader.txt"), "PART;R;1;1k\n");

        assertThrows(IOException.class, () -> storage.load("missing"));
        assertThrows(IOException.class, () -> storage.load("badvalue"));
        assertThrows(IOException.class, () -> storage.load("noheader"));
    }

    @Test
    void circuitsCanBeListedAndLoadedByNumber() throws IOException {
        storage.save(new Circuit("beta"));
        storage.save(new Circuit("alpha"));

        assertEquals("Saved circuits:\n1 - alpha.txt\n2 - beta.txt", storage.listSavedCircuits());
        assertEquals("alpha", storage.load("1").getName());
        assertEquals("beta", storage.load("2").getName());
        assertThrows(IOException.class, () -> storage.load("5"));
    }

    @Test
    void listingAnEmptyFolderSaysSo() throws IOException {
        assertEquals("No saved circuits found", storage.listSavedCircuits());
    }
}
