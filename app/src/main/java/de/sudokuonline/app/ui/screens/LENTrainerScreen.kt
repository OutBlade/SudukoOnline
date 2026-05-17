package de.sudokuonline.app.ui.screens

import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

// ════════════════════════════════════════════
//  LaTeX Rendering (KaTeX WebView)
// ════════════════════════════════════════════

private fun lenBuildLatexHtml(text: String, fontSize: Int, fontWeight: String, hexColor: String): String {
    val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
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
private fun LenLatexText(text: String, modifier: Modifier = Modifier, fontSize: Int = 15, fontWeight: String = "normal", color: Color = MaterialTheme.colorScheme.onSurface) {
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
                        view?.evaluateJavascript("(function(){return document.getElementById('c').offsetHeight})()") { r ->
                            val h = r.toFloatOrNull() ?: return@evaluateJavascript
                            if (h > 0) heightDp = (h + 4).dp
                        }
                    }
                }
                wv.loadDataWithBaseURL(null, lenBuildLatexHtml(text, fontSize, fontWeight, hexColor), "text/html", "UTF-8", null)
            }
        },
        modifier = modifier.fillMaxWidth().height(heightDp)
    )
}

@Composable
private fun LENText(text: String, modifier: Modifier = Modifier, fontSize: Int = 15, fontWeight: FontWeight = FontWeight.Normal, color: Color = MaterialTheme.colorScheme.onSurface) {
    if ("\\" in text) {
        LenLatexText(text, modifier, fontSize, if (fontWeight >= FontWeight.Bold) "bold" else if (fontWeight >= FontWeight.Medium) "500" else "normal", color)
    } else {
        Text(text, modifier = modifier, fontSize = fontSize.sp, fontWeight = fontWeight, color = color)
    }
}

// ════════════════════════════════════════════
//  Data Models
// ════════════════════════════════════════════

private enum class LENQTopic(val displayName: String, val icon: ImageVector, val color: Color) {
    ORTSKURVE("Ortskurve", Icons.Default.Timeline, Color(0xFF2196F3)),
    NETZWERK("Netzwerkanalyse", Icons.Default.AccountTree, Color(0xFF4CAF50)),
    WECHSELSTROM("Wechselstromlehre", Icons.Default.ElectricBolt, Color(0xFFFF9800)),
    BODE("Bodediagramm", Icons.Default.ShowChart, Color(0xFF9C27B0)),
    OPV("Operationsverstärker", Icons.Default.Memory, Color(0xFFF44336))
}

private enum class LENQDifficulty(val displayName: String, val color: Color) {
    LEICHT("Leicht", Color(0xFF4CAF50)),
    MITTEL("Mittel", Color(0xFFFF9800)),
    SCHWER("Schwer", Color(0xFFF44336))
}

private data class LENQuestion(
    val topic: LENQTopic,
    val difficulty: LENQDifficulty,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val solutionExplanation: String
)

// ════════════════════════════════════════════
//  ORTSKURVE
// ════════════════════════════════════════════

