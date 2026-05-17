package de.sudokuonline.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient

// ════════════════════════════════════════════
//  LaTeX Rendering via KaTeX WebView
// ════════════════════════════════════════════

private fun buildLatexHtml(text: String, fontSize: Int, fontWeight: String, hexColor: String): String {
    val escaped = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br>")
    return """<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/contrib/auto-render.min.js"
  onload="renderMathInElement(document.getElementById('c'),{delimiters:[{left:'\\(',right:'\\)',display:false},{left:'\\[',right:'\\]',display:true}],throwOnError:false})"></script>
<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Roboto',sans-serif;font-size:${fontSize}px;color:${hexColor};background:transparent;font-weight:${fontWeight};line-height:1.7}.katex{font-size:1.15em}.katex-display{margin:0.5em 0}</style>
</head><body><div id="c">${escaped}</div></body></html>"""
}

@Composable
private fun LatexText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
    fontWeight: String = "normal",
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val hexColor = String.format("#%06X", 0xFFFFFF and color.toArgb())
    val estimatedLines = text.count { it == '\n' } + (text.length / 35) + 1
    var heightDp by remember(text) { mutableStateOf((estimatedLines * 22 + 12).dp) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                isClickable = false
                isFocusable = false
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        },
        update = { wv ->
            val tag = wv.tag as? String
            if (tag != text) {
                wv.tag = text
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            "(function(){return document.getElementById('c').offsetHeight})()"
                        ) { result ->
                            val h = result.toFloatOrNull() ?: return@evaluateJavascript
                            if (h > 0) heightDp = (h + 4).dp
                        }
                    }
                }
                wv.loadDataWithBaseURL(
                    null,
                    buildLatexHtml(text, fontSize, fontWeight, hexColor),
                    "text/html", "UTF-8", null
                )
            }
        },
        modifier = modifier.fillMaxWidth().height(heightDp)
    )
}

@Composable
private fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    if ("\\" in text) {
        LatexText(text, modifier, fontSize, if (fontWeight >= FontWeight.Bold) "bold" else if (fontWeight >= FontWeight.Medium) "500" else "normal", color)
    } else {
        Text(text, modifier = modifier, fontSize = fontSize.sp, fontWeight = fontWeight, color = color)
    }
}

private enum class MathTopic(val displayName: String, val icon: ImageVector, val color: Color) {
    BETRAG("Beträge & Ungleichungen", Icons.Default.Balance, Color(0xFF4CAF50)),
    INDUKTION("Vollständige Induktion", Icons.Default.Repeat, Color(0xFF2196F3)),
    FOLGEN("Folgen & Konvergenz", Icons.Default.TrendingUp, Color(0xFFFF9800)),
    REIHEN("Reihen", Icons.Default.Functions, Color(0xFF9C27B0)),
    ABLEITUNG("Ableitungen", Icons.Default.ShowChart, Color(0xFFE91E63)),
    LINEARE_ALGEBRA("Lineare Algebra", Icons.Default.GridOn, Color(0xFF00BCD4)),
    STETIGKEIT("Stetigkeit", Icons.Default.Timeline, Color(0xFF795548))
}

private enum class Difficulty(val displayName: String, val color: Color) {
    LEICHT("Leicht", Color(0xFF4CAF50)),
    MITTEL("Mittel", Color(0xFFFF9800)),
    SCHWER("Schwer", Color(0xFFF44336))
}

private data class MathQuestion(
    val topic: MathTopic,
    val difficulty: Difficulty,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val solutionExplanation: String
)

// ════════════════════════════════════════════
//  BETRÄGE
// ════════════════════════════════════════════

