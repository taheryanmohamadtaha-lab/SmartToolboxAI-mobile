package com.smarttoolbox.ai.core

import java.math.BigDecimal
import java.math.MathContext

object CalcEngine {
    fun eval(src: String): Double {
        val s = src.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "")
        if (s.isEmpty()) throw IllegalArgumentException("empty")
        val p = Parser(s)
        val v = p.parseExpr()
        if (p.pos != s.length) throw IllegalArgumentException("syntax")
        if (v.isNaN() || v.isInfinite()) throw ArithmeticException("invalid")
        return v
    }

    fun format(d: Double): String {
        if (d == Math.floor(d) && Math.abs(d) < 1e15) return d.toLong().toString()
        return BigDecimal(d).round(MathContext(12)).stripTrailingZeros().toPlainString()
    }

    private class Parser(val s: String) {
        var pos = 0
        private var lastPct = false
        private var plainPct = false

        fun parseExpr(): Double {
            var left = parseTerm()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                var right = parseTerm()
                if (plainPct) right = left * right
                left = if (op == '+') left + right else left - right
            }
            return left
        }

        private fun parseTerm(): Double {
            var v = parseFactor()
            var single = lastPct
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                val op = s[pos++]
                val r = parseFactor()
                if (op == '*') v *= r else {
                    if (r == 0.0) throw ArithmeticException("div0")
                    v /= r
                }
                single = false
            }
            plainPct = single
            return v
        }

        private fun parseFactor(): Double {
            if (pos < s.length && s[pos] == '-') { pos++; return -parseFactor() }
            if (pos < s.length && s[pos] == '+') { pos++; return parseFactor() }
            var v = parsePrimary()
            lastPct = false
            while (pos < s.length && s[pos] == '%') { pos++; v /= 100.0; lastPct = true }
            return v
        }

        private fun parsePrimary(): Double {
            if (pos < s.length && s[pos] == '(') {
                pos++
                val v = parseExpr()
                if (pos >= s.length || s[pos] != ')') throw IllegalArgumentException("paren")
                pos++
                return v
            }
            val st = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (st == pos) throw IllegalArgumentException("number")
            return s.substring(st, pos).toDouble()
        }
    }
}
