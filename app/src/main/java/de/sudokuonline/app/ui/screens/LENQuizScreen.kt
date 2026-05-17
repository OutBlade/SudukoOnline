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
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.delay

// ════════════════════════════════════════════
//  LEN TOPICS & DATA MODELS
// ════════════════════════════════════════════

private enum class LENTopic(val displayName: String, val icon: ImageVector, val color: Color, val weight: String) {
    ORTSKURVE("Ortskurve", Icons.Default.Timeline, Color(0xFF00897B), "~20%"),
    NETZWERK("Netzwerkanalyse", Icons.Default.AccountTree, Color(0xFF1E88E5), "~14%"),
    WECHSELSTROM("Wechselstromlehre", Icons.Default.Bolt, Color(0xFFE53935), "~21%"),
    BODE("Bodediagramm", Icons.Default.ShowChart, Color(0xFF8E24AA), "~24%"),
    OPV("Operationsverstärker", Icons.Default.Memory, Color(0xFFFF8F00), "~15%")
}

private data class LENProblem(
    val id: String,
    val topic: LENTopic,
    val points: Int,
    val questionLatex: String,
    val solutionSteps: List<String>,
    val hints: List<String>,
    val difficulty: Int // 1-3
)

private data class LENState(
    val problems: List<LENProblem>,
    val currentProblemIndex: Int = 0,
    val answers: MutableMap<String, String> = mutableMapOf(),
    val selfGradedPoints: MutableMap<String, Int> = mutableMapOf(),
    val startTime: Long = System.currentTimeMillis(),
    val totalTimeMinutes: Int = 120,
    val isFinished: Boolean = false,
    val showSolution: Boolean = false
)

// ════════════════════════════════════════════
//  LEN PROBLEMS (KIT, Prof. Dössel, WS17–SS22)
// ════════════════════════════════════════════