private val betragQuestions = listOf(
    // LEICHT
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Was ist \\(|-7|\\)?", listOf("\\(7\\)", "\\(-7\\)", "\\(0\\)", "\\(49\\)"), 0, "\\(|-7| = 7\\)\nDer Betrag gibt den Abstand zur \\(0\\) an."),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Löse \\(|x| = 5\\):", listOf("\\(x = 5\\) oder \\(x = -5\\)", "\\(x = 5\\)", "\\(x = -5\\)", "\\(x = 25\\)"), 0, "\\(|x| = 5 \\Leftrightarrow x = 5\\) oder \\(x = -5\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Löse \\(|x - 2| = 3\\):", listOf("\\(x = 5\\) oder \\(x = -1\\)", "\\(x = 5\\)", "\\(x = 1\\)", "\\(x = -1\\)"), 0, "\\(|x-2| = 3 \\Leftrightarrow x-2 = 3\\) oder \\(x-2 = -3\\)\n\\(\\Leftrightarrow x = 5\\) oder \\(x = -1\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Löse \\(|2x| = 8\\):", listOf("\\(x = 4\\) oder \\(x = -4\\)", "\\(x = 4\\)", "\\(x = 16\\)", "\\(x = -4\\)"), 0, "\\(|2x| = 8 \\Leftrightarrow 2x = 8\\) oder \\(2x = -8\\)\n\\(\\Leftrightarrow x = 4\\) oder \\(x = -4\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Was ist \\(|3 - 8|\\)?", listOf("\\(5\\)", "\\(-5\\)", "\\(11\\)", "\\(8\\)"), 0, "\\(|3-8| = |-5| = 5\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Löse \\(|x| < 3\\):", listOf("\\(-3 < x < 3\\)", "\\(x < 3\\)", "\\(x > -3\\)", "\\(|x| > -3\\)"), 0, "\\(|x| < 3 \\Leftrightarrow -3 < x < 3\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Dreiecksungleichung: \\(|a+b| \\leq\\) ?", listOf("\\(|a| + |b|\\)", "\\(|a| \\cdot |b|\\)", "\\(|a - b|\\)", "\\(a + b\\)"), 0, "Dreiecksungleichung: \\(|a+b| \\leq |a| + |b|\\)\nFundamentale Eigenschaft des Betrags."),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Löse \\(|3x - 6| < 9\\):", listOf("\\(-1 < x < 5\\)", "\\(x < 5\\)", "\\(x > -1\\)", "\\(-3 < x < 3\\)"), 0, "\\(|3x-6| < 9 \\Leftrightarrow -9 < 3x-6 < 9\\)\n\\(\\Leftrightarrow -3 < 3x < 15 \\Leftrightarrow -1 < x < 5\\)"),
    // MITTEL
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Löse \\(|x - 4| = |x + 1|\\):", listOf("\\(x = \\frac{3}{2}\\)", "\\(x = \\frac{5}{2}\\)", "\\(x = 2\\)", "\\(x = -1\\)"), 0, "\\((x-4)^2 = (x+1)^2\\)\n\\(\\Leftrightarrow x^2-8x+16 = x^2+2x+1\\)\n\\(\\Leftrightarrow 10x = 15 \\Leftrightarrow x = \\frac{3}{2}\\)\nGeometrisch: Mitte von \\(-1\\) und \\(4\\)."),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Für welche \\(x\\) gilt \\(|2x| > |5 - 2x|\\)?", listOf("\\(x > \\frac{5}{4}\\)", "\\(x > \\frac{5}{2}\\)", "\\(x > 1\\)", "\\(x > 2\\)"), 0, "\\(\\left|\\frac{5-2x}{2x}\\right| < 1 \\Leftrightarrow 0 < \\frac{5}{2x} < 2\\)\n\\(\\Leftrightarrow x > \\frac{5}{4}\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Vereinfache \\(|x-3| + |x+1|\\) für \\(x \\in [-1,3]\\):", listOf("\\(4\\)", "\\(2x-2\\)", "\\(2x+4\\)", "\\(|2x-2|\\)"), 0, "Für \\(-1 \\leq x \\leq 3\\): \\(|x-3|=3-x\\), \\(|x+1|=x+1\\)\nSumme \\(= 3-x+x+1 = 4\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Löse \\(|x^2 - 4| = 0\\):", listOf("\\(x = 2\\) oder \\(x = -2\\)", "\\(x = 2\\)", "\\(x = 4\\)", "\\(x = 0\\)"), 0, "\\(|x^2-4| = 0 \\Leftrightarrow x^2-4 = 0 \\Leftrightarrow x^2 = 4\\)\n\\(\\Leftrightarrow x = \\pm 2\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Löse \\(|x+2| \\geq |x-1|\\):", listOf("\\(x \\geq -\\frac{1}{2}\\)", "\\(x \\geq 1\\)", "\\(x \\leq -2\\)", "alle \\(x \\in \\mathbb{R}\\)"), 0, "Quadrieren: \\((x+2)^2 \\geq (x-1)^2\\)\n\\(x^2+4x+4 \\geq x^2-2x+1\\)\n\\(6x \\geq -3 \\Leftrightarrow x \\geq -\\frac{1}{2}\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Löse \\(\\big||x| - 3\\big| = 1\\):", listOf("\\(x \\in \\{-4,-2,2,4\\}\\)", "\\(x \\in \\{2,4\\}\\)", "\\(x \\in \\{-3,3\\}\\)", "\\(x \\in \\{-4,4\\}\\)"), 0, "\\(\\big||x|-3\\big| = 1 \\Leftrightarrow |x|-3 = 1\\) oder \\(|x|-3 = -1\\)\n\\(\\Leftrightarrow |x| = 4\\) oder \\(|x| = 2\\)\n\\(\\Leftrightarrow x \\in \\{-4,-2,2,4\\}\\)"),
    // SCHWER
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Löse \\(\\big|2 - |2 - x|\\big| \\leq 1\\):", listOf("\\([-1,1] \\cup [3,5]\\)", "\\([0,2] \\cup [4,6]\\)", "\\([-1,5]\\)", "\\([1,3]\\)"), 0, "\\(\\big|2-|2-x|\\big| \\leq 1 \\Leftrightarrow 1 \\leq |2-x| \\leq 3\\)\n\\(\\Leftrightarrow x \\in [-1,1] \\cup [3,5]\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Für welche \\(x\\) gilt \\(\\frac{3x}{1+|x|} < 4x^2\\)?", listOf("\\((-\\infty,0) \\cup (\\frac{1}{2},\\infty)\\)", "\\((0,\\infty)\\)", "\\(\\mathbb{R}\\setminus\\{0\\}\\)", "\\((-\\infty,-\\frac{1}{2}) \\cup (\\frac{1}{2},\\infty)\\)"), 0, "Fall \\(x<0\\): \\(\\frac{3x}{1+|x|} < 0 < 4x^2\\) ✓\nFall \\(x \\geq 0\\): Umformen ergibt \\(x > \\frac{1}{2}\\)\nAlso \\(x \\in (-\\infty,0) \\cup (\\frac{1}{2},\\infty)\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Zeige: \\(|a|-|b| \\leq |a-b|\\). Wie heißt das?", listOf("Umgekehrte Dreiecksungl.", "Dreiecksungleichung", "Cauchy-Schwarz", "Bernoulli-Ungl."), 0, "Umgekehrte Dreiecksungleichung:\n\\[\\big||a|-|b|\\big| \\leq |a-b|\\]\nFolgt aus der Dreiecksungleichung:\n\\(|a| = |(a-b)+b| \\leq |a-b|+|b|\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Löse \\(|x-1| + |x-3| + |x-5| = 6\\):", listOf("\\([1,5]\\)", "\\(x = 3\\)", "\\(\\{1,3,5\\}\\)", "\\([0,6]\\)"), 0, "Für \\(x \\in [1,5]\\): \\(|x-1|+|x-3|+|x-5|\\)\n\\(= (x-1)+|x-3|+(5-x) = 4+|x-3|\\)\n\\(= 6 \\Leftrightarrow |x-3|=2\\)\n\\(\\Rightarrow x \\in [1,5]\\)"),
    // Neue LEICHT Fragen
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Was ist \\(|5 - 12|\\)?", listOf("\\(7\\)", "\\(-7\\)", "\\(17\\)", "\\(12\\)"), 0, "\\(|5-12| = |-7| = 7\\)\nDer Betrag ist immer nicht-negativ."),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Löse \\(|x + 4| = 7\\):", listOf("\\(x = 3\\) oder \\(x = -11\\)", "\\(x = 3\\)", "\\(x = -11\\)", "\\(x = 11\\)"), 0, "\\(|x+4| = 7 \\Leftrightarrow x+4 = 7\\) oder \\(x+4 = -7\\)\n\\(\\Leftrightarrow x = 3\\) oder \\(x = -11\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Für welche \\(x\\) gilt \\(|x| \\geq 2\\)?", listOf("\\(x \\leq -2\\) oder \\(x \\geq 2\\)", "\\(x \\geq 2\\)", "\\(-2 \\leq x \\leq 2\\)", "\\(x > 2\\)"), 0, "\\(|x| \\geq 2 \\Leftrightarrow x \\leq -2\\) oder \\(x \\geq 2\\)\nDas ist das Komplement von \\((-2,2)\\)."),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Was ist \\(|(-3)^2|\\)?", listOf("\\(9\\)", "\\(-9\\)", "\\(3\\)", "\\(6\\)"), 0, "\\((-3)^2 = 9\\), also \\(|9| = 9\\).\nDer Betrag einer positiven Zahl ist die Zahl selbst."),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Löse \\(|4x| = 20\\):", listOf("\\(x = 5\\) oder \\(x = -5\\)", "\\(x = 5\\)", "\\(x = -5\\)", "\\(x = 80\\)"), 0, "\\(|4x| = 20 \\Leftrightarrow 4|x| = 20 \\Leftrightarrow |x| = 5\\)\n\\(\\Leftrightarrow x = 5\\) oder \\(x = -5\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.LEICHT, "Was gilt für \\(|a \\cdot b|\\)?", listOf("\\(|a \\cdot b| = |a| \\cdot |b|\\)", "\\(|a \\cdot b| = |a| + |b|\\)", "\\(|a \\cdot b| \\leq |a| \\cdot |b|\\)", "\\(|a \\cdot b| = a \\cdot b\\)"), 0, "Multiplikativität des Betrags:\n\\(|a \\cdot b| = |a| \\cdot |b|\\)\nDies gilt für alle \\(a, b \\in \\mathbb{R}\\)."),
    // Neue MITTEL Fragen
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Löse \\(|2x - 3| \\leq |x + 1|\\):", listOf("\\(-4 \\leq x \\leq \\frac{2}{3}\\) oder \\(x \\geq \\frac{4}{3}\\)", "\\(x \\leq \\frac{2}{3}\\)", "\\(x \\geq -4\\)", "\\(\\frac{2}{3} \\leq x \\leq \\frac{4}{3}\\)"), 0, "Fallunterscheidung oder Quadrieren:\n\\((2x-3)^2 \\leq (x+1)^2\\)\n\\(4x^2-12x+9 \\leq x^2+2x+1\\)\n\\(3x^2-14x+8 \\leq 0\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Bestimme alle \\(x\\) mit \\(|x^2 - 9| < 7\\):", listOf("\\((-4,-\\sqrt{2}) \\cup (\\sqrt{2},4)\\)", "\\((-4,4)\\)", "\\((-3,3)\\)", "\\((\\sqrt{2},4)\\)"), 0, "\\(|x^2-9| < 7 \\Leftrightarrow -7 < x^2-9 < 7\\)\n\\(\\Leftrightarrow 2 < x^2 < 16\\)\n\\(\\Leftrightarrow \\sqrt{2} < |x| < 4\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Vereinfache \\(|x-2| - |x+2|\\) für \\(x > 2\\):", listOf("\\(-4\\)", "\\(4\\)", "\\(2x\\)", "\\(-2x+4\\)"), 0, "Für \\(x > 2\\): \\(x-2 > 0\\) und \\(x+2 > 0\\)\n\\(|x-2| - |x+2| = (x-2) - (x+2) = -4\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Löse \\(|x - 1| = 2x - 4\\):", listOf("\\(x = 5\\)", "\\(x = 1\\) oder \\(x = 5\\)", "\\(x = 3\\)", "keine Lösung"), 0, "Für \\(x \\geq 1\\): \\(x-1 = 2x-4 \\Rightarrow x = 3\\)\nPrüfung: \\(|3-1| = 2 \\neq 2 \\cdot 3 - 4 = 2\\) ✓\nFür \\(x < 1\\): \\(-x+1 = 2x-4 \\Rightarrow x = \\frac{5}{3}\\) ✗\nAlso \\(x = 3\\) und weitere Prüfung ergibt \\(x=5\\)."),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Zeige: \\(|a+b| \\geq |a| - |b|\\). Welche Ungleichung nutzt man?", listOf("Dreiecksungleichung auf \\(a = (a+b) - b\\)", "Cauchy-Schwarz", "Bernoulli-Ungleichung", "AM-GM-Ungleichung"), 0, "\\(|a| = |(a+b) + (-b)| \\leq |a+b| + |{-b}| = |a+b| + |b|\\)\n\\(\\Rightarrow |a| - |b| \\leq |a+b|\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.MITTEL, "Für welche \\(a \\in \\mathbb{R}\\) hat \\(|x - a| + |x + a| = 4\\) Lösungen?", listOf("\\(|a| \\leq 2\\)", "\\(a = 2\\)", "\\(|a| < 2\\)", "alle \\(a\\)"), 0, "Für \\(-a \\leq x \\leq a\\) (falls \\(a > 0\\)):\n\\(|x-a|+|x+a| = (a-x)+(x+a) = 2a\\)\nAlso \\(2a = 4 \\Leftrightarrow a = 2\\)\nAllgemein: Lösungen existieren gdw. \\(|a| \\leq 2\\)."),
    // Neue SCHWER Fragen
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Löse \\(|x^2 - 2x| = |x^2 - 4|\\):", listOf("\\(x \\in \\{-1, 2\\}\\)", "\\(x = 2\\)", "\\(x \\in \\{1, 2\\}\\)", "\\(x \\in \\{-2, 1, 2\\}\\)"), 0, "Quadrieren: \\((x^2-2x)^2 = (x^2-4)^2\\)\n\\((x^2-2x-x^2+4)(x^2-2x+x^2-4) = 0\\)\n\\((4-2x)(2x^2-2x-4) = 0\\)\n\\(x = 2\\) oder \\(x^2-x-2 = 0\\)\n\\(x = 2\\) oder \\(x = -1\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Bestimme \\(\\min_{x \\in \\mathbb{R}} (|x-1| + |x-2| + |x-5|)\\):", listOf("\\(4\\)", "\\(5\\)", "\\(6\\)", "\\(3\\)"), 0, "Minimum bei Median der Punkte, also \\(x = 2\\).\n\\(|2-1| + |2-2| + |2-5| = 1 + 0 + 3 = 4\\)"),
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Löse \\(\\sqrt{(x-3)^2} + \\sqrt{(x+1)^2} = 6\\):", listOf("\\(x \\in [-1, 3]\\)", "\\(x = 1\\)", "\\(x \\in \\{-1, 3\\}\\)", "\\(x \\in [-3, 1]\\)"), 0, "\\(\\sqrt{(x-3)^2} = |x-3|\\), \\(\\sqrt{(x+1)^2} = |x+1|\\)\n\\(|x-3| + |x+1| = 6\\)\nAbstand von \\(-1\\) bis \\(3\\) ist \\(4\\).\nFür \\(x \\in [-1,3]\\): Summe \\(= 4 < 6\\), außerhalb: \\(\\geq 4\\).\nLösung: \\(x \\in [-1,3]\\) mit Randwerten passend."),
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Für welche \\(k > 0\\) hat \\(|x - 1| + |x - 4| = k\\) genau eine Lösung?", listOf("Keine solchen \\(k\\)", "\\(k = 3\\)", "\\(k = 5\\)", "\\(k > 3\\)"), 0, "Für \\(k < 3\\): keine Lösung\nFür \\(k = 3\\): Intervall \\([1,4]\\) als Lösung\nFür \\(k > 3\\): genau zwei Lösungen\nAlso gibt es kein \\(k\\) mit genau einer Lösung."),
    MathQuestion(MathTopic.BETRAG, Difficulty.SCHWER, "Zeige: \\(||a| - |b|| \\leq |a - b|\\). Wie heißt diese Ungleichung?", listOf("Umgekehrte Dreiecksungleichung", "Dreiecksungleichung", "Parallelogrammgleichung", "Minkowski-Ungleichung"), 0, "Umgekehrte Dreiecksungleichung:\n\\(|a| = |(a-b)+b| \\leq |a-b|+|b|\\)\n\\(\\Rightarrow |a|-|b| \\leq |a-b|\\)\nAnalog: \\(|b|-|a| \\leq |a-b|\\)\n\\(\\Rightarrow ||a|-|b|| \\leq |a-b|\\)"),
)

// ════════════════════════════════════════════
//  INDUKTION
// ════════════════════════════════════════════

private val induktionQuestions = listOf(
    // LEICHT
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "Was ist der Induktionsanfang (IA)?", listOf("Aussage für \\(n=1\\) (oder \\(n_0\\)) zeigen", "Aussage für alle \\(n\\) zeigen", "Annahme dass Aussage gilt", "Grenzwert berechnen"), 0, "IA: Man zeigt, dass die Aussage für den kleinsten Wert (meist \\(n=1\\)) gilt."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "Was ist die Induktionsvoraussetzung (IV)?", listOf("Annahme: Aussage gilt für ein \\(n\\)", "Beweis für \\(n+1\\)", "Aussage gilt für alle \\(n\\)", "Widerspruchsannahme"), 0, "IV: Man nimmt an, die Aussage gilt für ein beliebiges aber festes \\(n \\in \\mathbb{N}\\)."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "\\(\\sum_{k=1}^{n} k = ?\\) (Kleiner Gauß)", listOf("\\(\\frac{n(n+1)}{2}\\)", "\\(\\frac{n^2}{2}\\)", "\\(\\frac{n(n-1)}{2}\\)", "\\(\\frac{(n+1)^2}{2}\\)"), 0, "Gauß'sche Summenformel:\n\\[\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}\\]\nBeispiel: \\(1+2+...+100 = \\frac{100 \\cdot 101}{2} = 5050\\)"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "Was zeigt man im Induktionsschritt (IS)?", listOf("Aus Gültigkeit für \\(n\\) folgt für \\(n+1\\)", "Aussage gilt für \\(n=1\\)", "Aussage gilt für \\(n=\\infty\\)", "Gegenbeispiel"), 0, "IS: Unter Verwendung der IV (Aussage gilt für \\(n\\)) zeigt man, dass die Aussage auch für \\(n+1\\) gilt."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "\\(2+4+6+...+2n = ?\\)", listOf("\\(n(n+1)\\)", "\\(n^2\\)", "\\(2n^2\\)", "\\(\\frac{n(n+1)}{2}\\)"), 0, "\\(\\sum_{k=1}^{n} 2k = 2 \\cdot \\sum k = 2 \\cdot \\frac{n(n+1)}{2} = n(n+1)\\)"),
    // MITTEL
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "\\(\\sum_{k=1}^{n} (2k-1) = ?\\)", listOf("\\(n^2\\)", "\\(\\frac{n(n+1)}{2}\\)", "\\(2n^2-n\\)", "\\(n(2n-1)\\)"), 0, "IA: \\(n=1\\): \\(1 = 1^2\\) ✓\nIS: \\(n^2+2(n+1)-1 = n^2+2n+1 = (n+1)^2\\)"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "\\(\\sum_{k=1}^{n} k^2 = ?\\)", listOf("\\(\\frac{n(n+1)(2n+1)}{6}\\)", "\\(\\frac{n^2(n+1)}{2}\\)", "\\(\\frac{n(n+1)}{2}\\)", "\\(\\frac{(2n+1)!}{6}\\)"), 0, "IA: \\(1 = \\frac{1 \\cdot 2 \\cdot 3}{6}\\) ✓\nIS: \\(\\frac{n(n+1)(2n+1)}{6} + (n+1)^2 = \\frac{(n+1)(n+2)(2n+3)}{6}\\)"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "Geometrische Summenformel:\n\\(\\sum_{k=0}^{n-1} q^k = ?\\) für \\(q \\neq 1\\)", listOf("\\(\\frac{1-q^n}{1-q}\\)", "\\(\\frac{1-q^{n+1}}{1-q}\\)", "\\(\\frac{q^n}{1-q}\\)", "\\(n \\cdot q^n\\)"), 0, "IA: \\(n=1\\): \\(q^0 = 1 = \\frac{1-q}{1-q}\\) ✓\nIS: \\(\\frac{1-q^n}{1-q} + q^n = \\frac{1-q^{n+1}}{1-q}\\)"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "Zeige per Induktion: \\(n! \\geq 2^{n-1}\\) für \\(n \\geq 1\\).\nWas ist der IS?", listOf("\\((n+1)! = (n+1) \\cdot n! \\geq (n+1) \\cdot 2^{n-1} \\geq 2^n\\)", "\\(n! \\geq n^2\\)", "\\((n+1)! = n!+1\\)", "\\(n! = n \\cdot (n-1)!\\)"), 0, "\\((n+1)! = (n+1) \\cdot n! \\geq (n+1) \\cdot 2^{n-1}\\)\nDa \\(n+1 \\geq 2\\) für \\(n \\geq 1\\):\n\\(\\geq 2 \\cdot 2^{n-1} = 2^n\\) ✓"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "Zeige: \\(6^n - 5n + 4\\) teilbar durch 5.\nWas ist der Schlüsseltrick?", listOf("\\(6^{n+1}+4 = 6(6^n+4)-20\\)", "\\(6^{n+1}+4 = 6^n+6 \\cdot 4\\)", "\\(6 \\cdot 6^n = 5 \\cdot 6^n+6^n\\)", "Mod-Rechnung"), 0, "\\(6^{n+1}+4 = 6(6^n+4)-20\\)\nNach IV: \\(6^n+4 = 5k\\)\nAlso: \\(6 \\cdot 5k - 20 = 5(6k-4)\\) ✓"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "\\(\\sum_{k=1}^{n} k^3 = ?\\)", listOf("\\(\\left[\\frac{n(n+1)}{2}\\right]^2\\)", "\\(\\frac{n^2(n+1)^2}{2}\\)", "\\(\\frac{n(n+1)(2n+1)}{6}\\)", "\\(\\frac{n^4}{4}\\)"), 0, "\\(\\sum k^3 = \\left[\\frac{n(n+1)}{2}\\right]^2 = \\left(\\sum k\\right)^2\\)\nBeispiel: \\(1^3+2^3+3^3 = 36 = 6^2 = (1+2+3)^2\\)"),
    // SCHWER
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "\\(\\prod_{k=1}^{n}\\left(1+\\frac{1}{k}\\right)^k = ?\\)", listOf("\\(\\frac{(n+1)^{n+1}}{(n+1)!}\\)", "\\(\\frac{(n+1)^n}{n!}\\)", "\\(\\frac{n^n}{n!}\\)", "\\(\\frac{e^n}{(n+1)!}\\)"), 0, "IA: \\((1+1)^1 = 2 = \\frac{2^2}{2!}\\) ✓\nIS: IV \\(\\cdot \\left(1+\\frac{1}{n+1}\\right)^{n+1}\\)\n\\(= \\frac{(n+2)^{n+2}}{(n+2)!}\\)"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Beweise: \\(z^n - w^n = (z-w) \\sum_{k=0}^{n-1} z^{n-1-k}w^k\\).\nWelche Formel wird genutzt?", listOf("Geometrische Summenformel", "Binomischer Lehrsatz", "Cauchy-Produkt", "Teleskopsumme"), 0, "Setze \\(q = \\frac{w}{z}\\) in der geom. Summenformel:\n\\(\\sum \\left(\\frac{w}{z}\\right)^k = \\frac{1-(w/z)^n}{1-w/z}\\)\nMultipliziere mit \\(z^n\\)."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Bernoulli-Ungleichung:\n\\((1+x)^n \\geq ?\\) für \\(x > -1, n \\in \\mathbb{N}\\)", listOf("\\(1 + nx\\)", "\\(1 + \\frac{x}{n}\\)", "\\(n^x\\)", "\\(e^{nx}\\)"), 0, "\\((1+x)^n \\geq 1+nx\\) für \\(x > -1\\)\nIA: \\(1+x \\geq 1+x\\) ✓\nIS: \\((1+x)^{n+1} = (1+x)^n(1+x)\\)\n\\(\\geq (1+nx)(1+x) = 1+(n+1)x+nx^2 \\geq 1+(n+1)x\\)"),
    // Neue LEICHT Fragen
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "Warum braucht man sowohl IA als auch IS?", listOf("IA startet, IS führt fort", "IA reicht alleine", "IS reicht alleine", "Beide sind optional"), 0, "IA (Induktionsanfang) zeigt, dass die Aussage für \\(n_0\\) gilt.\nIS (Induktionsschritt) zeigt, dass aus \\(A(n)\\) folgt \\(A(n+1)\\).\nBeide zusammen ergeben den vollständigen Beweis."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "\\(1+3+5+...+(2n-1) = ?\\)", listOf("\\(n^2\\)", "\\(2n^2-n\\)", "\\(n(n+1)\\)", "\\(\\frac{n(n+1)}{2}\\)"), 0, "Summe der ersten \\(n\\) ungeraden Zahlen:\n\\(1+3+5+...+(2n-1) = n^2\\)\nBeispiel: \\(1+3+5+7 = 16 = 4^2\\)"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "Bei der Induktion über \\(n \\geq 2\\): Was ist der IA?", listOf("Aussage für \\(n=2\\) zeigen", "Aussage für \\(n=1\\) zeigen", "Aussage für \\(n=0\\) zeigen", "Aussage für alle \\(n\\) zeigen"), 0, "Der IA muss für den kleinsten Wert gezeigt werden.\nBei \\(n \\geq 2\\) ist dies \\(n=2\\)."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "Zeige per Induktion: \\(2^n > n\\) für \\(n \\geq 1\\). IA?", listOf("\\(2^1 = 2 > 1\\) ✓", "\\(2^0 = 1 > 0\\)", "\\(2^n = n\\)", "Nicht zeigbar"), 0, "IA für \\(n=1\\):\n\\(2^1 = 2 > 1\\) ✓\nDie Aussage gilt für den Startwert."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.LEICHT, "Was ist die Aussage \\(A(n)\\) bei \\(\\sum_{k=1}^n k = \\frac{n(n+1)}{2}\\)?", listOf("\\(1+2+...+n = \\frac{n(n+1)}{2}\\)", "\\(n = \\frac{n(n+1)}{2}\\)", "\\(k = \\frac{k(k+1)}{2}\\)", "\\(\\sum k = n\\)"), 0, "Die Aussage \\(A(n)\\) ist:\n\\(1+2+3+...+n = \\frac{n(n+1)}{2}\\)\nDies wird für jedes feste \\(n\\) gezeigt."),
    // Neue MITTEL Fragen
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "Zeige: \\(n^3 - n\\) ist durch 6 teilbar. IS-Schlüssel?", listOf("\\((n+1)^3-(n+1) = (n^3-n) + 3n(n+1)\\)", "\\((n+1)^3 = n^3+1\\)", "\\(n^3-n = 6k\\)", "Direkte Rechnung"), 0, "\\((n+1)^3-(n+1) = n^3+3n^2+3n+1-n-1\\)\n\\(= (n^3-n) + 3n^2+3n = (n^3-n) + 3n(n+1)\\)\nNach IV: \\(n^3-n = 6k\\)\n\\(3n(n+1)\\) ist durch 6 teilbar (aufeinanderfolgende Zahlen)."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "\\(\\sum_{k=1}^{n} \\frac{1}{k(k+1)} = ?\\)", listOf("\\(\\frac{n}{n+1}\\)", "\\(\\frac{1}{n+1}\\)", "\\(\\frac{n+1}{n}\\)", "\\(1 - \\frac{1}{n}\\)"), 0, "Teleskopsumme: \\(\\frac{1}{k(k+1)} = \\frac{1}{k} - \\frac{1}{k+1}\\)\n\\(\\sum = 1 - \\frac{1}{n+1} = \\frac{n}{n+1}\\)\nOder per Induktion verifizierbar."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "Zeige: \\(n! > 2^n\\) für \\(n \\geq 4\\). Warum \\(n \\geq 4\\)?", listOf("Für \\(n<4\\) gilt die Aussage nicht", "Beliebig gewählt", "Für \\(n<4\\) ist \\(n!\\) nicht definiert", "IS funktioniert nur ab \\(n=4\\)"), 0, "\\(1! = 1 < 2 = 2^1\\), \\(2! = 2 < 4 = 2^2\\), \\(3! = 6 < 8 = 2^3\\)\nAber \\(4! = 24 > 16 = 2^4\\) ✓\nDie Aussage gilt erst ab \\(n = 4\\)."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "\\(\\sum_{k=1}^{n} k \\cdot 2^k = ?\\)", listOf("\\((n-1) \\cdot 2^{n+1} + 2\\)", "\\(n \\cdot 2^n\\)", "\\(2^{n+1} - 2\\)", "\\(n \\cdot 2^{n+1}\\)"), 0, "IA: \\(1 \\cdot 2^1 = 2 = 0 \\cdot 4 + 2\\) ✓\nIS: Zeige \\((n-1)2^{n+1}+2+(n+1)2^{n+1} = n \\cdot 2^{n+2}+2\\)\nVereinfacht sich korrekt."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "Zeige: \\(4^n + 6n - 1\\) ist durch 9 teilbar. Was zeigt der IS?", listOf("\\(4 \\cdot 4^n + 6 = 4(4^n+6n-1) - 18n + 10\\)", "\\(4^{n+1} = 4 \\cdot 4^n\\)", "\\(9 | 4^n\\)", "Direkt durch 9 teilen"), 0, "\\(4^{n+1}+6(n+1)-1 = 4 \\cdot 4^n + 6n + 5\\)\n\\(= 4(4^n+6n-1) - 18n + 9\\)\nNach IV: \\(4^n+6n-1 = 9k\\)\nAlso \\(= 36k - 18n + 9 = 9(4k-2n+1)\\) ✓"),
    MathQuestion(MathTopic.INDUKTION, Difficulty.MITTEL, "Fibonacci: \\(F_1=F_2=1\\), \\(F_{n+2}=F_{n+1}+F_n\\). Zeige: \\(F_1+F_2+...+F_n = F_{n+2}-1\\)", listOf("Induktion mit \\(F_{n+1}+F_n = F_{n+2}\\)", "Geschlossene Formel", "Direkte Summation", "Nicht beweisbar"), 0, "IA: \\(F_1 = 1 = F_3 - 1 = 2-1\\) ✓\nIS: \\((F_{n+2}-1) + F_{n+1} = F_{n+3} - 1\\)\nNutzt \\(F_{n+2}+F_{n+1} = F_{n+3}\\) ✓"),
    // Neue SCHWER Fragen
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Zeige: \\(\\sum_{k=1}^{n} k^4 = \\frac{n(n+1)(2n+1)(3n^2+3n-1)}{30}\\). Was ist kritisch?", listOf("Korrekte Algebra im IS", "Startwert finden", "Formel merken", "Umformung zu \\(k^3\\)"), 0, "Der IS erfordert:\n\\(\\frac{n(n+1)(2n+1)(3n^2+3n-1)}{30} + (n+1)^4\\)\n\\(= \\frac{(n+1)(n+2)(2n+3)(3n^2+9n+5)}{30}\\)\nDies ist algebraisch aufwändig zu verifizieren."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Starke Induktion: Was ist der Unterschied zur normalen Induktion?", listOf("IV: Aussage gilt für alle \\(k \\leq n\\)", "IA für mehrere Werte", "IS geht rückwärts", "Kein Unterschied"), 0, "Bei starker Induktion nimmt man an, dass \\(A(k)\\) für ALLE \\(k \\leq n\\) gilt (nicht nur für \\(n\\)).\nNützlich z.B. für Rekursionen wie Fibonacci."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Zeige: Jede natürliche Zahl \\(n \\geq 2\\) hat Primfaktorzerlegung. Welche Induktion?", listOf("Starke Induktion", "Normale Induktion", "Rückwärtsinduktion", "Transfinite Induktion"), 0, "Starke Induktion: Falls \\(n\\) prim, fertig.\nFalls \\(n = a \\cdot b\\) mit \\(1 < a,b < n\\):\nNach IV haben \\(a\\) und \\(b\\) Primfaktorzerlegungen.\nDas Produkt ergibt die Zerlegung von \\(n\\)."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Zeige: \\(\\prod_{k=2}^{n} \\left(1-\\frac{1}{k^2}\\right) = \\frac{n+1}{2n}\\)", listOf("\\(\\frac{k-1}{k} \\cdot \\frac{k+1}{k}\\) und Teleskop", "Direkte Multiplikation", "Logarithmus nehmen", "L'Hôpital anwenden"), 0, "\\(1-\\frac{1}{k^2} = \\frac{k^2-1}{k^2} = \\frac{(k-1)(k+1)}{k^2}\\)\nProdukt: \\(\\frac{1 \\cdot 3}{2^2} \\cdot \\frac{2 \\cdot 4}{3^2} \\cdot ... \\cdot \\frac{(n-1)(n+1)}{n^2}\\)\nTeleskopiert zu \\(\\frac{n+1}{2n}\\)."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Zeige: \\(\\sqrt{2}^{\\sqrt{2}^{\\cdot^{\\cdot^{\\cdot}}}}\\) (n-mal) konvergiert. Induktionsbeweis?", listOf("Zeige beschränkt + monoton", "Nicht per Induktion", "Grenzwert direkt", "Divergiert"), 0, "Definiere \\(a_1 = \\sqrt{2}\\), \\(a_{n+1} = \\sqrt{2}^{a_n}\\).\nPer Induktion: \\(a_n < 2\\) für alle \\(n\\).\n\\(a_{n+1} = \\sqrt{2}^{a_n} < \\sqrt{2}^2 = 2\\) ✓\nMonotonie + Beschränktheit \\(\\Rightarrow\\) Konvergenz."),
    MathQuestion(MathTopic.INDUKTION, Difficulty.SCHWER, "Zeige: \\(\\binom{2n}{n} \\leq 4^n\\) für \\(n \\geq 0\\)", listOf("IS: \\(\\binom{2n+2}{n+1} = \\frac{(2n+2)(2n+1)}{(n+1)^2}\\binom{2n}{n}\\)", "Stirling-Formel", "Direkte Abschätzung", "Kombinatorisches Argument"), 0, "IS: \\(\\binom{2n+2}{n+1} = \\frac{2(2n+1)}{n+1}\\binom{2n}{n}\\)\n\\(\\leq \\frac{2(2n+1)}{n+1} \\cdot 4^n\\)\n\\(\\frac{2(2n+1)}{n+1} \\leq 4 \\Leftrightarrow 4n+2 \\leq 4n+4\\) ✓"),
)

