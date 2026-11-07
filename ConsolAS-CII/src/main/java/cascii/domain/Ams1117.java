package cascii.domain;

import cascii.config.Config;

import java.util.ArrayList;

// AMS1117-3.3: fixed 3.3V low-dropout linear regulator
// Pins:
/*
 * 1 - GND
 * 2 - VOUT
 * 3 - VIN
 * 4 - TAB (internally connected to VOUT)
 */
public class Ams1117 extends IC {
    public Ams1117() {
        addPin("GND", true);
        addPin("VOUT", true);
        addPin("VIN", true);
        addPin("TAB", false);
    }

    @Override
    public String getTypeName() {
        return "AMS1117-3.3";
    }

    @Override
    public String getTypeKey() {
        return "AMS1117-3.3";
    }

    @Override
    public String describe() {
        return "AMS1117-3.3 (LDO voltage regulator)";
    }

    @Override
    public ArrayList<String> check() {
        ArrayList<String> problems = super.check();
        Net gnd = getPin("GND").getNet();
        Net vout = getPin("VOUT").getNet();
        Net vin = getPin("VIN").getNet();
        Net tab = getPin("TAB").getNet();

        if (vin != null && vin == vout) {
            problems.add(getRef() + ": VIN and VOUT are on the same net (" + vin.getName() + ")");
        }
        if (gnd != null && (gnd == vin || gnd == vout)) {
            problems.add(getRef() + ": GND is shorted to VIN or VOUT (net " + gnd.getName() + ")");
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

        double nominal = Config.getDouble("ams1117_3v3.vout", 3.3);
        double dropout = Config.getDouble("ams1117_3v3.dropout", 1.3);
        double maxVin = Config.getDouble("ams1117_3v3.max_vin", 15.0);
        double maxCurrent = Config.getDouble("ams1117_3v3.max_current", 1.0);

        double vout;
        String mode;
        if (vin >= nominal + dropout) {
            vout = nominal;
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
}