private val ortskurveQuestions = listOf(
    // LEICHT
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.LEICHT,
        "Wie lautet die Impedanz einer Spule?",
        listOf("\\(Z_L = j\\omega L\\)", "\\(Z_L = \\frac{1}{j\\omega L}\\)", "\\(Z_L = \\omega L\\)", "\\(Z_L = j\\frac{L}{\\omega}\\)"),
        0, "Induktiver Blindwiderstand:\n\\[Z_L = j\\omega L\\]\nDer Imaginärteil ist positiv — eine Spule ist induktiv."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.LEICHT,
        "Wie verhält sich eine Spule bei \\(\\omega \\to 0\\)?",
        listOf("Kurzschluss (\\(Z_L \\to 0\\))", "Leerlauf (\\(Z_L \\to \\infty\\))", "Widerstand R", "Kondensator"),
        0, "\\(Z_L = j\\omega L \\to 0\\) für \\(\\omega \\to 0\\)\nDie Spule wird zum idealen Kurzschluss bei Gleichstrom."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.LEICHT,
        "Wie verhält sich ein Kondensator bei \\(\\omega \\to \\infty\\)?",
        listOf("Kurzschluss (\\(Z_C \\to 0\\))", "Leerlauf (\\(Z_C \\to \\infty\\))", "Widerstand R", "Spule"),
        0, "\\(Z_C = \\frac{1}{j\\omega C} \\to 0\\) für \\(\\omega \\to \\infty\\)\nBei hohen Frequenzen wird der Kondensator zum Kurzschluss."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.LEICHT,
        "Eine Impedanz mit positivem Imaginärteil (\\(\\text{Im}\\{Z\\} > 0\\)) ist:",
        listOf("Induktiv", "Kapazitiv", "Rein reell (ohmsich)", "Resonant"),
        0, "Positive Reaktanz → induktives Verhalten.\nNegative Reaktanz → kapazitives Verhalten.\nDie Spule hat \\(\\text{Im}\\{Z_L\\} = \\omega L > 0\\)."),
    // MITTEL
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.MITTEL,
        "Die Resonanzfrequenz \\(\\omega_0\\) eines RLC-Parallelschwingkreises lautet:",
        listOf("\\(\\omega_0 = \\frac{1}{\\sqrt{LC}}\\)", "\\(\\omega_0 = \\sqrt{\\frac{L}{C}}\\)", "\\(\\omega_0 = R\\sqrt{\\frac{C}{L}}\\)", "\\(\\omega_0 = \\frac{R}{L}\\)"),
        0, "Bei Resonanz ist \\(\\text{Im}\\{Y\\} = 0\\):\n\\[\\omega C - \\frac{1}{\\omega L} = 0 \\implies \\omega_0 = \\frac{1}{\\sqrt{LC}}\\]\nGilt auch für den Reihenschwingkreis."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.MITTEL,
        "Die Admittanz \\(Y\\) eines RLC-Parallelkreises \\((R \\| L \\| C)\\) lautet:",
        listOf("\\(Y = \\frac{1}{R} + j\\left(\\omega C - \\frac{1}{\\omega L}\\right)\\)", "\\(Y = R + j\\left(\\omega L - \\frac{1}{\\omega C}\\right)\\)", "\\(Y = \\frac{1}{R} + j\\omega C\\)", "\\(Y = \\frac{1}{R} - j\\frac{1}{\\omega L}\\)"),
        0, "Parallelschaltung: Admittanzen addieren sich.\n\\[Y = Y_R + Y_L + Y_C = \\frac{1}{R} + \\frac{1}{j\\omega L} + j\\omega C\\]\n\\[= \\frac{1}{R} + j\\!\\left(\\omega C - \\frac{1}{\\omega L}\\right)\\]"),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.MITTEL,
        "Die Güte \\(Q\\) eines RLC-Parallelschwingkreises berechnet sich als:",
        listOf("\\(Q = R\\sqrt{\\frac{C}{L}}\\)", "\\(Q = \\frac{1}{R}\\sqrt{\\frac{L}{C}}\\)", "\\(Q = \\frac{\\omega_0 L}{R}\\)", "\\(Q = \\frac{R}{\\omega_0 L}\\)"),
        0, "Für den Parallelschwingkreis:\n\\[Q = R \\cdot \\sqrt{\\frac{C}{L}} = \\frac{\\omega_0}{\\Delta\\omega} = \\frac{\\omega_0}{\\omega_1 - \\omega_2}\\]\nGroße Q → schmale Resonanzkurve."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.MITTEL,
        "Was ist die Impedanz \\(Z\\) eines LC-Parallelkreises (ohne R)?",
        listOf("\\(Z = \\frac{j\\omega L}{1 - \\omega^2 LC}\\)", "\\(Z = j\\omega L + \\frac{1}{j\\omega C}\\)", "\\(Z = \\frac{1}{j\\omega C}\\)", "\\(Z = j\\left(\\omega L - \\frac{1}{\\omega C}\\right)\\)"),
        0, "LC-Parallelschaltung:\n\\[Z = Z_L \\| Z_C = \\frac{j\\omega L \\cdot \\frac{1}{j\\omega C}}{j\\omega L + \\frac{1}{j\\omega C}} = \\frac{j\\omega L}{1 - \\omega^2 LC}\\]\nPol bei \\(\\omega_0 = 1/\\sqrt{LC}\\)."),
    // SCHWER
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.SCHWER,
        "Für welche Frequenzen \\(\\omega_1, \\omega_2\\) beträgt die Phasenverschiebung des Parallelschwingkreises \\(\\pm 45°\\)?",
        listOf("\\(\\text{Im}\\{Y\\} = \\pm\\text{Re}\\{Y\\}\\) lösen", "\\(\\text{Im}\\{Z\\} = 0\\) lösen", "\\(\\omega = \\omega_0\\)", "\\(|Y| = 1\\)"),
        0, "Bei \\(\\pm 45°\\) gilt \\(\\text{Im}\\{Y\\} = \\pm \\text{Re}\\{Y\\} = \\pm \\frac{1}{R}\\).\nDas ergibt eine quadratische Gleichung in \\(\\omega\\):\n\\[\\omega C - \\frac{1}{\\omega L} = \\pm\\frac{1}{R}\\]\nDiese Halbwertsfrequenzen bestimmen die Bandbreite \\(B = \\omega_1 - \\omega_2\\)."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.SCHWER,
        "Wie verändert sich die Ortskurve der Impedanz \\(Z_{RC}\\) einer R-C-Parallelschaltung geometrisch?",
        listOf("Halbkreis in der unteren Halbebene", "Gerade parallel zur Im-Achse", "Halbkreis in der oberen Halbebene", "Gerade parallel zur Re-Achse"),
        0, "\\(Z_{RC} = \\frac{R}{1+j\\omega RC}\\)\nParameterdarstellung zeigt: Dies ist ein Halbkreis mit Durchmesser R auf der reellen Achse.\nFür \\(\\omega: 0\\to\\infty\\) läuft Z von R nach 0 im Uhrzeigersinn — **untere Halbebene** (kapazitiv)."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.SCHWER,
        "Warum ist die Impedanzortskurve die Kehrwert-Abbildung der Admittanzortskurve?",
        listOf("Weil \\(Z = 1/Y\\) — Inversion am Einheitskreis", "Weil \\(Z = -Y\\) — Spiegelung", "Weil \\(Z = Y^2\\) — Quadrierung", "Kein Zusammenhang"),
        0, "\\(Z = \\frac{1}{Y}\\) ist eine konforme Abbildung (Möbius-Transformation).\nGeraden und Kreise werden auf Geraden und Kreise abgebildet.\nDaher: Gerade im Y-Bild → Kreis im Z-Bild und umgekehrt."),
    LENQuestion(LENQTopic.ORTSKURVE, LENQDifficulty.SCHWER,
        "Bei der RLC-Parallelschaltung: Was gilt für die Impedanz \\(Z\\) genau bei Resonanz?",
        listOf("\\(Z(\\omega_0) = R\\) (rein reell, maximal)", "\\(Z(\\omega_0) = 0\\)", "\\(Z(\\omega_0) = j\\omega_0 L\\)", "\\(Z(\\omega_0) = R + j\\omega_0 L\\)"),
        0, "Bei Resonanz heben sich Blind-Admittanzen auf (\\(\\text{Im}\\{Y\\}=0\\)).\nNur Leitwert \\(1/R\\) bleibt → \\(Z = R\\).\nDer Parallelschwingkreis hat bei \\(\\omega_0\\) maximale Impedanz.")
)

// ════════════════════════════════════════════
//  NETZWERKANALYSE
// ════════════════════════════════════════════

