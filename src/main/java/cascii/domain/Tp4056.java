package cascii.domain;

import cascii.config.Config;

import java.util.ArrayList;

// TP4056: 1A linear charger for a single cell Li-ion battery
// Pins:
/*
 * 1 - TEMP  (NTC input, ground it to disable)
 * 2 - PROG  (charge current setting: resistor to GND)
 * 3 - GND/EPAD (ground and the exposed pad)
 * 4 - VCC
 * 5 - BAT
 * 6 - STDBY (open drain, low when the charge is terminated)
 * 7 - CHRG  (open drain, low while charging)
 * 8 - CE    (chip enable, high = on)
 */
public class Tp4056 extends IC {
    public Tp4056() {
        addPin("TEMP", true);
        addPin("PROG", true);
        addPin("GND/EPAD", true);
        addPin("VCC", true);
        addPin("BAT", true);
        addPin("STDBY", false);
        addPin("CHRG", false);
        addPin("CE", true);
    }

    @Override
    public String getTypeName() {
        return "TP4056";
    }

    @Override
    public String getTypeKey() {
        return "TP4056";
    }

    @Override
    public String describe() {
        return "TP4056 (1S 1A Li-ion linear charger)";
    }

    @Override
    public ArrayList<String> check() {
        ArrayList<String> problems = super.check();
        Net prog = getPin("PROG").getNet();
        Net gnd = getPin("GND").getNet();
        Net vcc = getPin("VCC").getNet();
        Net bat = getPin("BAT").getNet();
        Net ce = getPin("CE").getNet();

        if (vcc != null && vcc == bat) {
            problems.add(getRef() + ": VCC and BAT are on the same net (" + vcc.getName() + ")");
        }
        if (gnd != null && (gnd == vcc || gnd == bat)) {
            problems.add(getRef() + ": GND is shorted to VCC or BAT (net " + gnd.getName() + ")");
        }
        if (prog != null && gnd != null) {
            if (prog == gnd) {
                problems.add(getRef() + ": PROG is shorted to GND, it needs a resistor (RPROG)");
            } else if (resistorBetween(prog, gnd.getName()) == null) {
                problems.add(getRef() + ": no resistor between PROG and GND (RPROG)");
            }
        }
        if (ce != null && ce == gnd) {
            problems.add(getRef() + ": CE is on GND, the charger is disabled");
        }
        if (vcc != null && !vcc.hasPinOfType("capacitor")) {
            problems.add(getRef() + ": no input capacitor on the VCC net");
        }
        return problems;
    }

    // Usage: analyze U1 <input voltage> <battery voltage>
    // example: analyze U1 5 3.7
    @Override
    public String analyze(String[] args) {
        if (args.length != 2) {
            throw new ConfigurationException("Usage: analyze " + getRef() + " <input voltage> <battery voltage>\nexample: analyze "
                    + getRef() + " 5 3.7");
        }
        double vcc = Units.parse(args[0]);
        double vbat = Units.parse(args[1]);

        Net prog = getPin("PROG").getNet();
        Net gnd = getPin("GND").getNet();
        if (prog == null || gnd == null) {
            throw new ConfigurationException(getRef() + ": connect PROG and GND first (RPROG goes between them)");
        }

        Resistor rprog = resistorBetween(prog, gnd.getName());
        if (rprog == null) {
            throw new ConfigurationException(getRef() + ": needs a resistor between PROG and GND (RPROG)");
        }
        if (rprog.getValue() <= 0) {
            throw new ConfigurationException(rprog.getRef() + " must be greater than 0");
        }

        double progK = Config.getDouble("tp4056.prog_k", 1200);
        double vfloat = Config.getDouble("tp4056.vfloat", 4.2);
        double vtrickle = Config.getDouble("tp4056.vtrickle", 2.9);
        double trickleRatio = Config.getDouble("tp4056.itrickle_ratio", 0.13);
        double vccMin = Config.getDouble("tp4056.vcc_min", 4.0);
        double vccMax = Config.getDouble("tp4056.vcc_max", 8.0);
        double maxCurrent = Config.getDouble("tp4056.max_current", 1.0);

        // IBAT = 1200 / RPROG
        double iprog = progK / rprog.getValue();
        double current;
        String mode;

        if (vcc < vccMin) {
            current = 0;
            mode = "VCC is below the minimum, no charging";
        } else if (vcc < vbat + 0.03) {
            current = 0;
            mode = "sleep mode, VCC is within 30 mV of BAT";
        } else if (vbat < vtrickle) {
            current = iprog * trickleRatio;
            mode = "trickle charge (deeply discharged cell)";
        } else if (vbat < vfloat) {
            current = iprog;
            mode = "constant current";
        } else {
            current = iprog;
            mode = "constant voltage, the current falls to " + Units.round(iprog / 10, 3)
                    + " A, then the charge ends";
        }
        double power = (vcc - vbat) * current;

        StringBuilder sb = new StringBuilder(getRef() + " analysis");
        sb.append("\n  Vcc   = ").append(Units.round(vcc, 3)).append(" V");
        sb.append("\n  Vbat  = ").append(Units.round(vbat, 3)).append(" V");
        sb.append("\n  Rprog = ").append(rprog.getValueText()).append(" ohm");
        sb.append("\n  Iprog = ").append(Units.round(iprog, 3)).append(" A (programmed charge current)");
        sb.append("\n  Mode  = ").append(mode);
        sb.append("\n  Ibat  = ").append(Units.round(current, 3)).append(" A");
        sb.append("\n  Power = ").append(Units.round(power, 3)).append(" W dissipated in the charger");
        if (vcc > vccMax) {
            sb.append("\n  WARNING: the input voltage is above the maximum (").append(vccMax).append(" V)");
        }
        if (iprog > maxCurrent) {
            sb.append("\n  WARNING: the programmed current is above the maximum (").append(maxCurrent).append(" A)");
        }
        return sb.toString();
    }
}