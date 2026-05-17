package de.sudokuonline.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.CoinReason
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// ════════════════════════════════════════════
//  EXAM TOPICS & DATA MODELS
// ════════════════════════════════════════════

private enum class ExamTopic(val displayName: String, val icon: ImageVector, val color: Color) {
    COMPLEX("Komplexe Zahlen", Icons.Default.Adjust, Color(0xFF9C27B0)),
    INDUCTION("Vollständige Induktion", Icons.Default.Repeat, Color(0xFF2196F3)),
    SERIES("Reihen & Konvergenz", Icons.Default.Functions, Color(0xFFFF9800)),
    CONTINUITY("Stetigkeit & Diff'barkeit", Icons.Default.Timeline, Color(0xFF4CAF50)),
    INTEGRATION("Integration", Icons.Default.ShowChart, Color(0xFFE91E63)),
    LINEAR_ALGEBRA("Lineare Algebra", Icons.Default.GridOn, Color(0xFF00BCD4)),
    TAYLOR("Taylor & Potenzreihen", Icons.Default.AutoGraph, Color(0xFF795548))
}

private data class ExamProblem(
    val id: String,
    val topic: ExamTopic,
    val points: Int,
    val questionLatex: String,
    val solutionSteps: List<String>,
    val hints: List<String>,
    val difficulty: Int // 1-3
)

private data class ExamState(
    val problems: List<ExamProblem>,
    val currentProblemIndex: Int = 0,
    val answers: MutableMap<String, String> = mutableMapOf(),
    val selfGradedPoints: MutableMap<String, Int> = mutableMapOf(),
    val startTime: Long = System.currentTimeMillis(),
    val totalTimeMinutes: Int = 90,
    val isFinished: Boolean = false,
    val showSolution: Boolean = false
)

// ════════════════════════════════════════════
//  EXAM PROBLEMS (from real KIT exams)
// ════════════════════════════════════════════

