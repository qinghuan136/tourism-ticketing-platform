package com.qinghuan.visitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VisitorFingerprintGeneratorTest {

    private final VisitorFingerprintGenerator generator = new VisitorFingerprintGenerator();

    @Test
    void shouldNormalizeWhitespaceAndCaseBeforeHashing() {
        assertEquals(
                generator.generate(" ID_CARD ", " abcd1234 "),
                generator.generate("id_card", "ABCD1234"));
    }

    @Test
    void shouldGenerateDifferentFingerprintForDifferentDocument() {
        assertNotEquals(
                generator.generate("ID_CARD", "ABCD1234"),
                generator.generate("ID_CARD", "ABCD1235"));
    }
}