private val netzwerkQuestions = listOf(
    // LEICHT
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.LEICHT,
        "Was besagt die Kirchhoffsche Knotenregel (KCL)?",
        listOf("Die Summe aller Ströme an einem Knoten ist null", "Die Summe aller Spannungen in einer Masche ist null", "Strom ist proportional zur Spannung", "Leistung ist Spannung mal Strom"),
        0, "Kirchhoffs Knotenregel (1. Kirchhoffsches Gesetz):\n\\[\\sum_k I_k = 0\\]\nPhysikalische Grundlage: Ladungserhaltung. Zufließende Ströme = abfließende Ströme."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.LEICHT,
        "Was besagt die Kirchhoffsche Maschenregel (KVL)?",
        listOf("Die Summe aller Spannungen in einer Masche ist null", "Die Summe aller Ströme an einem Knoten ist null", "Alle Widerstände sind gleichwertig", "Der Strom ist in jeder Masche gleich"),
        0, "Kirchhoffs Maschenregel (2. Kirchhoffsches Gesetz):\n\\[\\sum_m U_m = 0\\]\nPhysikalische Grundlage: Energieerhaltung (konservatives Feld)."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.LEICHT,
        "Bei der Quellenumwandlung gilt für den Querstrom der Ersatzstromquelle:",
        listOf("\\(I_q = U_0 / R_i\\)", "\\(I_q = U_0 \\cdot R_i\\)", "\\(I_q = R_i / U_0\\)", "\\(I_q = U_0^2 / R_i\\)"),
        0, "Reale Spannungsquelle \\((U_0, R_i)\\) → Stromquelle \\((I_q, R_i)\\):\n\\[I_q = \\frac{U_0}{R_i}\\]\n\\(R_i\\) liegt jetzt **parallel** zur Stromquelle (vorher in Reihe)."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.LEICHT,
        "Bei Leistungsanpassung gilt für den Lastwiderstand \\(R_L\\):",
        listOf("\\(R_L = R_i\\)", "\\(R_L = 2R_i\\)", "\\(R_L \\gg R_i\\)", "\\(R_L = 0\\)"),
        0, "Leistungsanpassung: maximale Leistung an \\(R_L\\) wenn\n\\[R_L = R_i\\]\nDann ist \\(\\eta = 50\\%\\) — die Hälfte der Quellleistung wird in \\(R_i\\) verbraucht."),
    // MITTEL
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.MITTEL,
        "Stern-Dreieck-Transformation: \\(R_{12}\\) (Dreieckswiderstand) in Abhängigkeit der Sternwiderstände \\(R_1, R_2, R_3\\)?",
        listOf("\\(R_{12} = R_1 + R_2 + \\frac{R_1 R_2}{R_3}\\)", "\\(R_{12} = \\frac{R_1 R_2}{R_1 + R_2 + R_3}\\)", "\\(R_{12} = R_1 \\| R_2\\)", "\\(R_{12} = R_1 + R_2\\)"),
        0, "Stern → Dreieck:\n\\[R_{12} = R_1 + R_2 + \\frac{R_1 R_2}{R_3}\\]\nMerkregel: Summe der zwei anliegenden Sternwiderstände + Produkt durch dritten."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.MITTEL,
        "Was steht auf der Diagonalen der Leitwertsmatrix beim Knotenpotentialverfahren (KPV)?",
        listOf("Summe aller Leitwerte am Knoten", "Summe aller Quellströme am Knoten", "Negative Koppelleitwerte", "Determinante der Matrix"),
        0, "KPV-Matrix: Diagonalelement \\(Y_{ii}\\) = Summe aller Leitwerte, die an Knoten \\(i\\) angeschlossen sind.\nNeben-Elemente \\(Y_{ij}\\) = negativer Leitwert zwischen Knoten \\(i\\) und \\(j\\)."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.MITTEL,
        "Was steht auf der Diagonalen der Impedanzmatrix beim Maschenstromverfahren (MSV)?",
        listOf("Summe aller Impedanzen in der Masche", "Summe aller Quellspannungen", "Negative Koppelimpedanzen", "Leitwert der Masche"),
        0, "MSV-Matrix: Diagonalelement \\(Z_{ii}\\) = Summe aller Impedanzen in Masche \\(i\\).\nNeben-Elemente \\(Z_{ij}\\) = negative gemeinsame Impedanz der Maschen \\(i\\) und \\(j\\)."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.MITTEL,
        "Wie lautet die Cramersche Regel für \\(x_1\\) im System \\([A]\\vec{x} = \\vec{b}\\)?",
        listOf("\\(x_1 = \\frac{\\det(A_1)}{\\det(A)}\\)", "\\(x_1 = \\frac{\\det(A)}{\\det(A_1)}\\)", "\\(x_1 = \\det(A) \\cdot b_1\\)", "\\(x_1 = b_1 / a_{11}\\)"),
        0, "Cramersche Regel:\n\\[x_i = \\frac{\\det(A_i)}{\\det(A)}\\]\n\\(A_i\\) entsteht durch Ersetzen der \\(i\\)-ten Spalte von \\(A\\) durch den Vektor \\(\\vec{b}\\)."),
    // SCHWER
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.SCHWER,
        "Wie viele unabhängige Maschengleichungen gibt es bei \\(z\\) Zweigen und \\(k\\) Knoten?",
        listOf("\\(z - k + 1\\)", "\\(z + k - 1\\)", "\\(k - 1\\)", "\\(z\\)"),
        0, "Anzahl unabhängiger Maschen:\n\\[m = z - k + 1\\]\nDiese Maschen bilden einen Baum des Graphen. Für KPV gilt: \\(k - 1\\) unabhängige Knoten."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.SCHWER,
        "Was sagt das Thevenin-Theorem (Ersatzspannungsquelle)?",
        listOf("Jedes lineare Netzwerk kann durch \\(U_{Th}\\) und \\(R_{Th}\\) in Reihe ersetzt werden", "Jedes Netzwerk hat denselben Wirkungsgrad", "Die Leerlaufspannung ist null", "Netzwerke können nur mit Superposition gelöst werden"),
        0, "Thevenin-Theorem:\nJedes lineare Zweipol-Netzwerk verhält sich wie eine ideale Spannungsquelle \\(U_{Th}\\) (= Leerlaufspannung) in Reihe mit dem Innenwiderstand \\(R_{Th}\\) (= Widerstand bei abgeschalteten Quellen)."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.SCHWER,
        "Wie berechnet man den Innenwiderstand \\(R_i\\) einer realen Quelle aus Messungen?",
        listOf("\\(R_i = U_{Leer} / I_{Kurz}\\)", "\\(R_i = U_{Leer} \\cdot I_{Kurz}\\)", "\\(R_i = I_{Kurz} / U_{Leer}\\)", "\\(R_i = U_{Leer}^2 / P_{max}\\)"),
        0, "Leerlaufspannung messen: \\(U_0 = U_{Leer}\\)\nKurzschlussstrom messen: \\(I_K = I_{Kurz}\\)\nDann:\n\\[R_i = \\frac{U_0}{I_K}\\]\nAlternativ: Zwei Lastpunkte messen und aus \\(U(I)\\) die Gerade berechnen."),
    LENQuestion(LENQTopic.NETZWERK, LENQDifficulty.SCHWER,
        "Dreieck → Stern-Transformation: \\(R_1\\) in Abhängigkeit der Dreieckswiderstände \\(R_{12}, R_{23}, R_{31}\\)?",
        listOf("\\(R_1 = \\frac{R_{12} \\cdot R_{31}}{R_{12} + R_{23} + R_{31}}\\)", "\\(R_1 = R_{12} + R_{31} + \\frac{R_{12} R_{31}}{R_{23}}\\)", "\\(R_1 = R_{12} \\| R_{31}\\)", "\\(R_1 = \\frac{R_{23}}{R_{12} + R_{31}}\\)"),
        0, "Dreieck → Stern:\n\\[R_1 = \\frac{R_{12} \\cdot R_{31}}{R_{12} + R_{23} + R_{31}}\\]\nMerkregel: Produkt der zwei anliegenden Dreieckswiderstände geteilt durch Summe aller drei.")
)

// ════════════════════════════════════════════
//  WECHSELSTROMLEHRE
// ════════════════════════════════════════════

