package cascii.persistence;

import cascii.domain.Circuit;
import cascii.domain.Component;
import cascii.domain.ConfigurationException;
import cascii.domain.Net;
import cascii.domain.Pin;
import cascii.domain.TextUtil;
import cascii.service.ComponentFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

// Saves a circuit as a simple text file, one line per item
/*
 * CIRCUIT;name
 * PART;typeKey;number;value
 * WIRE;pinA;pinB
 */
public class CircuitStorage {
    private final Path directory;
    private final ComponentFactory factory = new ComponentFactory();

    public CircuitStorage(String directory) {
        this.directory = Path.of(directory);
    }

    public void save(Circuit circuit) throws IOException {
        Files.createDirectories(directory);
        try (BufferedWriter writer = Files.newBufferedWriter(fileFor(circuit.getName()))) {
            writer.write("CIRCUIT;" + circuit.getName());
            writer.newLine();

            for (Component component : circuit.getComponents()) {
                String value = component.getValueText() == null ? "-" : component.getValueText();
                writer.write("PART;" + component.getTypeKey() + ";" + component.getNumber() + ";" + value);
                writer.newLine();
            }

            for (Net net : circuit.getNets()) {
                ArrayList<Pin> pins = net.getPins();

                if (net.isNamed()) {
                    for (Pin pin : pins) {
                        writer.write("WIRE;" + pin.getLabel() + ";" + net.getName());
                        writer.newLine();
                    }
                } else {
                    for (int i = 1; i < pins.size(); i++) {
                        writer.write("WIRE;" + pins.get(0).getLabel() + ";" + pins.get(i).getLabel());
                        writer.newLine();
                    }
                }
            }
        }
    }

    public Circuit load(String name) throws IOException {
        Path file = fileFor(name);

        if (!Files.exists(file)) {
            throw new IOException("No saved circuit named '" + name + "'");
        }

        Circuit circuit = null;

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (TextUtil.isBlank(line)) {
                    continue;
                }

                String[] f = line.split(";");

                if (f[0].equals("CIRCUIT")) {
                    circuit = new Circuit(f[1]);
                } else if (circuit == null) {
                    throw new IOException("Invalid file: the CIRCUIT line is missing");
                } else if (f[0].equals("PART")) {
                    String value = f[3].equals("-") ? null : f[3];
                    circuit.add(factory.create(f[1], value), Integer.parseInt(f[2]));
                } else if (f[0].equals("WIRE")) {
                    circuit.connect(f[1], f[2]);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Corrupt circuit file " + file.getFileName() + ": a line has missing fields");
        } catch (NumberFormatException | ConfigurationException e) {
            throw new IOException("Corrupt circuit file " + file.getFileName() + ": " + e.getMessage());
        }

        if (circuit == null) {
            throw new IOException("The circuit file is empty");
        }

        return circuit;
    }

    private Path fileFor(String name) {
        return directory.resolve(name.trim().replaceAll("[^A-Za-z0-9_-]", "_") + ".txt");
    }
}