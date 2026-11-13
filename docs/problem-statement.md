# Problem statement

Planning a small electronic circuit usually needs a full EDA tool, even when the goal is only to check
a wiring idea or a regulator's operating point. **ConsolAS-CII** is a console schematic builder: the user
places components, connects pins, and gets an ASCII drawing, a design rule check and simple
calculations, without any GUI.

Scope:

- Resistors, capacitors, inductors, diodes, LEDs and three ICs (AMS1117 in all versions, TP4056, MT3608).
- Named nets (`GND`, `VCC(5V)`) and automatic nets (`N1`, `N2`, ...).
- Design rule check (missing capacitors, unconnected pins, wrong TAB connection, ...).
- Per-IC analysis (`analyze`): output voltage, dissipation, charge current, boost duty cycle.
- Saving and loading circuits as text files.