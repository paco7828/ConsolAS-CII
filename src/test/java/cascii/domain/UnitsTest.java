package cascii.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitsTest {
    @Test
    void parsesPrefixes() {
        assertEquals(50000, Units.parse("50k"), 1e-9);
        assertEquals(4700, Units.parse("4.7k"), 1e-9);
        assertEquals(1e-7, Units.parse("100n"), 1e-18);
        assertEquals(1e-5, Units.parse("10u"), 1e-18);
        assertEquals(220, Units.parse("220"), 1e-9);
    }

    @Test
    void sameValueInDifferentPrefixesIsEqual() {
        assertEquals(Units.parse("0.1u"), Units.parse("100n"), 0);
    }

    @Test
    void acceptsUnitLetters() {
        assertEquals(5, Units.parse("5V"), 1e-9);
        assertEquals(0.1, Units.parse("100mA"), 1e-12);
        assertEquals(1e-5, Units.parse("10uF"), 1e-18);
        assertEquals(220, Units.parse("220R"), 1e-9);
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(ConfigurationException.class, () -> Units.parse("abc"));
        assertThrows(ConfigurationException.class, () -> Units.parse("1.2.3"));
        assertThrows(ConfigurationException.class, () -> Units.parse("  "));
        assertThrows(ConfigurationException.class, () -> Units.parse(null));
    }

    @Test
    void formatsValues() {
        assertEquals("50k", Units.format(50000));
        assertEquals("4.7k", Units.format(4700));
        assertEquals("100n", Units.format(1e-7));
        assertEquals("10u", Units.format(10e-6));
        assertEquals("220", Units.format(220));
        assertEquals("0", Units.format(0));
    }

    @Test
    void roundsToGivenDecimals() {
        assertEquals(23.8, Units.round(23.8472, 1), 0);
    }
}
