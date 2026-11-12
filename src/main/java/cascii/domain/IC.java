package cascii.domain;

import java.util.ArrayList;

// An integrated circuit with N number of pins (drawn as a box)
// Half of the pins are on the left side, the other half is on the right side
public abstract class IC extends Component {
    protected IC() {
        super("U");
    }

    // Finds a resistor with one pin on <net> and the other pin on the net called <otherNetName>
    protected Resistor resistorBetween(Net net, String otherNetName) {
        return (Resistor) partBetween(net, otherNetName, "resistor");
    }

    // Finds a part of the given type (example: "inductor") with one pin on <net> and another pin on the net called <otherNetName>
    protected Component partBetween(Net net, String otherNetName, String typeName) {
        for (Pin pin : net.getPins()) {
            if (pin.getOwner().getTypeName().equals(typeName)) {
                for (Pin other : pin.getOwner().getPins()) {
                    if (other != pin && other.getNet() != null && other.getNet().getName().equals(otherNetName)) {
                        return pin.getOwner();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String draw() {
        ArrayList<Pin> pins = getPins();
        int count = pins.size();
        int rows = (count + 1) / 2;
        int rightCount = count - rows;

        String[] leftLabel = new String[rows];
        String[] leftText = new String[rows];
        String[] rightLabel = new String[rows];
        String[] rightText = new String[rows];
        int labelWidth = 0;
        int leftWidth = 0;
        int rightWidth = 0;

        for (int row = 0; row < rows; row++) {
            Pin left = pins.get(row);
            leftLabel[row] = left.getConnectionLabel();
            leftText[row] = left.getNumber() + " " + left.getName();
            labelWidth = Math.max(labelWidth, leftLabel[row].length());
            leftWidth = Math.max(leftWidth, leftText[row].length());

            if (row < rightCount) {
                Pin right = pins.get(count - 1 - row);
                rightLabel[row] = right.getConnectionLabel();
                rightText[row] = right.getName() + " " + right.getNumber();
                rightWidth = Math.max(rightWidth, rightText[row].length());
            } else {
                rightLabel[row] = null;
                rightText[row] = "";
            }
        }

        int innerWidth = leftWidth + rightWidth + 4;
        String margin = "    " + TextUtil.spaces(labelWidth + 3);

        StringBuilder sb = new StringBuilder(getRef() + "  " + describe() + "\n");
        sb.append(margin).append("┌").append(TextUtil.repeat("─", innerWidth)).append("┐");
        for (int row = 0; row < rows; row++) {
            sb.append("\n    ")
                    .append(TextUtil.padLeft(leftLabel[row], labelWidth))
                    .append(" ──┤ ")
                    .append(TextUtil.padRight(leftText[row], leftWidth))
                    .append("  ")
                    .append(TextUtil.padLeft(rightText[row], rightWidth))
                    .append(" ");
            if (rightLabel[row] != null) {
                sb.append("├── ").append(rightLabel[row]);
            } else {
                sb.append("│");
            }
        }
        sb.append("\n").append(margin).append("└").append(TextUtil.repeat("─", innerWidth)).append("┘");
        return sb.toString();
    }
}