private val wechselstromQuestions = listOf(
    // LEICHT
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.LEICHT,
        "Wie lautet die komplexe Scheinleistung \\(S\\)?",
        listOf("\\(S = U \\cdot I^*\\)", "\\(S = U \\cdot I\\)", "\\(S = U^* \\cdot I\\)", "\\(S = |U| \\cdot |I|\\)"),
        0, "Komplexe Scheinleistung:\n\\[S = U \\cdot I^* = P + jQ\\]\nDas konjugiert-komplexe \\(I^*\\) ist entscheidend für das Vorzeichen der Blindleistung."),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.LEICHT,
        "Was ist die Wirkleistung P bei gegebener Scheinleistung S?",
        listOf("\\(P = \\text{Re}\\{S\\}\\)", "\\(P = \\text{Im}\\{S\\}\\)", "\\(P = |S|\\)", "\\(P = S / \\cos\\varphi\\)"),
        0, "Wirkleistung ist der Realteil der Scheinleistung:\n\\[P = \\text{Re}\\{S\\} = |U||I|\\cos\\varphi\\]\nNur die Wirkleistung leistet echte Arbeit."),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.LEICHT,
        "Was ist der Effektivwert \\(U\\) einer Sinusspannung mit Amplitude \\(\\hat{U}\\)?",
        listOf("\\(U = \\hat{U} / \\sqrt{2}\\)", "\\(U = \\hat{U} \\cdot \\sqrt{2}\\)", "\\(U = \\hat{U} / 2\\)", "\\(U = \\hat{U} \\cdot \\pi\\)"),
        0, "Effektivwert (RMS) einer Sinusspannung:\n\\[U = \\frac{\\hat{U}}{\\sqrt{2}}\\]\nBei Netzspannung 230 V (Effektivwert) ist \\(\\hat{U} \\approx 325\\) V."),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.LEICHT,
        "Eine induktive Last hat positiven Imaginärteil der Impedanz. Was gilt für die Blindleistung Q?",
        listOf("\\(Q > 0\\) (induktiv)", "\\(Q < 0\\) (kapazitiv)", "\\(Q = 0\\)", "\\(Q = P\\)"),
        0, "Bei induktiver Last (\\(Z = R + jX\\), \\(X > 0\\)):\n\\[Q = \\text{Im}\\{S\\} = |U||I|\\sin\\varphi > 0\\]\nKapazitiv: \\(Q < 0\\). Induktiv: \\(Q > 0\\) (nach Konvention in LEN)."),
    // MITTEL
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.MITTEL,
        "Was gilt für die Übertragungsgleichungen eines idealen Transformators mit Übersetzungsverhältnis \\(ü = N_1/N_2\\)?",
        listOf("\\(U_2 = U_1/ü\\) und \\(I_2 = I_1 \\cdot ü\\)", "\\(U_2 = U_1 \\cdot ü\\) und \\(I_2 = I_1 / ü\\)", "\\(U_2 = U_1\\) und \\(I_2 = I_1\\)", "\\(U_2 = U_1/ü\\) und \\(I_2 = I_1 / ü\\)"),
        0, "Idealer Transformator:\n\\[U_2 = \\frac{U_1}{ü}, \\quad I_2 = I_1 \\cdot ü\\]\nLeistung bleibt erhalten: \\(U_1 I_1 = U_2 I_2\\)\n\\(ü > 1\\): Abwärtstransformator, \\(ü < 1\\): Aufwärts."),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.MITTEL,
        "Was ist der Leistungsfaktor \\(\\cos\\varphi\\) und was beschreibt er?",
        listOf("\\(\\cos\\varphi = P/|S|\\) — Anteil der Wirkleistung", "\\(\\cos\\varphi = Q/|S|\\) — Blindleistungsanteil", "\\(\\cos\\varphi = |S|/P\\)", "\\(\\cos\\varphi = R/|Z|\\) immer"),
        0, "Leistungsfaktor:\n\\[\\cos\\varphi = \\frac{P}{|S|} = \\frac{P}{\\sqrt{P^2+Q^2}}\\]\nBei rein ohmscher Last: \\(\\cos\\varphi = 1\\). Bei rein reaktiver Last: \\(\\cos\\varphi = 0\\)."),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.MITTEL,
        "Wann ist ein Vierpol kopplungssymmetrisch?",
        listOf("\\(\\det([A]) = 1\\)", "\\([A]\\) ist symmetrisch \\((a_{12} = a_{21})\\)", "\\([A] = [A]^T\\)", "Wenn \\(Z_{11} = Z_{22}\\)"),
        0, "Kopplungssymmetrie: \\(\\det([A]) = a_{11}a_{22} - a_{12}a_{21} = 1\\)\nFolge: Eingang und Ausgang sind vertauschbar.\nKettenschaltung: \\(\\det([A_1 A_2]) = \\det([A_1]) \\cdot \\det([A_2]) = 1\\cdot 1 = 1\\)"),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.MITTEL,
        "Was ist die Kettenmatrix \\([A]\\) eines Serienzweigs mit Impedanz \\(Z\\)?",
        listOf("\\(\\begin{pmatrix}1 & Z \\\\ 0 & 1\\end{pmatrix}\\)", "\\(\\begin{pmatrix}1 & 0 \\\\ Z & 1\\end{pmatrix}\\)", "\\(\\begin{pmatrix}Z & 0 \\\\ 0 & Z\\end{pmatrix}\\)", "\\(\\begin{pmatrix}0 & Z \\\\ Z & 0\\end{pmatrix}\\)"),
        0, "Serienzweig \\(Z\\):\n\\[[A] = \\begin{pmatrix}1 & Z \\\\ 0 & 1\\end{pmatrix}\\]\nParallelzweig \\(Y\\):\n\\[[A] = \\begin{pmatrix}1 & 0 \\\\ Y & 1\\end{pmatrix}\\]"),
    // SCHWER
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.SCHWER,
        "Wie lautet die Blindleistung \\(Q\\) für eine RLC-Reihenschaltung?",
        listOf("\\(Q = I^2(\\omega L - 1/(\\omega C))\\)", "\\(Q = I^2 R\\)", "\\(Q = U^2 / R\\)", "\\(Q = \\omega L \\cdot I\\)"),
        0, "Blindleistung:\n\\[Q = I^2 X = I^2\\left(\\omega L - \\frac{1}{\\omega C}\\right)\\]\nBei Resonanz \\(Q = 0\\), da \\(X_L = X_C\\)."),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.SCHWER,
        "Was ist die Impedanzmatrix \\([Z]\\) eines Zweitors und welche Einträge hat sie?",
        listOf("\\(Z_{ij}\\) = Leerlauf-Eingangsimpedanz: \\(U_i/I_j\\) bei \\(I_{k\\neq j}=0\\)", "\\(Z_{ij}\\) = Kurzschluss-Übertragungsimpedanz", "\\([Z] = [Y]\\)", "\\(Z_{ij}\\) = \\(R_{ij}\\) ohne Blindanteile"),
        0, "Impedanzmatrix:\n\\[\\begin{pmatrix}U_1\\\\U_2\\end{pmatrix} = \\begin{pmatrix}Z_{11}&Z_{12}\\\\Z_{21}&Z_{22}\\end{pmatrix}\\begin{pmatrix}I_1\\\\I_2\\end{pmatrix}\\]\n\\(Z_{11}\\): Eingangsimpedanz bei Leerlauf am Ausgang."),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.SCHWER,
        "Wie berechnet man aus der Kettenmatrix \\([A]\\) die Übertragungsfunktion \\(U_2/U_1\\) bei Last \\(Z_L\\)?",
        listOf("\\(U_2/U_1 = Z_L / (a_{11} Z_L + a_{12})\\)", "\\(U_2/U_1 = a_{11}\\)", "\\(U_2/U_1 = 1/a_{22}\\)", "\\(U_2/U_1 = a_{12}/a_{11}\\)"),
        0, "Mit Kettenmatrix und Last \\(Z_L\\) (\\(I_2 = U_2/Z_L\\)):\n\\[U_1 = a_{11} U_2 + a_{12} I_2 = a_{11} U_2 + \\frac{a_{12}}{Z_L} U_2\\]\n\\[\\Rightarrow \\frac{U_2}{U_1} = \\frac{Z_L}{a_{11} Z_L + a_{12}}\\]"),
    LENQuestion(LENQTopic.WECHSELSTROM, LENQDifficulty.SCHWER,
        "Was versteht man unter Kompensation der Blindleistung?",
        listOf("Hinzufügen von \\(C\\) oder \\(L\\) um \\(Q_{ges} \\to 0\\) zu bringen", "Leistung auf \\(P = 0\\) reduzieren", "Erhöhung des Wirkungsgrads auf 100%", "Angleichung von \\(R_L = R_i\\)"),
        0, "Kompensation:\n\\[Q_C = -Q_L \\implies Q_{ges} = 0\\]\nKondensator parallel zu induktiver Last: \\(C = Q_L / (\\omega U^2)\\)\nVorteil: \\(|S| = P\\), Leitungsströme sinken.")
)

