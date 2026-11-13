package cascii.service;

import cascii.domain.Ams1117;
import cascii.domain.Capacitor;
import cascii.domain.Component;
import cascii.domain.ConfigurationException;
import cascii.domain.Diode;
import cascii.domain.Inductor;
import cascii.domain.Led;
import cascii.domain.Mt3608;
import cascii.domain.Resistor;
import cascii.domain.Tp4056;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentFactoryTest {
    private final ComponentFactory factory = new ComponentFactory();

    @Test
    void createsPassivePartsWithValues() {
        Component resistor = factory.create("R", "50k");
        Component capacitor = factory.create("C", "100n");
        Component inductor = factory.create("L", "10u");

        assertTrue(resistor instanceof Resistor);
        assertEquals(50000, resistor.getValue(), 1e-9);
        assertTrue(capacitor instanceof Capacitor);
        assertTrue(inductor instanceof Inductor);
    }

    @Test
    void acceptsFullNamesInAnyCase() {
        assertTrue(factory.create("resistor", "1k") instanceof Resistor);
        assertTrue(factory.create("Capacitor", "1u") instanceof Capacitor);
        assertTrue(factory.create("INDUCTOR", "1m") instanceof Inductor);
        assertTrue(factory.create("diode", null) instanceof Diode);
    }

    @Test
    void createsPartsWithoutValues() {
        assertTrue(factory.create("D", null) instanceof Diode);
        assertTrue(factory.create("LED", null) instanceof Led);
        assertTrue(factory.create("TP4056", null) instanceof Tp4056);
        assertTrue(factory.create("MT3608", null) instanceof Mt3608);
    }

    @Test
    void createsEveryRegulatorVersion() {
        assertTrue(factory.create("AMS1117-3.3", null) instanceof Ams1117);
        assertEquals("AMS1117-ADJ", factory.create("ams1117-adj", null).getTypeKey());
    }

    @Test
    void rejectsWrongInput() {
        assertThrows(ConfigurationException.class, () -> factory.create("R", null));
        assertThrows(ConfigurationException.class, () -> factory.create("R", "abc"));
        assertThrows(ConfigurationException.class, () -> factory.create("LED", "5"));
        assertThrows(ConfigurationException.class, () -> factory.create("X", null));
        assertThrows(ConfigurationException.class, () -> factory.create("AMS1117-9", null));
    }
}
