package com.example.core

import kotlin.math.roundToLong

object CalculatorEngine {

    fun evaluate(expression: String): Double? {
        val clean = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")
        if (clean.isEmpty()) return null

        return try {
            val tokens = tokenize(clean) ?: return null
            val result = parseExpression(tokens)
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (_: Exception) {
            null
        }
    }

    fun formatResult(value: Double): String {
        val rounded = (value * 100.0).roundToLong() / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun tokenize(input: String): List<String>? {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    var dotSeen = false
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                        if (input[i] == '.') {
                            if (dotSeen) return null
                            dotSeen = true
                        }
                        sb.append(input[i])
                        i++
                    }
                    tokens.add(sb.toString())
                    continue
                }
                c == '+' || c == '-' || c == '*' || c == '/' -> {
                    // Handle leading unary minus
                    if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                        val sb = StringBuilder("-")
                        i++
                        while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                            sb.append(input[i])
                            i++
                        }
                        if (sb.length == 1) return null
                        tokens.add(sb.toString())
                        continue
                    }
                    tokens.add(c.toString())
                }
                else -> return null
            }
            i++
        }
        return tokens
    }

    private fun parseExpression(tokens: List<String>): Double {
        if (tokens.isEmpty()) return 0.0
        // Trim trailing operator if user hits '=' prematurely
        val effective = if (tokens.last() in listOf("+", "-", "*", "/")) {
            tokens.dropLast(1)
        } else {
            tokens
        }
        if (effective.isEmpty()) return 0.0

        // Pass 1: * and /
        val pass1 = mutableListOf<String>()
        var idx = 0
        while (idx < effective.size) {
            val tok = effective[idx]
            if ((tok == "*" || tok == "/") && pass1.isNotEmpty() && idx + 1 < effective.size) {
                val left = pass1.removeAt(pass1.lastIndex).toDouble()
                val right = effective[idx + 1].toDouble()
                val computed = if (tok == "*") left * right else {
                    if (right == 0.0) Double.NaN else left / right
                }
                pass1.add(computed.toString())
                idx += 2
            } else {
                pass1.add(tok)
                idx++
            }
        }

        // Pass 2: + and -
        var acc = pass1.first().toDouble()
        var j = 1
        while (j + 1 < pass1.size) {
            val op = pass1[j]
            val rhs = pass1[j + 1].toDouble()
            acc = if (op == "+") acc + rhs else acc - rhs
            j += 2
        }
        return acc
    }
}