private val examProblems = listOf(
    // ══════ KOMPLEXE ZAHLEN ══════
    ExamProblem(
        id = "complex1",
        topic = ExamTopic.COMPLEX,
        points = 4,
        questionLatex = "Bestimmen Sie den Real- und Imaginärteil der Zahl \\((1 - \\sqrt{3}i)^{12}\\).",
        solutionSteps = listOf(
            "Betrag berechnen: \\(|1 - \\sqrt{3}i| = \\sqrt{1^2 + (\\sqrt{3})^2} = \\sqrt{4} = 2\\)",
            "Argument berechnen: \\(\\text{Im} < 0\\), also \\(\\theta = -\\arccos\\left(\\frac{1}{2}\\right) = -\\frac{\\pi}{3}\\)",
            "Polarform: \\(1 - \\sqrt{3}i = 2e^{-i\\pi/3}\\)",
            "Potenz: \\((2e^{-i\\pi/3})^{12} = 2^{12} \\cdot e^{-i \\cdot 4\\pi} = 4096 \\cdot e^{0} = 4096\\)",
            "Ergebnis: \\(\\text{Re} = 4096\\), \\(\\text{Im} = 0\\)"
        ),
        hints = listOf("Verwende Polarform", "e^{i·2πk} = 1 für k ∈ ℤ"),
        difficulty = 2
    ),
    ExamProblem(
        id = "complex2",
        topic = ExamTopic.COMPLEX,
        points = 4,
        questionLatex = "Zeigen Sie, dass \\(\\log_3(8) \\cdot \\log_{\\sqrt{8}}(9)\\) eine ganze Zahl ist und bestimmen Sie diese.",
        solutionSteps = listOf(
            "Umschreiben: \\(\\log_3(8) \\cdot \\log_{\\sqrt{8}}(9) = \\frac{\\ln 8}{\\ln 3} \\cdot \\frac{\\ln 9}{\\ln \\sqrt{8}}\\)",
            "Vereinfachen: \\(= \\frac{\\ln 8}{\\ln \\sqrt{8}} \\cdot \\frac{\\ln 9}{\\ln 3} = \\log_{\\sqrt{8}}(8) \\cdot \\log_3(9)\\)",
            "\\(\\log_{\\sqrt{8}}(8) = 2\\) da \\((\\sqrt{8})^2 = 8\\)",
            "\\(\\log_3(9) = 2\\) da \\(3^2 = 9\\)",
            "Ergebnis: \\(2 \\cdot 2 = 4\\)"
        ),
        hints = listOf("Basiswechselformel", "Vereinfache zu bekannten Logarithmen"),
        difficulty = 2
    ),

    // ══════ INDUKTION ══════
    ExamProblem(
        id = "induction1",
        topic = ExamTopic.INDUCTION,
        points = 4,
        questionLatex = "Sei \\(f: \\mathbb{R} \\to \\mathbb{R}\\), \\(f(x) = x^n\\), wobei \\(n \\in \\mathbb{N}\\). Zeigen Sie anhand der Definition der Ableitung, dass \\(f'(x) = nx^{n-1}\\).",
        solutionSteps = listOf(
            "Definition: \\(f'(x_0) = \\lim_{x \\to x_0} \\frac{f(x) - f(x_0)}{x - x_0} = \\lim_{x \\to x_0} \\frac{x^n - x_0^n}{x - x_0}\\)",
            "Verwende: \\(x^n - x_0^n = (x - x_0) \\sum_{k=0}^{n-1} x_0^k x^{n-1-k}\\)",
            "Kürzen: \\(= \\lim_{x \\to x_0} \\sum_{k=0}^{n-1} x_0^k x^{n-1-k}\\)",
            "Grenzwert: \\(= \\sum_{k=0}^{n-1} x_0^k x_0^{n-1-k} = \\sum_{k=0}^{n-1} x_0^{n-1} = n \\cdot x_0^{n-1}\\)"
        ),
        hints = listOf("Geometrische Summenformel für x^n - y^n", "Faktorisierung nutzen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "induction2",
        topic = ExamTopic.INDUCTION,
        points = 6,
        questionLatex = "Sei \\(f(x) = \\sqrt{x}\\) und \\(a_n = (-1)^{n+1} \\frac{(2n-2)!}{2^{2n-1}(n-1)!}\\). Zeigen Sie mit vollständiger Induktion, dass für alle \\(n \\in \\mathbb{N}\\) gilt:\n\\[f^{(n)}(x) = a_n x^{-\\frac{2n-1}{2}}\\]",
        solutionSteps = listOf(
            "IA (n=1): \\(f'(x) = \\frac{1}{2}x^{-1/2}\\) und \\(a_1 = (-1)^2 \\frac{0!}{2^1 \\cdot 0!} = \\frac{1}{2}\\) ✓",
            "IV: Annahme \\(f^{(n)}(x) = a_n x^{-\\frac{2n-1}{2}}\\) für ein \\(n \\in \\mathbb{N}\\)",
            "IS: \\(f^{(n+1)}(x) = (f^{(n)}(x))' = a_n \\cdot (-\\frac{2n-1}{2}) x^{-\\frac{2n-1}{2}-1}\\)",
            "\\(= a_n \\cdot (-1) \\cdot \\frac{2n-1}{2} \\cdot x^{-\\frac{2n+1}{2}}\\)",
            "Zeige: \\(a_n \\cdot (-1) \\cdot \\frac{2n-1}{2} = a_{n+1}\\) durch Einsetzen der Definition"
        ),
        hints = listOf("Induktionsanfang sorgfältig prüfen", "Beim IS die Kettenregel beachten"),
        difficulty = 3
    ),

    // ══════ REIHEN ══════
    ExamProblem(
        id = "series1",
        topic = ExamTopic.SERIES,
        points = 6,
        questionLatex = "Untersuchen Sie die folgende Reihe auf Konvergenz:\n\\[\\sum_{n=1}^{\\infty} \\left(\\sqrt{4+n^4} - n^2\\right)\\]",
        solutionSteps = listOf(
            "Trick: Erweitern mit konjugiertem Ausdruck",
            "\\(\\sqrt{4+n^4} - n^2 = \\frac{(\\sqrt{4+n^4} - n^2)(\\sqrt{4+n^4} + n^2)}{\\sqrt{4+n^4} + n^2}\\)",
            "\\(= \\frac{4+n^4 - n^4}{\\sqrt{4+n^4} + n^2} = \\frac{4}{\\sqrt{4+n^4} + n^2}\\)",
            "Abschätzung: \\(\\sqrt{4+n^4} \\geq n^2\\), also \\(\\frac{4}{\\sqrt{4+n^4} + n^2} \\leq \\frac{4}{2n^2} = \\frac{2}{n^2}\\)",
            "Da \\(\\sum \\frac{1}{n^2}\\) konvergiert, konvergiert auch die Reihe (Majorantenkriterium)"
        ),
        hints = listOf("Konjugiert erweitern: (a-b)(a+b) = a² - b²", "Vergleich mit bekannter konvergenter Reihe"),
        difficulty = 2
    ),
    ExamProblem(
        id = "series2",
        topic = ExamTopic.SERIES,
        points = 4,
        questionLatex = "Zeigen Sie unter Verwendung des Binomialsatzes, dass für alle \\(n \\in \\mathbb{N}\\) gilt:\n\\[\\frac{(2n)!}{n! \\cdot n!} < 2^{2n}\\]",
        solutionSteps = listOf(
            "Binomialsatz: \\(2^{2n} = (1+1)^{2n} = \\sum_{k=0}^{2n} \\binom{2n}{k}\\)",
            "Beobachtung: \\(\\binom{2n}{n} = \\frac{(2n)!}{n! \\cdot n!}\\) ist nur EIN Summand",
            "Da alle Binomialkoeffizienten positiv sind:",
            "\\(2^{2n} = \\sum_{k=0}^{2n} \\binom{2n}{k} > \\binom{2n}{n} = \\frac{(2n)!}{n! \\cdot n!}\\)"
        ),
        hints = listOf("Binomialsatz: (a+b)^n = Σ (n über k) a^k b^{n-k}", "Der mittlere Binomialkoeffizient"),
        difficulty = 2
    ),

    // ══════ ANALYSIS (Stetigkeit, Nullstellen) ══════
    ExamProblem(
        id = "analysis1",
        topic = ExamTopic.CONTINUITY,
        points = 8,
        questionLatex = "Zeigen Sie, dass die Funktion \\(f: \\mathbb{R} \\to \\mathbb{R}\\) mit \\(f(x) = xe^x - 1\\) genau eine Nullstelle besitzt. Bestimmen Sie außerdem das globale Minimum von \\(f\\).",
        solutionSteps = listOf(
            "Ableitung: \\(f'(x) = e^x + xe^x = (1+x)e^x\\)",
            "\\(f'(x) < 0\\) für \\(x < -1\\), \\(f'(x) > 0\\) für \\(x > -1\\)",
            "Also: \\(f\\) streng monoton fallend auf \\((-\\infty, -1)\\), steigend auf \\((-1, \\infty)\\)",
            "Minimum bei \\(x = -1\\): \\(f(-1) = -e^{-1} - 1 = -\\frac{1}{e} - 1 < 0\\)",
            "\\(f(0) = -1 < 0\\), \\(f(1) = e - 1 > 0\\)",
            "Nach ZWS: \\(\\exists c \\in (0,1)\\) mit \\(f(c) = 0\\)",
            "Da \\(f\\) auf \\((0, \\infty)\\) streng monoton steigend: genau eine Nullstelle"
        ),
        hints = listOf("Monotonieverhalten über f' bestimmen", "Zwischenwertsatz anwenden"),
        difficulty = 2
    ),

    // ══════ INTEGRATION ══════
    ExamProblem(
        id = "integration1",
        topic = ExamTopic.INTEGRATION,
        points = 6,
        questionLatex = "Bestimmen Sie den Wert des Integrals\n\\[\\int_1^4 \\frac{\\ln(\\sqrt{x})}{2\\sqrt{x}} \\, dx\\]\nSubstituieren Sie zunächst geeignet und integrieren Sie danach partiell.",
        solutionSteps = listOf(
            "Substitution: \\(u = \\sqrt{x}\\), \\(du = \\frac{1}{2\\sqrt{x}}dx\\)",
            "Grenzen: \\(x=1 \\Rightarrow u=1\\), \\(x=4 \\Rightarrow u=2\\)",
            "\\(\\int_1^4 \\frac{\\ln(\\sqrt{x})}{2\\sqrt{x}} dx = \\int_1^2 \\ln(u) \\, du\\)",
            "Partielle Integration: \\(\\int \\ln(u) du = u\\ln(u) - u\\)",
            "\\(= [u\\ln(u) - u]_1^2 = (2\\ln 2 - 2) - (1 \\cdot 0 - 1) = 2\\ln 2 - 1\\)"
        ),
        hints = listOf("u = √x wählen", "∫ ln(u) du = u·ln(u) - u"),
        difficulty = 2
    ),
    ExamProblem(
        id = "integration2",
        topic = ExamTopic.INTEGRATION,
        points = 6,
        questionLatex = "Zeigen Sie, dass das folgende uneigentliche Integral konvergiert:\n\\[\\int_1^{\\infty} \\frac{1}{\\sqrt{x^4-1}} \\, dx\\]",
        solutionSteps = listOf(
            "Aufteilen: \\(\\int_1^{\\infty} = \\int_1^{2} + \\int_2^{\\infty}\\)",
            "Für \\(x \\geq 2\\): \\(x^4 - 1 \\geq \\frac{x^4}{2}\\), also \\(\\frac{1}{\\sqrt{x^4-1}} \\leq \\frac{\\sqrt{2}}{x^2}\\)",
            "\\(\\int_2^{\\infty} \\frac{1}{x^2} dx\\) konvergiert \\(\\Rightarrow\\) Majorante",
            "Für \\(x \\in (1,2)\\): \\(x^4-1 = (x^2-1)(x^2+1) > (x-1)(x+1) \\cdot 1 = x-1\\)",
            "\\(\\frac{1}{\\sqrt{x^4-1}} < \\frac{1}{\\sqrt{x-1}}\\)",
            "\\(\\int_1^2 \\frac{1}{\\sqrt{x-1}} dx = 2\\sqrt{x-1}|_1^2 = 2\\) konvergiert",
            "Beide Teile konvergieren \\(\\Rightarrow\\) Integral konvergiert"
        ),
        hints = listOf("Aufteilen bei kritischer Stelle", "Verschiedene Majoranten für verschiedene Bereiche"),
        difficulty = 3
    ),

    // ══════ LINEARE ALGEBRA ══════
    ExamProblem(
        id = "la1",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 8,
        questionLatex = "Bestimmen Sie mittels Zeilenumformungen die Inverse der Matrix\n\\[A = \\begin{pmatrix} 1 & 2 \\\\ 2 & 6 \\end{pmatrix}\\]\nErklären Sie, warum der Algorithmus die inverse Matrix liefert.",
        solutionSteps = listOf(
            "Erweiterte Matrix: \\(\\begin{pmatrix} 1 & 2 & | & 1 & 0 \\\\ 2 & 6 & | & 0 & 1 \\end{pmatrix}\\)",
            "\\(Z_2 \\to Z_2 - 2Z_1\\): \\(\\begin{pmatrix} 1 & 2 & | & 1 & 0 \\\\ 0 & 2 & | & -2 & 1 \\end{pmatrix}\\)",
            "\\(Z_2 \\to \\frac{1}{2}Z_2\\): \\(\\begin{pmatrix} 1 & 2 & | & 1 & 0 \\\\ 0 & 1 & | & -1 & \\frac{1}{2} \\end{pmatrix}\\)",
            "\\(Z_1 \\to Z_1 - 2Z_2\\): \\(\\begin{pmatrix} 1 & 0 & | & 3 & -1 \\\\ 0 & 1 & | & -1 & \\frac{1}{2} \\end{pmatrix}\\)",
            "Also: \\(A^{-1} = \\begin{pmatrix} 3 & -1 \\\\ -1 & \\frac{1}{2} \\end{pmatrix}\\)",
            "Erklärung: Zeilenumformungen entsprechen Multiplikation mit Elementarmatrizen von links. Auf der rechten Seite entsteht das Produkt dieser Matrizen = \\(A^{-1}\\)."
        ),
        hints = listOf("(A|I) → (I|A⁻¹)", "Gauß-Jordan-Algorithmus"),
        difficulty = 2
    ),
    ExamProblem(
        id = "la2",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 6,
        questionLatex = "Sei \\(C = \\begin{pmatrix} 1 & 1 \\\\ 1 & 2 \\\\ 1 & 3 \\end{pmatrix}\\) und \\(D = \\{\\vec{b} \\in \\mathbb{R}^3 : C\\vec{x} = \\vec{b} \\text{ ist lösbar}\\}\\).\n\n(i) Zeigen Sie mittels der Definition, dass \\(D\\) ein Unterraum von \\(\\mathbb{R}^3\\) ist.\n\n(ii) Bestimmen Sie die Orthogonalprojektion von \\(\\vec{w} = \\begin{pmatrix} 4 \\\\ 0 \\\\ 2 \\end{pmatrix}\\) auf \\(D\\).",
        solutionSteps = listOf(
            "(i) \\(D = \\text{Bild}(C)\\), also Unterraum da:",
            "- \\(\\vec{0} \\in D\\) da \\(C\\vec{0} = \\vec{0}\\)",
            "- \\(\\vec{b}_1, \\vec{b}_2 \\in D \\Rightarrow \\exists \\vec{x}_1, \\vec{x}_2\\) mit \\(C\\vec{x}_i = \\vec{b}_i\\)",
            "- Dann \\(C(\\vec{x}_1 + \\vec{x}_2) = \\vec{b}_1 + \\vec{b}_2\\) und \\(C(\\lambda\\vec{x}_1) = \\lambda\\vec{b}_1\\)",
            "(ii) ONB von D mit Gram-Schmidt:",
            "- \\(\\vec{b}_1 = \\frac{1}{\\sqrt{3}}(1,1,1)^T\\)",
            "- \\(\\vec{b}_2 = \\frac{1}{\\sqrt{2}}(-1,0,1)^T\\)",
            "Projektion: \\((\\vec{w} \\cdot \\vec{b}_1)\\vec{b}_1 + (\\vec{w} \\cdot \\vec{b}_2)\\vec{b}_2 = 2(1,1,1)^T - 1(-1,0,1)^T = (3,2,1)^T\\)"
        ),
        hints = listOf("D = Bild(C)", "Gram-Schmidt für ONB, dann Projektion"),
        difficulty = 3
    ),
    ExamProblem(
        id = "la3",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 4,
        questionLatex = "Zeigen Sie, dass es keine Matrizen \\(A \\in \\mathbb{R}^{3 \\times 2}\\) und \\(B \\in \\mathbb{R}^{2 \\times 3}\\) gibt, sodass\n\\[AB = \\begin{pmatrix} 1 & 0 & 0 \\\\ 0 & 1 & 0 \\\\ 0 & 0 & 1 \\end{pmatrix}\\]\nHinweis: Die Dimensionsformel kann hilfreich sein.",
        solutionSteps = listOf(
            "Annahme: \\(AB = I_3\\)",
            "Dann: \\(\\text{Bild}(AB) = \\mathbb{R}^3\\), also \\(\\dim \\text{Bild}(AB) = 3\\)",
            "Aber: \\(A\\) hat nur 2 Spalten, also \\(\\dim \\text{Bild}(A) \\leq 2\\)",
            "Es gilt: \\(\\text{Bild}(AB) = \\{AB\\vec{v} : \\vec{v} \\in \\mathbb{R}^3\\} \\subseteq \\{A\\vec{y} : \\vec{y} \\in \\mathbb{R}^2\\} = \\text{Bild}(A)\\)",
            "Also: \\(\\dim \\text{Bild}(AB) \\leq \\dim \\text{Bild}(A) \\leq 2 < 3\\) — Widerspruch!"
        ),
        hints = listOf("Rang von AB ≤ min(Rang A, Rang B)", "Dimensionsformel"),
        difficulty = 2
    ),

    // ══════ TAYLOR ══════
    ExamProblem(
        id = "taylor1",
        topic = ExamTopic.TAYLOR,
        points = 6,
        questionLatex = "Sei \\(f(x) = \\sqrt{x}\\). Zeigen Sie mit Hilfe des Satzes von Taylor, dass für \\(x > 0\\) und \\(n \\in \\mathbb{N}\\) ein \\(c\\) zwischen 1 und \\(x\\) existiert mit:\n\\[f(x) = 1 + \\sum_{k=1}^{n} \\frac{a_k}{k!}(x-1)^k + \\frac{a_{n+1}}{(n+1)!} c^{-\\frac{2n+1}{2}}(x-1)^{n+1}\\]",
        solutionSteps = listOf(
            "Taylor mit Restglied (Lagrange): \\(f(x) = \\sum_{k=0}^{n} \\frac{f^{(k)}(1)}{k!}(x-1)^k + \\frac{f^{(n+1)}(c)}{(n+1)!}(x-1)^{n+1}\\)",
            "Mit \\(f^{(k)}(x) = a_k x^{-\\frac{2k-1}{2}}\\) (aus Induktion)",
            "\\(f^{(k)}(1) = a_k \\cdot 1 = a_k\\)",
            "\\(f(1) = \\sqrt{1} = 1\\)",
            "Restglied: \\(f^{(n+1)}(c) = a_{n+1} c^{-\\frac{2n+1}{2}}\\)"
        ),
        hints = listOf("Taylor-Formel mit Lagrange-Restglied", "Entwicklungspunkt x₀ = 1"),
        difficulty = 3
    ),

    // ════════════════════════════════════════════════════════════════════════
    //  NEUE AUFGABEN - KOMPLEXE ZAHLEN
    // ════════════════════════════════════════════════════════════════════════

    ExamProblem(
        id = "complex3",
        topic = ExamTopic.COMPLEX,
        points = 6,
        questionLatex = "Bestimmen Sie alle komplexen Lösungen der Gleichung\n\\[z^4 = -16\\]\nund stellen Sie diese in kartesischer Form \\(a + bi\\) dar.",
        solutionSteps = listOf(
            "Polarform von \\(-16\\): \\(-16 = 16 \\cdot e^{i\\pi}\\)",
            "Allgemein: \\(-16 = 16 \\cdot e^{i(\\pi + 2\\pi k)}\\) für \\(k \\in \\mathbb{Z}\\)",
            "Wurzeln: \\(z_k = \\sqrt[4]{16} \\cdot e^{i(\\pi + 2\\pi k)/4} = 2 \\cdot e^{i\\pi(1+2k)/4}\\)",
            "Für \\(k = 0\\): \\(z_0 = 2e^{i\\pi/4} = 2(\\cos\\frac{\\pi}{4} + i\\sin\\frac{\\pi}{4}) = \\sqrt{2} + i\\sqrt{2}\\)",
            "Für \\(k = 1\\): \\(z_1 = 2e^{i3\\pi/4} = -\\sqrt{2} + i\\sqrt{2}\\)",
            "Für \\(k = 2\\): \\(z_2 = 2e^{i5\\pi/4} = -\\sqrt{2} - i\\sqrt{2}\\)",
            "Für \\(k = 3\\): \\(z_3 = 2e^{i7\\pi/4} = \\sqrt{2} - i\\sqrt{2}\\)"
        ),
        hints = listOf("Schreibe -16 in Polarform", "Es gibt genau 4 verschiedene 4-te Wurzeln"),
        difficulty = 2
    ),
    ExamProblem(
        id = "complex4",
        topic = ExamTopic.COMPLEX,
        points = 5,
        questionLatex = "Berechnen Sie mit Hilfe des Satzes von de Moivre:\n\\[\\cos(3\\varphi) \\text{ und } \\sin(3\\varphi)\\]\nals Polynome in \\(\\cos(\\varphi)\\) und \\(\\sin(\\varphi)\\).",
        solutionSteps = listOf(
            "De Moivre: \\((\\cos\\varphi + i\\sin\\varphi)^3 = \\cos(3\\varphi) + i\\sin(3\\varphi)\\)",
            "Binomische Formel: \\((c + is)^3 = c^3 + 3c^2(is) + 3c(is)^2 + (is)^3\\)",
            "\\(= c^3 + 3ic^2s - 3cs^2 - is^3 = (c^3 - 3cs^2) + i(3c^2s - s^3)\\)",
            "Realteil: \\(\\cos(3\\varphi) = \\cos^3\\varphi - 3\\cos\\varphi\\sin^2\\varphi\\)",
            "Mit \\(\\sin^2\\varphi = 1 - \\cos^2\\varphi\\): \\(\\cos(3\\varphi) = 4\\cos^3\\varphi - 3\\cos\\varphi\\)",
            "Imaginärteil: \\(\\sin(3\\varphi) = 3\\cos^2\\varphi\\sin\\varphi - \\sin^3\\varphi\\)",
            "Mit \\(\\cos^2\\varphi = 1 - \\sin^2\\varphi\\): \\(\\sin(3\\varphi) = 3\\sin\\varphi - 4\\sin^3\\varphi\\)"
        ),
        hints = listOf("(cos φ + i sin φ)³ ausmultiplizieren", "Real- und Imaginärteil vergleichen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "complex5",
        topic = ExamTopic.COMPLEX,
        points = 4,
        questionLatex = "Berechnen Sie den Hauptwert des komplexen Logarithmus \\(\\text{Log}(z)\\) für\n\\[z = -e^2\\]\nund geben Sie das Ergebnis in der Form \\(a + bi\\) an.",
        solutionSteps = listOf(
            "Definition: \\(\\text{Log}(z) = \\ln|z| + i \\cdot \\text{Arg}(z)\\)",
            "Betrag: \\(|{-e^2}| = e^2\\)",
            "Hauptargument: \\(-e^2\\) liegt auf der negativen reellen Achse, also \\(\\text{Arg}(-e^2) = \\pi\\)",
            "Somit: \\(\\text{Log}(-e^2) = \\ln(e^2) + i\\pi = 2 + i\\pi\\)"
        ),
        hints = listOf("Log(z) = ln|z| + i·Arg(z)", "Arg ∈ (-π, π]"),
        difficulty = 1
    ),
    ExamProblem(
        id = "complex6",
        topic = ExamTopic.COMPLEX,
        points = 6,
        questionLatex = "Bestimmen Sie alle \\(z \\in \\mathbb{C}\\) mit\n\\[e^z = 1 + i\\]",
        solutionSteps = listOf(
            "Polarform von \\(1+i\\): \\(|1+i| = \\sqrt{2}\\), \\(\\arg(1+i) = \\frac{\\pi}{4}\\)",
            "Also: \\(1 + i = \\sqrt{2} \\cdot e^{i\\pi/4}\\)",
            "Setze \\(z = x + iy\\): \\(e^z = e^x \\cdot e^{iy}\\)",
            "Vergleich: \\(e^x = \\sqrt{2}\\) und \\(y = \\frac{\\pi}{4} + 2\\pi k\\) für \\(k \\in \\mathbb{Z}\\)",
            "\\(x = \\ln(\\sqrt{2}) = \\frac{1}{2}\\ln 2\\)",
            "Lösung: \\(z = \\frac{\\ln 2}{2} + i\\left(\\frac{\\pi}{4} + 2\\pi k\\right)\\) für \\(k \\in \\mathbb{Z}\\)"
        ),
        hints = listOf("Schreibe 1+i in Polarform", "e^z ist 2πi-periodisch"),
        difficulty = 2
    ),
    ExamProblem(
        id = "complex7",
        topic = ExamTopic.COMPLEX,
        points = 5,
        questionLatex = "Sei \\(z = 2 - 2i\\). Berechnen Sie \\(z^{10}\\) mit Hilfe der Exponentialform.",
        solutionSteps = listOf(
            "Betrag: \\(|z| = \\sqrt{4 + 4} = 2\\sqrt{2}\\)",
            "Argument: \\(\\arg(z) = \\arctan\\left(\\frac{-2}{2}\\right) = -\\frac{\\pi}{4}\\) (4. Quadrant)",
            "Exponentialform: \\(z = 2\\sqrt{2} \\cdot e^{-i\\pi/4}\\)",
            "Potenz: \\(z^{10} = (2\\sqrt{2})^{10} \\cdot e^{-i \\cdot 10\\pi/4}\\)",
            "\\((2\\sqrt{2})^{10} = 2^{10} \\cdot (\\sqrt{2})^{10} = 1024 \\cdot 32 = 32768\\)",
            "\\(e^{-i \\cdot 10\\pi/4} = e^{-i \\cdot 5\\pi/2} = e^{-i\\pi/2} = -i\\)",
            "Ergebnis: \\(z^{10} = -32768i\\)"
        ),
        hints = listOf("Verwende z = |z|·e^(iφ)", "Vereinfache den Winkel modulo 2π"),
        difficulty = 2
    ),

    // ════════════════════════════════════════════════════════════════════════
    //  NEUE AUFGABEN - INDUKTION
    // ════════════════════════════════════════════════════════════════════════

    ExamProblem(
        id = "induction3",
        topic = ExamTopic.INDUCTION,
        points = 5,
        questionLatex = "Beweisen Sie mittels vollständiger Induktion für alle \\(n \\in \\mathbb{N}\\):\n\\[\\sum_{k=1}^{n} k^2 = \\frac{n(n+1)(2n+1)}{6}\\]",
        solutionSteps = listOf(
            "IA (n=1): \\(\\sum_{k=1}^{1} k^2 = 1\\) und \\(\\frac{1 \\cdot 2 \\cdot 3}{6} = 1\\) ✓",
            "IV: Annahme gilt für ein \\(n \\in \\mathbb{N}\\)",
            "IS \\((n \\to n+1)\\): \\(\\sum_{k=1}^{n+1} k^2 = \\sum_{k=1}^{n} k^2 + (n+1)^2\\)",
            "\\(\\stackrel{\\text{IV}}{=} \\frac{n(n+1)(2n+1)}{6} + (n+1)^2\\)",
            "\\(= \\frac{n(n+1)(2n+1) + 6(n+1)^2}{6} = \\frac{(n+1)[n(2n+1) + 6(n+1)]}{6}\\)",
            "\\(= \\frac{(n+1)(2n^2 + 7n + 6)}{6} = \\frac{(n+1)(n+2)(2n+3)}{6}\\)",
            "\\(= \\frac{(n+1)((n+1)+1)(2(n+1)+1)}{6}\\) ✓"
        ),
        hints = listOf("Beim IS: summand (n+1)² hinzufügen", "Ausklammern von (n+1)"),
        difficulty = 2
    ),
    ExamProblem(
        id = "induction4",
        topic = ExamTopic.INDUCTION,
        points = 6,
        questionLatex = "Beweisen Sie mittels vollständiger Induktion für alle \\(n \\in \\mathbb{N}\\):\n\\[n! > 2^n \\quad \\text{für } n \\geq 4\\]",
        solutionSteps = listOf(
            "IA (n=4): \\(4! = 24\\) und \\(2^4 = 16\\), also \\(24 > 16\\) ✓",
            "IV: Für ein \\(n \\geq 4\\) gelte \\(n! > 2^n\\)",
            "IS \\((n \\to n+1)\\): Zeige \\((n+1)! > 2^{n+1}\\)",
            "\\((n+1)! = (n+1) \\cdot n! \\stackrel{\\text{IV}}{>} (n+1) \\cdot 2^n\\)",
            "Für \\(n \\geq 4\\) gilt \\(n+1 \\geq 5 > 2\\), also:",
            "\\((n+1) \\cdot 2^n > 2 \\cdot 2^n = 2^{n+1}\\) ✓"
        ),
        hints = listOf("Induktionsanfang bei n=4", "Im IS: (n+1) > 2 für n ≥ 4 nutzen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "induction5",
        topic = ExamTopic.INDUCTION,
        points = 5,
        questionLatex = "Beweisen Sie mittels vollständiger Induktion für alle \\(n \\in \\mathbb{N}\\):\n\\[\\sum_{k=1}^{n} \\frac{1}{k(k+1)} = \\frac{n}{n+1}\\]",
        solutionSteps = listOf(
            "IA (n=1): \\(\\frac{1}{1 \\cdot 2} = \\frac{1}{2}\\) und \\(\\frac{1}{2}\\) ✓",
            "IV: Annahme \\(\\sum_{k=1}^{n} \\frac{1}{k(k+1)} = \\frac{n}{n+1}\\) für ein \\(n\\)",
            "IS: \\(\\sum_{k=1}^{n+1} \\frac{1}{k(k+1)} = \\sum_{k=1}^{n} \\frac{1}{k(k+1)} + \\frac{1}{(n+1)(n+2)}\\)",
            "\\(\\stackrel{\\text{IV}}{=} \\frac{n}{n+1} + \\frac{1}{(n+1)(n+2)}\\)",
            "\\(= \\frac{n(n+2) + 1}{(n+1)(n+2)} = \\frac{n^2 + 2n + 1}{(n+1)(n+2)}\\)",
            "\\(= \\frac{(n+1)^2}{(n+1)(n+2)} = \\frac{n+1}{n+2}\\) ✓"
        ),
        hints = listOf("Partialbruchzerlegung: 1/(k(k+1)) = 1/k - 1/(k+1)", "Teleskopsumme als Alternative"),
        difficulty = 2
    ),
    ExamProblem(
        id = "induction6",
        topic = ExamTopic.INDUCTION,
        points = 6,
        questionLatex = "Sei \\((F_n)_{n \\geq 0}\\) die Fibonacci-Folge mit \\(F_0 = 0\\), \\(F_1 = 1\\), \\(F_{n+2} = F_{n+1} + F_n\\).\n\nBeweisen Sie mittels vollständiger Induktion:\n\\[F_1 + F_3 + F_5 + \\ldots + F_{2n-1} = F_{2n}\\]",
        solutionSteps = listOf(
            "IA (n=1): \\(F_1 = 1 = F_2\\) ✓",
            "IV: Für ein \\(n\\) gelte \\(\\sum_{k=1}^{n} F_{2k-1} = F_{2n}\\)",
            "IS: \\(\\sum_{k=1}^{n+1} F_{2k-1} = \\sum_{k=1}^{n} F_{2k-1} + F_{2n+1}\\)",
            "\\(\\stackrel{\\text{IV}}{=} F_{2n} + F_{2n+1}\\)",
            "Nach Fibonacci-Rekursion: \\(F_{2n} + F_{2n+1} = F_{2n+2} = F_{2(n+1)}\\) ✓"
        ),
        hints = listOf("Verwende F_{n+2} = F_{n+1} + F_n", "Induktionsschritt nutzt Rekursion direkt"),
        difficulty = 2
    ),
    ExamProblem(
        id = "induction7",
        topic = ExamTopic.INDUCTION,
        points = 5,
        questionLatex = "Beweisen Sie mittels vollständiger Induktion für alle \\(n \\in \\mathbb{N}\\):\n\\[\\prod_{k=2}^{n} \\left(1 - \\frac{1}{k^2}\\right) = \\frac{n+1}{2n}\\]",
        solutionSteps = listOf(
            "IA (n=2): \\(1 - \\frac{1}{4} = \\frac{3}{4}\\) und \\(\\frac{2+1}{2 \\cdot 2} = \\frac{3}{4}\\) ✓",
            "IV: Annahme gilt für ein \\(n \\geq 2\\)",
            "IS: \\(\\prod_{k=2}^{n+1} \\left(1 - \\frac{1}{k^2}\\right) = \\prod_{k=2}^{n} \\left(1 - \\frac{1}{k^2}\\right) \\cdot \\left(1 - \\frac{1}{(n+1)^2}\\right)\\)",
            "\\(\\stackrel{\\text{IV}}{=} \\frac{n+1}{2n} \\cdot \\frac{(n+1)^2 - 1}{(n+1)^2} = \\frac{n+1}{2n} \\cdot \\frac{n(n+2)}{(n+1)^2}\\)",
            "\\(= \\frac{n(n+2)}{2n(n+1)} = \\frac{n+2}{2(n+1)}\\) ✓"
        ),
        hints = listOf("1 - 1/k² = (k²-1)/k² = (k-1)(k+1)/k²", "Sorgfältig kürzen"),
        difficulty = 2
    ),

    // ════════════════════════════════════════════════════════════════════════
    //  NEUE AUFGABEN - REIHEN
    // ════════════════════════════════════════════════════════════════════════

    ExamProblem(
        id = "series3",
        topic = ExamTopic.SERIES,
        points = 5,
        questionLatex = "Untersuchen Sie die Reihe auf absolute und bedingte Konvergenz:\n\\[\\sum_{n=1}^{\\infty} \\frac{(-1)^n}{\\sqrt{n}}\\]",
        solutionSteps = listOf(
            "Absolutkonvergenz: \\(\\sum \\frac{1}{\\sqrt{n}} = \\sum \\frac{1}{n^{1/2}}\\) ist p-Reihe mit \\(p = 1/2 < 1\\)",
            "Also divergiert \\(\\sum |a_n|\\) → keine Absolutkonvergenz",
            "Bedingte Konvergenz mit Leibniz-Kriterium:",
            "1. \\(a_n = \\frac{1}{\\sqrt{n}} > 0\\) ✓",
            "2. \\((a_n)\\) monoton fallend: \\(\\frac{1}{\\sqrt{n}} > \\frac{1}{\\sqrt{n+1}}\\) ✓",
            "3. \\(\\lim_{n \\to \\infty} a_n = 0\\) ✓",
            "Leibniz-Kriterium erfüllt → bedingte Konvergenz"
        ),
        hints = listOf("Prüfe zuerst Absolutkonvergenz mit p-Reihe", "Leibniz-Kriterium für alternierende Reihen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "series4",
        topic = ExamTopic.SERIES,
        points = 6,
        questionLatex = "Bestimmen Sie den Konvergenzradius der Potenzreihe:\n\\[\\sum_{n=0}^{\\infty} \\frac{n!}{n^n} x^n\\]",
        solutionSteps = listOf(
            "Quotientenkriterium: \\(\\left|\\frac{a_{n+1}}{a_n}\\right| = \\frac{(n+1)!}{(n+1)^{n+1}} \\cdot \\frac{n^n}{n!}\\)",
            "\\(= \\frac{(n+1) \\cdot n^n}{(n+1)^{n+1}} = \\frac{n^n}{(n+1)^n} = \\left(\\frac{n}{n+1}\\right)^n\\)",
            "\\(= \\left(1 - \\frac{1}{n+1}\\right)^n \\to e^{-1}\\) für \\(n \\to \\infty\\)",
            "Also: \\(\\lim \\left|\\frac{a_{n+1}}{a_n}\\right| = \\frac{1}{e}\\)",
            "Konvergenzradius: \\(R = e\\)"
        ),
        hints = listOf("Quotientenkriterium anwenden", "(1 - 1/n)^n → 1/e"),
        difficulty = 2
    ),
    ExamProblem(
        id = "series5",
        topic = ExamTopic.SERIES,
        points = 5,
        questionLatex = "Untersuchen Sie die Konvergenz der Reihe:\n\\[\\sum_{n=2}^{\\infty} \\frac{1}{n \\cdot (\\ln n)^2}\\]",
        solutionSteps = listOf(
            "Integralkriterium: Betrachte \\(f(x) = \\frac{1}{x(\\ln x)^2}\\) für \\(x \\geq 2\\)",
            "\\(f\\) ist positiv, stetig und monoton fallend",
            "\\(\\int_2^{\\infty} \\frac{1}{x(\\ln x)^2} dx\\)",
            "Substitution: \\(u = \\ln x\\), \\(du = \\frac{1}{x}dx\\)",
            "\\(= \\int_{\\ln 2}^{\\infty} \\frac{1}{u^2} du = \\left[-\\frac{1}{u}\\right]_{\\ln 2}^{\\infty} = \\frac{1}{\\ln 2}\\)",
            "Das Integral konvergiert → Die Reihe konvergiert"
        ),
        hints = listOf("Integralkriterium mit Substitution", "u = ln x wählen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "series6",
        topic = ExamTopic.SERIES,
        points = 6,
        questionLatex = "Zeigen Sie, dass die Reihe konvergiert und berechnen Sie ihren Wert:\n\\[\\sum_{n=1}^{\\infty} \\frac{1}{n(n+2)}\\]",
        solutionSteps = listOf(
            "Partialbruchzerlegung: \\(\\frac{1}{n(n+2)} = \\frac{A}{n} + \\frac{B}{n+2}\\)",
            "\\(1 = A(n+2) + Bn\\), setze \\(n=0\\): \\(A = 1/2\\), setze \\(n=-2\\): \\(B = -1/2\\)",
            "Also: \\(\\frac{1}{n(n+2)} = \\frac{1}{2}\\left(\\frac{1}{n} - \\frac{1}{n+2}\\right)\\)",
            "Teleskopsumme: \\(S_N = \\frac{1}{2}\\sum_{n=1}^{N}\\left(\\frac{1}{n} - \\frac{1}{n+2}\\right)\\)",
            "\\(= \\frac{1}{2}\\left(1 + \\frac{1}{2} - \\frac{1}{N+1} - \\frac{1}{N+2}\\right)\\)",
            "\\(\\lim_{N \\to \\infty} S_N = \\frac{1}{2}\\left(1 + \\frac{1}{2}\\right) = \\frac{3}{4}\\)"
        ),
        hints = listOf("Partialbruchzerlegung", "Teleskopsumme erkennen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "series7",
        topic = ExamTopic.SERIES,
        points = 5,
        questionLatex = "Untersuchen Sie die Reihe auf Konvergenz:\n\\[\\sum_{n=1}^{\\infty} \\frac{n^2 + 1}{n^4 + n^2 + 1}\\]",
        solutionSteps = listOf(
            "Verhalten für große \\(n\\): \\(\\frac{n^2 + 1}{n^4 + n^2 + 1} \\sim \\frac{n^2}{n^4} = \\frac{1}{n^2}\\)",
            "Vergleichskriterium mit \\(\\sum \\frac{1}{n^2}\\) (konvergent)",
            "Genauer: \\(\\frac{n^2+1}{n^4+n^2+1} \\leq \\frac{n^2+1}{n^4} = \\frac{1}{n^2} + \\frac{1}{n^4} \\leq \\frac{2}{n^2}\\)",
            "Da \\(\\sum \\frac{2}{n^2}\\) konvergiert, konvergiert auch die Reihe"
        ),
        hints = listOf("Vergleich mit p-Reihe", "Höchste Potenzen dominieren"),
        difficulty = 1
    ),

    // ════════════════════════════════════════════════════════════════════════
    //  NEUE AUFGABEN - STETIGKEIT
    // ════════════════════════════════════════════════════════════════════════

    ExamProblem(
        id = "continuity2",
        topic = ExamTopic.CONTINUITY,
        points = 6,
        questionLatex = "Zeigen Sie mit der \\(\\varepsilon\\)-\\(\\delta\\)-Definition, dass die Funktion\n\\[f(x) = x^2\\]\nstetig in \\(x_0 = 2\\) ist.",
        solutionSteps = listOf(
            "Zu zeigen: \\(\\forall \\varepsilon > 0 \\; \\exists \\delta > 0: |x - 2| < \\delta \\Rightarrow |x^2 - 4| < \\varepsilon\\)",
            "\\(|x^2 - 4| = |x-2| \\cdot |x+2|\\)",
            "Wähle zunächst \\(\\delta \\leq 1\\), dann \\(|x-2| < 1\\), also \\(1 < x < 3\\)",
            "Damit: \\(|x+2| < 5\\)",
            "Also: \\(|x^2 - 4| < 5|x-2|\\)",
            "Wähle \\(\\delta = \\min\\left(1, \\frac{\\varepsilon}{5}\\right)\\)",
            "Dann: \\(|x-2| < \\delta \\Rightarrow |x^2 - 4| < 5 \\cdot \\frac{\\varepsilon}{5} = \\varepsilon\\) ✓"
        ),
        hints = listOf("|x² - 4| = |x-2||x+2| faktorisieren", "δ ≤ 1 wählen um |x+2| abzuschätzen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "continuity3",
        topic = ExamTopic.CONTINUITY,
        points = 5,
        questionLatex = "Sei \\(f: \\mathbb{R} \\to \\mathbb{R}\\) definiert durch\n\\[f(x) = \\begin{cases} \\frac{\\sin x}{x} & x \\neq 0 \\\\ 1 & x = 0 \\end{cases}\\]\nZeigen Sie, dass \\(f\\) stetig auf ganz \\(\\mathbb{R}\\) ist.",
        solutionSteps = listOf(
            "Für \\(x \\neq 0\\): \\(f\\) ist als Quotient stetiger Funktionen stetig (\\(x \\neq 0\\))",
            "Zu zeigen: \\(f\\) stetig in \\(x_0 = 0\\)",
            "\\(\\lim_{x \\to 0} f(x) = \\lim_{x \\to 0} \\frac{\\sin x}{x}\\)",
            "Bekannter Grenzwert: \\(\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1\\)",
            "Da \\(f(0) = 1 = \\lim_{x \\to 0} f(x)\\), ist \\(f\\) stetig in 0",
            "Also ist \\(f\\) stetig auf ganz \\(\\mathbb{R}\\)"
        ),
        hints = listOf("Bekannter Grenzwert: lim sin(x)/x = 1", "Stetigkeit in x=0 gesondert prüfen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "continuity4",
        topic = ExamTopic.CONTINUITY,
        points = 6,
        questionLatex = "Zeigen Sie, dass die Gleichung\n\\[x^3 + x - 1 = 0\\]\ngenau eine reelle Lösung besitzt.",
        solutionSteps = listOf(
            "Sei \\(f(x) = x^3 + x - 1\\), \\(f\\) ist stetig auf \\(\\mathbb{R}\\)",
            "\\(f(0) = -1 < 0\\) und \\(f(1) = 1 > 0\\)",
            "Nach ZWS existiert \\(c \\in (0,1)\\) mit \\(f(c) = 0\\)",
            "Eindeutigkeit: \\(f'(x) = 3x^2 + 1 > 0\\) für alle \\(x\\)",
            "Also ist \\(f\\) streng monoton wachsend auf \\(\\mathbb{R}\\)",
            "Eine streng monotone Funktion hat höchstens eine Nullstelle",
            "Zusammen: genau eine Nullstelle"
        ),
        hints = listOf("Zwischenwertsatz für Existenz", "Monotonie für Eindeutigkeit"),
        difficulty = 2
    ),
    ExamProblem(
        id = "continuity5",
        topic = ExamTopic.CONTINUITY,
        points = 7,
        questionLatex = "Zeigen Sie, dass \\(f(x) = \\frac{1}{x}\\) auf \\((0, \\infty)\\) stetig, aber nicht gleichmäßig stetig ist.",
        solutionSteps = listOf(
            "Stetigkeit: Für \\(x_0 > 0\\) und \\(\\varepsilon > 0\\) wähle \\(\\delta = \\min(x_0/2, \\varepsilon x_0^2/2)\\)",
            "Dann: \\(|1/x - 1/x_0| = |x-x_0|/(x \\cdot x_0) < \\varepsilon\\) für \\(|x-x_0| < \\delta\\)",
            "Nicht gleichmäßig stetig: Wähle \\(\\varepsilon = 1\\)",
            "Für alle \\(\\delta > 0\\) wähle \\(x = \\delta/2\\), \\(y = \\delta/4\\)",
            "Dann: \\(|x - y| = \\delta/4 < \\delta\\), aber:",
            "\\(|1/x - 1/y| = |2/\\delta - 4/\\delta| = 2/\\delta\\)",
            "Für \\(\\delta < 2\\) ist \\(2/\\delta > 1 = \\varepsilon\\) → nicht gleichmäßig stetig"
        ),
        hints = listOf("Für gleichmäßige Stetigkeit: δ darf nicht von x₀ abhängen", "Gegenbeispiel nahe bei 0 suchen"),
        difficulty = 3
    ),
    ExamProblem(
        id = "continuity6",
        topic = ExamTopic.CONTINUITY,
        points = 5,
        questionLatex = "Zeigen Sie, dass die Funktion\n\\[f(x) = \\begin{cases} x \\cdot \\sin(1/x) & x \\neq 0 \\\\ 0 & x = 0 \\end{cases}\\]\nstetig in \\(x = 0\\) ist.",
        solutionSteps = listOf(
            "Zu zeigen: \\(\\lim_{x \\to 0} x \\sin(1/x) = 0 = f(0)\\)",
            "Abschätzung: \\(|\\sin(1/x)| \\leq 1\\) für alle \\(x \\neq 0\\)",
            "Also: \\(|x \\sin(1/x)| \\leq |x|\\)",
            "Für \\(\\varepsilon > 0\\) wähle \\(\\delta = \\varepsilon\\)",
            "Dann: \\(|x| < \\delta \\Rightarrow |x \\sin(1/x)| \\leq |x| < \\varepsilon\\)",
            "Nach Einschnürungssatz: \\(\\lim_{x \\to 0} x \\sin(1/x) = 0\\) ✓"
        ),
        hints = listOf("|sin(.)| ≤ 1 verwenden", "Sandwich-/Einschnürungssatz"),
        difficulty = 2
    ),

    // ════════════════════════════════════════════════════════════════════════
    //  NEUE AUFGABEN - INTEGRATION
    // ════════════════════════════════════════════════════════════════════════

    ExamProblem(
        id = "integration3",
        topic = ExamTopic.INTEGRATION,
        points = 6,
        questionLatex = "Berechnen Sie das Integral:\n\\[\\int \\frac{1}{x^2 - 4} \\, dx\\]",
        solutionSteps = listOf(
            "Partialbruchzerlegung: \\(\\frac{1}{x^2-4} = \\frac{1}{(x-2)(x+2)} = \\frac{A}{x-2} + \\frac{B}{x+2}\\)",
            "\\(1 = A(x+2) + B(x-2)\\)",
            "\\(x = 2\\): \\(1 = 4A \\Rightarrow A = 1/4\\)",
            "\\(x = -2\\): \\(1 = -4B \\Rightarrow B = -1/4\\)",
            "\\(\\int \\frac{1}{x^2-4} dx = \\frac{1}{4} \\int \\frac{1}{x-2} dx - \\frac{1}{4} \\int \\frac{1}{x+2} dx\\)",
            "\\(= \\frac{1}{4} \\ln|x-2| - \\frac{1}{4} \\ln|x+2| + C = \\frac{1}{4} \\ln\\left|\\frac{x-2}{x+2}\\right| + C\\)"
        ),
        hints = listOf("x² - 4 = (x-2)(x+2) faktorisieren", "Partialbruchzerlegung"),
        difficulty = 2
    ),
    ExamProblem(
        id = "integration4",
        topic = ExamTopic.INTEGRATION,
        points = 7,
        questionLatex = "Berechnen Sie das Integral:\n\\[\\int \\frac{x^2}{\\sqrt{1-x^2}} \\, dx\\]",
        solutionSteps = listOf(
            "Substitution: \\(x = \\sin t\\), \\(dx = \\cos t \\, dt\\)",
            "\\(\\sqrt{1-x^2} = \\sqrt{1-\\sin^2 t} = \\cos t\\)",
            "\\(\\int \\frac{\\sin^2 t}{\\cos t} \\cos t \\, dt = \\int \\sin^2 t \\, dt\\)",
            "\\(\\sin^2 t = \\frac{1 - \\cos(2t)}{2}\\)",
            "\\(= \\int \\frac{1 - \\cos(2t)}{2} dt = \\frac{t}{2} - \\frac{\\sin(2t)}{4} + C\\)",
            "\\(\\sin(2t) = 2\\sin t \\cos t = 2x\\sqrt{1-x^2}\\)",
            "Rücksubstitution: \\(= \\frac{\\arcsin x}{2} - \\frac{x\\sqrt{1-x^2}}{2} + C\\)"
        ),
        hints = listOf("Trigonometrische Substitution x = sin t", "sin²t = (1-cos(2t))/2"),
        difficulty = 2
    ),
    ExamProblem(
        id = "integration5",
        topic = ExamTopic.INTEGRATION,
        points = 6,
        questionLatex = "Berechnen Sie mittels partieller Integration:\n\\[\\int x^2 e^x \\, dx\\]",
        solutionSteps = listOf(
            "Partielle Integration: \\(\\int u \\, dv = uv - \\int v \\, du\\)",
            "Setze \\(u = x^2\\), \\(dv = e^x dx\\) → \\(du = 2x \\, dx\\), \\(v = e^x\\)",
            "\\(= x^2 e^x - \\int 2x e^x dx\\)",
            "Nochmals partiell: \\(u = 2x\\), \\(dv = e^x dx\\)",
            "\\(= x^2 e^x - (2x e^x - \\int 2e^x dx)\\)",
            "\\(= x^2 e^x - 2x e^x + 2e^x + C\\)",
            "\\(= e^x(x^2 - 2x + 2) + C\\)"
        ),
        hints = listOf("Zweimal partiell integrieren", "e^x bleibt beim Integrieren erhalten"),
        difficulty = 2
    ),
    ExamProblem(
        id = "integration6",
        topic = ExamTopic.INTEGRATION,
        points = 7,
        questionLatex = "Untersuchen Sie die Konvergenz des uneigentlichen Integrals und berechnen Sie ggf. den Wert:\n\\[\\int_0^{\\infty} x e^{-x} \\, dx\\]",
        solutionSteps = listOf(
            "\\(\\int_0^{\\infty} x e^{-x} dx = \\lim_{b \\to \\infty} \\int_0^{b} x e^{-x} dx\\)",
            "Partielle Integration: \\(u = x\\), \\(dv = e^{-x}dx\\)",
            "\\(\\int x e^{-x} dx = -xe^{-x} - \\int (-e^{-x}) dx = -xe^{-x} - e^{-x}\\)",
            "\\(= -e^{-x}(x+1)\\)",
            "\\(\\left[-e^{-x}(x+1)\\right]_0^{b} = -e^{-b}(b+1) + 1\\)",
            "\\(\\lim_{b \\to \\infty} e^{-b}(b+1) = 0\\) (l'Hôpital oder exp dominiert)",
            "Ergebnis: \\(\\int_0^{\\infty} x e^{-x} dx = 1\\)"
        ),
        hints = listOf("Partielle Integration", "e^(-x) fällt schneller als x wächst"),
        difficulty = 2
    ),
    ExamProblem(
        id = "integration7",
        topic = ExamTopic.INTEGRATION,
        points = 5,
        questionLatex = "Berechnen Sie:\n\\[\\int \\frac{1}{x^2 + 2x + 5} \\, dx\\]",
        solutionSteps = listOf(
            "Quadratische Ergänzung: \\(x^2 + 2x + 5 = (x+1)^2 + 4\\)",
            "Substitution: \\(u = x + 1\\), \\(du = dx\\)",
            "\\(\\int \\frac{1}{u^2 + 4} du = \\int \\frac{1}{4(u^2/4 + 1)} du\\)",
            "\\(= \\frac{1}{4} \\int \\frac{1}{(u/2)^2 + 1} du\\)",
            "Substitution: \\(t = u/2\\), \\(dt = du/2\\)",
            "\\(= \\frac{1}{2} \\int \\frac{1}{t^2 + 1} dt = \\frac{1}{2} \\arctan(t) + C\\)",
            "Rücksubstitution: \\(= \\frac{1}{2} \\arctan\\left(\\frac{x+1}{2}\\right) + C\\)"
        ),
        hints = listOf("Quadratische Ergänzung", "Auf arctan-Form bringen"),
        difficulty = 2
    ),

    // ════════════════════════════════════════════════════════════════════════
    //  NEUE AUFGABEN - LINEARE ALGEBRA
    // ════════════════════════════════════════════════════════════════════════

    ExamProblem(
        id = "la4",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 8,
        questionLatex = "Sei \\(A = \\begin{pmatrix} 2 & 1 \\\\ 0 & 2 \\end{pmatrix}\\).\n\n(a) Bestimmen Sie die Eigenwerte und deren algebraische Vielfachheiten.\n(b) Bestimmen Sie die Eigenräume.\n(c) Ist \\(A\\) diagonalisierbar?",
        solutionSteps = listOf(
            "(a) Charakteristisches Polynom: \\(\\det(A - \\lambda I) = (2-\\lambda)^2\\)",
            "Eigenwert: \\(\\lambda = 2\\) mit algebraischer Vielfachheit 2",
            "(b) Eigenraum: \\((A - 2I)v = 0\\)",
            "\\(A - 2I = \\begin{pmatrix} 0 & 1 \\\\ 0 & 0 \\end{pmatrix}\\)",
            "Kern: \\(v_2 = 0\\), also \\(E_2 = \\text{span}\\{(1,0)^T\\}\\)",
            "Geometrische Vielfachheit: \\(\\dim E_2 = 1\\)",
            "(c) Algebraische VFH (2) ≠ Geometrische VFH (1)",
            "Also ist \\(A\\) NICHT diagonalisierbar"
        ),
        hints = listOf("Charakteristisches Polynom aufstellen", "Alg. VFH ≠ geom. VFH → nicht diagonalisierbar"),
        difficulty = 2
    ),
    ExamProblem(
        id = "la5",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 7,
        questionLatex = "Bestimmen Sie eine Orthonormalbasis des Unterraums\n\\[U = \\text{span}\\left\\{\\begin{pmatrix} 1 \\\\ 1 \\\\ 0 \\end{pmatrix}, \\begin{pmatrix} 1 \\\\ 0 \\\\ 1 \\end{pmatrix}\\right\\} \\subseteq \\mathbb{R}^3\\]\nmittels Gram-Schmidt-Verfahren.",
        solutionSteps = listOf(
            "Gegeben: \\(v_1 = (1,1,0)^T\\), \\(v_2 = (1,0,1)^T\\)",
            "Schritt 1: \\(u_1 = v_1\\), \\(\\|u_1\\| = \\sqrt{2}\\)",
            "\\(e_1 = \\frac{1}{\\sqrt{2}}(1,1,0)^T\\)",
            "Schritt 2: \\(u_2 = v_2 - (v_2 \\cdot e_1)e_1\\)",
            "\\(v_2 \\cdot e_1 = \\frac{1}{\\sqrt{2}}(1 + 0 + 0) = \\frac{1}{\\sqrt{2}}\\)",
            "\\(u_2 = (1,0,1)^T - \\frac{1}{2}(1,1,0)^T = (\\frac{1}{2}, -\\frac{1}{2}, 1)^T\\)",
            "\\(\\|u_2\\| = \\sqrt{1/4 + 1/4 + 1} = \\sqrt{3/2}\\)",
            "\\(e_2 = \\sqrt{\\frac{2}{3}}(\\frac{1}{2}, -\\frac{1}{2}, 1)^T = \\frac{1}{\\sqrt{6}}(1, -1, 2)^T\\)"
        ),
        hints = listOf("Gram-Schmidt: Orthogonalisieren, dann normieren", "Sorgfältig rechnen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "la6",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 6,
        questionLatex = "Sei \\(A = \\begin{pmatrix} 3 & 1 \\\\ 0 & 3 \\end{pmatrix}\\). Berechnen Sie \\(A^n\\) für \\(n \\in \\mathbb{N}\\).",
        solutionSteps = listOf(
            "Schreibe \\(A = 3I + N\\) mit \\(N = \\begin{pmatrix} 0 & 1 \\\\ 0 & 0 \\end{pmatrix}\\)",
            "\\(N\\) ist nilpotent: \\(N^2 = 0\\)",
            "\\(3I\\) und \\(N\\) kommutieren: \\((3I)N = N(3I)\\)",
            "Binomische Formel: \\(A^n = (3I + N)^n = \\sum_{k=0}^{n} \\binom{n}{k} (3I)^{n-k} N^k\\)",
            "Da \\(N^k = 0\\) für \\(k \\geq 2\\):",
            "\\(A^n = 3^n I + n \\cdot 3^{n-1} N = \\begin{pmatrix} 3^n & n \\cdot 3^{n-1} \\\\ 0 & 3^n \\end{pmatrix}\\)"
        ),
        hints = listOf("A = sI + N zerlegen mit N nilpotent", "N² = 0 nutzen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "la7",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 6,
        questionLatex = "Berechnen Sie die orthogonale Projektion des Vektors \\(v = (1, 2, 3)^T\\) auf die Ebene\n\\[E = \\{(x, y, z)^T \\in \\mathbb{R}^3 : x + y + z = 0\\}\\]",
        solutionSteps = listOf(
            "Normalenvektor der Ebene: \\(n = (1, 1, 1)^T\\)",
            "Projektion auf Normalenrichtung: \\(\\text{proj}_n(v) = \\frac{v \\cdot n}{\\|n\\|^2} n\\)",
            "\\(v \\cdot n = 1 + 2 + 3 = 6\\), \\(\\|n\\|^2 = 3\\)",
            "\\(\\text{proj}_n(v) = \\frac{6}{3}(1,1,1)^T = (2,2,2)^T\\)",
            "Projektion auf Ebene: \\(\\text{proj}_E(v) = v - \\text{proj}_n(v)\\)",
            "\\(= (1,2,3)^T - (2,2,2)^T = (-1, 0, 1)^T\\)",
            "Probe: \\(-1 + 0 + 1 = 0\\) ✓"
        ),
        hints = listOf("Proj auf Ebene = v - Proj auf Normale", "Normalenvektor aus Ebenengleichung"),
        difficulty = 2
    ),
    ExamProblem(
        id = "la8",
        topic = ExamTopic.LINEAR_ALGEBRA,
        points = 7,
        questionLatex = "Bestimmen Sie alle Eigenwerte und eine Basis aus Eigenvektoren für die Matrix\n\\[A = \\begin{pmatrix} 1 & 2 & 0 \\\\ 0 & 1 & 0 \\\\ 0 & 0 & 2 \\end{pmatrix}\\]",
        solutionSteps = listOf(
            "Dreiecksmatrix → Eigenwerte auf der Diagonalen: \\(\\lambda_1 = 1\\) (VFH 2), \\(\\lambda_2 = 2\\)",
            "Eigenraum zu \\(\\lambda = 1\\): \\((A - I)v = 0\\)",
            "\\(A - I = \\begin{pmatrix} 0 & 2 & 0 \\\\ 0 & 0 & 0 \\\\ 0 & 0 & 1 \\end{pmatrix}\\)",
            "Kern: \\(v_2 = 0\\), \\(v_3 = 0\\), \\(v_1\\) frei → \\(E_1 = \\text{span}\\{(1,0,0)^T\\}\\)",
            "Eigenraum zu \\(\\lambda = 2\\): \\((A - 2I)v = 0\\)",
            "\\(A - 2I = \\begin{pmatrix} -1 & 2 & 0 \\\\ 0 & -1 & 0 \\\\ 0 & 0 & 0 \\end{pmatrix}\\)",
            "Kern: \\(v_2 = 0\\), \\(v_1 = 0\\), \\(v_3\\) frei → \\(E_2 = \\text{span}\\{(0,0,1)^T\\}\\)",
            "Nicht diagonalisierbar, da geom. VFH von \\(\\lambda=1\\) nur 1"
        ),
        hints = listOf("Bei Dreiecksmatrizen: EW auf Diagonale", "Eigenräume durch Kern(A-λI) bestimmen"),
        difficulty = 2
    ),

    // ════════════════════════════════════════════════════════════════════════
    //  NEUE AUFGABEN - TAYLOR
    // ════════════════════════════════════════════════════════════════════════

    ExamProblem(
        id = "taylor2",
        topic = ExamTopic.TAYLOR,
        points = 6,
        questionLatex = "Bestimmen Sie das Taylorpolynom 3. Grades von \\(f(x) = \\ln(1+x)\\) um \\(x_0 = 0\\).",
        solutionSteps = listOf(
            "\\(f(x) = \\ln(1+x)\\), \\(f(0) = 0\\)",
            "\\(f'(x) = \\frac{1}{1+x}\\), \\(f'(0) = 1\\)",
            "\\(f''(x) = -\\frac{1}{(1+x)^2}\\), \\(f''(0) = -1\\)",
            "\\(f'''(x) = \\frac{2}{(1+x)^3}\\), \\(f'''(0) = 2\\)",
            "Taylor: \\(T_3(x) = f(0) + f'(0)x + \\frac{f''(0)}{2!}x^2 + \\frac{f'''(0)}{3!}x^3\\)",
            "\\(T_3(x) = 0 + x - \\frac{1}{2}x^2 + \\frac{2}{6}x^3 = x - \\frac{x^2}{2} + \\frac{x^3}{3}\\)"
        ),
        hints = listOf("Ableitungen systematisch berechnen", "Taylorformel anwenden"),
        difficulty = 1
    ),
    ExamProblem(
        id = "taylor3",
        topic = ExamTopic.TAYLOR,
        points = 7,
        questionLatex = "Berechnen Sie mit Hilfe von Taylorreihen:\n\\[\\lim_{x \\to 0} \\frac{e^x - 1 - x}{x^2}\\]",
        solutionSteps = listOf(
            "Taylorentwicklung von \\(e^x\\): \\(e^x = 1 + x + \\frac{x^2}{2} + \\frac{x^3}{6} + O(x^4)\\)",
            "\\(e^x - 1 - x = \\frac{x^2}{2} + \\frac{x^3}{6} + O(x^4)\\)",
            "\\(\\frac{e^x - 1 - x}{x^2} = \\frac{\\frac{x^2}{2} + \\frac{x^3}{6} + O(x^4)}{x^2}\\)",
            "\\(= \\frac{1}{2} + \\frac{x}{6} + O(x^2)\\)",
            "\\(\\lim_{x \\to 0} \\frac{e^x - 1 - x}{x^2} = \\frac{1}{2}\\)"
        ),
        hints = listOf("Taylorentwicklung von e^x einsetzen", "Genügend Terme mitnehmen"),
        difficulty = 2
    ),
    ExamProblem(
        id = "taylor4",
        topic = ExamTopic.TAYLOR,
        points = 6,
        questionLatex = "Schätzen Sie den Fehler ab, wenn \\(\\sin(0.1)\\) durch das Taylorpolynom 3. Grades approximiert wird.",
        solutionSteps = listOf(
            "\\(T_3(x) = x - \\frac{x^3}{6}\\) für \\(\\sin x\\) um \\(x_0 = 0\\)",
            "Lagrange-Restglied: \\(R_3(x) = \\frac{f^{(4)}(c)}{4!}x^4\\) für ein \\(c\\) zwischen 0 und \\(x\\)",
            "\\(f^{(4)}(x) = \\sin x\\), also \\(|f^{(4)}(c)| \\leq 1\\)",
            "\\(|R_3(0.1)| \\leq \\frac{1}{24} \\cdot (0.1)^4 = \\frac{0.0001}{24}\\)",
            "\\(|R_3(0.1)| \\leq 4.17 \\cdot 10^{-6}\\)",
            "Der Fehler ist also kleiner als \\(5 \\cdot 10^{-6}\\)"
        ),
        hints = listOf("Lagrange-Restglied verwenden", "|sin(c)| ≤ 1"),
        difficulty = 2
    ),
    ExamProblem(
        id = "taylor5",
        topic = ExamTopic.TAYLOR,
        points = 7,
        questionLatex = "Bestimmen Sie die Taylorreihe von \\(f(x) = \\frac{1}{1-x}\\) um \\(x_0 = 0\\) und geben Sie den Konvergenzradius an.",
        solutionSteps = listOf(
            "Geometrische Reihe: \\(\\frac{1}{1-x} = \\sum_{n=0}^{\\infty} x^n\\) für \\(|x| < 1\\)",
            "Alternativ per Taylor: \\(f^{(n)}(x) = \\frac{n!}{(1-x)^{n+1}}\\)",
            "\\(f^{(n)}(0) = n!\\)",
            "\\(T(x) = \\sum_{n=0}^{\\infty} \\frac{f^{(n)}(0)}{n!}x^n = \\sum_{n=0}^{\\infty} x^n\\)",
            "Konvergenzradius: \\(R = \\lim_{n \\to \\infty} \\left|\\frac{a_n}{a_{n+1}}\\right| = \\lim_{n \\to \\infty} 1 = 1\\)",
            "Die Reihe konvergiert für \\(|x| < 1\\)"
        ),
        hints = listOf("Geometrische Reihe als Spezialfall", "Quotientenkriterium für R"),
        difficulty = 2
    ),
    ExamProblem(
        id = "taylor6",
        topic = ExamTopic.TAYLOR,
        points = 6,
        questionLatex = "Berechnen Sie mit Hilfe von Taylorreihen:\n\\[\\lim_{x \\to 0} \\frac{1 - \\cos x}{x \\cdot \\sin x}\\]",
        solutionSteps = listOf(
            "\\(\\cos x = 1 - \\frac{x^2}{2} + \\frac{x^4}{24} - O(x^6)\\)",
            "\\(1 - \\cos x = \\frac{x^2}{2} - \\frac{x^4}{24} + O(x^6)\\)",
            "\\(\\sin x = x - \\frac{x^3}{6} + O(x^5)\\)",
            "\\(x \\cdot \\sin x = x^2 - \\frac{x^4}{6} + O(x^6)\\)",
            "\\(\\frac{1 - \\cos x}{x \\sin x} = \\frac{\\frac{x^2}{2} - \\frac{x^4}{24} + O(x^6)}{x^2 - \\frac{x^4}{6} + O(x^6)}\\)",
            "\\(= \\frac{\\frac{1}{2} - \\frac{x^2}{24} + O(x^4)}{1 - \\frac{x^2}{6} + O(x^4)} \\to \\frac{1/2}{1} = \\frac{1}{2}\\)"
        ),
        hints = listOf("Zähler und Nenner entwickeln", "x² ausklammern"),
        difficulty = 2
    )
)

// ════════════════════════════════════════════
//  LaTeX RENDERING (reuse from MathTrainer)
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
    var heightDp by remember(text) { mutableStateOf((estimatedLines * 24 + 16).dp) }

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
                            if (h > 0) heightDp = (h + 8).dp
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

// ════════════════════════════════════════════
//  MAIN SCREEN
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSimulatorScreen(onBackClick: () -> Unit) {
    var examState by remember { mutableStateOf<ExamState?>(null) }
    var showStartDialog by remember { mutableStateOf(true) }
    var selectedDuration by remember { mutableStateOf(90) }
    var selectedProblemCount by remember { mutableStateOf(5) }
    val context = LocalContext.current
    val currencyRepository = remember { CurrencyRepository.getInstance(context) }

    // Timer
    var remainingSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(examState) {
        examState?.let { state ->
            if (!state.isFinished) {
                val endTime = state.startTime + state.totalTimeMinutes * 60 * 1000L
                while (true) {
                    val now = System.currentTimeMillis()
                    remainingSeconds = maxOf(0, (endTime - now) / 1000)
                    if (remainingSeconds <= 0) {
                        examState = state.copy(isFinished = true)
                        break
                    }
                    delay(1000)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (examState != null && !examState!!.isFinished) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            Text(
                                String.format("%02d:%02d", mins, secs),
                                fontWeight = FontWeight.Bold,
                                color = if (remainingSeconds < 300) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text("Klausur-Simulator", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (examState != null && !examState!!.isFinished) {
                            examState = examState!!.copy(isFinished = true)
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    examState?.let { state ->
                        if (!state.isFinished) {
                            Text(
                                "${state.currentProblemIndex + 1}/${state.problems.size}",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            showStartDialog -> {
                StartExamDialog(
                    selectedDuration = selectedDuration,
                    onDurationChange = { selectedDuration = it },
                    selectedProblemCount = selectedProblemCount,
                    onProblemCountChange = { selectedProblemCount = it },
                    onStart = {
                        val selectedProblems = examProblems.shuffled().take(selectedProblemCount)
                        examState = ExamState(
                            problems = selectedProblems,
                            totalTimeMinutes = selectedDuration
                        )
                        remainingSeconds = selectedDuration * 60L
                        showStartDialog = false
                    },
                    onDismiss = onBackClick,
                    modifier = Modifier.padding(padding)
                )
            }
            examState != null && examState!!.isFinished -> {
                ResultsView(
                    state = examState!!,
                    onRestart = {
                        showStartDialog = true
                        examState = null
                    },
                    onBackClick = onBackClick,
                    modifier = Modifier.padding(padding)
                )
            }
            examState != null -> {
                ExamProblemView(
                    state = examState!!,
                    onAnswerChange = { answer ->
                        val id = examState!!.problems[examState!!.currentProblemIndex].id
                        examState!!.answers[id] = answer
                    },
                    onNext = {
                        if (examState!!.currentProblemIndex < examState!!.problems.size - 1) {
                            examState = examState!!.copy(
                                currentProblemIndex = examState!!.currentProblemIndex + 1,
                                showSolution = false
                            )
                        }
                    },
                    onPrevious = {
                        if (examState!!.currentProblemIndex > 0) {
                            examState = examState!!.copy(
                                currentProblemIndex = examState!!.currentProblemIndex - 1,
                                showSolution = false
                            )
                        }
                    },
                    onShowSolution = {
                        examState = examState!!.copy(showSolution = true)
                    },
                    onGradeChange = { points ->
                        val id = examState!!.problems[examState!!.currentProblemIndex].id
                        examState!!.selfGradedPoints[id] = points
                    },
                    onFinish = {
                        examState = examState!!.copy(isFinished = true)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun StartExamDialog(
    selectedDuration: Int,
    onDurationChange: (Int) -> Unit,
    selectedProblemCount: Int,
    onProblemCountChange: (Int) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
        }

        Text(
            "KIT HM1 Klausur-Simulator",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            "Trainiere unter echten Klausurbedingungen mit Aufgaben aus vergangenen KIT-Prüfungen",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        // Duration selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Zeitlimit", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30 to "30 Min", 60 to "60 Min", 90 to "90 Min", 120 to "120 Min").forEach { (mins, label) ->
                        FilterChip(
                            selected = selectedDuration == mins,
                            onClick = { onDurationChange(mins) },
                            label = { Text(label, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Problem count selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Anzahl Aufgaben", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 7, 10).forEach { count ->
                        FilterChip(
                            selected = selectedProblemCount == count,
                            onClick = { onProblemCountChange(count) },
                            label = { Text("$count", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Topic overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Themengebiete", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ExamTopic.entries.forEach { topic ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(topic.icon, null, tint = topic.color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(topic.displayName, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        val count = examProblems.count { it.topic == topic }
                        Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Start button
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Klausur starten", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        TextButton(onClick = onDismiss) {
            Text("Abbrechen")
        }
    }
}

@Composable
private fun ExamProblemView(
    state: ExamState,
    onAnswerChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShowSolution: () -> Unit,
    onGradeChange: (Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val problem = state.problems[state.currentProblemIndex]
    val currentAnswer = state.answers[problem.id] ?: ""
    val currentGrade = state.selfGradedPoints[problem.id]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Topic badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = problem.topic.color.copy(alpha = 0.15f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(problem.topic.icon, null, tint = problem.topic.color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(problem.topic.displayName, fontSize = 12.sp, color = problem.topic.color, fontWeight = FontWeight.Medium)
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF9800).copy(alpha = 0.15f)
            ) {
                Text(
                    "${problem.points} Punkte",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Question
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Aufgabe ${state.currentProblemIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LatexText(problem.questionLatex, fontSize = 16)
            }
        }

        // Hints button
        var showHints by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showHints = !showHints },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (showHints) "Hinweise ausblenden" else "Hinweise anzeigen")
        }

        if (showHints) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(Modifier.padding(12.dp)) {
                    problem.hints.forEachIndexed { idx, hint ->
                        Row {
                            Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Spacer(Modifier.width(8.dp))
                            Text(hint, color = Color(0xFFE65100))
                        }
                    }
                }
            }
        }

        // Solution (if shown)
        if (state.showSolution) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF1565C0))
                        Spacer(Modifier.width(8.dp))
                        Text("Musterlösung", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    }
                    Spacer(Modifier.height(12.dp))
                    problem.solutionSteps.forEachIndexed { idx, step ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${idx + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                            LatexText(step, fontSize = 14, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Selbstbewertung:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0..problem.points).forEach { pts ->
                            FilterChip(
                                selected = currentGrade == pts,
                                onClick = { onGradeChange(pts) },
                                label = { Text("$pts") }
                            )
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = onShowSolution,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text("Lösung anzeigen")
            }
        }

        Spacer(Modifier.weight(1f))

        // Navigation
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = state.currentProblemIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(4.dp))
                Text("Zurück")
            }

            if (state.currentProblemIndex < state.problems.size - 1) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Weiter")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Done, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Abgeben")
                }
            }
        }
    }
}

@Composable
private fun ResultsView(
    state: ExamState,
    onRestart: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPoints = state.problems.sumOf { it.points }
    val earnedPoints = state.selfGradedPoints.values.sum()
    val percentage = if (totalPoints > 0) (earnedPoints * 100f / totalPoints) else 0f
    val grade = when {
        percentage >= 90 -> "1,0"
        percentage >= 80 -> "1,7"
        percentage >= 70 -> "2,3"
        percentage >= 60 -> "3,0"
        percentage >= 50 -> "3,7"
        percentage >= 40 -> "4,0"
        else -> "5,0"
    }

    // Topic analysis
    val topicScores = ExamTopic.entries.associateWith { topic ->
        val problems = state.problems.filter { it.topic == topic }
        val maxPts = problems.sumOf { it.points }
        val earned = problems.sumOf { state.selfGradedPoints[it.id] ?: 0 }
        if (maxPts > 0) (earned * 100f / maxPts) else -1f
    }.filter { it.value >= 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result header
        val gradeColor = when {
            percentage >= 60 -> Color(0xFF4CAF50)
            percentage >= 40 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(gradeColor.copy(alpha = 0.15f))
                .border(4.dp, gradeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(grade, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = gradeColor)
                Text("Note", fontSize = 14.sp, color = gradeColor)
            }
        }

        Text(
            "$earnedPoints / $totalPoints Punkte",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            String.format("%.1f%%", percentage),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Topic breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Themenanalyse", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                topicScores.toList().sortedBy { it.second }.forEach { (topic, score) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(topic.icon, null, tint = topic.color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(topic.displayName, fontSize = 14.sp, modifier = Modifier.weight(1f))

                        val scoreColor = when {
                            score >= 70 -> Color(0xFF4CAF50)
                            score >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scoreColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${score.toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recommendations
        val weakTopics = topicScores.filter { it.value < 60 }.keys.toList()
        if (weakTopics.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TipsAndUpdates, null, tint = Color(0xFFE65100))
                        Spacer(Modifier.width(8.dp))
                        Text("Empfehlung", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Fokussiere dich in den nächsten Tagen auf: ${weakTopics.joinToString(", ") { it.displayName }}",
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Neue Klausur", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zurück zum Menü")
        }
    }
}
