# User stories

1. As a user, I want to add components by type and value (`add resistor 220R`), so that I can build a circuit quickly.
2. As a user, I want to connect pins to each other or to a named net (`connect R1.1 GND`), so that I can describe the wiring.
3. As a user, I want to disconnect a pin or remove a component, so that I can fix mistakes.
4. As a user, I want to name nets (`rename net N1 VIN`) and the circuit (`rename circuit <name>`), so that the circuit is readable.
5. As a user, I want to draw a component with its connections (`draw R1`), so that I can see what it is wired to.
6. As a user, I want to list the components (sorted by designator, type or value) and the nets, so that I get an overview.
7. As a user, I want to see the pins of a component (`show pins U1`), so that I know what to connect.
8. As a user, I want a design rule check (`run drc`), so that wiring mistakes are reported.
9. As a user, I want to analyze an IC (`analyze U1 5V 100mA`), so that I can see output voltage, power loss and warnings.
10. As a user, I want to save, list and load circuits (`save`, `circuits`, `load <name or number>`), so that my work is not lost.
11. As a user, I want clear error messages instead of crashes, so that a wrong input never ends the program.
12. As a user, I want to configure constants (reference voltages, limits) in `config.properties`, so that I can adjust them without recompiling.