// ════════════════════════════════════════════
//  BODEDIAGRAMM
// ════════════════════════════════════════════

private val bodeQuestions = listOf(
    // LEICHT
    LENQuestion(LENQTopic.BODE, LENQDifficulty.LEICHT,
        "Wie lautet die normierte Übertragungsfunktion eines Tiefpasses 1. Ordnung?",
        listOf("\\(G(j\\Omega) = \\frac{1}{j\\Omega + 1}\\)", "\\(G(j\\Omega) = \\frac{j\\Omega}{j\\Omega + 1}\\)", "\\(G(j\\Omega) = j\\Omega + 1\\)", "\\(G(j\\Omega) = \\frac{1}{(j\\Omega)^2 + 1}\\)"),
        0, "Tiefpass 1. Ordnung (normiert):\n\\[G(j\\Omega) = \\frac{1}{j\\Omega + 1}, \\quad \\Omega = \\frac{\\omega}{\\omega_n}\\]\nBei \\(\\Omega = 1\\): \\(|G| = 1/\\sqrt{2}\\) (\\(-3\\) dB Grenzfrequenz)."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.LEICHT,
        "Wie lautet die normierte Übertragungsfunktion eines Hochpasses 1. Ordnung?",
        listOf("\\(G(j\\Omega) = \\frac{j\\Omega}{j\\Omega + 1}\\)", "\\(G(j\\Omega) = \\frac{1}{j\\Omega + 1}\\)", "\\(G(j\\Omega) = \\frac{(j\\Omega)^2}{(j\\Omega)^2 + 1}\\)", "\\(G(j\\Omega) = j\\Omega\\)"),
        0, "Hochpass 1. Ordnung (normiert):\n\\[G(j\\Omega) = \\frac{j\\Omega}{j\\Omega + 1}\\]\nBei \\(\\Omega \\to 0\\): \\(G \\to 0\\). Bei \\(\\Omega \\to \\infty\\): \\(G \\to 1\\)."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.LEICHT,
        "Was bedeutet \\(-3\\) dB in der Grenzfrequenz?",
        listOf("\\(|G| = 1/\\sqrt{2} \\approx 0{,}707\\)", "\\(|G| = 0\\)", "\\(|G| = 1/2\\)", "\\(|G| = \\sqrt{2}\\)"),
        0, "\\(-3\\) dB-Grenzfrequenz:\n\\[20\\log_{10}\\left(\\frac{1}{\\sqrt{2}}\\right) \\approx -3{,}01\\text{ dB}\\]\nDie Signalleistung ist auf die Hälfte gesunken."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.LEICHT,
        "Welcher Amplitudenabfall gilt für einen Filter 1. Ordnung außerhalb der Durchlasskurve?",
        listOf("\\(-20\\) dB/Dekade", "\\(-40\\) dB/Dekade", "\\(-10\\) dB/Dekade", "\\(-6\\) dB/Oktave — nur bei 2. Ordnung"),
        0, "Filter 1. Ordnung: \\(-20\\) dB/Dekade (= \\(-6\\) dB/Oktave)\nFilter 2. Ordnung: \\(-40\\) dB/Dekade (= \\(-12\\) dB/Oktave)\nAllgemein: \\(-20n\\) dB/Dekade für n-te Ordnung."),
    // MITTEL
    LENQuestion(LENQTopic.BODE, LENQDifficulty.MITTEL,
        "Was ist die Normierungsfrequenz \\(\\Omega\\) beim Bodediagramm?",
        listOf("\\(\\Omega = \\omega / \\omega_n\\) (dimensionslos)", "\\(\\Omega = \\omega \\cdot R\\cdot C\\)", "\\(\\Omega = 2\\pi f\\)", "\\(\\Omega = \\omega_0 / \\omega\\)"),
        0, "Normierung:\n\\[\\Omega = \\frac{\\omega}{\\omega_n}\\]\n\\(\\omega_n\\) ist die Normierungsfrequenz (oft \\(\\omega_g\\) oder \\(\\omega_0\\)).\nMacht \\(G(j\\Omega)\\) dimensionslos und universell vergleichbar."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.MITTEL,
        "Wie berechnet sich der Amplitudengang \\(a_v\\) in dB?",
        listOf("\\(a_v = 20 \\cdot \\log_{10}|G(j\\Omega)|\\)", "\\(a_v = \\log_{10}|G(j\\Omega)|\\)", "\\(a_v = 20 \\cdot \\ln|G(j\\Omega)|\\)", "\\(a_v = 10 \\cdot \\log_{10}|G|^2\\)"),
        0, "Amplitudengang:\n\\[a_v = 20\\log_{10}|G(j\\Omega)|\\text{ [dB]}\\]\nWichtig: Faktor 20, nicht 10! (\\(|G|\\) ist Spannungsverhältnis, Leistung wäre \\(|G|^2\\))."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.MITTEL,
        "Wie erkennt man einen Allpass im Bode-Diagramm?",
        listOf("Amplitudengang konstant 0 dB, aber Phasengang dreht", "Amplitudengang steigt mit 20 dB/Dek, Phase konstant", "Amplitudengang fällt mit −20 dB/Dek", "Amplitudengang hat ein Maximum bei \\(\\omega_0\\)"),
        0, "Allpass:\n- Amplitudengang: konstant \\(0\\) dB für alle Frequenzen\n- Phasengang: dreht von 0° nach −180° (1. Ordnung)\nTypische Anwendung: Phasenkorrektur ohne Amplitudenänderung."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.MITTEL,
        "Warum addiert man beim Bode-Diagramm die Teilkurven?",
        listOf("Weil \\(|G_{ges}| = \\prod|G_i|\\) und \\(\\log\\) des Produkts = Summe der \\(\\log\\)", "Weil die Schaltungen in Reihe liegen", "Weil Phasen sich immer addieren", "Weil dB eine lineare Skala ist"),
        0, "Kettenschaltung: \\(G_{ges} = G_1 \\cdot G_2 \\cdot \\ldots\\)\n\\[a_{v,ges} = 20\\log|G_{ges}| = \\sum_i 20\\log|G_i| = \\sum_i a_{v,i}\\]\nGleichung gilt auch für den Phasengang (Argument-Addition)."),
    // SCHWER
    LENQuestion(LENQTopic.BODE, LENQDifficulty.SCHWER,
        "Was beschreibt die Güte \\(Q\\) eines Bandpasses im Bode-Diagramm?",
        listOf("\\(Q = \\omega_0 / B\\) — Schmalheit der Resonanz", "\\(Q = B / \\omega_0\\) — Breite", "\\(Q = |G(\\omega_0)|\\)", "\\(Q = -20\\log|G_{max}|\\)"),
        0, "Bandbreite \\(B = \\omega_2 - \\omega_1\\) (−3 dB-Punkte):\n\\[Q = \\frac{\\omega_0}{B}\\]\nGroßes Q: schmaler Bandpass, starke Resonanzüberhöhung.\nKleines Q: breiter Bandpass."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.SCHWER,
        "Wie sieht der Phasengang eines Tiefpasses 1. Ordnung aus für \\(\\Omega \\to 0\\) und \\(\\Omega \\to \\infty\\)?",
        listOf("\\(0°\\) bei \\(\\Omega=0\\), \\(-90°\\) bei \\(\\Omega\\to\\infty\\)", "\\(-90°\\) bei \\(\\Omega=0\\), \\(0°\\) bei \\(\\Omega\\to\\infty\\)", "\\(0°\\) bei \\(\\Omega=0\\), \\(-180°\\) bei \\(\\Omega\\to\\infty\\)", "\\(+90°\\) bei \\(\\Omega=0\\), \\(0°\\) bei \\(\\Omega\\to\\infty\\)"),
        0, "Tiefpass \\(G = 1/(j\\Omega+1)\\):\n\\[\\varphi = -\\arctan(\\Omega)\\]\nBei \\(\\Omega=0\\): \\(\\varphi=0°\\)\nBei \\(\\Omega=1\\): \\(\\varphi=-45°\\)\nBei \\(\\Omega\\to\\infty\\): \\(\\varphi\\to-90°\\)"),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.SCHWER,
        "Wie erkennt man einen Bandpass im Amplitudengang?",
        listOf("Anstieg bei tiefen Frequenzen + Abfall bei hohen, Maximum bei \\(\\omega_0\\)", "Nur Abfall bei hohen Frequenzen", "Konstanter Amplitudengang", "Abfall bei allen Frequenzen"),
        0, "Bandpass: Kombination aus Hochpass (Anstieg +20 dB/Dek) und Tiefpass (Abfall −20 dB/Dek).\nMaximum bei \\(\\omega_0 = \\sqrt{\\omega_{g1} \\cdot \\omega_{g2}}\\).\nGesamtabfall \\(-40\\) dB/Dek für 2. Ordnung."),
    LENQuestion(LENQTopic.BODE, LENQDifficulty.SCHWER,
        "Welche Phase dreht ein Allpass 1. Ordnung insgesamt, und warum heißt er so?",
        listOf("−180°, weil \\(|G|=1\\) für alle \\(\\omega\\) aber \\(\\varphi\\) von 0° auf −180° dreht", "−90°, weil nur eine RC-Stufe", "0°, da kein Phasenhub", "+180°, invertierender Charakter"),
        0, "Allpass 1. Ordnung:\n\\[G = \\frac{1 - j\\Omega}{1 + j\\Omega}\\]\n\\(|G| = 1\\) fuer alle \\(\\Omega\\) - daher 'Allpass'.\n\\(\\varphi = -2\\arctan(\\Omega)\\): dreht von 0 Grad (\\(\\Omega=0\\)) auf -180 Grad (\\(\\Omega\\to\\infty\\)).")
)

