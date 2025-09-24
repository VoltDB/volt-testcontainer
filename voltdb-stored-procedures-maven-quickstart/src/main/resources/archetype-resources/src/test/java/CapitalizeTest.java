package ${package};

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


class CapitalizeTest {

    @Test
    void testCapitalizeFully() {
        CapitalizeAndPut proc = new CapitalizeAndPut();
        
        String result = proc.capitalize("hello world");
        
        assertEquals("Hello World", result);
    }
}