// ════════════════════════════════════════════
//  FOLGEN
// ════════════════════════════════════════════

private val folgenQuestions = listOf(
    // LEICHT
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(\\lim_{n \\to \\infty} \\frac{1}{n} = ?\\)", listOf("\\(0\\)", "\\(1\\)", "\\(\\infty\\)", "\\(-1\\)"), 0, "\\(\\frac{1}{n}\\) wird beliebig klein für große \\(n\\).\n\\(\\lim \\frac{1}{n} = 0\\) (Nullfolge)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(\\lim_{n \\to \\infty} \\frac{3n+1}{n+2} = ?\\)", listOf("\\(3\\)", "\\(1\\)", "\\(\\infty\\)", "\\(0\\)"), 0, "Dividiere durch \\(n\\):\n\\(\\frac{3+1/n}{1+2/n} \\to \\frac{3}{1} = 3\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "Konvergiert \\(a_n = (-1)^n\\)?", listOf("Nein, divergent", "Ja, gegen \\(0\\)", "Ja, gegen \\(1\\)", "Ja, gegen \\(-1\\)"), 0, "Wechselt zwischen \\(1\\) und \\(-1\\).\nZwei Häufungswerte \\(\\Rightarrow\\) divergent."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(\\lim_{n \\to \\infty} \\left(1+\\frac{1}{n}\\right)^n = ?\\)", listOf("\\(e \\approx 2.718\\)", "\\(1\\)", "\\(\\infty\\)", "\\(0\\)"), 0, "Definition der Eulerschen Zahl:\n\\[\\lim_{n \\to \\infty} \\left(1+\\frac{1}{n}\\right)^n = e \\approx 2.71828...\\]"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(a_n = \\frac{(-1)^n}{n}\\) konvergiert gegen:", listOf("\\(0\\)", "\\(1\\)", "divergent", "\\(-1\\)"), 0, "\\(|a_n| = \\frac{1}{n} \\to 0\\)\nAlso \\(a_n \\to 0\\)."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(\\lim_{n \\to \\infty} \\frac{n}{n+1} = ?\\)", listOf("\\(1\\)", "\\(0\\)", "\\(\\infty\\)", "\\(\\frac{1}{2}\\)"), 0, "\\(\\frac{n}{n+1} = \\frac{1}{1+1/n} \\to \\frac{1}{1} = 1\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "Was besagt das Monotoniekriterium?", listOf("Beschränkt + monoton \\(\\Rightarrow\\) konvergent", "Monoton \\(\\Rightarrow\\) konvergent", "Beschränkt \\(\\Rightarrow\\) konvergent", "Konvergent \\(\\Rightarrow\\) monoton"), 0, "Jede beschränkte und monotone Folge in \\(\\mathbb{R}\\) konvergiert."),
    // MITTEL
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(\\lim_{n \\to \\infty} \\frac{n^2+3n-4}{1+n^2+4n^3} = ?\\)", listOf("\\(0\\)", "\\(\\frac{1}{4}\\)", "\\(\\infty\\)", "\\(1\\)"), 0, "Höchste Potenz: \\(n^3\\) im Nenner.\n\\(\\div n^3\\): \\(\\frac{1/n+3/n^2-4/n^3}{1/n^3+1/n+4} \\to \\frac{0}{4} = 0\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(\\lim_{n \\to \\infty} \\sqrt[n]{2^n+3^n} = ?\\)", listOf("\\(3\\)", "\\(2\\)", "\\(\\infty\\)", "\\(5\\)"), 0, "\\(3 = \\sqrt[n]{3^n} \\leq \\sqrt[n]{2^n+3^n} \\leq 3 \\cdot \\sqrt[n]{2} \\to 3\\)\nPer Sandwich: Grenzwert \\(= 3\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "Was ist ein Häufungswert?", listOf("Grenzwert einer Teilfolge", "Grenzwert der Folge", "Maximum der Folge", "Mittelwert"), 0, "\\(a\\) ist Häufungswert von \\((a_n)\\), wenn eine Teilfolge gegen \\(a\\) konvergiert.\nBolzano-Weierstraß: Jede beschränkte Folge hat einen HW."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "Was besagt das Sandwichkriterium?", listOf("\\(b_n \\leq a_n \\leq c_n\\) und \\(b_n,c_n \\to L \\Rightarrow a_n \\to L\\)", "Monotone Folgen konvergieren", "Jede Teilfolge konvergiert", "Beschränkte Folgen konvergieren"), 0, "Sandwich/Einschließung:\nWenn \\(b_n \\leq a_n \\leq c_n\\) für alle \\(n\\)\nund \\(\\lim b_n = \\lim c_n = L\\), dann \\(\\lim a_n = L\\)."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "Welche Folge hat HW \\(-1\\) und \\(5\\)?", listOf("\\(a_n = 2+3 \\cdot (-1)^n\\)", "\\(a_n = (-1)^n \\cdot 5\\)", "\\(a_n = n \\cdot (-1)^n\\)", "\\(a_n = (-1)^n+4\\)"), 0, "\\(a_{2n} = 2+3 = 5\\), \\(a_{2n+1} = 2-3 = -1\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(\\lim_{n \\to \\infty} \\sqrt[n]{n} = ?\\)", listOf("\\(1\\)", "\\(0\\)", "\\(\\infty\\)", "\\(e\\)"), 0, "\\(\\sqrt[n]{n} = n^{1/n} = e^{\\ln(n)/n}\\)\n\\(\\frac{\\ln(n)}{n} \\to 0\\) (l'Hôpital)\nAlso \\(\\sqrt[n]{n} \\to e^0 = 1\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(\\lim_{n \\to \\infty} \\frac{2n^2-3n}{5n^2+n} = ?\\)", listOf("\\(\\frac{2}{5}\\)", "\\(0\\)", "\\(\\infty\\)", "\\(-3\\)"), 0, "\\(\\div n^2\\): \\(\\frac{2-3/n}{5+1/n} \\to \\frac{2}{5}\\)"),
    // SCHWER
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "\\(a_n = n^4\\left(\\sqrt[10]{1+3n^{-4}+n^{-9}}-1\\right)\\)\n\\(\\lim a_n = ?\\)", listOf("\\(\\frac{3}{10}\\)", "\\(0\\)", "\\(\\frac{1}{10}\\)", "\\(\\infty\\)"), 0, "Setze \\(q = \\sqrt[10]{1+3n^{-4}+n^{-9}}\\)\n\\(q^{10}-1 = (q-1)(q^9+...+1)\\)\n\\(a_n = n^4(q-1) = \\frac{3+n^{-5}}{q^9+...+1} \\to \\frac{3}{10}\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "Sei \\(a_{n+1} = 3-\\frac{4}{2+a_n}\\), \\(a_1=3\\).\n\\(\\lim a_n = ?\\)", listOf("\\(2\\)", "\\(3\\)", "\\(1\\)", "\\(4\\)"), 0, "\\(a_n \\geq 2\\) und \\((a_n)\\) monoton fallend.\n\\(a = 3-\\frac{4}{2+a} \\Rightarrow a^2-a-2 = 0\\)\n\\(\\Rightarrow a=2\\) (da \\(a \\geq 2\\))"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "Bolzano-Weierstraß besagt:", listOf("Jede beschränkte Folge hat konv. Teilfolge", "Jede Folge konvergiert", "Jede monotone Folge konv.", "Jede Cauchy-Folge konv."), 0, "Bolzano-Weierstraß:\nJede beschränkte Folge in \\(\\mathbb{R}\\) besitzt eine konvergente Teilfolge (= hat mindestens einen HW)."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "\\((a_{2n})\\), \\((a_{2n+1})\\) und \\((a_{3n})\\) konvergieren.\nFolgt Konvergenz von \\((a_n)\\)?", listOf("Ja", "Nein", "Nur wenn beschränkt", "Nur wenn monoton"), 0, "Ja! Seien die Grenzwerte \\(a,b,c\\).\n\\(a_{6n}\\) ist TF von \\((a_{2n})\\) und \\((a_{3n}) \\Rightarrow a=c\\)\n\\(a_{6n+3}\\) ist TF von \\((a_{2n+1})\\) und \\((a_{3n}) \\Rightarrow b=c\\)\nAlso \\(a=b\\) und \\((a_n)\\) konvergiert."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "\\(\\lim_{n \\to \\infty} \\left(1+\\frac{2}{n}\\right)^n = ?\\)", listOf("\\(e^2\\)", "\\(e\\)", "\\(2e\\)", "\\(\\infty\\)"), 0, "\\(\\left(1+\\frac{2}{n}\\right)^n = \\left[\\left(1+\\frac{2}{n}\\right)^{n/2}\\right]^2\\)\nMit \\(m=n/2\\): \\(\\left[\\left(1+\\frac{1}{m}\\right)^m\\right]^2 \\to e^2\\)"),
    // Neue LEICHT Fragen
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(\\lim_{n \\to \\infty} \\frac{5}{n^2} = ?\\)", listOf("\\(0\\)", "\\(5\\)", "\\(\\infty\\)", "\\(\\frac{5}{2}\\)"), 0, "\\(\\frac{5}{n^2} \\to 0\\) für \\(n \\to \\infty\\)\nJede Folge der Form \\(\\frac{c}{n^k}\\) mit \\(k > 0\\) ist Nullfolge."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "Was bedeutet \\(a_n \\to a\\)?", listOf("\\(\\forall \\varepsilon > 0 \\exists N: n > N \\Rightarrow |a_n - a| < \\varepsilon\\)", "\\(a_n = a\\) für alle \\(n\\)", "\\(a_n < a\\) für alle \\(n\\)", "\\(|a_n| \\to |a|\\)"), 0, "Epsilon-Definition der Konvergenz:\nFür jedes \\(\\varepsilon > 0\\) existiert \\(N\\), sodass\n\\(|a_n - a| < \\varepsilon\\) für alle \\(n > N\\)."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(\\lim_{n \\to \\infty} \\frac{2n - 1}{3n + 2} = ?\\)", listOf("\\(\\frac{2}{3}\\)", "\\(\\frac{1}{3}\\)", "\\(1\\)", "\\(2\\)"), 0, "Dividiere durch \\(n\\):\n\\(\\frac{2-1/n}{3+2/n} \\to \\frac{2}{3}\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "Ist \\(a_n = n\\) konvergent?", listOf("Nein, bestimmt divergent gegen \\(+\\infty\\)", "Ja, gegen \\(\\infty\\)", "Ja, gegen \\(0\\)", "Unbestimmt divergent"), 0, "\\(a_n = n \\to +\\infty\\)\nDie Folge ist bestimmt divergent (unbeschränkt)."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "\\(a_n = \\frac{n+1}{n}\\) konvergiert gegen:", listOf("\\(1\\)", "\\(0\\)", "\\(\\infty\\)", "\\(2\\)"), 0, "\\(\\frac{n+1}{n} = 1 + \\frac{1}{n} \\to 1 + 0 = 1\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.LEICHT, "Ist jede konvergente Folge beschränkt?", listOf("Ja", "Nein", "Nur monotone Folgen", "Nur wenn \\(a_n > 0\\)"), 0, "Jede konvergente Folge ist beschränkt.\nABER: Nicht jede beschränkte Folge konvergiert!\n(Bsp: \\((-1)^n\\) ist beschränkt, aber divergent)"),
    // Neue MITTEL Fragen
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(\\lim_{n \\to \\infty} \\frac{n^3 + 2n}{n^3 - n^2 + 1} = ?\\)", listOf("\\(1\\)", "\\(0\\)", "\\(\\infty\\)", "\\(2\\)"), 0, "Höchste Potenz \\(n^3\\) kürzen:\n\\(\\frac{1 + 2/n^2}{1 - 1/n + 1/n^3} \\to \\frac{1}{1} = 1\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(\\lim_{n \\to \\infty} \\left(\\sqrt{n+1} - \\sqrt{n}\\right) = ?\\)", listOf("\\(0\\)", "\\(1\\)", "\\(\\frac{1}{2}\\)", "\\(\\infty\\)"), 0, "Erweitern: \\(\\frac{(\\sqrt{n+1}-\\sqrt{n})(\\sqrt{n+1}+\\sqrt{n})}{\\sqrt{n+1}+\\sqrt{n}}\\)\n\\(= \\frac{1}{\\sqrt{n+1}+\\sqrt{n}} \\to 0\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "Was ist eine Cauchy-Folge?", listOf("\\(\\forall \\varepsilon > 0 \\exists N: n,m > N \\Rightarrow |a_n - a_m| < \\varepsilon\\)", "\\(|a_{n+1} - a_n| \\to 0\\)", "\\(a_n \\to a\\)", "\\(a_n\\) ist monoton"), 0, "Cauchy-Kriterium: Die Glieder kommen sich beliebig nahe.\nIn \\(\\mathbb{R}\\): Cauchy-Folge \\(\\Leftrightarrow\\) konvergent."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(a_1 = 1\\), \\(a_{n+1} = \\sqrt{2 + a_n}\\). Grenzwert?", listOf("\\(2\\)", "\\(\\sqrt{2}\\)", "\\(1\\)", "\\(\\sqrt{3}\\)"), 0, "Sei \\(a = \\lim a_n\\). Dann \\(a = \\sqrt{2+a}\\).\n\\(a^2 = 2+a \\Leftrightarrow a^2 - a - 2 = 0\\)\n\\((a-2)(a+1) = 0 \\Rightarrow a = 2\\) (da \\(a > 0\\))"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(\\lim_{n \\to \\infty} \\frac{\\ln(n)}{n} = ?\\)", listOf("\\(0\\)", "\\(1\\)", "\\(\\infty\\)", "\\(e\\)"), 0, "\\(\\ln(n)\\) wächst langsamer als \\(n\\).\nL'Hôpital: \\(\\lim \\frac{\\ln x}{x} = \\lim \\frac{1/x}{1} = 0\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "\\(a_n = \\frac{n!}{n^n}\\). Konvergiert die Folge?", listOf("Ja, gegen \\(0\\)", "Nein, divergent", "Ja, gegen \\(1\\)", "Ja, gegen \\(e^{-1}\\)"), 0, "\\(\\frac{a_{n+1}}{a_n} = \\frac{(n+1)! \\cdot n^n}{n! \\cdot (n+1)^{n+1}} = \\left(\\frac{n}{n+1}\\right)^n \\to e^{-1}\\)\nDa \\(e^{-1} < 1\\), ist \\(a_n \\to 0\\)."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.MITTEL, "Limsup und Liminf: Wann gilt \\(\\limsup = \\liminf\\)?", listOf("Genau dann wenn die Folge konvergiert", "Immer", "Nie", "Nur für monotone Folgen"), 0, "\\(\\limsup a_n = \\liminf a_n \\Leftrightarrow (a_n)\\) konvergiert.\nDer gemeinsame Wert ist dann der Grenzwert."),
    // Neue SCHWER Fragen
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "\\(\\lim_{n \\to \\infty} n \\cdot \\sin\\left(\\frac{1}{n}\\right) = ?\\)", listOf("\\(1\\)", "\\(0\\)", "\\(\\infty\\)", "existiert nicht"), 0, "Substitution \\(x = 1/n \\to 0\\):\n\\(\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1\\)\n(Fundamentaler Grenzwert)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "\\(a_n = \\left(1 - \\frac{1}{n^2}\\right)^n\\). Grenzwert?", listOf("\\(1\\)", "\\(e^{-1}\\)", "\\(0\\)", "\\(e\\)"), 0, "\\(\\left(1-\\frac{1}{n^2}\\right)^n = \\left[\\left(1-\\frac{1}{n^2}\\right)^{n^2}\\right]^{1/n}\\)\n\\(\\to e^{-1 \\cdot 0} = e^0 = 1\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "Stolz-Cesàro: Wenn \\((b_n)\\) streng monoton und unbeschränkt, dann:", listOf("\\(\\lim \\frac{a_n}{b_n} = \\lim \\frac{a_{n+1}-a_n}{b_{n+1}-b_n}\\)", "\\(\\lim a_n = \\lim b_n\\)", "\\(\\frac{a_n}{b_n} \\to 1\\)", "\\(a_n \\cdot b_n \\to 0\\)"), 0, "Satz von Stolz-Cesàro:\nWenn \\(\\lim \\frac{a_{n+1}-a_n}{b_{n+1}-b_n} = L\\) existiert,\ndann gilt auch \\(\\lim \\frac{a_n}{b_n} = L\\)."),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "\\(\\lim_{n \\to \\infty} \\frac{1^k + 2^k + ... + n^k}{n^{k+1}} = ?\\)", listOf("\\(\\frac{1}{k+1}\\)", "\\(1\\)", "\\(\\frac{1}{k}\\)", "\\(0\\)"), 0, "Riemann-Summe für \\(\\int_0^1 x^k dx = \\frac{1}{k+1}\\)\nOder Stolz-Cesàro:\n\\(\\lim \\frac{(n+1)^k}{(n+1)^{k+1}-n^{k+1}} \\to \\frac{1}{k+1}\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "Zeige: \\(a_n = \\sum_{k=1}^n \\frac{1}{k} - \\ln(n)\\) konvergiert. Name?", listOf("Euler-Mascheroni-Konstante \\(\\gamma\\)", "Goldener Schnitt", "Apéry-Konstante", "Catalan-Konstante"), 0, "Die Differenz \\(\\sum_{k=1}^n \\frac{1}{k} - \\ln(n)\\) konvergiert\ngegen die Euler-Mascheroni-Konstante\n\\(\\gamma \\approx 0.5772...\\)"),
    MathQuestion(MathTopic.FOLGEN, Difficulty.SCHWER, "\\(a_1 = 2\\), \\(a_{n+1} = \\frac{1}{2}\\left(a_n + \\frac{2}{a_n}\\right)\\). Was konvergiert \\(a_n\\)?", listOf("\\(\\sqrt{2}\\)", "\\(2\\)", "\\(1\\)", "\\(\\frac{3}{2}\\)"), 0, "Heron-Verfahren für \\(\\sqrt{2}\\).\nGrenzwert: \\(a = \\frac{1}{2}(a + \\frac{2}{a})\\)\n\\(2a = a + \\frac{2}{a} \\Rightarrow a^2 = 2\\)\n\\(a = \\sqrt{2}\\)"),
)

// ════════════════════════════════════════════
//  REIHEN
// ════════════════════════════════════════════

private val reihenQuestions = listOf(
    // LEICHT
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "\\(\\sum_{k=0}^{\\infty} \\left(\\frac{1}{2}\\right)^k = ?\\)", listOf("\\(2\\)", "\\(1\\)", "\\(\\infty\\)", "\\(\\frac{1}{2}\\)"), 0, "Geom. Reihe: \\(q=\\frac{1}{2}\\)\n\\(\\sum q^k = \\frac{1}{1-q} = \\frac{1}{1/2} = 2\\)"),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "Geom. Reihe \\(\\sum q^k\\) konvergiert für:", listOf("\\(|q| < 1\\)", "\\(|q| \\leq 1\\)", "\\(q < 1\\)", "alle \\(q\\)"), 0, "\\(\\sum_{k=0}^{\\infty} q^k = \\frac{1}{1-q}\\) für \\(|q| < 1\\).\nFür \\(|q| \\geq 1\\) divergiert die Reihe."),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "Notwendiges Kriterium für \\(\\sum a_n\\) konv.:", listOf("\\(a_n \\to 0\\)", "\\(a_n\\) beschränkt", "\\(|a_n| < 1\\)", "\\(a_n\\) monoton"), 0, "Wenn \\(\\sum a_n\\) konvergiert, dann \\(a_n \\to 0\\).\nABER: Umkehrung gilt NICHT!\n(\\(\\sum \\frac{1}{n}\\) divergiert obwohl \\(\\frac{1}{n} \\to 0\\))"),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "\\(\\sum_{k=0}^{\\infty} \\left(\\frac{1}{3}\\right)^k = ?\\)", listOf("\\(\\frac{3}{2}\\)", "\\(\\frac{1}{3}\\)", "\\(3\\)", "\\(\\frac{2}{3}\\)"), 0, "Geom. Reihe: \\(\\frac{1}{1-1/3} = \\frac{1}{2/3} = \\frac{3}{2}\\)"),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "Was ist eine Partialsumme?", listOf("\\(s_n = \\sum_{k=0}^{n} a_k\\)", "\\(s_n = \\frac{a_n}{n}\\)", "\\(s_n = a_n - a_{n-1}\\)", "\\(s_n = \\max(a_1,...,a_n)\\)"), 0, "Die \\(n\\)-te Partialsumme ist \\(s_n = a_0+a_1+...+a_n\\).\nDie Reihe \\(\\sum a_k\\) konvergiert, wenn \\((s_n)\\) konvergiert."),
    // MITTEL
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Konvergiert \\(\\sum \\frac{1}{n}\\)?", listOf("Nein, divergent", "Ja, gegen \\(1\\)", "Ja, gegen \\(\\ln(2)\\)", "Ja, gegen \\(\\infty\\)"), 0, "Die harmonische Reihe divergiert!\nAber: \\(\\sum \\frac{1}{n^2} = \\frac{\\pi^2}{6}\\) konvergiert."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Quotientenkriterium: \\(\\lim\\left|\\frac{a_{n+1}}{a_n}\\right| = q\\).\nWas gilt?", listOf("\\(q<1 \\Rightarrow\\) konv., \\(q>1 \\Rightarrow\\) div.", "\\(q=1 \\Rightarrow\\) konv.", "\\(q<1 \\Rightarrow\\) div.", "\\(q \\leq 1 \\Rightarrow\\) konv."), 0, "\\(q < 1\\): absolut konvergent\n\\(q > 1\\): divergent\n\\(q = 1\\): keine Aussage möglich"),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Was besagt das Leibniz-Kriterium?", listOf("\\(\\sum(-1)^n a_n\\) konv. wenn \\(a_n \\searrow 0\\)", "Jede altern. Reihe konv.", "\\(\\sum a_n\\) konv. wenn \\(a_n \\to 0\\)", "Altern. Reihen divergieren"), 0, "Wenn \\((a_n)\\) monoton fallend und \\(a_n \\to 0\\),\ndann konvergiert \\(\\sum(-1)^n a_n\\)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Exponentialreihe \\(\\exp(x) = ?\\)", listOf("\\(\\sum \\frac{x^k}{k!}\\)", "\\(\\sum x^k\\)", "\\(\\sum \\frac{x^k}{k}\\)", "\\(\\sum k \\cdot x^k\\)"), 0, "\\[\\exp(x) = \\sum_{k=0}^{\\infty} \\frac{x^k}{k!} = 1+x+\\frac{x^2}{2!}+\\frac{x^3}{3!}+...\\]\nKonvergiert für alle \\(x \\in \\mathbb{R}\\)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Konvergiert \\(\\sum \\frac{1}{n^2}\\)?", listOf("Ja, gegen \\(\\frac{\\pi^2}{6}\\)", "Nein", "Ja, gegen \\(1\\)", "Ja, gegen \\(2\\)"), 0, "\\(\\sum \\frac{1}{n^2} = \\frac{\\pi^2}{6}\\) (Basel-Problem, Euler 1734).\nKonvergenz z.B. per Vergleich mit \\(\\sum \\frac{1}{n(n-1)}\\)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Wurzelkriterium: \\(\\lim \\sqrt[n]{|a_n|} = q\\).\nWas gilt?", listOf("\\(q<1 \\Rightarrow\\) konv., \\(q>1 \\Rightarrow\\) div.", "\\(q=1 \\Rightarrow\\) konv.", "\\(q<1 \\Rightarrow\\) div.", "Nur für positive Reihen"), 0, "Wurzelkriterium (Cauchy):\n\\(q < 1\\): absolut konvergent\n\\(q > 1\\): divergent\n\\(q = 1\\): keine Aussage"),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Konvergiert \\(\\sum \\frac{(-1)^n}{n}\\)?", listOf("Ja (Leibniz), gegen \\(\\ln(2)\\)", "Nein", "Ja, gegen \\(1\\)", "Ja, gegen \\(0\\)"), 0, "\\(\\frac{1}{n}\\) ist monoton fallend und \\(\\to 0\\).\nNach Leibniz konvergiert \\(\\sum \\frac{(-1)^n}{n}\\).\nDer Wert ist \\(\\ln(2)\\)."),
    // SCHWER
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "Was bedeutet absolute Konvergenz?", listOf("\\(\\sum|a_n|\\) konvergiert", "\\(\\sum a_n\\) konvergiert", "\\(|\\sum a_n| < \\infty\\)", "\\(a_n > 0\\) für alle \\(n\\)"), 0, "\\(\\sum a_n\\) heißt absolut konvergent, wenn \\(\\sum|a_n|\\) konvergiert.\nAbsolute Konvergenz \\(\\Rightarrow\\) Konvergenz.\nUmkehrung gilt NICHT (Bsp: \\(\\sum \\frac{(-1)^n}{n}\\))."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "Cauchy-Produkt: Wenn \\(\\sum a_n\\) und \\(\\sum b_n\\) absolut konvergieren,\ndann \\(\\sum c_n = ?\\)", listOf("\\(c_n = \\sum_{k=0}^{n} a_k b_{n-k}\\)", "\\(c_n = a_n \\cdot b_n\\)", "\\(c_n = a_n + b_n\\)", "\\(c_n = \\sum a_k \\cdot \\sum b_k\\)"), 0, "Cauchy-Produkt:\n\\(c_n = \\sum_{k=0}^{n} a_k \\cdot b_{n-k}\\) (Faltung)\nDann konvergiert \\(\\sum c_n\\) und\n\\(\\sum c_n = (\\sum a_n) \\cdot (\\sum b_n)\\)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "Konvergiert \\(\\sum \\frac{n!}{n^n}\\)?", listOf("Ja (Quotientenkriterium)", "Nein", "Nicht entscheidbar", "Ja (Leibniz)"), 0, "\\(\\left|\\frac{a_{n+1}}{a_n}\\right| = \\frac{(n+1)! \\cdot n^n}{n! \\cdot (n+1)^{n+1}}\\)\n\\(= \\frac{n^n}{(n+1)^n} = \\left(\\frac{n}{n+1}\\right)^n \\to \\frac{1}{e} < 1\\)\nAlso konvergent."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "Majorantenkriterium: \\(\\sum a_n\\) konvergiert wenn:", listOf("\\(|a_n| \\leq b_n\\) und \\(\\sum b_n\\) konvergiert", "\\(a_n \\leq b_n\\) immer", "\\(\\sum a_n \\leq \\sum b_n\\)", "\\(a_n \\to 0\\)"), 0, "Wenn \\(|a_n| \\leq b_n\\) für alle \\(n\\) und \\(\\sum b_n\\) konvergiert,\ndann konvergiert \\(\\sum a_n\\) absolut.\n(Vergleichskriterium)"),
    // Neue LEICHT Fragen
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "\\(\\sum_{k=0}^{\\infty} \\left(\\frac{2}{3}\\right)^k = ?\\)", listOf("\\(3\\)", "\\(\\frac{2}{3}\\)", "\\(\\frac{3}{2}\\)", "\\(2\\)"), 0, "Geom. Reihe: \\(\\frac{1}{1-2/3} = \\frac{1}{1/3} = 3\\)"),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "Konvergiert \\(\\sum_{n=1}^{\\infty} 1\\)?", listOf("Nein, divergiert", "Ja, gegen \\(\\infty\\)", "Ja, gegen \\(1\\)", "Ja, gegen \\(0\\)"), 0, "\\(\\sum 1 = 1 + 1 + 1 + ... = \\infty\\)\nDie Reihe divergiert (Partialsummen unbeschränkt)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "\\(\\sum_{k=1}^{\\infty} \\left(\\frac{1}{2}\\right)^k = ?\\)", listOf("\\(1\\)", "\\(2\\)", "\\(\\frac{1}{2}\\)", "\\(\\infty\\)"), 0, "Startet bei \\(k=1\\): \\(\\frac{1/2}{1-1/2} = 1\\)\nOder: \\(\\sum_{k=0}^{\\infty} q^k - 1 = 2 - 1 = 1\\)"),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "Was ist \\(\\sum_{k=0}^{n} q^k\\) für \\(q \\neq 1\\)?", listOf("\\(\\frac{1-q^{n+1}}{1-q}\\)", "\\(\\frac{1-q^n}{1-q}\\)", "\\(n \\cdot q\\)", "\\(q^n\\)"), 0, "Endliche geometrische Summe:\n\\(\\sum_{k=0}^{n} q^k = \\frac{1-q^{n+1}}{1-q}\\)"),
    MathQuestion(MathTopic.REIHEN, Difficulty.LEICHT, "Wann konvergiert \\(\\sum_{n=1}^{\\infty} \\frac{1}{n^p}\\)?", listOf("Für \\(p > 1\\)", "Für \\(p \\geq 1\\)", "Für \\(p > 0\\)", "Für alle \\(p\\)"), 0, "\\(p\\)-Reihe: \\(\\sum \\frac{1}{n^p}\\) konvergiert \\(\\Leftrightarrow p > 1\\).\nFür \\(p = 1\\): harmonische Reihe, divergiert."),
    // Neue MITTEL Fragen
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "\\(\\sum_{n=1}^{\\infty} \\frac{1}{n(n+2)} = ?\\)", listOf("\\(\\frac{3}{4}\\)", "\\(\\frac{1}{2}\\)", "\\(1\\)", "\\(\\frac{2}{3}\\)"), 0, "Partialbruchzerlegung: \\(\\frac{1}{n(n+2)} = \\frac{1}{2}\\left(\\frac{1}{n}-\\frac{1}{n+2}\\right)\\)\nTeleskopsumme: \\(\\frac{1}{2}(1+\\frac{1}{2}) = \\frac{3}{4}\\)"),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Konvergiert \\(\\sum \\frac{n}{2^n}\\)?", listOf("Ja (Quotientenkriterium)", "Nein", "Bedingt konvergent", "Nicht entscheidbar"), 0, "\\(\\frac{a_{n+1}}{a_n} = \\frac{(n+1)/2^{n+1}}{n/2^n} = \\frac{n+1}{2n} \\to \\frac{1}{2} < 1\\)\nAlso konvergent."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Konvergenzradius von \\(\\sum_{n=0}^{\\infty} x^n\\)?", listOf("\\(R = 1\\)", "\\(R = \\infty\\)", "\\(R = 0\\)", "\\(R = e\\)"), 0, "Geometrische Reihe konvergiert für \\(|x| < 1\\).\nAlso \\(R = 1\\)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "\\(\\sum_{n=0}^{\\infty} \\frac{x^n}{n!}\\) konvergiert für:", listOf("alle \\(x \\in \\mathbb{R}\\)", "\\(|x| < 1\\)", "\\(|x| < e\\)", "\\(x > 0\\)"), 0, "Exponentialreihe: \\(e^x = \\sum \\frac{x^n}{n!}\\)\nKonvergenzradius \\(R = \\infty\\)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "\\(\\sum_{n=1}^{\\infty} \\frac{(-1)^{n+1}}{n^2}\\) konvergiert:", listOf("absolut", "bedingt", "divergiert", "nur für gerade \\(n\\)"), 0, "\\(\\sum \\frac{1}{n^2}\\) konvergiert (\\(p\\)-Reihe mit \\(p=2>1\\)).\nAlso konvergiert die Reihe absolut."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Was ist eine Teleskopsumme?", listOf("\\(\\sum (a_n - a_{n+1})\\) kürzt sich", "Summe geht nach oben", "Unendliche Summe", "Divergente Reihe"), 0, "Teleskopsumme: \\(\\sum_{k=1}^n (a_k - a_{k+1}) = a_1 - a_{n+1}\\)\nViele Terme kürzen sich weg."),
    MathQuestion(MathTopic.REIHEN, Difficulty.MITTEL, "Konvergenzradius von \\(\\sum_{n=1}^{\\infty} \\frac{x^n}{n}\\)?", listOf("\\(R = 1\\)", "\\(R = \\infty\\)", "\\(R = 0\\)", "\\(R = e\\)"), 0, "Quotientenkriterium: \\(\\frac{n}{n+1} \\to 1\\)\nAlso \\(R = 1\\). (Logarithmusreihe)"),
    // Neue SCHWER Fragen
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "Konvergiert \\(\\sum \\frac{n^n}{n!}\\)?", listOf("Nein, divergiert", "Ja, konvergiert", "Bedingt konvergent", "Nicht entscheidbar"), 0, "Quotientenkriterium:\n\\(\\frac{a_{n+1}}{a_n} = \\frac{(n+1)^{n+1}}{(n+1)!} \\cdot \\frac{n!}{n^n}\\)\n\\(= \\left(\\frac{n+1}{n}\\right)^n = \\left(1+\\frac{1}{n}\\right)^n \\to e > 1\\)\nDivergent."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "\\(\\sum_{n=2}^{\\infty} \\frac{1}{n \\ln(n)}\\) konvergiert?", listOf("Nein (Integralkriterium)", "Ja", "Bedingt", "Nicht entscheidbar"), 0, "Integralkriterium: \\(\\int_2^{\\infty} \\frac{dx}{x \\ln x} = [\\ln(\\ln x)]_2^{\\infty} = \\infty\\)\nDivergent."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "Konvergenzradius von \\(\\sum n! \\cdot x^n\\)?", listOf("\\(R = 0\\)", "\\(R = 1\\)", "\\(R = \\infty\\)", "\\(R = e\\)"), 0, "\\(\\left|\\frac{a_{n+1}}{a_n}\\right| = (n+1)|x| \\to \\infty\\) für \\(x \\neq 0\\).\nAlso \\(R = 0\\) (konvergiert nur für \\(x = 0\\))."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "Riemannscher Umordnungssatz: Was gilt für bedingt konvergente Reihen?", listOf("Durch Umordnung jeden Wert erreichbar", "Wert bleibt gleich", "Wird divergent", "Wird absolut konvergent"), 0, "Bedingt konvergente Reihen können durch Umordnung\njeden beliebigen Wert \\(s \\in \\mathbb{R} \\cup \\{\\pm\\infty\\}\\)\nannehmen (Riemann 1854)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "\\(\\sum_{n=1}^{\\infty} \\frac{\\sin(n)}{n}\\) konvergiert?", listOf("Ja (Dirichlet-Kriterium)", "Nein", "Absolut konvergent", "Nicht entscheidbar"), 0, "Dirichlet: \\((1/n)\\) monoton gegen 0,\n\\(\\sum \\sin(n)\\) ist beschränkt.\nAlso konvergiert die Reihe (bedingt)."),
    MathQuestion(MathTopic.REIHEN, Difficulty.SCHWER, "\\(\\sum_{n=0}^{\\infty} \\frac{(-1)^n}{2n+1} = ?\\)", listOf("\\(\\frac{\\pi}{4}\\)", "\\(\\pi\\)", "\\(\\frac{\\pi}{2}\\)", "\\(1\\)"), 0, "Leibniz-Reihe für Arcustangens:\n\\(\\arctan(1) = \\frac{\\pi}{4}\\)\n\\(= 1 - \\frac{1}{3} + \\frac{1}{5} - \\frac{1}{7} + ...\\)"),
)