// ════════════════════════════════════════════
//  OPERATIONSVERSTÄRKER
// ════════════════════════════════════════════

private val opvQuestions = listOf(
    // LEICHT
    LENQuestion(LENQTopic.OPV, LENQDifficulty.LEICHT,
        "Welche Eigenschaft hat der ideale OPV bezüglich des Eingangswiderstands?",
        listOf("\\(R_e = \\infty\\) (kein Strom fließt in den Eingang)", "\\(R_e = 0\\) (Kurzschluss)", "\\(R_e = R_N\\)", "\\(R_e\\) ist nicht definiert"),
        0, "Idealer OPV — wichtigste Eigenschaften:\n1. \\(R_e = \\infty\\) → Eingangsstrom \\(I_q = 0\\)\n2. \\(R_a = 0\\) → ideale Spannungsquelle am Ausgang\n3. \\(U_d = 0\\) bei Gegenkopplung\n4. Unendliche Leerlaufverstärkung \\(A_0 = \\infty\\)"),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.LEICHT,
        "Was gilt für die Differenzeingangsspannung \\(U_d\\) bei einem ideal gegengekoppelten OPV?",
        listOf("\\(U_d = 0\\) (virtueller Kurzschluss)", "\\(U_d = U_e\\)", "\\(U_d = U_a / A_0\\)", "\\(U_d\\) ist beliebig"),
        0, "Gegenkopplung + unendliche Verstärkung \\(A_0\\):\n\\[U_a = A_0 \\cdot U_d \\implies U_d = \\frac{U_a}{A_0} \\to 0\\]\nDieser 'virtuelle Kurzschluss' ist das zentrale Hilfsmittel zur OPV-Berechnung."),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.LEICHT,
        "Wie lautet die Verstärkung des invertierenden Verstärkers?",
        listOf("\\(G = -R_N / R_1\\)", "\\(G = 1 + R_N / R_1\\)", "\\(G = R_1 / R_N\\)", "\\(G = -R_1 / R_N\\)"),
        0, "Invertierender Verstärker:\n\\[G = \\frac{U_a}{U_e} = -\\frac{R_N}{R_1}\\]\nMinus-Zeichen: Spannung wird invertiert (Phasenumkehr um 180°).\nHerleitung aus virtuellem Kurzschluss und \\(I_q = 0\\)."),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.LEICHT,
        "Wie lautet die Verstärkung des nicht-invertierenden Verstärkers?",
        listOf("\\(G = 1 + R_N / R_1\\)", "\\(G = -R_N / R_1\\)", "\\(G = R_N / R_1\\)", "\\(G = R_1 / R_N\\)"),
        0, "Nicht-invertierender Verstärker:\n\\[G = 1 + \\frac{R_N}{R_1}\\]\nImmer \\(G \\geq 1\\). Kein Vorzeichenwechsel (0° Phasenverschiebung).\nSpannungsfolger: \\(R_N = 0\\) oder \\(R_1 = \\infty\\) → \\(G = 1\\)."),
    // MITTEL
    LENQuestion(LENQTopic.OPV, LENQDifficulty.MITTEL,
        "Was ist die Funktion des Spannungsfolgers (Impedanzwandler)?",
        listOf("\\(U_a = U_e\\), aber \\(R_e = \\infty\\) und \\(R_a = 0\\) entkoppeln Source und Last", "\\(U_a = -U_e\\) mit hoher Impedanz", "\\(U_a = 2 U_e\\) verstärken", "Strom verstärken, Spannung konstant lassen"),
        0, "Spannungsfolger: Gegenkopplung Ausgang direkt auf invertierten Eingang.\n\\(U_a = U_e\\), \\(G = 1\\) (0 dB), \\(\\varphi = 0°\\)\nVorteil: Quelle mit hohem \\(R_i\\) treibt Last ohne Spannungsabfall — Entkopplung."),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.MITTEL,
        "Was begrenzt den Ausgangsstrom eines realen OPV?",
        listOf("Die Aussteuerbegrenzung (interne Stromquelle ist begrenzt)", "Der Eingangswiderstand", "Die Gegenkopplung", "Das Gainbandbreiten-Produkt"),
        0, "Realer OPV: maximaler Ausgangsstrom \\(I_{a,max}\\) (typisch 20–40 mA).\nBei \\(I_L = U_a / R_L > I_{a,max}\\): Ausgang bricht ein (Sättigung).\nIm \\(I_L\\)-über-\\(1/R_L\\)-Diagramm: Knick bei \\(I_{a,max}\\)."),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.MITTEL,
        "Wie leitet man die Verstärkung \\(G = -R_N/R_1\\) des invertierenden Verstärkers her?",
        listOf("\\(U_d=0\\) → \\(U_-=0\\), KCL am \\((-\\))-Eingang: \\(U_e/R_1 + U_a/R_N = 0\\)", "\\(U_a = A_0 (U_+ - U_-)\\) direkt", "KVL in der Rückkopplungsmasche", "Superposition von \\(U_e\\) und \\(U_a\\)"),
        0, "Herleitung:\n1. \\(U_d = 0\\) → \\(U_- = U_+ = 0\\) (virtueller Kurzschluss)\n2. \\(I_q = 0\\) → KCL am (−)-Knoten:\n\\[\\frac{U_e - 0}{R_1} + \\frac{U_a - 0}{R_N} = 0\\]\n\\[\\Rightarrow G = -\\frac{R_N}{R_1}\\]"),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.MITTEL,
        "Was passiert, wenn die Versorgungsspannung des OPV überschritten wird?",
        listOf("Sättigung: \\(U_a\\) begrenzt auf \\(\\pm U_{cc}\\)", "OPV verstärkt weiter linear", "Verstärkung wird kleiner", "\\(U_d = 0\\) gilt nicht mehr"),
        0, "Aussteuerbegrenzung:\n\\[|U_a| \\leq U_{cc} - U_{sat}\\]\n\\(U_{sat}\\approx 1{,}5\\) V bei Standard-OPV. Der OPV geht in Sättigung — kein lineares Verhalten mehr.\nBeim Spannungsfolger: \\(U_a \\leq U_{cc}\\) → für \\(U_e > U_{cc}\\) gilt \\(U_a \\neq U_e\\)."),
    // SCHWER
    LENQuestion(LENQTopic.OPV, LENQDifficulty.SCHWER,
        "Was ist das Gainbandbreiten-Produkt (GBP) eines realen OPV?",
        listOf("\\(GBP = A_0 \\cdot f_g = const\\) — je mehr Verstärkung, desto weniger Bandbreite", "\\(GBP = A_0 / f_g\\)", "\\(GBP = f_g / A_0\\)", "GBP ist nur bei Tiefpässen definiert"),
        0, "Realer OPV: offene Schleifenverstärkung \\(A_0(f)\\) fällt mit \\(-20\\) dB/Dek ab.\n\\[GBP = |G| \\cdot f_{g,ges} = \\text{const}\\]\nMit Rückkopplung \\(G = -R_N/R_1\\): Bandbreite = \\(GBP / |G|\\).\nHohe Verstärkung → geringe Bandbreite."),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.SCHWER,
        "Wie funktioniert ein invertierender Summierverstärker mit Eingängen \\(U_1, U_2\\)?",
        listOf("\\(U_a = -R_N(U_1/R_1 + U_2/R_2)\\)", "\\(U_a = U_1 + U_2\\)", "\\(U_a = R_N/(R_1+R_2) \\cdot (U_1+U_2)\\)", "\\(U_a = -U_1 - U_2\\)"),
        0, "KCL am (−)-Eingang (virtueller Kurzschluss):\n\\[\\frac{U_1}{R_1} + \\frac{U_2}{R_2} + \\frac{U_a}{R_N} = 0\\]\n\\[\\Rightarrow U_a = -R_N\\left(\\frac{U_1}{R_1} + \\frac{U_2}{R_2}\\right)\\]\nBei \\(R_1 = R_2 = R\\): \\(U_a = -\\frac{R_N}{R}(U_1 + U_2)\\)"),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.SCHWER,
        "Wie unterscheidet sich ein Komparator von einem linearen OPV-Verstärker?",
        listOf("Komparator hat keine Gegenkopplung — Ausgang schaltet digital \\(\\pm U_{sat}\\)", "Komparator hat \\(G = 1\\)", "Komparator hat unendliche Bandbreite", "Komparator ist nur mit Negativrückkopplung sinnvoll"),
        0, "Komparator: OPV ohne Gegenkopplung (Open-Loop).\n\\[U_a = \\begin{cases}+U_{sat} & U_+ > U_-\\\\-U_{sat} & U_+ < U_-\\end{cases}\\]\nKein linearer Betrieb — der Ausgang ist binär (High/Low).\nAnwendung: Schwellenwertdetektoren, ADC."),
    LENQuestion(LENQTopic.OPV, LENQDifficulty.SCHWER,
        "Warum braucht man einen OPV als Impedanzwandler zwischen zwei Schaltungsstufen?",
        listOf("Damit \\(R_{a,St1} \\ll R_{e,St2}\\) nicht verletzt wird und kein Spannungsabfall entsteht", "Um die Spannung zu verdoppeln", "Um Gleichtaktstörungen zu unterdrücken", "Um die Phase zu drehen"),
        0, "Ohne Impedanzwandler: Spannungsteiler zwischen \\(R_{a,St1}\\) und \\(R_{e,St2}\\).\nMit Spannungsfolger:\n- \\(R_e = \\infty\\) → keine Belastung von Stufe 1\n- \\(R_a = 0\\) → ideale Treiberquelle für Stufe 2\nBodediagramm-Aufgaben: OPV als Pufferstufe zwischen HP und TP nötig.")
)

