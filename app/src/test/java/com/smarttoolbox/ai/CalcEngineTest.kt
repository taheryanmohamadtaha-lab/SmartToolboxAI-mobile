package com.smarttoolbox.ai

import com.smarttoolbox.ai.core.CalcEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcEngineTest {
    @Test fun basic() { assertEquals(14.0, CalcEngine.eval("2+3×4"), 1e-9) }
    @Test fun percent() { assertEquals(220.0, CalcEngine.eval("200+10%"), 1e-9) }
    @Test fun divide() { assertEquals(2.5, CalcEngine.eval("10÷4"), 1e-9) }
    @Test fun parens() { assertEquals(9.0, CalcEngine.eval("(1+2)×3"), 1e-9) }
    @Test fun unary() { assertEquals(-3.0, CalcEngine.eval("−5+2"), 1e-9) }
    @Test fun format() { assertEquals("2.5", CalcEngine.format(2.5)); assertEquals("14", CalcEngine.format(14.0)) }
}
