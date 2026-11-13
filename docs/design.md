# Design

## Pattern: Factory

`ComponentFactory.create(type, value)` turns the text typed by the user (`R`, `resistor`, `AMS1117-3.3`, `TP4056`, ...)
into a `Component` object. `Simulator` never uses `new Resistor(...)` or `new Ams1117(...)` directly, it only works
with the abstract `Component`. Because of this, a new part type needs one new subclass and one new case in the
factory; the commands (`add`, `draw`, `analyze`, ...) do not change.

Participants: `ComponentFactory` (creator), `Component` (product), `Resistor`, `Capacitor`, `Inductor`, `Diode`,
`Led`, `Ams1117`, `Tp4056`, `Mt3608` (concrete products). `CircuitStorage` uses the same factory when loading a file.

## Polymorphism

`draw()`, `describe()`, `check()` and `analyze()` are called on `Component`; each subclass answers in its own way
(a resistor draws `[ 1k ]`, an IC draws a box, `Ams1117.analyze` calculates dropout, `Tp4056.analyze` the charge current).

## Design decisions

- **Layers:** `ui` (Main), `service` (Simulator, ComponentFactory), `domain` (circuit model), `persistence` (files), `config`.
- **Nets instead of wires:** pins are connected through `Net` objects. Named nets (`GND`, `VCC(5V)`) are merged by name,
  automatic nets (`N1`, ...) are merged when connected. Two different named nets can not be joined (short circuit error).
- **Designators:** `R1`, `C2`, `U1`; the number is unique per prefix. Components are `Comparable` (prefix, then number).
- **Errors:** `ConfigurationException` (unchecked) carries user-facing messages; `Simulator.execute` callers print them
  and the program keeps running.
- **Constants** (reference voltages, limits) are in `config.properties` with defaults in code, so the program also
  runs without the file.
- **Persistence:** plain text, one record per line (`CIRCUIT;`, `PART;`, `WIRE;`), saved under `circuits/<name>.txt`
  with a sanitized file name. Loading replays the same `add` / `connect` operations, so the file can not create
  a state that the commands could not.
- **Units:** `Units.parse` accepts SI prefixes (p n u m k M G) and optional unit letters (`5V`, `100mA`, `220R`).

## Known limitations

- The analysis is a first-order calculation (ideal divider, fixed dropout, typical datasheet values), not a simulation.
- No undo; a mistake is fixed with `disconnect` / `remove`.
- The ASCII drawing shows one component at a time with its connections, not the whole schematic.
- Only the listed components exist; there are no wires with junction drawing, buses or sub-circuits.
- Circuit files are not versioned: a file from a future program version may not load.