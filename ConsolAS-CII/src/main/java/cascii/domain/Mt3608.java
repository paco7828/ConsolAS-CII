package cascii.domain;

import cascii.config.Config;

import java.util.ArrayList;

// MT3608: 1.2MHz 2A step-up (boost) converter, SOT23-6
// Pins:
/*
 * 1 - SW
 * 2 - GND
 * 3 - FB  (feedback, 0.6V reference, resistor divider)
 * 4 - EN  (high = on)
 * 5 - IN
 * 6 - NC
 */
public class Mt3608 extends IC {
    public Mt3608() {
        addPin("SW", true);
        addPin("GND", true);
        addPin("FB", true);
        addPin("EN", true);
        addPin("IN", true);
        addPin("NC", false);
    }

    @Override
    public String getTypeName() {
        return "MT3608";
    }

    @Override
    public String getTypeKey() {
        return "MT3608";
    }

    @Override
    public String describe() {
        return "MT3608 (2A step-up converter)";
    }

    @Override
    public ArrayList<String> check() {
        ArrayList<String> problems = super.check();
        Net sw = getPin("SW").getNet();
        Net gnd = getPin("GND").getNet();
        Net fb = getPin("FB").getNet();
        Net en = getPin("EN").getNet();
        Net in = getPin("IN").getNet();

        if (in != null && in == gnd) {
            problems.add(getRef() + ": IN is shorted to GND (net " + in.getName() + ")");
        }
        if (sw != null && (sw == gnd || sw == in)) {
            problems.add(getRef() + ": SW is shorted to GND or IN (net " + sw.getName() + ")");
        }
        if (en != null && en == gnd) {
            problems.add(getRef() + ": EN is on GND, the converter is off");
        }
        if (in != null && !in.hasPinOfType("capacitor")) {
            problems.add(getRef() + ": no input capacitor on the IN net");
        }
        if (in != null && sw != null && sw != in && partBetween(in, sw.getName(), "inductor") == null) {
            problems.add(getRef() + ": no inductor between IN and SW");
        }

        // The output net is the cathode side of the diode connected to SW
        Net out = null;
        if (sw != null) {
            boolean diodeFound = false;
            for (Pin pin : sw.getPins()) {
                if (pin.getOwner().getTypeName().equals("diode") && pin.getName().equals("A")) {
                    diodeFound = true;
                    out = pin.getOwner().getPin("K").getNet();
                }
            }

            if (!diodeFound) {
                problems.add(getRef() + ": no diode with its anode on the SW net");
            } else if (out == null) {
                problems.add(getRef() + ": the diode cathode (output) is not connected");
            } else if (!out.hasPinOfType("capacitor")) {
                problems.add(getRef() + ": no output capacitor on the output net (" + out.getName() + ")");
            }
        }

        if (fb != null && gnd != null) {
            if (fb == gnd) {
                problems.add(getRef() + ": FB is shorted to GND");
            } else {
                if (resistorBetween(fb, gnd.getName()) == null) {
                    problems.add(getRef() + ": no resistor between FB and GND (R2)");
                }
                if (dividerTop(fb, gnd) == null) {
                    problems.add(getRef() + ": no resistor between the output and FB (R1)");
                } else if (out != null && resistorBetween(fb, out.getName()) == null) {
                    problems.add(getRef() + ": the FB resistor (R1) should go to the output net (" + out.getName() + ")");
                }
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

        Net fb = getPin("FB").getNet();
        Net gnd = getPin("GND").getNet();
        if (fb == null || gnd == null) {
            throw new ConfigurationException(getRef() + ": connect FB and GND first (divider: output-R1-FB-R2-GND)");
        }

        Resistor r2 = resistorBetween(fb, gnd.getName());
        Resistor r1 = dividerTop(fb, gnd);
        if (r1 == null || r2 == null) {
            throw new ConfigurationException(getRef() + ": needs R1 (output to FB) and R2 (FB to GND)");
        }
        if (r2.getValue() <= 0) {
            throw new ConfigurationException(r2.getRef() + " must be greater than 0");
        }

        double vref = Config.getDouble("mt3608.vref", 0.6);
        double vinMin = Config.getDouble("mt3608.vin_min", 2.0);
        double vinMax = Config.getDouble("mt3608.vin_max", 24.0);
        double voutMax = Config.getDouble("mt3608.vout_max", 28.0);
        double maxDuty = Config.getDouble("mt3608.max_duty", 0.9);
        double currentLimit = Config.getDouble("mt3608.current_limit", 4.0);
        double efficiency = Config.getDouble("mt3608.efficiency", 0.85);

        // Vout = Vref * (1 + R1 / R2)
        double vout = vref * (1 + r1.getValue() / r2.getValue());
        double duty = Math.max(0, 1 - vin / vout);
        double powerOut = vout * load;
        double powerIn = powerOut / efficiency;
        double iin = powerIn / vin;

        StringBuilder sb = new StringBuilder(getRef() + " analysis");
        sb.append("\n  Vin   = ").append(Units.round(vin, 3)).append(" V");
        sb.append("\n  Vout  = ").append(Units.round(vout, 3)).append(" V (set by ").append(r1.getRef())
                .append(" and ").append(r2.getRef()).append(")");
        sb.append("\n  Iout  = ").append(Units.round(load, 3)).append(" A");
        sb.append("\n  Duty  = ").append(Units.round(duty * 100, 1)).append(" %");
        sb.append("\n  Iin   = ").append(Units.round(iin, 3)).append(" A (at ")
                .append(Units.round(efficiency * 100, 0)).append(" % efficiency)");
        sb.append("\n  Loss  = ").append(Units.round(powerIn - powerOut, 3)).append(" W");
        if (vin < vinMin || vin > vinMax) {
            sb.append("\n  WARNING: the input voltage is outside ").append(vinMin).append("-").append(vinMax).append(" V");
        }
        if (vout <= vin) {
            sb.append("\n  WARNING: a boost converter needs Vout above Vin");
        }
        if (vout > voutMax) {
            sb.append("\n  WARNING: the output voltage is above the maximum (").append(voutMax).append(" V)");
        }
        if (duty > maxDuty) {
            sb.append("\n  WARNING: the duty cycle is above the maximum (").append(Units.round(maxDuty * 100, 0)).append(" %)");
        }
        if (iin > currentLimit) {
            sb.append("\n  WARNING: the input current is above the switch current limit (").append(currentLimit).append(" A)");
        }
        return sb.toString();
    }

    // The upper resistor of the FB divider: on the FB net, its other pin is on another net (not GND)
    private Resistor dividerTop(Net fb, Net gnd) {
        for (Pin pin : fb.getPins()) {
            if (pin.getOwner() instanceof Resistor) {
                for (Pin other : pin.getOwner().getPins()) {
                    if (other != pin && other.getNet() != null && other.getNet() != gnd && other.getNet() != fb) {
                        return (Resistor) pin.getOwner();
                    }
                }
            }
        }
        return null;
    }
}