private val lenProblems = listOf(

    // ══════════════════════════════════════
    //  ORTSKURVE
    // ══════════════════════════════════════

    LENProblem(
        id = "ok1",
        topic = LENTopic.ORTSKURVE,
        points = 5,
        questionLatex = """Gegeben: \(R\), \(L\), \(C\) in Parallelschaltung.

(a) Berechnen Sie die Admittanz \(Y(j\omega)\) und trennen Sie nach Real- und Imaginärteil.

(b) Bestimmen Sie die Resonanzkreisfrequenz \(\omega_0\).""",
        solutionSteps = listOf(
            """\(Y = \frac{1}{R} + \frac{1}{j\omega L} + j\omega C = \frac{1}{R} + j\!\left(\omega C - \frac{1}{\omega L}\right)\)""",
            """Realteil: \(\mathrm{Re}\{Y\} = \frac{1}{R}\), Imaginärteil: \(\mathrm{Im}\{Y\} = \omega C - \frac{1}{\omega L}\)""",
            """Resonanz bei \(\mathrm{Im}\{Y\} = 0\): \(\omega_0 C = \frac{1}{\omega_0 L} \Rightarrow \omega_0 = \frac{1}{\sqrt{LC}}\)"""
        ),
        hints = listOf("Y = 1/Z, Parallelschaltung: Y = Y₁ + Y₂ + Y₃", "Resonanz: Im{Y} = 0"),
        difficulty = 1
    ),

    LENProblem(
        id = "ok2",
        topic = LENTopic.ORTSKURVE,
        points = 5,
        questionLatex = """Gegeben: \(R\) und \(L\) in Reihenschaltung.

(a) Berechnen Sie \(Z(j\omega)\) und \(Y(j\omega)\).

(b) Beschreiben Sie die Impedanzortskurve für \(\omega: 0 \to \infty\).

(c) Was ist der Startpunkt (\(\omega \to 0\)) und der Endpunkt (\(\omega \to \infty\))?""",
        solutionSteps = listOf(
            """\(Z = R + j\omega L\)""",
            """\(Y = \frac{R - j\omega L}{R^2 + \omega^2 L^2}\)""",
            """Impedanzortskurve: Senkrechte Gerade parallel zur imaginären Achse bei \(\mathrm{Re}\{Z\} = R\)""",
            """Startpunkt \(\omega \to 0\): \(Z = R\) (reell), Endpunkt \(\omega \to \infty\): \(Z \to j\infty\) (senkrecht nach oben)"""
        ),
        hints = listOf("Z_L = jωL → 0 für ω→0, → j∞ für ω→∞", "Gerade parallel zur Im-Achse"),
        difficulty = 1
    ),

    LENProblem(
        id = "ok3",
        topic = LENTopic.ORTSKURVE,
        points = 6,
        questionLatex = """Gegeben: \(R\) und \(C\) in Reihenschaltung.

(a) Berechnen Sie \(Z(j\omega)\), trennen Sie nach Real-/Imaginärteil.

(b) Beschreiben Sie die Impedanzortskurve und die Admittanzortskurve.

(c) Startpunkt und Endpunkt für beide Kurven.""",
        solutionSteps = listOf(
            """\(Z = R + \frac{1}{j\omega C} = R - \frac{j}{\omega C}\)""",
            """Realteil: \(R\), Imaginärteil: \(-\frac{1}{\omega C}\)""",
            """Impedanzortskurve: Senkrechte Gerade bei Re = R; \(\omega \to 0\): \(Z \to -j\infty\), \(\omega \to \infty\): \(Z \to R\)""",
            """Admittanzortskurve: Kreis durch Ursprung; \(\omega \to 0\): \(Y \to 0\), \(\omega \to \infty\): \(Y \to \frac{1}{R}\)"""
        ),
        hints = listOf("Z_C = 1/(jωC) → -j∞ für ω→0, → 0 für ω→∞", "Y = 1/Z: Kreis durch Ursprung"),
        difficulty = 2
    ),

    LENProblem(
        id = "ok4",
        topic = LENTopic.ORTSKURVE,
        points = 7,
        questionLatex = """Gegeben: \(R\), \(L\), \(C\) Parallelkreis mit \(R = 5\,\mathrm{k\Omega}\), \(L = 50\,\mathrm{mH}\), \(C = 200\,\mathrm{nF}\).

(a) Berechnen Sie die Güte \(Q = R\sqrt{C/L}\).

(b) Die Halbwertbreite (3-dB-Breite) des Parallelkreises ist \(\Delta\omega = \omega_0/Q\). Berechnen Sie \(\Delta\omega\) und \(\Delta f\).

(c) Interpretieren Sie: Was bedeutet eine hohe Güte physikalisch?""",
        solutionSteps = listOf(
            """\(\omega_0 = \frac{1}{\sqrt{LC}} = \frac{1}{\sqrt{50 \cdot 10^{-3} \cdot 200 \cdot 10^{-9}}} = \frac{1}{\sqrt{10^{-8}}} = 10^4\,\mathrm{s}^{-1}\)""",
            """\(Q = R\sqrt{\frac{C}{L}} = 5000 \cdot \sqrt{\frac{200 \cdot 10^{-9}}{50 \cdot 10^{-3}}} = 5000 \cdot \sqrt{4 \cdot 10^{-6}} = 5000 \cdot 2 \cdot 10^{-3} = 10\)""",
            """\(\Delta\omega = \frac{\omega_0}{Q} = \frac{10^4}{10} = 1000\,\mathrm{s}^{-1}\), \(\quad \Delta f = \frac{\Delta\omega}{2\pi} \approx 159\,\mathrm{Hz}\)""",
            """Hohe Güte = schmales Resonanzband = geringe Dämpfung = hohe Selektivität"""
        ),
        hints = listOf("Q = R·√(C/L) für Parallelkreis", "Δω = ω₀/Q"),
        difficulty = 2
    ),

    LENProblem(
        id = "ok5",
        topic = LENTopic.ORTSKURVE,
        points = 8,
        questionLatex = """Gegeben: Drei unbekannte passive Bauteile X₁, X₂, X₃ in Parallelschaltung.
Messungen bei \(\omega_a = 500\,\mathrm{s}^{-1}\): alle drei Ströme gleich groß, \(I_2\) ist \(+90°\) und \(I_3\) ist \(-90°\) gegenüber \(I_1\) phasenverschoben.

(a) Um welche Bauteile handelt es sich?

(b) Was gilt für \(I_\mathrm{ges}\) bei \(\omega_a\)? Wie heißt diese Frequenz?

(c) Bestimmen Sie aus \(\hat{U} = 1\,\mathrm{V}\) und \(|I_1| = 20\,\mathrm{mA}\) den Wert von Bauteil 1.""",
        solutionSteps = listOf(
            """X₁: kein Phasenversatz → Widerstand \(R\); X₂: +90° voreilend → Kondensator \(C\); X₃: −90° nacheilend → Spule \(L\)""",
            """Bei \(\omega_a\): \(I_2 + I_3 = 0\) (heben sich auf), \(I_\mathrm{ges} = I_1\) ist rein reell → \(0°\) Phasverschiebung = Resonanzfrequenz""",
            """\(R = \frac{\hat{U}}{|I_1|} = \frac{1}{20 \cdot 10^{-3}} = 50\,\Omega\)"""
        ),
        hints = listOf("+90° = voreilend = C, −90° = nacheilend = L, kein Versatz = R", "Resonanz: Im-Anteile kompensieren sich"),
        difficulty = 2
    ),

    LENProblem(
        id = "ok6",
        topic = LENTopic.ORTSKURVE,
        points = 6,
        questionLatex = """Gegeben: Schaltung mit \(R_1 = 100\,\Omega\) in Reihe zu \((R_2 \parallel C)\), mit \(R_2 = 300\,\Omega\), \(\omega = 2\pi \cdot 50\,\mathrm{Hz}\).

(a) Stellen Sie \(Z(j\omega, C)\) auf und trennen Sie nach Real- und Imaginärteil.

(b) Für welchen Wert \(C_\mathrm{min}\) wird \(|\mathrm{Im}\{Z\}|\) minimal?""",
        solutionSteps = listOf(
            """\(Z = R_1 + \frac{R_2}{1 + j\omega C R_2} = R_1 + \frac{R_2(1 - j\omega C R_2)}{1 + \omega^2 C^2 R_2^2}\)""",
            """\(\mathrm{Re}\{Z\} = R_1 + \frac{R_2}{1 + \omega^2 C^2 R_2^2}\), \quad \mathrm{Im}\{Z\} = \frac{-\omega C R_2^2}{1 + \omega^2 C^2 R_2^2}\)""",
            """Minimum von \(|\mathrm{Im}\{Z\}|\) durch Ableitung nach \(C\) und Nullsetzen: \(C_\mathrm{min} = \frac{1}{\omega R_2}\)""",
            """Mit \(\omega = 2\pi \cdot 50\) und \(R_2 = 300\): \(C_\mathrm{min} \approx 10{,}6\,\mu\mathrm{F}\)"""
        ),
        hints = listOf("Z_parallel = R₂/(1+jωCR₂)", "Ableitung dIm/dC = 0 setzen"),
        difficulty = 3
    ),

    LENProblem(
        id = "ok7",
        topic = LENTopic.ORTSKURVE,
        points = 5,
        questionLatex = """Zuordnung: Welches Bauteilverhalten beschreibt die Impedanzortskurve?

(A) Senkrechte Gerade im rechten Halbplan, Startpunkt auf reeller Achse → Startpunkt \(\omega = 0\) bei \(R\).

(B) Halbkreis im unteren Halbplan, Startpunkt bei \(\omega \to 0\) im Ursprung.

(C) Senkrechte Gerade im rechten Halbplan, die bei \(\omega \to 0\) gegen \(-j\infty\) geht.

Ordnen Sie zu: (A) = ?, (B) = ?, (C) = ?

Mögliche Schaltungen: \(R+L\) (Reihenschaltung), \(R \parallel C\), \(R+C\) (Reihenschaltung).""",
        solutionSteps = listOf(
            """(A): Startpunkt reell = kein C-Anteil bei ω→0 → \(R + L\) Reihenschaltung ✓""",
            """(B): Halbkreis unten = RC-Parallelschaltung (Y ist senkrechte Gerade → Z ist Kreis durch Ursprung, unten weil C) → \(R \parallel C\) ✓""",
            """(C): ω→0 geht nach −j∞ = C dominiert bei tiefen Frequenzen → \(R + C\) Reihenschaltung ✓"""
        ),
        hints = listOf("L→0 für ω→0, L→∞ für ω→∞; C→∞ für ω→0, C→0 für ω→∞", "Halbkreis im Z-Bild = Parallelschaltung"),
        difficulty = 2
    ),

    // ══════════════════════════════════════
    //  NETZWERKANALYSE
    // ══════════════════════════════════════

    LENProblem(
        id = "nw1",
        topic = LENTopic.NETZWERK,
        points = 4,
        questionLatex = """Theoriefragen zur Netzwerkanalyse:

(a) Nennen Sie die beiden Kirchhoffschen Gesetze mit physikalischer Begründung.

(b) Nennen Sie drei formale Analyseverfahren für elektrische Netzwerke.

(c) Was versteht man unter Leistungsanpassung? Geben Sie die Bedingung an.""",
        solutionSteps = listOf(
            """Knotenregel: \(\sum I_k = 0\) (Ladungserhaltung — kein Ladungsspeicher am Knoten)""",
            """Maschenregel: \(\sum U_m = 0\) (Energieerhaltung — geschlossener Umlauf)""",
            """Drei Verfahren: Zweigstromverfahren, Maschenstromverfahren, Knotenpotentialverfahren""",
            """Leistungsanpassung: maximale Leistung am Verbraucher wenn \(R_L = R_i\) (Lastwiderstand = Innenwiderstand)"""
        ),
        hints = listOf("KCL = Kirchhoff's Current Law, KVL = Kirchhoff's Voltage Law", "Anpassung: R_L = R_i"),
        difficulty = 1
    ),

    LENProblem(
        id = "nw2",
        topic = LENTopic.NETZWERK,
        points = 5,
        questionLatex = """Quellenumwandlung:

Gegeben: Spannungsquelle \(U_0 = 6\,\mathrm{V}\) mit Innenwiderstand \(R_i = 2\,\Omega\).

(a) Wandeln Sie in eine äquivalente Stromquelle um. Geben Sie \(I_K\) und \(R_i\) an.

(b) Skizzieren Sie das Ersatzschaltbild der realen Spannungsquelle: \(U_L(I_L)\). Bei welchem \(R_L\) gilt Leistungsanpassung?""",
        solutionSteps = listOf(
            """Stromquelle: \(I_K = \frac{U_0}{R_i} = \frac{6}{2} = 3\,\mathrm{A}\), Parallelwiderstand \(R_p = R_i = 2\,\Omega\)""",
            """\(U_L = U_0 - R_i \cdot I_L = 6 - 2 I_L\) (lineare Kennlinie: Leerlauf 6V, Kurzschluss 3A)""",
            """Leistungsanpassung: \(R_L = R_i = 2\,\Omega\), dann: \(P_{L,\max} = \frac{U_0^2}{4 R_i} = \frac{36}{8} = 4{,}5\,\mathrm{W}\)"""
        ),
        hints = listOf("I_K = U_0/R_i, Parallelwiderstand bleibt gleich", "P_L = U_L²/R_L maximieren"),
        difficulty = 1
    ),

    LENProblem(
        id = "nw3",
        topic = LENTopic.NETZWERK,
        points = 6,
        questionLatex = """Stern-Dreieck-Transformation:

Gegeben: Dreieck mit \(R_{12} = 6\,\Omega\), \(R_{23} = 3\,\Omega\), \(R_{31} = 2\,\Omega\).

(a) Berechnen Sie das äquivalente Stern-Netzwerk \(R_1\), \(R_2\), \(R_3\).

(b) Geben Sie die allgemeine Formel an (Dreieck → Stern).""",
        solutionSteps = listOf(
            """Allgemein: \(R_k = \frac{\text{Produkt der anliegenden Dreieck-Widerstände}}{\text{Summe aller Dreieck-Widerstände}}\)""",
            """\(R_\Sigma = R_{12} + R_{23} + R_{31} = 6 + 3 + 2 = 11\,\Omega\)""",
            """\(R_1 = \frac{R_{12} \cdot R_{31}}{R_\Sigma} = \frac{6 \cdot 2}{11} = \frac{12}{11}\,\Omega\)""",
            """\(R_2 = \frac{R_{12} \cdot R_{23}}{R_\Sigma} = \frac{6 \cdot 3}{11} = \frac{18}{11}\,\Omega\)""",
            """\(R_3 = \frac{R_{23} \cdot R_{31}}{R_\Sigma} = \frac{3 \cdot 2}{11} = \frac{6}{11}\,\Omega\)"""
        ),
        hints = listOf("Dreieck→Stern: R_k = (Produkt der beiden angrenzenden Δ-Widerstände) / Σ Δ-Widerstände", "Σ = 11 Ω"),
        difficulty = 2
    ),

    LENProblem(
        id = "nw4",
        topic = LENTopic.NETZWERK,
        points = 7,
        questionLatex = """Knotenpotentialverfahren:

Gegeben: Netzwerk mit 3 unabhängigen Knoten A, B, C (D = Referenz). Leitwertmatrix:

\[\begin{pmatrix} G_{AA} & -G_{AB} & -G_{AC} \\ -G_{BA} & G_{BB} & -G_{BC} \\ -G_{CA} & -G_{CB} & G_{CC} \end{pmatrix} \begin{pmatrix} U_A \\ U_B \\ U_C \end{pmatrix} = \begin{pmatrix} I_{A} \\ I_{B} \\ I_{C} \end{pmatrix}\]

(a) Erklären Sie: Was steht auf der Hauptdiagonale? Was in den Nebendiagonalen?

(b) Wie berechnet man \(G_{AA}\)? Wie \(-G_{AB}\)?""",
        solutionSteps = listOf(
            """Hauptdiagonale \(G_{kk}\): Summe aller Leitwerte, die am Knoten \(k\) anliegen""",
            """Nebendiagonale \(-G_{kl}\): negativer Leitwert zwischen Knoten \(k\) und \(l\)""",
            """\(G_{AA} = \sum_{j} G_{Aj}\) (alle Leitwerte, die an A hängen)""",
            """\(-G_{AB} = -G_{BA}\) = negativer Leitwert des direkten Zweiges zwischen A und B (0 wenn kein direkter Zweig)"""
        ),
        hints = listOf("Hauptdiagonale immer positiv (Summe der Leitwerte)", "Nebendiagonale immer negativ"),
        difficulty = 2
    ),

    LENProblem(
        id = "nw5",
        topic = LENTopic.NETZWERK,
        points = 8,
        questionLatex = """Cramersche Regel:

Gegeben: Gleichungssystem \([G][U] = [I]\) mit

\[[G] = \begin{pmatrix} 3 & -1 \\ -1 & 2 \end{pmatrix}, \quad [I] = \begin{pmatrix} 5 \\ 4 \end{pmatrix}\]

(a) Berechnen Sie \(\det([G])\) mit der Sarrus-Regel (oder direkt).

(b) Lösen Sie nach \(U_1\) und \(U_2\) mit der Cramerschen Regel.""",
        solutionSteps = listOf(
            """\(\det([G]) = 3 \cdot 2 - (-1)(-1) = 6 - 1 = 5\)""",
            """\(U_1 = \frac{\det\!\begin{pmatrix} 5 & -1 \\ 4 & 2 \end{pmatrix}}{5} = \frac{5 \cdot 2 - (-1) \cdot 4}{5} = \frac{14}{5} = 2{,}8\,\mathrm{V}\)""",
            """\(U_2 = \frac{\det\!\begin{pmatrix} 3 & 5 \\ -1 & 4 \end{pmatrix}}{5} = \frac{3 \cdot 4 - 5 \cdot (-1)}{5} = \frac{17}{5} = 3{,}4\,\mathrm{V}\)"""
        ),
        hints = listOf("det(2×2) = ad - bc", "Cramer: U_k = det(G mit k-ter Spalte durch I) / det(G)"),
        difficulty = 2
    ),

    LENProblem(
        id = "nw6",
        topic = LENTopic.NETZWERK,
        points = 6,
        questionLatex = """Maschenstromverfahren:

(a) Nennen Sie das Aufstellungsprinzip für die Maschenimpedanzmatrix \([Z]\).

(b) Was steht auf der Hauptdiagonale von \([Z]\)? Was in den Nebendiagonalen?

(c) Wie unterscheidet sich das Maschenstromverfahren vom Knotenpotentialverfahren in der Wahl der Unbekannten?""",
        solutionSteps = listOf(
            """Aufstellung: Für jede unabhängige Masche eine Gleichung aufstellen (Maschenregel: ΣZ·I = ΣU)""",
            """Hauptdiagonale \(Z_{kk}\): Summe aller Impedanzen in Masche k""",
            """Nebendiagonale \(Z_{kl}\): negative gemeinsame Impedanz zwischen Masche k und l (wenn gleicher Umlaufsinn), positiv wenn entgegengesetzt""",
            """Unterschied: Maschenstromverfahren hat Ströme als Unbekannte; Knotenpotentialverfahren hat Spannungen als Unbekannte"""
        ),
        hints = listOf("Hauptdiagonale: Summe der Zweigimpedanzen der Masche", "Nebendiagonale: gemeinsame Impedanzen (mit Vorzeichen)"),
        difficulty = 2
    ),

    // ══════════════════════════════════════
    //  KOMPLEXE WECHSELSTROMLEHRE
    // ══════════════════════════════════════

    LENProblem(
        id = "wl1",
        topic = LENTopic.WECHSELSTROM,
        points = 5,
        questionLatex = """Leistungsberechnung:

Gegeben: \(\underline{U} = 12\,\mathrm{V}\), \(\underline{I} = (3 + j4)\,\mathrm{A}\).

(a) Berechnen Sie die komplexe Scheinleistung \(\underline{S} = \underline{U} \cdot \underline{I}^*\).

(b) Bestimmen Sie Wirkleistung \(P\), Blindleistung \(Q\) und Leistungsfaktor \(\cos\varphi\).""",
        solutionSteps = listOf(
            """\(\underline{I}^* = (3 - j4)\,\mathrm{A}\)""",
            """\(\underline{S} = 12 \cdot (3 - j4) = (36 - j48)\,\mathrm{VA}\)""",
            """\(P = \mathrm{Re}\{\underline{S}\} = 36\,\mathrm{W}\), \(\quad Q = \mathrm{Im}\{\underline{S}\} = -48\,\mathrm{var}\)""",
            """\(|\underline{S}| = \sqrt{36^2 + 48^2} = 60\,\mathrm{VA}\), \(\quad \cos\varphi = \frac{P}{|\underline{S}|} = \frac{36}{60} = 0{,}6\)""",
            """Negatives \(Q\): kapazitive Last"""
        ),
        hints = listOf("S = U · I* (konjugiert komplex!)", "P = Re{S}, Q = Im{S}, cos φ = P/|S|"),
        difficulty = 1
    ),

    LENProblem(
        id = "wl2",
        topic = LENTopic.WECHSELSTROM,
        points = 6,
        questionLatex = """Induktiv oder kapazitiv?

Gegeben: Impedanz \(\underline{Z}_V = (1{,}2 + j1{,}6)\,\Omega\).

(a) Ist der Verbraucher induktiv oder kapazitiv? Begründen Sie.

(b) Welche einfache Schaltung realisiert diese Impedanz?

(c) Berechnen Sie für \(\omega = 1000\,\mathrm{s}^{-1}\) die Werte von \(R\) und \(L\).""",
        solutionSteps = listOf(
            """Imaginärteil > 0 → induktiv (Spule dominiert)""",
            """Schaltung: \(R\) und \(L\) in Reihe: \(\underline{Z} = R + j\omega L\)""",
            """\(R = 1{,}2\,\Omega\)""",
            """\(\omega L = 1{,}6 \Rightarrow L = \frac{1{,}6}{1000} = 1{,}6\,\mathrm{mH}\)"""
        ),
        hints = listOf("Im{Z} > 0 → induktiv; Im{Z} < 0 → kapazitiv", "Z = R + jωL für RL-Reihenschaltung"),
        difficulty = 1
    ),

    LENProblem(
        id = "wl3",
        topic = LENTopic.WECHSELSTROM,
        points = 7,
        questionLatex = """Zeigerdiagramm:

Gegeben: Schaltung mit \(\underline{Z}_1 = (1-j1)\,\Omega\) in Reihe und \(\underline{Z}_V\) parallel zu \(\underline{Z}_2 = (1-j1)\,\Omega\).
\(\underline{U}_e = 12\,\mathrm{V}\), \(\underline{I}_e = (3+j4)\,\mathrm{A}\), \(\underline{I}_V = (2-j1)\,\mathrm{A}\).

(a) Berechnen Sie \(\underline{I}_C = \underline{I}_e - \underline{I}_V\).

(b) Berechnen Sie \(\underline{U}_V = \underline{U}_e - \underline{Z}_1 \cdot \underline{I}_e\).

(c) Zeichnen Sie qualitativ das Zeigerdiagramm der Ströme.""",
        solutionSteps = listOf(
            """\(\underline{I}_C = (3+j4) - (2-j1) = (1+j5)\,\mathrm{A}\)""",
            """\(\underline{Z}_1 \cdot \underline{I}_e = (1-j1)(3+j4) = 3+j4-j3-j^2 4 = 7+j1\)""",
            """\(\underline{U}_V = 12 - (7+j1) = (5-j1)\,\mathrm{V}\)""",
            """Zeigerdiagramm: I_e ist Summe aus I_V und I_C (Knotenregel grafisch)"""
        ),
        hints = listOf("I_C = I_e - I_V (Knotenregel)", "U_V = U_e - Z₁·I_e (Maschenregel)"),
        difficulty = 2
    ),

    LENProblem(
        id = "wl4",
        topic = LENTopic.WECHSELSTROM,
        points = 8,
        questionLatex = """Resonanzfrequenz aus Blindleistungsbedingung:

Gegeben: Parallelschaltung von \(R\), \(L\), \(C\). Gesamte Blindleistung soll null sein (\(Q_e = 0\)).

(a) Drücken Sie \(Q_e\) durch Schaltungsgrößen aus.

(b) Zeigen Sie: Aus \(Q_e = 0\) folgt \(\omega_0 = \frac{1}{\sqrt{LC}}\).

(c) Was bedeutet \(Q_e = 0\) anschaulich für den Gesamtstrom?""",
        solutionSteps = listOf(
            """\(Q_e = Q_L + Q_C = \frac{U^2}{\omega L} \cdot (-1) + U^2 \omega C = U^2\!\left(\omega C - \frac{1}{\omega L}\right)\)""",
            """Wait: \(Q_L = \frac{U^2}{\omega L} > 0\) (induktiv), \(Q_C = -U^2 \omega C < 0\) (kapazitiv). Bedingung: \(\omega C = \frac{1}{\omega L}\)""",
            """\(\omega^2 = \frac{1}{LC} \Rightarrow \omega_0 = \frac{1}{\sqrt{LC}}\) ✓""",
            """\(Q_e = 0\): Gesamtstrom \(I_e\) in Phase mit \(U_e\) (rein reell) → niedrigster Gesamtstrom bei Resonanz"""
        ),
        hints = listOf("Q_L = U²/(ωL), Q_C = −U²ωC", "Q_e = 0 → Bedingung für Resonanz"),
        difficulty = 2
    ),

    LENProblem(
        id = "wl5",
        topic = LENTopic.WECHSELSTROM,
        points = 7,
        questionLatex = """Vierpol – Kettenmatrix:

(a) Definieren Sie die Kettenmatrix \([A]\) eines Vierpols durch die Gleichungen, die \(U_1, I_1\) mit \(U_2, I_2\) verknüpfen.

(b) Wann ist ein Vierpol kopplungssymmetrisch? Welche Bedingung muss die Kettenmatrix erfüllen?

(c) Geben Sie die Kettenmatrix eines idealen Übertragers (Transformators) mit Übersetzungsverhältnis \(\ddot{u} = N_1/N_2\) an.""",
        solutionSteps = listOf(
            """\(\begin{pmatrix}U_1 \\ I_1\end{pmatrix} = \begin{pmatrix}A_{11} & A_{12} \\ A_{21} & A_{22}\end{pmatrix}\begin{pmatrix}U_2 \\ -I_2\end{pmatrix}\)""",
            """Kopplungssymmetrie: \(\det([A]) = A_{11}A_{22} - A_{12}A_{21} = 1\)""",
            """Idealer Transformator: \([A_T] = \begin{pmatrix}\ddot{u} & 0 \\ 0 & 1/\ddot{u}\end{pmatrix}\)""",
            """Prüfung: \(\det([A_T]) = \ddot{u} \cdot \frac{1}{\ddot{u}} - 0 = 1\) ✓ → kopplungssymmetrisch"""
        ),
        hints = listOf("Kettenmatrix: [U1,I1]ᵀ = [A]·[U2,−I2]ᵀ", "det([A]) = 1 für kopplungssymmetrisch"),
        difficulty = 3
    ),

    LENProblem(
        id = "wl6",
        topic = LENTopic.WECHSELSTROM,
        points = 5,
        questionLatex = """Wirkungsgrad und Leistungsanpassung:

(a) Definieren Sie den Wirkungsgrad \(\eta\) bei der Übertragung von Leistung von einer Quelle (\(U_0, R_i\)) zu einem Verbraucher \(R_L\).

(b) Berechnen Sie \(\eta\) als Funktion von \(R_L/R_i\).

(c) Bei Leistungsanpassung (\(R_L = R_i\)): Wie groß ist \(\eta\)?""",
        solutionSteps = listOf(
            """\(\eta = \frac{P_L}{P_\text{ges}} = \frac{I^2 R_L}{I^2(R_i + R_L)} = \frac{R_L}{R_i + R_L}\)""",
            """\(\eta = \frac{1}{1 + R_i/R_L}\) — steigt mit wachsendem \(R_L/R_i\)""",
            """Bei \(R_L = R_i\): \(\eta = \frac{R_i}{2R_i} = 0{,}5 = 50\,\%\)""",
            """Fazit: Leistungsanpassung ≠ optimaler Wirkungsgrad (η = 50 %, nicht 100 %)"""
        ),
        hints = listOf("η = P_L / P_ges = R_L/(R_i + R_L)", "Bei Anpassung: η = 50%"),
        difficulty = 1
    ),

    // ══════════════════════════════════════
    //  BODEDIAGRAMM
    // ══════════════════════════════════════

    LENProblem(
        id = "bd1",
        topic = LENTopic.BODE,
        points = 5,
        questionLatex = """Hochpass 1. Ordnung (R-C):

Gegeben: \(C\) in Reihe, \(R\) als Ausgangs-Parallelwiderstand (Spannungsteiler).

(a) Stellen Sie \(G(j\omega) = U_a/U_e\) auf.

(b) Normieren Sie mit \(\Omega = \omega/\omega_g\), wobei \(\omega_g = 1/(RC)\).

(c) Wie lautet der normierte Amplitudengang in dB?""",
        solutionSteps = listOf(
            """\(G(j\omega) = \frac{R}{R + 1/(j\omega C)} = \frac{j\omega RC}{1 + j\omega RC}\)""",
            """Mit \(\Omega = \omega RC\): \(G_{n}(j\Omega) = \frac{j\Omega}{1 + j\Omega}\)""",
            """\(a_v = 20\log|G_n| = 20\log\Omega - 20\log\sqrt{1 + \Omega^2}\)""",
            """Für \(\Omega \ll 1\): \(a_v \approx 20\log\Omega\) (+20 dB/Dek. Steigung von links)""",
            """Für \(\Omega \gg 1\): \(a_v \approx 0\,\mathrm{dB}\) (Durchlass)"""
        ),
        hints = listOf("Spannungsteiler: G = Z_R / (Z_R + Z_C)", "Normierung: Ω = ω/ω_g mit ω_g = 1/(RC)"),
        difficulty = 1
    ),

    LENProblem(
        id = "bd2",
        topic = LENTopic.BODE,
        points = 5,
        questionLatex = """Tiefpass 1. Ordnung (R-L):

Gegeben: \(R\) in Reihe, \(L\) als Ausgangsimpedanz.

(a) Stellen Sie \(G(j\omega) = U_a/U_e\) auf und normieren Sie.

(b) Bestimmen Sie die Grenzfrequenz \(\omega_g\) und den Wert \(|G(j\omega_g)|\).

(c) Zeichnen Sie qualitativ Amplitudengang und Phasengang.""",
        solutionSteps = listOf(
            """\(G = \frac{j\omega L}{R + j\omega L} = \frac{j\Omega}{1+j\Omega}\) — Moment, das ist Hochpass!""",
            """Richtig: Tiefpass R-L: R als Ausgang, L in Reihe: \(G = \frac{R}{R + j\omega L} = \frac{1}{1 + j\Omega}\) mit \(\Omega = \omega L/R\)""",
            """\(\omega_g = R/L\). Bei \(\omega = \omega_g\): \(|G| = \frac{1}{\sqrt{2}}\) entspricht \(-3\,\mathrm{dB}\)""",
            """Amplitudengang: 0 dB bei ω ≪ ω_g, fällt mit −20 dB/Dekade ab ω_g""",
            """Phasengang: 0° bei ω ≪ ω_g, −45° bei ω_g, −90° bei ω ≫ ω_g"""
        ),
        hints = listOf("Tiefpass: niedrige Frequenzen durch, R als Ausgang", "−3 dB ↔ |G| = 1/√2"),
        difficulty = 1
    ),

    LENProblem(
        id = "bd3",
        topic = LENTopic.BODE,
        points = 8,
        questionLatex = """Bodediagramm – Hochpass 2. Ordnung:

Gegeben: Übertragungsfunktion
\[G(j\omega) = \frac{(j\omega)^2}{(j\omega)^2 + j\omega \cdot d \cdot \omega_0 + \omega_0^2}\]

(a) Nennen Sie den Filtertyp.

(b) Geben Sie Amplitudengang (in dB) für \(\omega \ll \omega_0\) und \(\omega \gg \omega_0\) an.

(c) Welche Steigung hat der Amplitudengang für \(\omega \ll \omega_0\) in dB/Dekade?

(d) Was bewirkt der Parameter \(d\) (Dämpfungsgrad)?""",
        solutionSteps = listOf(
            """Filtertyp: Hochpass 2. Ordnung (Zähler: \((j\omega)^2\), Nenner: Polynom 2. Grades)""",
            """Für \(\omega \ll \omega_0\): Nenner \(\approx \omega_0^2\), also \(|G| \approx \omega^2/\omega_0^2 \to 0\)""",
            """In dB: \(a_v \approx 40\log(\omega/\omega_0)\) → Steigung \(+40\,\mathrm{dB/Dekade}\)""",
            """Für \(\omega \gg \omega_0\): Zähler dominiert Nenner → \(|G| \to 1\) → \(a_v \to 0\,\mathrm{dB}\)""",
            """Dämpfungsgrad \(d\): kleines \(d\) → Überschwingung (Resonanzüberhöhung) bei \(\omega_0\); \(d = \sqrt{2}\) → maximale Flachheit"""
        ),
        hints = listOf("Zählerordnung = Hochpassordnung", "+40 dB/Dek = 2. Ordnung"),
        difficulty = 2
    ),

    LENProblem(
        id = "bd4",
        topic = LENTopic.BODE,
        points = 7,
        questionLatex = """Normierung und Knickfrequenzen:

Gegeben:
\[G(j\omega) = \frac{10 \cdot j\omega}{(1 + j\omega/100)(1 + j\omega/1000)}\]

(a) Identifizieren Sie die einzelnen Faktoren (Verstärkung, Pol, Pol, Nullstelle).

(b) Bestimmen Sie die Knickfrequenzen und den Amplitudengang bei \(\omega = 1\).

(c) Skizzieren Sie den asymptotischen Amplitudengang.""",
        solutionSteps = listOf(
            """Faktoren: Konstante 10 (+20 dB), ein Zähler-Pol \(j\omega\) (+20 dB/Dek), zwei Nenner-Pole bei \(\omega_1 = 100\) und \(\omega_2 = 1000\,\mathrm{s}^{-1}\)""",
            """Knickfrequenzen: \(\omega_1 = 100\,\mathrm{s}^{-1}\) und \(\omega_2 = 1000\,\mathrm{s}^{-1}\)""",
            """Bei \(\omega = 1\): alle Pole inaktiv, \(|G| \approx 10 \cdot 1 = 10\) → \(a_v = 20\,\mathrm{dB}\)""",
            """Asymptotisch: +20 dB/Dek bis \(\omega_1\), dann 0 dB/Dek bis \(\omega_2\), dann −20 dB/Dek"""
        ),
        hints = listOf("Pole im Nenner: je ein Knick mit −20 dB/Dek ab Knickfrequenz", "jω im Zähler: +20 dB/Dek für alle ω"),
        difficulty = 2
    ),

    LENProblem(
        id = "bd5",
        topic = LENTopic.BODE,
        points = 8,
        questionLatex = """Filtertyp erkennen:

Ordnen Sie die folgenden normierten Übertragungsfunktionen den Filtertypen zu:

(A) \(G = \frac{1}{1+j\Omega}\)

(B) \(G = \frac{j\Omega}{1+j\Omega}\)

(C) \(G = \frac{j\Omega}{(1+j\Omega_1)(1+j\Omega_2)}\) mit \(\Omega_1 < \Omega_2\)

(D) \(G = \frac{1-j\Omega}{1+j\Omega}\)

Geben Sie für jede Funktion: Typ, Amplitudengang \(|G(\Omega=0)|\) und \(|G(\Omega \to \infty)|\).""",
        solutionSteps = listOf(
            """(A) Tiefpass 1. Ordnung: \(|G(0)| = 1\), \(|G(\infty)| = 0\)""",
            """(B) Hochpass 1. Ordnung: \(|G(0)| = 0\), \(|G(\infty)| = 1\)""",
            """(C) Bandpass: \(|G(0)| = 0\), \(|G(\infty)| = 0\), Maximum dazwischen""",
            """(D) Allpass: \(|G(\Omega)| = 1\) für alle \(\Omega\) (nur Phase dreht sich)""",
            """Allpass-Prüfung: \(|1-j\Omega| = |1+j\Omega| = \sqrt{1+\Omega^2}\) → Betrag = 1"""
        ),
        hints = listOf("|G(0)|=0 & |G(∞)|=0 → Bandpass", "|G|=const → Allpass"),
        difficulty = 2
    ),

    LENProblem(
        id = "bd6",
        topic = LENTopic.BODE,
        points = 7,
        questionLatex = """Invertierender Verstärker + Hochpass:

Gegeben: Invertierender OPV-Verstärker mit Eingangswiderstand \(R_1\) und Rückkoppelwiderstand \(R_N\).

(a) Wie lautet die Übertragungsfunktion \(G = U_a/U_e\)?

(b) Durch welche einfache Maßnahme baut man daraus einen Hochpass?

(c) Was ändert sich im Bodediagramm durch den invertierenden Betrieb (Vorzeichen)?""",
        solutionSteps = listOf(
            """\(G = -\frac{R_N}{R_1}\) (rein reell, invertierend, frequenzunabhängig)""",
            """Hochpass: \(R_1\) durch \(R_1 + \frac{1}{j\omega C}\) ersetzen: \(G = -\frac{R_N}{R_1 + 1/(j\omega C)}\)""",
            """Amplitudengang unverändert (|−k| = k), aber Phase dreht um 180°""",
            """Im Bodediagramm: Phasenkurve um \(-180°\) verschoben (bei allen Frequenzen)"""
        ),
        hints = listOf("G = −R_N/R_1 für invertierenden Verstärker", "Invertierung → Phasenversatz −180°"),
        difficulty = 2
    ),

    // ══════════════════════════════════════
    //  OPERATIONSVERSTÄRKER
    // ══════════════════════════════════════

    LENProblem(
        id = "op1",
        topic = LENTopic.OPV,
        points = 4,
        questionLatex = """Eigenschaften des idealen OPV:

(a) Nennen Sie die drei wesentlichen Eigenschaften des idealen Operationsverstärkers.

(b) Was folgt aus \(U_d = 0\) bei negativer Rückkopplung?

(c) Was folgt aus \(I_q = 0\) für die Eingangswiderstände?""",
        solutionSteps = listOf(
            """Eigenschaften: (1) Leerlaufverstärkung \(A \to \infty\), (2) Eingangswiderstand \(R_e \to \infty\) → \(I_q = 0\), (3) Ausgangswiderstand \(R_a = 0\)""",
            """Aus \(A \to \infty\) und endlicher Ausgangsspannung folgt: \(U_d = U_+ - U_- = 0\) (virtuelle Kurze)""",
            """Aus \(I_q = 0\): Kein Strom in die Eingänge → die Eingangszweige belasten die Schaltung nicht"""
        ),
        hints = listOf("U_d = 0: virtueller Kurzschluss zwischen den Eingängen", "I_q = 0: keine Eingangsströme"),
        difficulty = 1
    ),

    LENProblem(
        id = "op2",
        topic = LENTopic.OPV,
        points = 5,
        questionLatex = """Invertierender Verstärker:

Gegeben: OPV-Schaltung mit \(R_1\) am Eingang, \(R_N\) in der Rückkopplung, \(U_-\) verbunden mit Ausgang über \(R_N\), \(U_+\) geerdet.

(a) Leiten Sie \(G = U_a/U_e\) her (mit Knotenregel am invertierenden Eingang).

(b) Welchen Wert hat die Eingangsspannung am invertierenden Eingang (\(U_-\))?

(c) Bestimmen Sie \(R_N\) für eine Verstärkung von \(-10\) mit \(R_1 = 10\,\mathrm{k\Omega}\).""",
        solutionSteps = listOf(
            """Knotenregel bei \(U_-\): \(\frac{U_e - U_-}{R_1} + \frac{U_a - U_-}{R_N} = 0\)""",
            """Da \(U_d = 0\) und \(U_+ = 0\): \(U_- = 0\) (virtuell geerdet)""",
            """\(\frac{U_e}{R_1} + \frac{U_a}{R_N} = 0 \Rightarrow G = -\frac{R_N}{R_1}\)""",
            """Für \(G = -10\): \(R_N = 10 \cdot R_1 = 100\,\mathrm{k\Omega}\)"""
        ),
        hints = listOf("U− = 0 (virtuell geerdet, da U+ = 0)", "G = −R_N/R_1"),
        difficulty = 1
    ),

    LENProblem(
        id = "op3",
        topic = LENTopic.OPV,
        points = 5,
        questionLatex = """Nicht-invertierender Verstärker und Spannungsfolger:

(a) Leiten Sie \(G = U_a/U_e\) für den nichtinvertierenden Verstärker her (\(R_1\) nach Masse, \(R_N\) in der Rückkopplung).

(b) Was erhält man für \(R_N = 0\) und \(R_1 \to \infty\)? Wie heißt diese Schaltung?

(c) Welchen Amplitudengang und Phasengang hat der Spannungsfolger?""",
        solutionSteps = listOf(
            """Spannungsteiler Rückkopplung: \(U_- = U_a \cdot \frac{R_1}{R_1 + R_N}\)""",
            """Da \(U_- = U_+= U_e\): \(U_e = U_a \cdot \frac{R_1}{R_1+R_N} \Rightarrow G = 1 + \frac{R_N}{R_1}\)""",
            """Für \(R_N = 0\): \(G = 1\) → Spannungsfolger (engl.: unity-gain buffer)""",
            """Amplitudengang: \(|G| = 1\) → \(a_v = 0\,\mathrm{dB}\), Phasengang: \(\varphi = 0°\)"""
        ),
        hints = listOf("G = 1 + R_N/R_1 für nicht-invertierend", "Spannungsfolger: G = 1, a_v = 0 dB"),
        difficulty = 1
    ),

    LENProblem(
        id = "op4",
        topic = LENTopic.OPV,
        points = 6,
        questionLatex = """OPV-Ausgangsstrom und Sättigung:

Gegeben: Spannungsfolger, \(U_e = 5\,\mathrm{V}\), Versorgungsspannung \(\pm 15\,\mathrm{V}\), max. Ausgangsstrom \(I_{L,\max} = 20\,\mathrm{mA}\).

(a) Wie groß ist \(U_L\) (Ausgangsspannung)?

(b) Bis zu welchem minimalen \(R_L\) arbeitet der OPV ohne Sättigung?

(c) Zeichnen Sie qualitativ \(I_L\) als Funktion von \(1/R_L\).""",
        solutionSteps = listOf(
            """\(U_L = U_e = 5\,\mathrm{V}\) (Spannungsfolger, solange kein Strom-/Spannungslimit)""",
            """\(I_L = U_L/R_L = 5/R_L \leq 20\,\mathrm{mA} \Rightarrow R_{L,\min} = \frac{5}{0{,}02} = 250\,\Omega\)""",
            """Kennlinie \(I_L(1/R_L)\): Gerade mit Steigung \(U_e = 5\), Sättigungsknick bei \(1/R_L = 4\,\mathrm{mS}\)"""
        ),
        hints = listOf("Spannungsfolger: U_L = U_e (Einheitsverstärkung)", "I_L = U_L/R_L, begrenzt durch I_L,max"),
        difficulty = 2
    ),

    LENProblem(
        id = "op5",
        topic = LENTopic.OPV,
        points = 6,
        questionLatex = """OPV-Schaltungsentwurf:

Gegeben: Sie möchten eine Schaltung entwerfen, bei der \(U_a = -2 \cdot U_e\) gilt, unabhängig von der Last \(R_L\).

(a) Welche Grundschaltung verwenden Sie?

(b) Dimensionieren Sie \(R_1\) und \(R_N\) für \(G = -2\) (wählen Sie \(R_1 = 10\,\mathrm{k\Omega}\)).

(c) Warum ist die Ausgangsspannung unabhängig von \(R_L\) (idealer OPV)?""",
        solutionSteps = listOf(
            """Invertierender Verstärker: \(G = -R_N/R_1 = -2\)""",
            """\(R_N = 2 \cdot R_1 = 20\,\mathrm{k\Omega}\)""",
            """Der ideale OPV hat Ausgangswiderstand \(R_a = 0\) → keine Spannungsabfall über Ausgangsimpedanz → \(U_a\) lastunabhängig""",
            """Praktisch: Gültig solange Ausgangsstrom unter \(I_{L,\max}\) bleibt"""
        ),
        hints = listOf("G = −R_N/R_1", "Idealer OPV: R_a = 0 → lastunabhängige Ausgangsspannung"),
        difficulty = 2
    )
)

