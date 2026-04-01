/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
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
