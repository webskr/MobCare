package com.mobcare.launcher.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.dashboard.MainActivity
import com.mobcare.security.StealthManager

/**
 * CamouflageGateActivity — the dummy screen opened when the disguised Home Screen shortcut is tapped.
 *
 * Appearance: A convincing fake Calculator UI.
 * Behaviour:
 *   - Normal calculator operations work (so anyone watching sees a real calculator).
 *   - If the user types their secret PIN and presses '=', MobCare's real dashboard opens.
 *   - Wrong PIN just shows the arithmetic result as any calculator would.
 *
 * This Activity is intentionally excluded from Recents (excludeFromRecents=true in manifest).
 */
class CamouflageGateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DISGUISE_LABEL  = "disguise_label"
        const val EXTRA_TARGET_PACKAGE  = "target_package"
    }

    // Calculator state
    private var displayText  = "0"
    private var expression   = StringBuilder()
    private lateinit var tvDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildCalculatorUI()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI Construction (programmatic — no XML layout needed)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildCalculatorUI() {
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        // ── Display ──────────────────────────────────────────────────────────
        tvDisplay = TextView(this).apply {
            text       = "0"
            textSize   = 56f
            setTextColor(Color.WHITE)
            gravity    = Gravity.END or Gravity.BOTTOM
            setPadding((16*dp).toInt(), (32*dp).toInt(), (24*dp).toInt(), (12*dp).toInt())
            typeface   = Typeface.DEFAULT
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 2.5f)
        }
        root.addView(tvDisplay)

        // ── Button Grid ───────────────────────────────────────────────────────
        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount    = 5
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 3f)
        }

        val buttons = listOf(
            listOf("AC", "+/-", "%", "÷"),
            listOf("7",  "8",  "9",  "×"),
            listOf("4",  "5",  "6",  "−"),
            listOf("1",  "2",  "3",  "+"),
            listOf("0",  ".",  "⌫",  "=")
        )

        buttons.forEachIndexed { row, rowBtns ->
            rowBtns.forEachIndexed { col, label ->
                val isOperator = label in listOf("÷", "×", "−", "+", "=")
                val isTopRow   = row == 0
                val isZero     = label == "0"

                val btn = Button(this).apply {
                    text     = label
                    textSize = 22f
                    setTextColor(if (isOperator) Color.WHITE else if (isTopRow) Color.parseColor("#1C1C1E") else Color.WHITE)
                    setTypeface(null, if (isOperator || label == "=") Typeface.BOLD else Typeface.NORMAL)
                    background = GradientDrawable().apply {
                        shape        = GradientDrawable.OVAL
                        setColor(when {
                            isOperator  -> Color.parseColor("#FF9F0A")
                            isTopRow    -> Color.parseColor("#A5A5A5")
                            else        -> Color.parseColor("#333333")
                        })
                    }
                    val spec = GridLayout.spec(row, 1, 1f)
                    val colSpec = GridLayout.spec(col, 1, if (isZero) 2f else 1f)
                    layoutParams = GridLayout.LayoutParams(spec, colSpec).apply {
                        width  = 0
                        height = 0
                        setMargins((6*dp).toInt(), (6*dp).toInt(), (6*dp).toInt(), (6*dp).toInt())
                    }
                    setOnClickListener { onCalcButton(label) }
                }
                // Wide "0" button spans 2 columns
                if (!isZero || col == 0) {
                    grid.addView(btn)
                }
            }
        }

        root.addView(grid)
        setContentView(root)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button Logic
    // ─────────────────────────────────────────────────────────────────────────

    private fun onCalcButton(key: String) {
        when (key) {
            "AC"  -> { expression.clear(); updateDisplay("0") }
            "⌫"  -> {
                if (expression.isNotEmpty()) {
                    expression.deleteCharAt(expression.length - 1)
                    updateDisplay(expression.toString().ifEmpty { "0" })
                }
            }
            "="   -> handleEquals()
            "+/-" -> {
                val cur = displayText.toDoubleOrNull()
                if (cur != null) updateDisplay((-cur).toFormattedString())
            }
            "%"   -> {
                val cur = displayText.toDoubleOrNull()
                if (cur != null) updateDisplay((cur / 100.0).toFormattedString())
            }
            else  -> {
                if (displayText == "0" && key.isDigit()) {
                    expression.clear()
                }
                expression.append(key)
                updateDisplay(expression.toString())
            }
        }
    }

    private fun handleEquals() {
        val raw = expression.toString()

        // ── Secret PIN check ─────────────────────────────────────────────────
        // Strip all non-digit chars from the raw expression to extract the entered PIN
        val digitsOnly = raw.filter { it.isDigit() }
        if (StealthManager.verifyPin(this, digitsOnly)) {
            // Correct PIN → open real MobCare
            Toast.makeText(this, "Unlocking…", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }, 300)
            return
        }

        // ── Normal calculator evaluation ─────────────────────────────────────
        val result = evalExpression(raw)
        expression.clear()
        expression.append(result)
        updateDisplay(result)
    }

    private fun updateDisplay(value: String) {
        displayText = value
        tvDisplay.text = value
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simple Expression Evaluator (handles +, −/-, ×/*, ÷//)
    // ─────────────────────────────────────────────────────────────────────────

    private fun evalExpression(expr: String): String {
        return try {
            // Normalise operators to standard chars
            val normalised = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
            val result = simpleEval(normalised)
            result.toFormattedString()
        } catch (e: Exception) {
            "Error"
        }
    }

    /** Evaluates a simple arithmetic string (left-to-right, no precedence). */
    private fun simpleEval(expr: String): Double {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        expr.forEach { c ->
            if (c in listOf('+', '-', '*', '/') && current.isNotEmpty()) {
                tokens.add(current.toString()); current.clear(); tokens.add(c.toString())
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())

        var result = tokens[0].toDouble()
        var i = 1
        while (i < tokens.size - 1) {
            val op  = tokens[i]
            val num = tokens[i + 1].toDouble()
            result = when (op) {
                "+" -> result + num
                "-" -> result - num
                "*" -> result * num
                "/" -> result / num
                else -> result
            }
            i += 2
        }
        return result
    }

    private fun Double.toFormattedString(): String =
        if (this == kotlin.math.floor(this) && !this.isInfinite()) this.toLong().toString()
        else this.toString()

    private fun String.isDigit(): Boolean = this.length == 1 && this[0].isDigit()
}
