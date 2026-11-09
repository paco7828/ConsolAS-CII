package cascii.ui;

import cascii.config.Config;
import cascii.domain.ConfigurationException;
import cascii.service.Simulator;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // The schematic uses box-drawing characters, so the output must be UTF-8
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        Config.load();
        Simulator simulator = new Simulator();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Consolas-CII - console schematic builder - type 'help' or 'exit'");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.equalsIgnoreCase("clear") || line.equalsIgnoreCase("cls")) {
                clearScreen();
                continue;
            }

            if (line.equalsIgnoreCase("exit")) {
                break;
            }

            try {
                System.out.println(simulator.execute(line));
            } catch (ConfigurationException | IOException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    // Clears the console like cls (Windows) or clear (Linux/Mac)
    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Could not clear the screen");
        }
    }
}