// ════════════════════════════════════════════
//  LaTeX RENDERING (identisch zu ExamSimulatorScreen)
// ════════════════════════════════════════════

private fun buildLenLatexHtml(text: String, fontSize: Int, fontWeight: String, hexColor: String): String {
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
private fun LenLatexText(
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
                    buildLenLatexHtml(text, fontSize, fontWeight, hexColor),
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
fun LENQuizScreen(onBackClick: () -> Unit) {
    var quizState by remember { mutableStateOf<LENState?>(null) }
    var showStartDialog by remember { mutableStateOf(true) }
    var selectedDuration by remember { mutableStateOf(120) }
    var selectedProblemCount by remember { mutableStateOf(5) }
    var selectedTopics by remember { mutableStateOf(LENTopic.entries.toSet()) }

    var remainingSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(quizState) {
        quizState?.let { state ->
            if (!state.isFinished) {
                val endTime = state.startTime + state.totalTimeMinutes * 60 * 1000L
                while (true) {
                    val now = System.currentTimeMillis()
                    remainingSeconds = maxOf(0, (endTime - now) / 1000)
                    if (remainingSeconds <= 0) {
                        quizState = state.copy(isFinished = true)
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
                    if (quizState != null && !quizState!!.isFinished) {
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
                        Text("LEN Trainer", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (quizState != null && !quizState!!.isFinished) {
                            quizState = quizState!!.copy(isFinished = true)
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    quizState?.let { state ->
                        if (!state.isFinished) {
                            Text(
                                "${state.currentProblemIndex + 1}/${state.problems.size}",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00695C).copy(alpha = 0.08f)
                )
            )
        }
    ) { padding ->
        when {
            showStartDialog -> {
                LENStartDialog(
                    selectedDuration = selectedDuration,
                    onDurationChange = { selectedDuration = it },
                    selectedProblemCount = selectedProblemCount,
                    onProblemCountChange = { selectedProblemCount = it },
                    selectedTopics = selectedTopics,
                    onTopicsChange = { selectedTopics = it },
                    onStart = {
                        val pool = lenProblems.filter { it.topic in selectedTopics }
                        val selected = pool.shuffled().take(selectedProblemCount)
                        quizState = LENState(
                            problems = selected,
                            totalTimeMinutes = selectedDuration
                        )
                        remainingSeconds = selectedDuration * 60L
                        showStartDialog = false
                    },
                    onDismiss = onBackClick,
                    modifier = Modifier.padding(padding)
                )
            }
            quizState != null && quizState!!.isFinished -> {
                LENResultsView(
                    state = quizState!!,
                    onRestart = {
                        showStartDialog = true
                        quizState = null
                    },
                    onBackClick = onBackClick,
                    modifier = Modifier.padding(padding)
                )
            }
            quizState != null -> {
                LENProblemView(
                    state = quizState!!,
                    onAnswerChange = { answer ->
                        val id = quizState!!.problems[quizState!!.currentProblemIndex].id
                        quizState!!.answers[id] = answer
                    },
                    onNext = {
                        if (quizState!!.currentProblemIndex < quizState!!.problems.size - 1) {
                            quizState = quizState!!.copy(
                                currentProblemIndex = quizState!!.currentProblemIndex + 1,
                                showSolution = false
                            )
                        }
                    },
                    onPrevious = {
                        if (quizState!!.currentProblemIndex > 0) {
                            quizState = quizState!!.copy(
                                currentProblemIndex = quizState!!.currentProblemIndex - 1,
                                showSolution = false
                            )
                        }
                    },
                    onShowSolution = {
                        quizState = quizState!!.copy(showSolution = true)
                    },
                    onGradeChange = { points ->
                        val id = quizState!!.problems[quizState!!.currentProblemIndex].id
                        quizState!!.selfGradedPoints[id] = points
                    },
                    onFinish = {
                        quizState = quizState!!.copy(isFinished = true)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

// ════════════════════════════════════════════
//  START DIALOG
// ════════════════════════════════════════════

@Composable
private fun LENStartDialog(
    selectedDuration: Int,
    onDurationChange: (Int) -> Unit,
    selectedProblemCount: Int,
    onProblemCountChange: (Int) -> Unit,
    selectedTopics: Set<LENTopic>,
    onTopicsChange: (Set<LENTopic>) -> Unit,
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
                    Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF4DB6AC)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bolt, null, modifier = Modifier.size(40.dp), tint = Color.White)
        }

        Text(
            "KIT LEN Klausur-Trainer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Lineare Elektrische Netzwerke · Prof. Dössel\n10 Klausuren WS17/18–SS22 · 94 Punkte · 2 Stunden",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Zeitlimit
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Zeitlimit", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 Min", 60 to "60 Min", 90 to "90 Min", 120 to "120 Min").forEach { (mins, label) ->
                        FilterChip(
                            selected = selectedDuration == mins,
                            onClick = { onDurationChange(mins) },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Aufgabenanzahl
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Anzahl Aufgaben", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // Themenauswahl
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Themengebiete", fontWeight = FontWeight.Bold)
                Text(
                    "Tippe zum Deaktivieren",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LENTopic.entries.forEach { topic ->
                    val isSelected = topic in selectedTopics
                    val count = lenProblems.count { it.topic == topic }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onTopicsChange(
                                    if (isSelected && selectedTopics.size > 1)
                                        selectedTopics - topic
                                    else
                                        selectedTopics + topic
                                )
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            topic.icon, null,
                            tint = if (isSelected) topic.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                topic.displayName,
                                fontSize = 14.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        Text(
                            topic.weight,
                            fontSize = 11.sp,
                            color = if (isSelected) topic.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$count",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Training starten", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        TextButton(onClick = onDismiss) { Text("Abbrechen") }
        Spacer(Modifier.height(8.dp))
    }
}

// ════════════════════════════════════════════
//  PROBLEM VIEW
// ════════════════════════════════════════════

@Composable
private fun LENProblemView(
    state: LENState,
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
        // Topic badge + Punkte
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = problem.topic.color.copy(alpha = 0.15f)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(problem.topic.icon, null, tint = problem.topic.color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(problem.topic.displayName, fontSize = 12.sp, color = problem.topic.color, fontWeight = FontWeight.Medium)
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFF8F00).copy(alpha = 0.15f)) {
                Text(
                    "${problem.points} Punkte",
                    fontSize = 12.sp,
                    color = Color(0xFFFF8F00),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            // Difficulty
            val diffColor = when (problem.difficulty) {
                1 -> Color(0xFF4CAF50); 2 -> Color(0xFFFF9800); else -> Color(0xFFF44336)
            }
            val diffLabel = when (problem.difficulty) {
                1 -> "Leicht"; 2 -> "Mittel"; else -> "Schwer"
            }
            Surface(shape = RoundedCornerShape(8.dp), color = diffColor.copy(alpha = 0.12f)) {
                Text(diffLabel, fontSize = 11.sp, color = diffColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }

        // Aufgabe
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Aufgabe ${state.currentProblemIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LenLatexText(problem.questionLatex, fontSize = 15)
            }
        }

        // Hinweise
        var showHints by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { showHints = !showHints }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (showHints) "Hinweise ausblenden" else "Hinweise anzeigen")
        }
        if (showHints) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(Modifier.padding(12.dp)) {
                    problem.hints.forEachIndexed { idx, hint ->
                        Row {
                            Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Spacer(Modifier.width(8.dp))
                            Text(hint, color = Color(0xFFE65100), fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Antwortfeld
        OutlinedTextField(
            value = currentAnswer,
            onValueChange = onAnswerChange,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
            label = { Text("Deine Lösung (Stichpunkte / Formeln)") },
            placeholder = { Text("Hier deine Antwort eingeben...") },
            maxLines = 10
        )

        // Lösung anzeigen
        if (!state.showSolution) {
            Button(
                onClick = onShowSolution,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text("Musterlösung anzeigen")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00695C))
                        Spacer(Modifier.width(8.dp))
                        Text("Musterlösung", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                    }
                    Spacer(Modifier.height(12.dp))
                    problem.solutionSteps.forEachIndexed { idx, step ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${idx + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), fontSize = 14.sp)
                            LenLatexText(step, fontSize = 14, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Selbstbewertung:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..problem.points).forEach { pts ->
                            FilterChip(
                                selected = currentGrade == pts,
                                onClick = { onGradeChange(pts) },
                                label = { Text("$pts", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when {
                                        pts == 0 -> Color(0xFFF44336).copy(alpha = 0.2f)
                                        pts == problem.points -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        else -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        // Navigation
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = state.currentProblemIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Zurück")
            }
            if (state.currentProblemIndex < state.problems.size - 1) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                ) {
                    Text("Weiter")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Icon(Icons.Default.Flag, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auswerten", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ════════════════════════════════════════════
//  RESULTS VIEW
// ════════════════════════════════════════════

@Composable
private fun LENResultsView(
    state: LENState,
    onRestart: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPoints = state.problems.sumOf { it.points }
    val earnedPoints = state.selfGradedPoints.values.sum()
    val percentage = if (totalPoints > 0) (earnedPoints * 100f / totalPoints) else 0f
    val grade = when {
        percentage >= 87.5f -> "Sehr gut"
        percentage >= 75f -> "Gut"
        percentage >= 62.5f -> "Befriedigend"
        percentage >= 50f -> "Ausreichend"
        else -> "Nicht bestanden"
    }
    val gradeColor = when {
        percentage >= 75f -> Color(0xFF4CAF50)
        percentage >= 50f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Score circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF4DB6AC)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$earnedPoints/$totalPoints",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text("Punkte", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }

        Text(grade, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = gradeColor)
        Text("${percentage.toInt()}% erreicht", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Themen-Auswertung
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Themenauswertung", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LENTopic.entries.forEach { topic ->
                    val topicProblems = state.problems.filter { it.topic == topic }
                    if (topicProblems.isNotEmpty()) {
                        val topicTotal = topicProblems.sumOf { it.points }
                        val topicEarned = topicProblems.sumOf { state.selfGradedPoints[it.id] ?: 0 }
                        val topicPct = if (topicTotal > 0) topicEarned * 100f / topicTotal else 0f
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(topic.icon, null, tint = topic.color, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(topic.displayName, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                "$topicEarned/$topicTotal (${topicPct.toInt()}%)",
                                fontSize = 13.sp,
                                color = when {
                                    topicPct >= 75 -> Color(0xFF4CAF50)
                                    topicPct >= 50 -> Color(0xFFFF9800)
                                    else -> Color(0xFFF44336)
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Aufgaben-Übersicht
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Aufgaben im Überblick", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                state.problems.forEachIndexed { idx, problem ->
                    val earned = state.selfGradedPoints[problem.id]
                    val answered = state.answers[problem.id]?.isNotBlank() == true
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), fontSize = 13.sp)
                        Icon(problem.topic.icon, null, tint = problem.topic.color, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(problem.topic.displayName, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        if (earned != null) {
                            Text(
                                "$earned/${problem.points}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    earned == problem.points -> Color(0xFF4CAF50)
                                    earned > 0 -> Color(0xFFFF9800)
                                    else -> Color(0xFFF44336)
                                }
                            )
                        } else {
                            Text("–/${problem.points}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Tipp-Bereich
        if (percentage < 75f) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Lerntipp", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text(
                            "Priorität nach Klausurgewicht: Bodediagramm (24%) → Wechselstrom (21%) → Ortskurve (20%) → OPV (15%) → Netzwerk (14%)",
                            fontSize = 13.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Nochmal trainieren", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Zum Hauptmenü")
        }

        Spacer(Modifier.height(16.dp))
    }
}
