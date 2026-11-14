package dev.lapis256.hauyne.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleTest {

    @Test
    void processorGeneratesCompilableSource() {
        assertTrue(new Example().generated());
    }
}
