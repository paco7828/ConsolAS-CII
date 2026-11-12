package cascii.domain;

import cascii.config.Config;

import java.util.ArrayList;

// AMS1117 low-dropout linear regulator: fixed (1.2 ... 5.0 V) and adjustable (ADJ) versions
// Pins:
/*
 * 1 - GND (fixed versions) or ADJ (adjustable version)
 * 2 - VOUT
 * 3 - VIN
 * 4 - TAB/VOUT (internally connected to VOUT)
 */
public class Ams1117 extends IC {
    private static final String[] VARIANTS = {"1.2", "1.5", "1.8", "2.5", "2.85", "3.3", "5.0", "ADJ"};
    private static final double[] VOUT = {1.2, 1.5, 1.8, 2.5, 2.85, 3.3, 5.0, 0};

    private final String variant;
    private final double nominal; // 0 for ADJ
    private final boolean adjustable;

    public Ams1117(String variant) {
        int index = -1;
        for (int i = 0; i < VARIANTS.length; i++) {
            if (VARIANTS[i].equalsIgnoreCase(variant)) {
                index = i;
            }
        }

        if (index < 0) {
            throw new ConfigurationException("Unknown AMS1117 version: " + variant
                    + " (1.2, 1.5, 1.8, 2.5, 2.85, 3.3, 5.0, ADJ)");
        }

        this.variant = VARIANTS[index];
        this.nominal = VOUT[index];
        this.adjustable = this.variant.equals("ADJ");

        addPin(adjustable ? "ADJ" : "GND", true);
        addPin("VOUT", true);
        addPin("VIN", true);
        addPin("TAB/VOUT", false);
    }

    @Override
    public String getTypeName() {
        return "AMS1117-" + variant;
    }

    @Override
    public String getTypeKey() {
        return "AMS1117-" + variant;
    }

    @Override
    public String describe() {
        if (adjustable) {
            return getTypeName() + " (adjustable LDO voltage regulator)";
        }
        return getTypeName() + " (" + nominal + " V LDO voltage regulator)";
    }

    @Override
    public ArrayList<String> check() {
        ArrayList<String> problems = super.check();
        Net first = getPin("1").getNet(); // GND or ADJ
        Net vout = getPin("VOUT").getNet();
        Net vin = getPin("VIN").getNet();
        Net tab = getPin("TAB").getNet();

        if (vin != null && vin == vout) {
            problems.add(getRef() + ": VIN and VOUT are on the same net (" + vin.getName() + ")");
        }
        if (!adjustable && first != null && (first == vin || first == vout)) {
            problems.add(getRef() + ": GND is shorted to VIN or VOUT (net " + first.getName() + ")");
        }
        if (adjustable && first != null && first == vin) {
            problems.add(getRef() + ": ADJ is on the VIN net (" + first.getName() + ")");
        }
        if (tab != null && tab != vout) {
            problems.add(getRef() + ": the TAB is internally connected to VOUT, it should be on the VOUT net");
        }
        if (vin != null && !vin.hasPinOfType("capacitor")) {
            problems.add(getRef() + ": no input capacitor on the VIN net");
        }
        if (vout != null && !vout.hasPinOfType("capacitor")) {
            problems.add(getRef() + ": no output capacitor on the VOUT net");
        }
        if (adjustable && first != null && vout != null) {
            if (resistorBetween(first, vout.getName()) == null) {
                problems.add(getRef() + ": no resistor between VOUT and ADJ (R1)");
            }
            if (resistorBetween(first, "GND") == null) {
                problems.add(getRef() + ": no resistor between ADJ and GND (R2)");
            }
        }
        return problems;
    }

    // Usage: analyze U1 <input voltage> [load current]
    // example: analyze U1 5 200m
    @Override
    public String analyze(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new ConfigurationException("Usage: analyze " + getRef() + " <input voltage> [load current]\nexample: analyze "
                    + getRef() + " 5 200m");
        }
        double vin = Units.parse(args[0]);
        double load = 0;
        if (args.length == 2) {
            load = Units.parse(args[1]);
        }

        double dropout = Config.getDouble("ams1117.dropout", 1.3);
        double maxVin = Config.getDouble("ams1117.max_vin", 15.0);
        double maxCurrent = Config.getDouble("ams1117.max_current", 1.0);

        double target = adjustable ? adjustableVout() : nominal;

        double vout;
        String mode;
        if (vin >= target + dropout) {
            vout = target;
            mode = "regulating";
        } else {
            vout = Math.max(0, vin - dropout);
            mode = "dropout, the input is too low to regulate";
        }
        double power = (vin - vout) * load;

        StringBuilder sb = new StringBuilder(getRef() + " analysis");
        sb.append("\n  Vin   = ").append(Units.round(vin, 3)).append(" V");
        sb.append("\n  Vout  = ").append(Units.round(vout, 3)).append(" V (").append(mode).append(")");
        sb.append("\n  Iload = ").append(Units.round(load, 3)).append(" A");
        sb.append("\n  Power = ").append(Units.round(power, 3)).append(" W dissipated in the regulator");
        if (vin > maxVin) {
            sb.append("\n  WARNING: the input voltage is above the maximum (").append(maxVin).append(" V)");
        }
        if (load > maxCurrent) {
            sb.append("\n  WARNING: the load current is above the maximum (").append(maxCurrent).append(" A)");
        }
        return sb.toString();
    }

    // Vout = Vref * (1 + R2 / R1) + Iadj * R2
    // R1 = VOUT - ADJ, R2 = ADJ - GND
    private double adjustableVout() {
        Net adj = getPin("ADJ").getNet();
        Net vout = getPin("VOUT").getNet();
        if (adj == null || vout == null) {
            throw new ConfigurationException(getRef() + ": connect ADJ and VOUT first (R1: VOUT-ADJ, R2: ADJ-GND)");
        }

        Resistor r1 = resistorBetween(adj, vout.getName());
        Resistor r2 = resistorBetween(adj, "GND");
        if (r1 == null || r2 == null) {
            throw new ConfigurationException(getRef() + ": needs a resistor between VOUT and ADJ (R1)"
                    + " and one between ADJ and the GND net (R2)");
        }
        if (r1.getValue() <= 0) {
            throw new ConfigurationException(r1.getRef() + " must be greater than 0");
        }

        double vref = Config.getDouble("ams1117.vref", 1.25);
        double iadj = Config.getDouble("ams1117.iadj", 0.000055);
        return vref * (1 + r2.getValue() / r1.getValue()) + iadj * r2.getValue();
    }
}