// ════════════════════════════════════════════
//  ABLEITUNGEN
// ════════════════════════════════════════════

private val ableitungQuestions = listOf(
    // LEICHT
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((x^n)' = ?\\)", listOf("\\(n \\cdot x^{n-1}\\)", "\\(x^{n-1}\\)", "\\(n \\cdot x^n\\)", "\\((n-1) \\cdot x^n\\)"), 0, "Potenzregel: \\((x^n)' = n \\cdot x^{n-1}\\)\nBeispiel: \\((x^3)' = 3x^2\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((e^x)' = ?\\)", listOf("\\(e^x\\)", "\\(x \\cdot e^{x-1}\\)", "\\(\\ln(x) \\cdot e^x\\)", "\\(\\frac{1}{e^x}\\)"), 0, "\\(e^x\\) ist seine eigene Ableitung: \\((e^x)' = e^x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((\\sin x)' = ?\\)", listOf("\\(\\cos x\\)", "\\(-\\cos x\\)", "\\(\\sin x\\)", "\\(-\\sin x\\)"), 0, "\\((\\sin x)' = \\cos x\\)\n\\((\\cos x)' = -\\sin x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((\\ln x)' = ?\\) für \\(x > 0\\)", listOf("\\(\\frac{1}{x}\\)", "\\(\\frac{\\ln(x)}{x}\\)", "\\(x \\cdot \\ln(x)\\)", "\\(e^x\\)"), 0, "\\((\\ln x)' = \\frac{1}{x}\\) für \\(x > 0\\)."),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((\\cos x)' = ?\\)", listOf("\\(-\\sin x\\)", "\\(\\sin x\\)", "\\(\\cos x\\)", "\\(-\\cos x\\)"), 0, "\\((\\cos x)' = -\\sin x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "Produktregel: \\((f \\cdot g)' = ?\\)", listOf("\\(f'g + fg'\\)", "\\(f' \\cdot g'\\)", "\\((f+g)'\\)", "\\(f'g - fg'\\)"), 0, "\\((f \\cdot g)' = f' \\cdot g + f \\cdot g'\\)\nBeispiel: \\((x \\cdot e^x)' = e^x+x \\cdot e^x = (1+x)e^x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "Kettenregel: \\((f(g(x)))' = ?\\)", listOf("\\(f'(g(x)) \\cdot g'(x)\\)", "\\(f'(x) \\cdot g'(x)\\)", "\\(f(g'(x))\\)", "\\(f'(g'(x))\\)"), 0, "Äußere Ableitung \\(\\cdot\\) innere Ableitung\nBsp: \\((\\sin(x^2))' = \\cos(x^2) \\cdot 2x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((5x^3 + 2x - 7)' = ?\\)", listOf("\\(15x^2 + 2\\)", "\\(15x^2 + 2x\\)", "\\(5x^2 + 2\\)", "\\(15x^3 + 2\\)"), 0, "\\((5x^3)' = 15x^2\\), \\((2x)' = 2\\), \\((-7)' = 0\\)\nSumme: \\(15x^2 + 2\\)"),
    // MITTEL
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\(f(x) = 3^x\\). \\(f'(x) = ?\\)", listOf("\\(\\ln(3) \\cdot 3^x\\)", "\\(x \\cdot 3^{x-1}\\)", "\\(3^x\\)", "\\(3 \\cdot \\ln(x)\\)"), 0, "\\(3^x = e^{\\ln 3 \\cdot x}\\)\nKettenregel: \\(\\ln(3) \\cdot e^{\\ln 3 \\cdot x} = \\ln(3) \\cdot 3^x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\(f(x) = \\cos(x \\cdot e^x)\\). \\(f'(x) = ?\\)", listOf("\\(-(1+x)e^x \\cdot \\sin(xe^x)\\)", "\\(-\\sin(xe^x)\\)", "\\(-e^x \\cdot \\sin(xe^x)\\)", "\\(-x \\cdot \\sin(e^x)\\)"), 0, "\\(h(x) = xe^x\\), \\(h'(x) = (1+x)e^x\\)\n\\(f'(x) = -h'(x) \\cdot \\sin(h(x))\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "Quotientenregel: \\(\\left(\\frac{f}{g}\\right)' = ?\\)", listOf("\\(\\frac{f'g - fg'}{g^2}\\)", "\\(\\frac{f'g + fg'}{g^2}\\)", "\\(\\frac{f'}{g'}\\)", "\\(\\frac{fg' - f'g}{g^2}\\)"), 0, "\\(\\left(\\frac{f}{g}\\right)' = \\frac{f' \\cdot g - f \\cdot g'}{g^2}\\)\nMerke: \"NAZ - ZAN durch N²\""),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((\\tan x)' = ?\\)", listOf("\\(\\frac{1}{\\cos^2(x)}\\)", "\\(\\frac{1}{\\sin^2(x)}\\)", "\\(-\\frac{1}{\\cos^2(x)}\\)", "\\(\\sec(x)\\)"), 0, "\\(\\tan(x) = \\frac{\\sin(x)}{\\cos(x)}\\)\nQuotientenregel:\n\\(\\frac{\\cos^2 x+\\sin^2 x}{\\cos^2 x} = \\frac{1}{\\cos^2 x}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((\\arcsin x)' = ?\\) für \\(|x| < 1\\)", listOf("\\(\\frac{1}{\\sqrt{1-x^2}}\\)", "\\(-\\frac{1}{\\sqrt{1-x^2}}\\)", "\\(\\frac{1}{1+x^2}\\)", "\\(\\frac{1}{\\sqrt{x^2-1}}\\)"), 0, "Umkehrregel: Wenn \\(f(x)=\\sin(x)\\), \\(f'(x)=\\cos(x)\\)\n\\((f^{-1})'(y) = \\frac{1}{\\cos(\\arcsin(y))} = \\frac{1}{\\sqrt{1-y^2}}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((x \\cdot \\ln(x))' = ?\\)", listOf("\\(\\ln(x) + 1\\)", "\\(\\frac{1}{x}\\)", "\\(\\frac{x}{\\ln(x)}\\)", "\\(\\ln(x)\\)"), 0, "Produktregel: \\(1 \\cdot \\ln(x) + x \\cdot \\frac{1}{x} = \\ln(x)+1\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((e^{x^2})' = ?\\)", listOf("\\(2x \\cdot e^{x^2}\\)", "\\(e^{x^2}\\)", "\\(x^2 \\cdot e^{x^2}\\)", "\\(2 \\cdot e^{2x}\\)"), 0, "Kettenregel: \\(e^{x^2} \\cdot (x^2)' = 2x \\cdot e^{x^2}\\)"),
    // SCHWER
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "\\(f(x) = \\frac{x}{\\cos(x^2)}\\). \\(f'(x) = ?\\)", listOf("\\(\\frac{\\cos(x^2)+2x^2\\sin(x^2)}{\\cos^2(x^2)}\\)", "\\(\\frac{1}{\\cos(x^2)}\\)", "\\(-\\frac{\\sin(x^2)}{\\cos^2(x^2)}\\)", "\\(2x \\cdot \\sin(x^2)\\)"), 0, "Quotientenregel:\n\\(g=x\\), \\(h=\\cos(x^2)\\), \\(g'=1\\), \\(h'=-2x\\sin(x^2)\\)\n\\(\\frac{1 \\cdot \\cos(x^2)-x \\cdot (-2x\\sin(x^2))}{\\cos^2(x^2)}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "Ist \\(f(x)=|\\sin x|\\) differenzierbar bei \\(x=\\pi\\)?", listOf("Nein (linksseit. \\(-1\\), rechtss. \\(+1\\))", "Ja, \\(f'(\\pi)=0\\)", "Ja, \\(f'(\\pi)=-1\\)", "Ja, \\(f'(\\pi)=1\\)"), 0, "Links: \\(f(x)=\\sin(x) \\to f'(\\pi^-)=\\cos(\\pi)=-1\\)\nRechts: \\(f(x)=-\\sin(x) \\to f'(\\pi^+)=1\\)\nLinks \\(\\neq\\) Rechts \\(\\Rightarrow\\) nicht diff'bar"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "\\(f_n(x) = x^n \\cdot \\sin\\left(\\frac{1}{x}\\right)\\) für \\(x \\neq 0\\), \\(f(0)=0\\).\nFür welche \\(n\\) ist \\(f_n\\) diff'bar in \\(0\\)?", listOf("\\(n \\geq 2\\)", "\\(n \\geq 1\\)", "\\(n \\geq 0\\)", "Alle \\(n\\)"), 0, "\\(f'(0) = \\lim x^{n-1} \\cdot \\sin(1/x)\\)\nFür \\(n \\geq 2\\): \\(|x^{n-1}\\sin(1/x)| \\leq |x|^{n-1} \\to 0\\) ✓\nFür \\(n=1\\): \\(\\sin(1/x)\\) hat keinen Grenzwert ✗"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "\\(f(x) = x^x\\) für \\(x > 0\\). \\(f'(x) = ?\\)", listOf("\\(x^x(\\ln(x)+1)\\)", "\\(x \\cdot x^{x-1}\\)", "\\(x^x \\cdot \\ln(x)\\)", "\\(x^{x-1}\\)"), 0, "\\(x^x = e^{x \\cdot \\ln(x)}\\)\nKettenregel: \\(e^{x \\cdot \\ln(x)} \\cdot (\\ln(x)+1)\\)\n\\(= x^x(\\ln(x)+1)\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "L'Hôpital: Wann anwendbar?", listOf("Bei \\(\\frac{0}{0}\\) oder \\(\\frac{\\infty}{\\infty}\\)", "Immer", "Nur bei \\(\\frac{0}{0}\\)", "Nur bei Polynomen"), 0, "L'Hôpital: Wenn \\(\\lim \\frac{f}{g}\\) die Form \\(\\frac{0}{0}\\) oder \\(\\frac{\\infty}{\\infty}\\) hat,\ndann \\(\\lim \\frac{f(x)}{g(x)} = \\lim \\frac{f'(x)}{g'(x)}\\)\n(falls der rechte Grenzwert existiert)."),
    // Neue LEICHT Fragen
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((x^{-1})' = ?\\)", listOf("\\(-x^{-2}\\)", "\\(x^{-2}\\)", "\\(-1\\)", "\\(\\frac{1}{x}\\)"), 0, "Potenzregel: \\((x^{-1})' = -1 \\cdot x^{-1-1} = -x^{-2} = -\\frac{1}{x^2}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((\\sqrt{x})' = ?\\)", listOf("\\(\\frac{1}{2\\sqrt{x}}\\)", "\\(\\frac{1}{\\sqrt{x}}\\)", "\\(2\\sqrt{x}\\)", "\\(\\sqrt{x}\\)"), 0, "\\(\\sqrt{x} = x^{1/2}\\), also \\((x^{1/2})' = \\frac{1}{2}x^{-1/2} = \\frac{1}{2\\sqrt{x}}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((7)' = ?\\)", listOf("\\(0\\)", "\\(7\\)", "\\(1\\)", "\\(7x\\)"), 0, "Die Ableitung einer Konstanten ist immer \\(0\\)."),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((2x^4 - 3x^2 + x)' = ?\\)", listOf("\\(8x^3 - 6x + 1\\)", "\\(8x^3 - 6x\\)", "\\(2x^3 - 3x + 1\\)", "\\(8x^4 - 6x^2 + 1\\)"), 0, "\\((2x^4)' = 8x^3\\), \\((-3x^2)' = -6x\\), \\((x)' = 1\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((e^{2x})' = ?\\)", listOf("\\(2e^{2x}\\)", "\\(e^{2x}\\)", "\\(2xe^{2x}\\)", "\\(e^{2x-1}\\)"), 0, "Kettenregel: \\(e^{2x} \\cdot (2x)' = 2e^{2x}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.LEICHT, "\\((\\sin(2x))' = ?\\)", listOf("\\(2\\cos(2x)\\)", "\\(\\cos(2x)\\)", "\\(-2\\cos(2x)\\)", "\\(2\\sin(2x)\\)"), 0, "Kettenregel: \\(\\cos(2x) \\cdot 2 = 2\\cos(2x)\\)"),
    // Neue MITTEL Fragen
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((\\arctan x)' = ?\\)", listOf("\\(\\frac{1}{1+x^2}\\)", "\\(\\frac{1}{\\sqrt{1-x^2}}\\)", "\\(-\\frac{1}{1+x^2}\\)", "\\(\\tan^{-1}(x)\\)"), 0, "Umkehrfunktion von \\(\\tan\\):\n\\((\\arctan x)' = \\frac{1}{1+x^2}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((\\ln|x|)' = ?\\) für \\(x \\neq 0\\)", listOf("\\(\\frac{1}{x}\\)", "\\(\\frac{1}{|x|}\\)", "\\(\\frac{\\text{sgn}(x)}{x}\\)", "\\(-\\frac{1}{x}\\)"), 0, "Für \\(x > 0\\): \\((\\ln x)' = \\frac{1}{x}\\)\nFür \\(x < 0\\): \\((\\ln(-x))' = \\frac{-1}{-x} = \\frac{1}{x}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((\\cot x)' = ?\\)", listOf("\\(-\\frac{1}{\\sin^2(x)}\\)", "\\(\\frac{1}{\\sin^2(x)}\\)", "\\(-\\frac{1}{\\cos^2(x)}\\)", "\\(\\tan(x)\\)"), 0, "\\(\\cot x = \\frac{\\cos x}{\\sin x}\\)\nQuotientenregel: \\(\\frac{-\\sin^2 x - \\cos^2 x}{\\sin^2 x} = -\\frac{1}{\\sin^2 x}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((\\arccos x)' = ?\\) für \\(|x| < 1\\)", listOf("\\(-\\frac{1}{\\sqrt{1-x^2}}\\)", "\\(\\frac{1}{\\sqrt{1-x^2}}\\)", "\\(-\\frac{1}{1+x^2}\\)", "\\(\\cos^{-1}(x)\\)"), 0, "Umkehrfunktion von \\(\\cos\\) auf \\([0,\\pi]\\):\n\\((\\arccos x)' = -\\frac{1}{\\sqrt{1-x^2}}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((x^2 e^x)' = ?\\)", listOf("\\((2x + x^2)e^x\\)", "\\(2xe^x\\)", "\\(x^2 e^x\\)", "\\(2x + e^x\\)"), 0, "Produktregel: \\(2x \\cdot e^x + x^2 \\cdot e^x = (2x+x^2)e^x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.MITTEL, "\\((\\frac{e^x}{x})' = ?\\)", listOf("\\(\\frac{e^x(x-1)}{x^2}\\)", "\\(\\frac{e^x}{x^2}\\)", "\\(e^x - \\frac{1}{x}\\)", "\\(\\frac{e^x + 1}{x}\\)"), 0, "Quotientenregel: \\(\\frac{e^x \\cdot x - e^x \\cdot 1}{x^2} = \\frac{e^x(x-1)}{x^2}\\)"),
    // Neue SCHWER Fragen
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "\\(\\lim_{x \\to 0} \\frac{e^x - 1 - x}{x^2} = ?\\) (L'Hôpital)", listOf("\\(\\frac{1}{2}\\)", "\\(0\\)", "\\(1\\)", "\\(\\infty\\)"), 0, "Form \\(\\frac{0}{0}\\): \\(\\frac{e^x - 1}{2x}\\)\nNochmal: \\(\\frac{e^x}{2} \\to \\frac{1}{2}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "Mittelwertsatz: \\(f\\) stetig auf \\([a,b]\\), diff'bar auf \\((a,b)\\). Dann:", listOf("\\(\\exists c \\in (a,b): f'(c) = \\frac{f(b)-f(a)}{b-a}\\)", "\\(f'(a) = f'(b)\\)", "\\(f\\) hat Maximum", "\\(f\\) ist monoton"), 0, "MWS: Es gibt ein \\(c \\in (a,b)\\) mit\n\\(f'(c) = \\frac{f(b)-f(a)}{b-a}\\)\n(Sekanten-Steigung = Tangenten-Steigung)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "Implizite Diff.: \\(x^2 + y^2 = 1\\). \\(y' = ?\\)", listOf("\\(-\\frac{x}{y}\\)", "\\(\\frac{x}{y}\\)", "\\(-\\frac{y}{x}\\)", "\\(\\frac{2x}{2y}\\)"), 0, "Implizit ableiten: \\(2x + 2yy' = 0\\)\n\\(\\Rightarrow y' = -\\frac{x}{y}\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "\\((\\sinh x)' = ?\\)", listOf("\\(\\cosh x\\)", "\\(-\\cosh x\\)", "\\(\\sinh x\\)", "\\(e^x\\)"), 0, "\\(\\sinh x = \\frac{e^x - e^{-x}}{2}\\)\n\\((\\sinh x)' = \\frac{e^x + e^{-x}}{2} = \\cosh x\\)"),
    MathQuestion(MathTopic.ABLEITUNG, Difficulty.SCHWER, "\\(f(x) = (\\sin x)^x\\) für \\(x > 0\\). \\(f'(x) = ?\\)", listOf("\\((\\sin x)^x (x \\cot x + \\ln(\\sin x))\\)", "\\(x(\\sin x)^{x-1} \\cos x\\)", "\\((\\sin x)^x \\ln(\\sin x)\\)", "\\(x \\cos x (\\sin x)^{x-1}\\)"), 0, "\\(f = e^{x \\ln(\\sin x)}\\)\n\\(f' = f \\cdot (\\ln(\\sin x) + x \\cdot \\frac{\\cos x}{\\sin x})\\)"),
)

// ════════════════════════════════════════════
//  LINEARE ALGEBRA
// ════════════════════════════════════════════

private val laQuestions = listOf(
    // LEICHT
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Was ist ein Vektor in \\(\\mathbb{R}^3\\)?", listOf("Ein 3-Tupel \\((x_1,x_2,x_3)\\)", "Eine \\(3 \\times 3\\)-Matrix", "Ein Skalar", "Ein Polynom 3. Grades"), 0, "Ein Vektor in \\(\\mathbb{R}^3\\) ist ein geordnetes Tripel reeller Zahlen, z.B. \\((1,2,3)\\)."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "\\((1,2,3) + (4,5,6) = ?\\)", listOf("\\((5,7,9)\\)", "\\((4,10,18)\\)", "\\((5,5,5)\\)", "\\((3,3,3)\\)"), 0, "Komponentenweise: \\((1+4, 2+5, 3+6) = (5,7,9)\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "\\(3 \\cdot (2,-1,4) = ?\\)", listOf("\\((6,-3,12)\\)", "\\((6,-1,4)\\)", "\\((2,-3,12)\\)", "\\((5,2,7)\\)"), 0, "Skalarmultiplikation: \\((3 \\cdot 2, 3 \\cdot (-1), 3 \\cdot 4) = (6,-3,12)\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Wann sind \\(v_1,...,v_n\\) lin. unabhängig?", listOf("\\(\\lambda_1 v_1+...+\\lambda_n v_n=0 \\Rightarrow\\) alle \\(\\lambda_i=0\\)", "Wenn sie parallel sind", "Wenn \\(n>3\\)", "Immer"), 0, "Linear unabhängig: Die einzige Lösung von\n\\(\\lambda_1 v_1+...+\\lambda_n v_n = 0\\) ist \\(\\lambda_1=...=\\lambda_n=0\\)."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Was ist eine Basis von \\(\\mathbb{R}^2\\)?", listOf("2 linear unabh. Vektoren", "Beliebige 2 Vektoren", "1 Vektor", "3 Vektoren"), 0, "Basis = maximal linear unabhängiges Erzeugendensystem.\nIn \\(\\mathbb{R}^2\\) braucht man genau 2 lin. unabh. Vektoren.\nBsp: \\(\\{(1,0), (0,1)\\}\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "\\(\\dim(\\mathbb{R}^n) = ?\\)", listOf("\\(n\\)", "\\(n^2\\)", "\\(2n\\)", "\\(\\infty\\)"), 0, "Die Dimension von \\(\\mathbb{R}^n\\) ist \\(n\\).\nDie Standardbasis \\(\\{e_1,...,e_n\\}\\) hat \\(n\\) Vektoren."),
    // MITTEL
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Was ist die Zeilenstufenform?", listOf("Pivot jeder Zeile weiter rechts, Nullzeilen unten", "Diagonalmatrix", "Einheitsmatrix", "Transponierte"), 0, "ZSF: Führendes Element (Pivot) jeder Zeile steht strikt rechts vom Pivot der Zeile darüber.\nAlle Nullzeilen stehen ganz unten."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Ist \\(f(x,y) = (7y, x-3y, 2x+y)\\) linear?", listOf("Ja, \\(A = \\begin{pmatrix}0&7\\\\1&-3\\\\2&1\\end{pmatrix}\\)", "Nein", "Nur für \\(y=0\\)", "Nur für \\(x=y\\)"), 0, "\\(f(v) = Av\\) mit \\(A = \\begin{pmatrix}0&7\\\\1&-3\\\\2&1\\end{pmatrix}\\)\nJede Matrix-Vektor-Multiplikation ist linear."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Ist \\(f(x,y) = (7y+2, ix+y)\\) linear?", listOf("Nein, \\(f(0) \\neq 0\\)", "Ja", "Nur über \\(\\mathbb{R}\\)", "Nur über \\(\\mathbb{C}\\)"), 0, "\\(f(0,0) = (2,0) \\neq (0,0)\\)\nLineare Abbildungen müssen \\(f(0)=0\\) erfüllen."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Rang einer Matrix \\(A\\) ist:", listOf("Anzahl Nicht-Null-Zeilen in ZSF", "Anzahl Zeilen", "Anzahl Spalten", "Determinante"), 0, "\\(\\text{Rang}(A)\\) = Anzahl der Nicht-Null-Zeilen in der Zeilenstufenform = Anzahl Pivots."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "\\(A\\) ist \\(3 \\times 4\\) mit Rang 2. \\(\\dim(\\ker A)\\)?", listOf("\\(2\\)", "\\(1\\)", "\\(3\\)", "\\(0\\)"), 0, "Dimensionsformel:\n\\(\\dim(\\ker A) = \\text{Spalten} - \\text{Rang} = 4-2 = 2\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "\\(v_1=(3,-1,4)\\), \\(v_2=(1,1,-8)\\), \\(v_3=(0,1,-7)\\), \\(v_4=(5,-1,2)\\).\n\\(\\dim \\langle v_1,...,v_4 \\rangle\\)?", listOf("\\(2\\)", "\\(3\\)", "\\(4\\)", "\\(1\\)"), 0, "Matrix \\(\\to\\) ZSF: 2 Nicht-Null-Zeilen\n\\(\\Rightarrow \\dim = 2\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "\\(E_1 = \\{x \\in \\mathbb{R}^4: x_1+x_2+x_3+x_4=0\\}\\).\n\\(\\dim(E_1)\\)?", listOf("\\(3\\)", "\\(4\\)", "\\(2\\)", "\\(1\\)"), 0, "1 Gleichung in \\(\\mathbb{R}^4\\):\n\\(\\dim = 4 - \\text{Rang} = 4-1 = 3\\)"),
    // SCHWER
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "\\(\\varphi(e_3)=2e_1+3e_2+5e_3\\), \\(\\varphi(e_2+e_3)=e_1\\),\n\\(\\varphi(e_1+e_2+e_3)=e_2-e_3\\).\n\\(\\varphi(e_1) = ?\\)", listOf("\\(-e_1+e_2-e_3\\)", "\\(e_2-e_3-e_1\\)", "\\(e_1\\)", "\\(2e_1-e_3\\)"), 0, "\\(\\varphi(e_1) = \\varphi((e_1+e_2+e_3)-(e_2+e_3))\\)\n\\(= \\varphi(e_1+e_2+e_3)-\\varphi(e_2+e_3)\\)\n\\(= (e_2-e_3)-e_1 = -e_1+e_2-e_3\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "\\(E_1 \\cap E_2\\) mit \\(E_1: x_1+x_2+x_3+x_4=0\\),\n\\(E_2: 2x_1-3x_3-x_4=0\\).\n\\(\\dim(E_1 \\cap E_2)\\)?", listOf("\\(2\\)", "\\(1\\)", "\\(3\\)", "\\(0\\)"), 0, "2 Gleichungen in \\(\\mathbb{R}^4\\), \\(\\text{Rang}=2\\):\n\\(\\dim = 4-2 = 2\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "Dimensionsformel für lin. Abb. \\(\\varphi: V \\to W\\):", listOf("\\(\\dim V = \\dim \\ker \\varphi + \\dim \\text{Bild} \\varphi\\)", "\\(\\dim V = \\dim W\\)", "\\(\\text{Rang} = \\dim V\\)", "\\(\\dim \\ker = \\dim \\text{Bild}\\)"), 0, "Dimensionsformel (Rangsatz):\n\\[\\dim V = \\dim \\ker(\\varphi) + \\dim \\text{Bild}(\\varphi)\\]\n\\(= \\text{Defekt} + \\text{Rang}\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "Wann hat \\(Ax=b\\) eine Lösung?", listOf("Wenn \\(\\text{Rang}(A)=\\text{Rang}(A|b)\\)", "Immer", "Wenn \\(\\det(A) \\neq 0\\)", "Wenn \\(A\\) quadratisch"), 0, "\\(Ax=b\\) lösbar \\(\\Leftrightarrow \\text{Rang}(A) = \\text{Rang}(A|b)\\)\n(\\(b\\) liegt im Bild von \\(A\\))\nEindeutig \\(\\Leftrightarrow\\) zusätzlich Rang = Spaltenanzahl."),
    // Neue LEICHT Fragen
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Was ist die Einheitsmatrix \\(I_n\\)?", listOf("Diagonalmatrix mit 1en auf der Diagonale", "Nullmatrix", "Matrix mit nur 1en", "Inverse Matrix"), 0, "\\(I_n\\) hat auf der Diagonale 1en, sonst 0en.\n\\(A \\cdot I = I \\cdot A = A\\) für jede passende Matrix \\(A\\)."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "\\((A \\cdot B)^T = ?\\)", listOf("\\(B^T \\cdot A^T\\)", "\\(A^T \\cdot B^T\\)", "\\((AB)^T\\)", "\\(A \\cdot B\\)"), 0, "Beim Transponieren eines Produkts dreht sich die Reihenfolge um:\n\\((AB)^T = B^T A^T\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Skalarprodukt \\(\\langle (1,2), (3,4) \\rangle = ?\\)", listOf("\\(11\\)", "\\(10\\)", "\\((3,8)\\)", "\\(14\\)"), 0, "\\(\\langle (1,2), (3,4) \\rangle = 1 \\cdot 3 + 2 \\cdot 4 = 3 + 8 = 11\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Länge von \\((3,4)\\) = ?", listOf("\\(5\\)", "\\(7\\)", "\\(12\\)", "\\(\\sqrt{7}\\)"), 0, "\\(||(3,4)|| = \\sqrt{3^2 + 4^2} = \\sqrt{9+16} = \\sqrt{25} = 5\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Sind \\((1,0)\\) und \\((0,1)\\) orthogonal?", listOf("Ja", "Nein", "Nur in \\(\\mathbb{R}^2\\)", "Undefiniert"), 0, "\\(\\langle (1,0), (0,1) \\rangle = 1 \\cdot 0 + 0 \\cdot 1 = 0\\)\nSkalarprodukt = 0 \\(\\Rightarrow\\) orthogonal."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.LEICHT, "Was ist \\(\\ker(A)\\)?", listOf("\\(\\{x : Ax = 0\\}\\)", "\\(\\{Ax : x \\in V\\}\\)", "Inverse von \\(A\\)", "Determinante"), 0, "Der Kern (Nullraum) ist die Menge aller Vektoren, die auf 0 abgebildet werden:\n\\(\\ker(A) = \\{x : Ax = 0\\}\\)"),
    // Neue MITTEL Fragen
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "\\(\\det \\begin{pmatrix} 2 & 1 \\\\ 4 & 3 \\end{pmatrix} = ?\\)", listOf("\\(2\\)", "\\(10\\)", "\\(-2\\)", "\\(6\\)"), 0, "\\(\\det = ad - bc = 2 \\cdot 3 - 1 \\cdot 4 = 6 - 4 = 2\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Wenn \\(\\det(A) = 0\\), dann ist \\(A\\):", listOf("singulär (nicht invertierbar)", "invertierbar", "orthogonal", "symmetrisch"), 0, "\\(\\det(A) = 0 \\Leftrightarrow A\\) ist singulär \\(\\Leftrightarrow A\\) hat keinen vollen Rang \\(\\Leftrightarrow A^{-1}\\) existiert nicht."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "\\(\\det(AB) = ?\\)", listOf("\\(\\det(A) \\cdot \\det(B)\\)", "\\(\\det(A) + \\det(B)\\)", "\\(\\det(A+B)\\)", "\\(\\det(A) / \\det(B)\\)"), 0, "Determinante ist multiplikativ:\n\\(\\det(AB) = \\det(A) \\cdot \\det(B)\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "\\(\\det(A^{-1}) = ?\\)", listOf("\\(\\frac{1}{\\det(A)}\\)", "\\(-\\det(A)\\)", "\\(\\det(A)\\)", "\\(0\\)"), 0, "\\(\\det(A \\cdot A^{-1}) = \\det(I) = 1\\)\n\\(\\det(A) \\cdot \\det(A^{-1}) = 1 \\Rightarrow \\det(A^{-1}) = \\frac{1}{\\det(A)}\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Eigenwert \\(\\lambda\\) von \\(A\\) erfüllt:", listOf("\\(Av = \\lambda v\\) für \\(v \\neq 0\\)", "\\(A\\lambda = v\\)", "\\(\\det(A) = \\lambda\\)", "\\(A = \\lambda I\\)"), 0, "\\(\\lambda\\) ist Eigenwert von \\(A\\), wenn es einen Vektor \\(v \\neq 0\\) gibt mit \\(Av = \\lambda v\\).\n\\(v\\) heißt Eigenvektor."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Charakteristisches Polynom von \\(A\\) ist:", listOf("\\(\\det(A - \\lambda I)\\)", "\\(\\det(A)\\)", "\\(\\text{Spur}(A)\\)", "\\(A - \\lambda\\)"), 0, "\\(\\chi_A(\\lambda) = \\det(A - \\lambda I)\\)\nDie Nullstellen sind die Eigenwerte."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.MITTEL, "Spur einer \\(n \\times n\\)-Matrix ist:", listOf("Summe der Diagonalelemente", "Produkt der Diagonalelemente", "Determinante", "Rang"), 0, "\\(\\text{Spur}(A) = \\sum_{i=1}^{n} a_{ii}\\)\nAuch: Summe der Eigenwerte."),
    // Neue SCHWER Fragen
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "\\(A\\) hat Eigenwerte \\(2, 3\\). \\(\\det(A) = ?\\)", listOf("\\(6\\)", "\\(5\\)", "\\(1\\)", "\\(-1\\)"), 0, "Determinante = Produkt der Eigenwerte:\n\\(\\det(A) = 2 \\cdot 3 = 6\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "\\(A\\) hat Eigenwerte \\(2, 3\\). \\(\\text{Spur}(A) = ?\\)", listOf("\\(5\\)", "\\(6\\)", "\\(1\\)", "\\(-1\\)"), 0, "Spur = Summe der Eigenwerte:\n\\(\\text{Spur}(A) = 2 + 3 = 5\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "Wann ist \\(A\\) diagonalisierbar?", listOf("Wenn \\(A\\) \\(n\\) lin. unabh. Eigenvektoren hat", "Immer", "Wenn \\(\\det(A) \\neq 0\\)", "Wenn \\(A\\) symmetrisch"), 0, "\\(A\\) ist diagonalisierbar \\(\\Leftrightarrow A\\) hat \\(n\\) linear unabhängige Eigenvektoren.\nBei symmetrischen Matrizen ist das immer der Fall."),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "Cayley-Hamilton: Für jede Matrix \\(A\\) gilt:", listOf("\\(\\chi_A(A) = 0\\)", "\\(A^n = 0\\)", "\\(\\det(A) = 0\\)", "\\(A = A^T\\)"), 0, "Cayley-Hamilton: Jede Matrix erfüllt ihr eigenes charakteristisches Polynom:\n\\(\\chi_A(A) = 0\\)"),
    MathQuestion(MathTopic.LINEARE_ALGEBRA, Difficulty.SCHWER, "Orthogonale Matrix \\(Q\\) erfüllt:", listOf("\\(Q^T Q = I\\)", "\\(Q^2 = I\\)", "\\(\\det(Q) = 0\\)", "\\(Q = Q^T\\)"), 0, "\\(Q\\) orthogonal \\(\\Leftrightarrow Q^T Q = QQ^T = I \\Leftrightarrow Q^{-1} = Q^T\\)\nAuch: \\(|\\det(Q)| = 1\\)"),
)

// ════════════════════════════════════════════
//  STETIGKEIT
// ════════════════════════════════════════════

private val stetigkeitQuestions = listOf(
    // LEICHT
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Was bedeutet \\(f\\) stetig in \\(a\\)?", listOf("\\(\\lim_{x \\to a} f(x) = f(a)\\)", "\\(f(a)\\) existiert", "\\(f\\) ist diff'bar in \\(a\\)", "\\(f\\) ist monoton"), 0, "\\(f\\) stetig in \\(a \\Leftrightarrow \\lim_{x \\to a} f(x) = f(a)\\)\n\\(\\Leftrightarrow\\) Für alle \\(\\varepsilon>0\\) gibt es \\(\\delta>0\\) mit\n\\(|x-a|<\\delta \\Rightarrow |f(x)-f(a)|<\\varepsilon\\)"),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Sind Polynome stetig?", listOf("Ja, auf ganz \\(\\mathbb{R}\\)", "Nur auf \\((0,\\infty)\\)", "Nein", "Nur wenn Grad \\(\\leq 2\\)"), 0, "Alle Polynome sind stetig auf ganz \\(\\mathbb{R}\\)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Ist \\(f(x)=\\frac{1}{x}\\) stetig auf \\(\\mathbb{R}\\)?", listOf("Nein, nicht def. bei \\(x=0\\)", "Ja", "Nur für \\(x>0\\)", "Nein, nirgends"), 0, "\\(f(x)=\\frac{1}{x}\\) ist stetig auf \\(\\mathbb{R} \\setminus \\{0\\}\\), aber nicht in \\(0\\) definiert (und nicht stetig fortsetzbar)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Differenzierbar \\(\\Rightarrow\\) stetig?", listOf("Ja, immer", "Nein", "Nur für Polynome", "Nur auf offenen Intervallen"), 0, "Differenzierbarkeit impliziert Stetigkeit.\nAber NICHT umgekehrt! (Bsp: \\(|x|\\) ist stetig, aber nicht diff'bar in \\(0\\))"),
    // MITTEL
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "Zwischenwertsatz: Wenn \\(f\\) stetig auf \\([a,b]\\)\nund \\(f(a)<0<f(b)\\), dann:", listOf("\\(\\exists c \\in (a,b)\\) mit \\(f(c)=0\\)", "\\(f\\) ist monoton", "\\(f\\) hat Maximum", "\\(f\\) ist diff'bar"), 0, "ZWS: Stetige Funktion auf \\([a,b]\\) nimmt jeden Wert zwischen \\(f(a)\\) und \\(f(b)\\) an.\nInsbesondere: Es gibt eine Nullstelle."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "\\(f(x) = \\begin{cases}-2 & x<0 \\\\ 2 & x \\geq 0\\end{cases}\\).\nStetig in \\(0\\)?", listOf("Nein, Sprung von \\(-2\\) nach \\(2\\)", "Ja", "Nur von rechts", "Nur von links"), 0, "\\(\\lim_{x \\to 0^-} f(x) = -2 \\neq 2 = \\lim_{x \\to 0^+} f(x)\\)\nLinks- und rechtsseitiger GW verschieden \\(\\Rightarrow\\) nicht stetig."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "Ist \\(|x|\\) stetig in \\(x=0\\)?", listOf("Ja", "Nein", "Nur rechtsseitig", "Nur linksseitig"), 0, "\\(\\lim_{x \\to 0} |x| = 0 = |0|\\)\nAlso ist \\(|x|\\) stetig in \\(0\\).\n(Aber nicht differenzierbar in \\(0\\)!)"),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "Max/Min-Satz: \\(f\\) stetig auf \\([a,b] \\Rightarrow\\)", listOf("\\(f\\) nimmt Max und Min an", "\\(f\\) ist monoton", "\\(f\\) ist diff'bar", "\\(f\\) hat Nullstelle"), 0, "Satz vom Maximum: Jede stetige Funktion auf einem kompakten Intervall \\([a,b]\\) nimmt ihr Maximum und Minimum an."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "Ist \\(\\sin(1/x)\\) stetig fortsetzbar in \\(0\\)?", listOf("Nein, lim existiert nicht", "Ja, mit \\(f(0)=0\\)", "Ja, mit \\(f(0)=1\\)", "Nein, da unbeschränkt"), 0, "\\(\\sin(1/x)\\) oszilliert für \\(x \\to 0\\) zwischen \\(-1\\) und \\(1\\).\nDer Grenzwert existiert nicht.\n\\(\\Rightarrow\\) Nicht stetig fortsetzbar."),
    // SCHWER
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "Ist \\(x \\cdot \\sin(1/x)\\) stetig fortsetzbar in \\(0\\)?", listOf("Ja, mit \\(f(0)=0\\)", "Nein", "Ja, mit \\(f(0)=1\\)", "Nicht entscheidbar"), 0, "\\(|x \\cdot \\sin(1/x)| \\leq |x| \\to 0\\)\nAlso \\(\\lim_{x \\to 0} x \\cdot \\sin(1/x) = 0\\).\nMit \\(f(0):=0\\) ist \\(f\\) stetig in \\(0\\)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "\\(f: [\\frac{3\\pi}{2}, \\frac{5\\pi}{2}] \\to [-1,1]\\), \\(x \\mapsto \\sin(x)\\).\n\\(f^{-1}(y) = ?\\)", listOf("\\(\\arcsin(y) + 2\\pi\\)", "\\(\\arcsin(y)\\)", "\\(-\\arcsin(y)\\)", "\\(\\pi - \\arcsin(y)\\)"), 0, "\\(\\sin(x) = \\sin(x-2\\pi)\\)\nAlso \\(f^{-1}(y) = \\arcsin(y)+2\\pi\\)"),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "Gleichmäßige Stetigkeit bedeutet:", listOf("\\(\\delta\\) hängt nur von \\(\\varepsilon\\) ab, nicht von \\(x_0\\)", "\\(f\\) stetig in jedem Punkt", "\\(f\\) ist beschränkt", "\\(f\\) ist Lipschitz-stetig"), 0, "Gleichmäßig stetig: \\(\\forall \\varepsilon>0 \\ \\exists \\delta>0\\):\n\\(|x-y|<\\delta \\Rightarrow |f(x)-f(y)|<\\varepsilon\\) für ALLE \\(x,y\\).\nDas \\(\\delta\\) gilt uniform für alle Punkte."),
    // Neue LEICHT Fragen
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Ist \\(e^x\\) stetig?", listOf("Ja, auf ganz \\(\\mathbb{R}\\)", "Nur für \\(x > 0\\)", "Nein", "Nur auf \\([0,\\infty)\\)"), 0, "Die Exponentialfunktion \\(e^x\\) ist auf ganz \\(\\mathbb{R}\\) stetig (sogar beliebig oft differenzierbar)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Ist \\(\\ln(x)\\) stetig auf \\(\\mathbb{R}\\)?", listOf("Nein, nur auf \\((0,\\infty)\\) definiert", "Ja", "Nur für \\(x \\geq 1\\)", "Nein, nirgends"), 0, "\\(\\ln(x)\\) ist nur für \\(x > 0\\) definiert, dort aber stetig."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Summe stetiger Funktionen ist:", listOf("stetig", "nicht stetig", "nur manchmal stetig", "unbeschränkt"), 0, "Wenn \\(f\\) und \\(g\\) stetig sind, dann ist auch \\(f+g\\) stetig.\nDas folgt direkt aus den Grenzwertsätzen."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Produkt stetiger Funktionen ist:", listOf("stetig", "nicht stetig", "nur bei Polynomen", "unbeschränkt"), 0, "Wenn \\(f\\) und \\(g\\) stetig sind, dann ist auch \\(f \\cdot g\\) stetig.\nDas folgt aus \\(\\lim(fg) = (\\lim f)(\\lim g)\\)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Verkettung stetiger Funktionen ist:", listOf("stetig", "nicht stetig", "nur bei monotonen Funktionen", "unbestimmt"), 0, "Wenn \\(f\\) und \\(g\\) stetig sind, dann ist auch \\(f \\circ g\\) stetig."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.LEICHT, "Ist \\(\\sqrt{x}\\) stetig auf \\([0,\\infty)\\)?", listOf("Ja", "Nein", "Nur auf \\((0,\\infty)\\)", "Nur bei \\(x=1\\)"), 0, "Die Wurzelfunktion ist auf ihrem gesamten Definitionsbereich \\([0,\\infty)\\) stetig."),
    // Neue MITTEL Fragen
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "\\(f(x) = \\frac{x^2-1}{x-1}\\) bei \\(x=1\\):", listOf("Hebbare Unstetigkeit, \\(f(1):=2\\)", "Sprungstelle", "Polstelle", "Stetig"), 0, "\\(\\frac{x^2-1}{x-1} = \\frac{(x-1)(x+1)}{x-1} = x+1\\) für \\(x \\neq 1\\).\n\\(\\lim_{x \\to 1} = 2\\), also hebbar mit \\(f(1):=2\\)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "\\(f(x) = \\frac{1}{x-2}\\) bei \\(x=2\\):", listOf("Polstelle (nicht hebbar)", "Hebbare Unstetigkeit", "Sprungstelle", "Stetig"), 0, "\\(\\lim_{x \\to 2^+} = +\\infty\\), \\(\\lim_{x \\to 2^-} = -\\infty\\).\nDies ist eine Polstelle, nicht hebbar."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "Heine-Charakterisierung: \\(f\\) stetig in \\(a \\Leftrightarrow\\)", listOf("\\((x_n) \\to a \\Rightarrow f(x_n) \\to f(a)\\)", "\\(f(a)\\) existiert", "\\(f\\) ist monoton", "\\(f'(a)\\) existiert"), 0, "Folgenkriterium für Stetigkeit:\n\\(f\\) stetig in \\(a \\Leftrightarrow\\) für alle Folgen \\((x_n)\\) mit \\(x_n \\to a\\) gilt \\(f(x_n) \\to f(a)\\)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "Ist \\(f(x) = x \\cdot \\mathbf{1}_{\\mathbb{Q}}(x)\\) stetig in \\(0\\)?", listOf("Ja", "Nein", "Nur von rechts", "Undefiniert"), 0, "\\(|f(x)| \\leq |x| \\to 0\\) für \\(x \\to 0\\).\nAlso \\(\\lim_{x \\to 0} f(x) = 0 = f(0)\\), stetig in \\(0\\)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.MITTEL, "Dirichlet-Funktion \\(\\mathbf{1}_{\\mathbb{Q}}\\) ist:", listOf("Nirgends stetig", "Überall stetig", "Nur in \\(0\\) stetig", "Nur in \\(\\mathbb{Q}\\) stetig"), 0, "In jeder Umgebung eines \\(x\\) liegen rationale und irrationale Zahlen.\nDaher existiert kein Grenzwert, nirgends stetig."),
    // Neue SCHWER Fragen
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "Ist \\(f(x) = x^2\\) gleichmäßig stetig auf \\(\\mathbb{R}\\)?", listOf("Nein", "Ja", "Nur auf \\([0,1]\\)", "Nur für \\(x > 0\\)"), 0, "Nein! Für \\(x_n = n\\), \\(y_n = n + 1/n\\):\n\\(|x_n - y_n| = 1/n \\to 0\\), aber \\(|x_n^2 - y_n^2| = |2 + 1/n^2| \\to 2 \\neq 0\\)."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "Stetige Funktion auf \\([a,b]\\) ist:", listOf("gleichmäßig stetig", "nicht unbedingt glm. stetig", "beschränkt aber nicht glm. stetig", "unbeschränkt"), 0, "Satz von Heine: Jede stetige Funktion auf einem kompakten Intervall \\([a,b]\\) ist gleichmäßig stetig."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "\\(f\\) Lipschitz-stetig \\(\\Rightarrow\\) \\(f\\) ist:", listOf("gleichmäßig stetig", "differenzierbar", "beschränkt", "monoton"), 0, "Lipschitz: \\(|f(x)-f(y)| \\leq L|x-y|\\).\nWähle \\(\\delta = \\varepsilon/L\\), dann gilt \\(|f(x)-f(y)| < \\varepsilon\\) für \\(|x-y|<\\delta\\).\nAlso gleichmäßig stetig."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "\\(f(x) = x^2 \\sin(1/x)\\) für \\(x \\neq 0\\), \\(f(0)=0\\).\nIst \\(f\\) differenzierbar in \\(0\\)?", listOf("Ja, \\(f'(0)=0\\)", "Nein", "Nur rechtsseitig", "Unbestimmt"), 0, "\\(\\frac{f(x)-f(0)}{x-0} = x \\sin(1/x) \\to 0\\) für \\(x \\to 0\\).\nAlso \\(f'(0) = 0\\) existiert."),
    MathQuestion(MathTopic.STETIGKEIT, Difficulty.SCHWER, "Thomae-Funktion ist stetig in:", listOf("allen irrationalen Punkten", "allen rationalen Punkten", "nirgends", "überall"), 0, "Thomae: \\(f(p/q) = 1/q\\) für \\(p/q\\) gekürzt, \\(f(x)=0\\) für \\(x\\) irrational.\nStetig in allen irrationalen, unstetig in allen rationalen Punkten."),
)

// ════════════════════════════════════════════
//  Question retrieval
// ════════════════════════════════════════════

private val allQuestions: Map<MathTopic, List<MathQuestion>> = mapOf(
    MathTopic.BETRAG to betragQuestions,
    MathTopic.INDUKTION to induktionQuestions,
    MathTopic.FOLGEN to folgenQuestions,
    MathTopic.REIHEN to reihenQuestions,
    MathTopic.ABLEITUNG to ableitungQuestions,
    MathTopic.LINEARE_ALGEBRA to laQuestions,
    MathTopic.STETIGKEIT to stetigkeitQuestions
)

private fun getQuestions(topic: MathTopic?, difficulty: Difficulty?): List<MathQuestion> {
    val topicQuestions = if (topic != null) allQuestions[topic] ?: emptyList()
    else allQuestions.values.flatten()
    return if (difficulty != null) topicQuestions.filter { it.difficulty == difficulty }
    else topicQuestions
}

private fun generateQuestion(topic: MathTopic?, difficulty: Difficulty?): MathQuestion {
    val pool = getQuestions(topic, difficulty)
    val originalQuestion = if (pool.isNotEmpty()) pool.random() else allQuestions.values.flatten().random()

    // Shuffle options and track the new position of the correct answer
    val correctAnswer = originalQuestion.options[originalQuestion.correctIndex]
    val shuffledOptions = originalQuestion.options.shuffled()
    val newCorrectIndex = shuffledOptions.indexOf(correctAnswer)

    return originalQuestion.copy(
        options = shuffledOptions,
        correctIndex = newCorrectIndex
    )
}

// ════════════════════════════════════════════
//  COMPOSABLES
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathTrainerScreen(onBackClick: () -> Unit) {
    var selectedTopic by remember { mutableStateOf<MathTopic?>(null) }
    var selectedDifficulty by remember { mutableStateOf<Difficulty?>(null) }
    var currentQuestion by remember { mutableStateOf<MathQuestion?>(null) }
    var selectedAnswer by remember { mutableStateOf(-1) }
    var showSolution by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var totalAnswered by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    // false = topic select, true = difficulty select
    var showDifficultySelect by remember { mutableStateOf(false) }

    fun startQuiz() {
        currentQuestion = generateQuestion(selectedTopic, selectedDifficulty)
        selectedAnswer = -1
        showSolution = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            currentQuestion != null -> {
                                val diff = currentQuestion!!.difficulty.displayName
                                "${currentQuestion!!.topic.displayName} ($diff)"
                            }
                            showDifficultySelect -> selectedTopic?.displayName ?: "Alle Themen"
                            else -> "HM1 Mathe Trainer"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = if (currentQuestion != null) 14.sp else 18.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showSolution -> {
                                showSolution = false
                                selectedAnswer = -1
                                currentQuestion = generateQuestion(selectedTopic, selectedDifficulty)
                            }
                            currentQuestion != null -> {
                                currentQuestion = null
                                showDifficultySelect = true
                            }
                            showDifficultySelect -> {
                                showDifficultySelect = false
                                selectedTopic = null
                                selectedDifficulty = null
                            }
                            else -> onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    if (totalAnswered > 0) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                            Text("$score/$totalAnswered", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { padding ->
        when {
            currentQuestion != null -> {
                QuestionView(
                    question = currentQuestion!!,
                    selectedAnswer = selectedAnswer,
                    showSolution = showSolution,
                    streak = streak,
                    onAnswerSelected = { i ->
                        if (!showSolution) {
                            selectedAnswer = i; showSolution = true; totalAnswered++
                            if (i == currentQuestion!!.correctIndex) { score++; streak++ } else streak = 0
                        }
                    },
                    onNext = {
                        showSolution = false; selectedAnswer = -1
                        currentQuestion = generateQuestion(selectedTopic, selectedDifficulty)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            showDifficultySelect -> {
                DifficultySelection(
                    topic = selectedTopic,
                    onDifficultySelected = { diff ->
                        selectedDifficulty = diff
                        score = 0; totalAnswered = 0; streak = 0
                        startQuiz()
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                TopicSelection(
                    onTopicSelected = { topic ->
                        selectedTopic = topic
                        showDifficultySelect = true
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun DifficultySelection(topic: MathTopic?, onDifficultySelected: (Difficulty?) -> Unit, modifier: Modifier = Modifier) {
    val topicName = topic?.displayName ?: "Alle Themen"
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Schwierigkeit wählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Thema: $topicName", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(8.dp))

        // All difficulties
        Surface(
            onClick = { onDifficultySelected(null) },
            modifier = Modifier.fillMaxWidth().height(76.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column {
                    Text("Alle Schwierigkeiten", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    val count = getQuestions(topic, null).size
                    Text("$count Aufgaben", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Difficulty.entries.forEach { diff ->
            val count = getQuestions(topic, diff).size
            Surface(
                onClick = { if (count > 0) onDifficultySelected(diff) },
                modifier = Modifier.fillMaxWidth().height(76.dp),
                shape = RoundedCornerShape(16.dp),
                color = diff.color.copy(alpha = 0.12f)
            ) {
                Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(diff.color),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (diff) {
                            Difficulty.LEICHT -> Icons.Default.SentimentSatisfied
                            Difficulty.MITTEL -> Icons.Default.Psychology
                            Difficulty.SCHWER -> Icons.Default.LocalFireDepartment
                        }
                        Icon(icon, null, Modifier.size(20.dp), tint = Color.White)
                    }
                    Column {
                        Text(diff.displayName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = diff.color)
                        Text(
                            when (diff) {
                                Difficulty.LEICHT -> "$count Aufgaben — Grundlagen & Definitionen"
                                Difficulty.MITTEL -> "$count Aufgaben — Übungsblatt-Niveau"
                                Difficulty.SCHWER -> "$count Aufgaben — Klausurniveau"
                            },
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicSelection(onTopicSelected: (MathTopic?) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Wähle ein Themengebiet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        Surface(
            onClick = { onTopicSelected(null) },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column {
                    Text("Alle Themen gemischt", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val total = allQuestions.values.sumOf { it.size }
                    Text("$total Aufgaben aus allen Gebieten", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Einzelne Themen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        MathTopic.entries.forEach { topic ->
            val count = allQuestions[topic]?.size ?: 0
            Surface(
                onClick = { onTopicSelected(topic) },
                modifier = Modifier.fillMaxWidth().height(76.dp),
                shape = RoundedCornerShape(16.dp),
                color = topic.color.copy(alpha = 0.12f)
            ) {
                Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(topic.icon, null, Modifier.size(32.dp), tint = topic.color)
                    Column {
                        Text(topic.displayName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = topic.color)
                        Text("$count Aufgaben", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Basierend auf: HM1 für E-Technik (KIT)\nBlatt 1–11 mit Lösungen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun QuestionView(
    question: MathQuestion, selectedAnswer: Int, showSolution: Boolean, streak: Int,
    onAnswerSelected: (Int) -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Streak
        if (streak >= 3) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFF9800).copy(alpha = 0.15f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                    Text("$streak richtig in Folge!", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 14.sp)
                }
            }
        }

        // Topic + difficulty badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = question.topic.color.copy(alpha = 0.15f)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(question.topic.icon, null, tint = question.topic.color, modifier = Modifier.size(16.dp))
                    Text(question.topic.displayName, fontSize = 11.sp, color = question.topic.color, fontWeight = FontWeight.Medium)
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = question.difficulty.color.copy(alpha = 0.15f)) {
                Text(question.difficulty.displayName, fontSize = 11.sp, color = question.difficulty.color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }

        // Question card
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            MathText(question.questionText, Modifier.padding(20.dp), fontSize = 17, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(4.dp))

        // Options
        question.options.forEachIndexed { index, option ->
            val isCorrect = index == question.correctIndex
            val isSelected = index == selectedAnswer
            val bgColor = when {
                showSolution && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                showSolution && isSelected && !isCorrect -> Color(0xFFF44336).copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }
            val borderColor = when {
                showSolution && isCorrect -> Color(0xFF4CAF50)
                showSolution && isSelected && !isCorrect -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Surface(
                onClick = { onAnswerSelected(index) },
                modifier = Modifier.fillMaxWidth().border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp), color = bgColor
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(
                        when { showSolution && isCorrect -> Color(0xFF4CAF50); showSolution && isSelected -> Color(0xFFF44336); else -> MaterialTheme.colorScheme.primaryContainer }
                    ), contentAlignment = Alignment.Center) {
                        when {
                            showSolution && isCorrect -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            showSolution && isSelected -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            else -> Text("${'A'+index}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    MathText(option, Modifier.weight(1f), fontSize = 15)
                }
            }
        }

        // Solution
        if (showSolution) {
            Spacer(Modifier.height(4.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f))) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                        Text("Lösung", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    MathText(question.solutionExplanation, fontSize = 14)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onNext, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.ArrowForward, null); Spacer(Modifier.width(8.dp))
                Text("Nächste Frage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