// ════════════════════════════════════════════
//  Question Pool
// ════════════════════════════════════════════

private val allLENQuestions: Map<LENQTopic, List<LENQuestion>> = mapOf(
    LENQTopic.ORTSKURVE to ortskurveQuestions,
    LENQTopic.NETZWERK to netzwerkQuestions,
    LENQTopic.WECHSELSTROM to wechselstromQuestions,
    LENQTopic.BODE to bodeQuestions,
    LENQTopic.OPV to opvQuestions
)

private fun getLENQuestions(topic: LENQTopic?, difficulty: LENQDifficulty?): List<LENQuestion> {
    val pool = if (topic == null) allLENQuestions.values.flatten() else allLENQuestions[topic] ?: emptyList()
    return if (difficulty == null) pool else pool.filter { it.difficulty == difficulty }
}

private fun generateLENQuestion(topic: LENQTopic?, difficulty: LENQDifficulty?): LENQuestion? {
    val pool = getLENQuestions(topic, difficulty)
    if (pool.isEmpty()) return null
    val q = pool.random()
    val shuffled = q.options.shuffled()
    val newCorrect = shuffled.indexOf(q.options[q.correctIndex])
    return q.copy(options = shuffled, correctIndex = newCorrect)
}

// ════════════════════════════════════════════
//  Main Screen
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LENTrainerScreen(onBackClick: () -> Unit) {
    var selectedTopic by remember { mutableStateOf<LENQTopic?>(null) }
    var selectedDifficulty by remember { mutableStateOf<LENQDifficulty?>(null) }
    var currentQuestion by remember { mutableStateOf<LENQuestion?>(null) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var showSolution by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var totalAnswered by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var showDifficultySelect by remember { mutableStateOf(false) }

    fun startQuiz() {
        currentQuestion = generateLENQuestion(selectedTopic, selectedDifficulty)
        selectedAnswer = -1
        showSolution = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            currentQuestion != null -> "${currentQuestion!!.topic.displayName} (${currentQuestion!!.difficulty.displayName})"
                            showDifficultySelect -> selectedTopic?.displayName ?: "Alle Themen"
                            else -> "LEN Trainer"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = if (currentQuestion != null) 13.sp else 18.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showSolution -> { showSolution = false; selectedAnswer = -1; currentQuestion = generateLENQuestion(selectedTopic, selectedDifficulty) }
                            currentQuestion != null -> { currentQuestion = null; showDifficultySelect = true }
                            showDifficultySelect -> { showDifficultySelect = false; selectedTopic = null; selectedDifficulty = null }
                            else -> onBackClick()
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
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
            currentQuestion != null -> LENQuestionView(
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
                onNext = { showSolution = false; selectedAnswer = -1; currentQuestion = generateLENQuestion(selectedTopic, selectedDifficulty) },
                modifier = Modifier.padding(padding)
            )
            showDifficultySelect -> LENDifficultySelection(
                topic = selectedTopic,
                onDifficultySelected = { diff -> selectedDifficulty = diff; score = 0; totalAnswered = 0; streak = 0; startQuiz() },
                modifier = Modifier.padding(padding)
            )
            else -> LENTopicSelection(
                onTopicSelected = { topic -> selectedTopic = topic; showDifficultySelect = true },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ════════════════════════════════════════════
//  Topic Selection
// ════════════════════════════════════════════

@Composable
private fun LENTopicSelection(onTopicSelected: (LENQTopic?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("LEN Thema wählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Text("KIT · Prof. Dössel · Klausurrelevante Konzepte", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))

        Surface(onClick = { onTopicSelected(null) }, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 4.dp) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column {
                    Text("Alle Themen gemischt", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val total = allLENQuestions.values.sumOf { it.size }
                    Text("$total Fragen aus allen Gebieten", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Einzelne Themen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        LENQTopic.entries.forEach { topic ->
            val count = allLENQuestions[topic]?.size ?: 0
            Surface(onClick = { onTopicSelected(topic) }, modifier = Modifier.fillMaxWidth().height(76.dp), shape = RoundedCornerShape(16.dp), color = topic.color.copy(alpha = 0.12f)) {
                Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(topic.icon, null, Modifier.size(32.dp), tint = topic.color)
                    Column {
                        Text(topic.displayName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = topic.color)
                        val pct = when (topic) {
                            LENQTopic.ORTSKURVE -> "~20%"; LENQTopic.NETZWERK -> "~14%"
                            LENQTopic.WECHSELSTROM -> "~21%"; LENQTopic.BODE -> "~24%"; LENQTopic.OPV -> "~15%"
                        }
                        Text("$count Fragen · Klausurgewicht $pct", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Basierend auf: LEN Altklausuren WS17/18 – SS22\n10 Klausuren, 94 Punkte, nur Konzeptfragen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ════════════════════════════════════════════
//  Difficulty Selection
// ════════════════════════════════════════════

@Composable
private fun LENDifficultySelection(topic: LENQTopic?, onDifficultySelected: (LENQDifficulty?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Schwierigkeit wählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Thema: ${topic?.displayName ?: "Alle Themen"}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        Surface(onClick = { onDifficultySelected(null) }, modifier = Modifier.fillMaxWidth().height(76.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column {
                    Text("Alle Schwierigkeiten", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${getLENQuestions(topic, null).size} Fragen", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LENQDifficulty.entries.forEach { diff ->
            val count = getLENQuestions(topic, diff).size
            Surface(onClick = { if (count > 0) onDifficultySelected(diff) }, modifier = Modifier.fillMaxWidth().height(76.dp), shape = RoundedCornerShape(16.dp), color = diff.color.copy(alpha = 0.12f)) {
                Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(diff.color), contentAlignment = Alignment.Center) {
                        val icon = when (diff) {
                            LENQDifficulty.LEICHT -> Icons.Default.SentimentSatisfied
                            LENQDifficulty.MITTEL -> Icons.Default.Psychology
                            LENQDifficulty.SCHWER -> Icons.Default.LocalFireDepartment
                        }
                        Icon(icon, null, Modifier.size(20.dp), tint = Color.White)
                    }
                    Column {
                        Text(diff.displayName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = diff.color)
                        Text(when (diff) {
                            LENQDifficulty.LEICHT -> "$count Fragen — Grundlagen & Definitionen"
                            LENQDifficulty.MITTEL -> "$count Fragen — Übungsblatt-Niveau"
                            LENQDifficulty.SCHWER -> "$count Fragen — Klausurniveau"
                        }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════
//  Question View
// ════════════════════════════════════════════

@Composable
private fun LENQuestionView(question: LENQuestion, selectedAnswer: Int, showSolution: Boolean, streak: Int, onAnswerSelected: (Int) -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        if (streak >= 3) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFF9800).copy(alpha = 0.15f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                    Text("$streak richtig in Folge!", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 14.sp)
                }
            }
        }

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

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            LENText(question.questionText, Modifier.padding(20.dp), fontSize = 17, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(4.dp))

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
            Surface(onClick = { onAnswerSelected(index) }, modifier = Modifier.fillMaxWidth().border(2.dp, borderColor, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), color = bgColor) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(
                        when { showSolution && isCorrect -> Color(0xFF4CAF50); showSolution && isSelected -> Color(0xFFF44336); else -> MaterialTheme.colorScheme.primaryContainer }
                    ), contentAlignment = Alignment.Center) {
                        when {
                            showSolution && isCorrect -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            showSolution && isSelected -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            else -> Text("${'A' + index}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    LENText(option, Modifier.weight(1f), fontSize = 15)
                }
            }
        }

        if (showSolution) {
            Spacer(Modifier.height(4.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f))) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                        Text("Lösung", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LENText(question.solutionExplanation, fontSize = 14)
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
