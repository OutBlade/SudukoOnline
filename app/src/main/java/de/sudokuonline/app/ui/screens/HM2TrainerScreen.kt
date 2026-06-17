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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

private fun hm2BuildLatexHtml(text: String, fontSize: Int, fontWeight: String, hexColor: String): String {
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
private fun Hm2LatexText(
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
                        view?.evaluateJavascript("(function(){return document.getElementById('c').offsetHeight})()") { r ->
                            val h = r.toFloatOrNull() ?: return@evaluateJavascript
                            if (h > 0) heightDp = (h + 4).dp
                        }
                    }
                }
                wv.loadDataWithBaseURL(null, hm2BuildLatexHtml(text, fontSize, fontWeight, hexColor), "text/html", "UTF-8", null)
            }
        },
        modifier = modifier.fillMaxWidth().height(heightDp)
    )
}

@Composable
private fun Hm2Text(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    if ("\\" in text) {
        Hm2LatexText(
            text, modifier, fontSize,
            if (fontWeight >= FontWeight.Bold) "bold" else if (fontWeight >= FontWeight.Medium) "500" else "normal",
            color
        )
    } else {
        Text(text, modifier = modifier, fontSize = fontSize.sp, fontWeight = fontWeight, color = color)
    }
}

// ════════════════════════════════════════════
//  Data Models
// ════════════════════════════════════════════

private enum class Hm2Topic(val displayName: String, val icon: ImageVector, val color: Color) {
    POTENTIALFELDER("Potentialfelder & Rotation", Icons.Default.Timeline, Color(0xFF1565C0)),
    EXTREMA("Extrema & Hesse-Matrix", Icons.Default.TrendingUp, Color(0xFF2E7D32)),
    EIGENWERTE("Eigenwerte & Eigenräume", Icons.Default.GridOn, Color(0xFF7B1FA2)),
    GAUSS("Divergenzsatz (Gauss)", Icons.Default.Functions, Color(0xFFE65100)),
    LAGRANGE("Lagrange-Multiplikatoren", Icons.Default.Tune, Color(0xFFC62828)),
    FLAECHENINTEGRAL("Flächenintegrale", Icons.Default.Layers, Color(0xFF00695C)),
    KOORDINATEN("Koordinatentransformation", Icons.Default.Explore, Color(0xFF283593))
}

private sealed class Hm2Card {
    abstract val id: String
    abstract val topic: Hm2Topic
    abstract val difficulty: Int

    data class Flashcard(
        override val id: String,
        override val topic: Hm2Topic,
        override val difficulty: Int,
        val front: String,
        val back: String,
        val hint: String? = null,
        val klausurTipp: String? = null
    ) : Hm2Card()

    data class Formula(
        override val id: String,
        override val topic: Hm2Topic,
        override val difficulty: Int,
        val name: String,
        val formula: String,
        val variables: List<Pair<String, String>> = emptyList(),
        val back: String,
        val klausurTipp: String? = null
    ) : Hm2Card()

    data class MC(
        override val id: String,
        override val topic: Hm2Topic,
        override val difficulty: Int,
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String
    ) : Hm2Card()

    data class TrueFalse(
        override val id: String,
        override val topic: Hm2Topic,
        override val difficulty: Int,
        val statement: String,
        val correct: Boolean,
        val explanation: String
    ) : Hm2Card()
}

// ════════════════════════════════════════════
//  Card Data (42 Karten aus hm2_lernkarten.json)
// ════════════════════════════════════════════

private val hm2Cards: List<Hm2Card> = listOf(

    // ── POTENTIALFELDER ──────────────────────

    Hm2Card.Flashcard(
        id = "hm2_001", topic = Hm2Topic.POTENTIALFELDER, difficulty = 2,
        front = """Wie prüft man ob ein Vektorfeld \(\mathbf{v}(x,y) = (v_1, v_2)\) ein Gradientenfeld ist?""",
        back = """Bedingung: \(\mathrm{rot}(\mathbf{v}) = 0\)
Für ebenes Feld: \(\dfrac{\partial v_2}{\partial x} = \dfrac{\partial v_1}{\partial y}\)

Wenn diese Bedingung gilt, existiert Stammfunktion \(F\) mit \(\nabla F = \mathbf{v}\):
\(\dfrac{\partial F}{\partial x} = v_1,\quad \dfrac{\partial F}{\partial y} = v_2\)

Berechnung: Integration von \(v_1\) nach \(x\) (mit \(g(y)\)), dann \(g(y)\) aus \(\dfrac{\partial F}{\partial y} = v_2\) bestimmen.""",
        hint = "Gemischte Ableitungen müssen gleich sein: Kreuztest!",
        klausurTipp = "Immer zuerst Gradientenfeld-Bedingung prüfen, dann Stammfunktion suchen."
    ),

    Hm2Card.Flashcard(
        id = "hm2_002", topic = Hm2Topic.POTENTIALFELDER, difficulty = 2,
        front = """Gegeben \(\mathbf{v} = (2xy + y^3,\; x^2 + 3xy^2)\). Ist das ein Gradientenfeld? Falls ja, bestimme \(F(x,y)\).""",
        back = """Kreuztest:
\(\dfrac{\partial v_2}{\partial x} = 2x + 3y^2\)
\(\dfrac{\partial v_1}{\partial y} = 2x + 3y^2\) \(\Rightarrow\) gleich! Gradientenfeld!

\(F\) aus \(\dfrac{\partial F}{\partial x} = v_1\):
\[F = \int (2xy + y^3)\,dx = x^2y + xy^3 + g(y)\]

Aus \(\dfrac{\partial F}{\partial y} = x^2 + 3xy^2 = v_2 \Rightarrow g'(y) = 0 \Rightarrow g = C\)

\[F(x,y) = x^2y + xy^3 + C\]"""
    ),

    Hm2Card.Formula(
        id = "hm2_003", topic = Hm2Topic.POTENTIALFELDER, difficulty = 2,
        name = "Rotation eines Vektorfeldes",
        formula = """\[\mathrm{rot}(\mathbf{v}) = \nabla \times \mathbf{v} = \begin{pmatrix} \partial_y v_3 - \partial_z v_2 \\ \partial_z v_1 - \partial_x v_3 \\ \partial_x v_2 - \partial_y v_1 \end{pmatrix}\]""",
        variables = listOf(
            "\\(\\mathbf{v}\\)" to "Vektorfeld \\((v_1, v_2, v_3)\\)",
            "\\(\\mathrm{rot}(\\mathbf{v})\\)" to "Rotation (Curl) von \\(\\mathbf{v}\\)"
        ),
        back = """Im 2D (\(v_3=0\)): \(\mathrm{rot}(\mathbf{v}) = \left(0,0,\dfrac{\partial v_2}{\partial x} - \dfrac{\partial v_1}{\partial y}\right)\)

Gradientenfeld \(\Leftrightarrow \mathrm{rot}(\mathbf{v}) = \mathbf{0}\)

Physikalisch: rot beschreibt die Wirbelstärke eines Feldes."""
    ),

    Hm2Card.Flashcard(
        id = "hm2_023", topic = Hm2Topic.POTENTIALFELDER, difficulty = 1,
        front = """Was ist der Gradient einer Funktion \(f(x,y,z)\) und was bedeutet er geometrisch?""",
        back = """\[\nabla f = \left(\frac{\partial f}{\partial x},\; \frac{\partial f}{\partial y},\; \frac{\partial f}{\partial z}\right)\]

Geometrische Bedeutung:
- Zeigt in Richtung des steilsten Anstiegs
- Steht senkrecht auf den Niveaumengen \(f = \mathrm{const}\)
- \(|\nabla f|\) = Betrag der maximalen Steigung

Richtungsableitung: \(D_\mathbf{v}f = \nabla f \cdot \dfrac{\mathbf{v}}{|\mathbf{v}|}\)"""
    ),

    Hm2Card.MC(
        id = "hm2_027", topic = Hm2Topic.POTENTIALFELDER, difficulty = 3,
        question = """Das Wegintegral \(\int_C \mathbf{v} \cdot d\mathbf{r}\) hängt nicht vom Weg \(C\) ab (nur von Start- und Endpunkt) gdw.?""",
        options = listOf(
            "\\(\\mathbf{v}\\) ist konstant",
            "\\(\\mathrm{rot}(\\mathbf{v}) = \\mathbf{0}\\) (v ist Gradientenfeld)",
            "\\(\\mathrm{div}(\\mathbf{v}) = 0\\)",
            "\\(|\\mathbf{v}| = 1\\)"
        ),
        correctIndex = 1,
        explanation = """Äquivalente Bedingungen (Pfadunabhängigkeit):
(1) \(\int_C \mathbf{v}\cdot d\mathbf{r}\) hängt nicht vom Weg ab
(2) \(\oint_C \mathbf{v}\cdot d\mathbf{r} = 0\) für jeden geschlossenen Weg
(3) \(\mathrm{rot}(\mathbf{v}) = \mathbf{0}\) (konservatives Feld)
(4) \(\exists F: \mathbf{v} = \nabla F\)

Dann: \(\int_C \mathbf{v}\cdot d\mathbf{r} = F(\text{Ende}) - F(\text{Start})\)

Physik: Konservative Kräfte (Gravitation, E-Feld) sind Gradientenfelder!"""
    ),

    Hm2Card.MC(
        id = "hm2_040", topic = Hm2Topic.POTENTIALFELDER, difficulty = 2,
        question = """\(\mathbf{v} = (y, -x)\) (Kreisfeld). Ist dies ein Gradientenfeld?""",
        options = listOf(
            "Ja, mit \\(F = \\tfrac{y^2-x^2}{2}\\)",
            "Nein, \\(\\mathrm{rot}(\\mathbf{v}) = -2 \\neq 0\\)",
            "Ja, mit \\(F = xy\\)",
            "Nein, \\(\\mathrm{div}(\\mathbf{v}) = 0\\)"
        ),
        correctIndex = 1,
        explanation = """Kreuztest:
\(\dfrac{\partial v_2}{\partial x} = -1\), \(\dfrac{\partial v_1}{\partial y} = 1\)

\(-1 \neq 1\) \(\Rightarrow\) kein Gradientenfeld!

\(\mathrm{rot}(\mathbf{v}) = -1 - 1 = -2 \neq 0\)

Das Kreisfeld dreht um den Ursprung — typisches Wirbelfeld. Das Linienintegral über einen Kreis ist \(\neq 0\)."""
    ),

    Hm2Card.Flashcard(
        id = "hm2_036", topic = Hm2Topic.POTENTIALFELDER, difficulty = 2,
        front = """Was sagt der Stokessche Integralsatz?""",
        back = """\[\oint_{\partial F} \mathbf{v} \cdot d\mathbf{r} = \iint_F \mathrm{rot}(\mathbf{v}) \cdot \mathbf{n}\,do\]

Linienintegral über Rand \(\partial F\) = Flächenintegral der Rotation

Bedingungen:
- \(F\) orientierte Fläche mit Rand \(\partial F\)
- \(\mathbf{n}\) und Durchlaufrichtung konsistent (Rechte-Hand-Regel)

Analog zu Gauss, aber für Linien/Flächen statt Flächen/Volumina."""
    ),

    // ── EXTREMA ──────────────────────────────

    Hm2Card.Formula(
        id = "hm2_004", topic = Hm2Topic.EXTREMA, difficulty = 2,
        name = "Stationäre Punkte & Hesse-Matrix",
        formula = """\[\nabla f = \mathbf{0} \;\Rightarrow\; H_f = \begin{pmatrix} f_{xx} & f_{xy} \\ f_{yx} & f_{yy} \end{pmatrix} \text{ auswerten}\]""",
        variables = listOf(
            "\\(\\nabla f\\)" to "Gradient \\(= (f_x, f_y)\\)",
            "\\(H_f\\)" to "Hesse-Matrix (Matrix der 2. partiellen Ableitungen)",
            "\\(\\det(H_f)\\)" to "Determinante der Hesse-Matrix"
        ),
        back = """Klassifikation stationärer Punkte:

\(\det(H_f) > 0\) und \(f_{xx} > 0\): LOKALES MINIMUM
\(\det(H_f) > 0\) und \(f_{xx} < 0\): LOKALES MAXIMUM
\(\det(H_f) < 0\): SATTELPUNKT
\(\det(H_f) = 0\): Keine Aussage (höhere Terme nötig)""",
        klausurTipp = "Zuerst beide partiellen Ableitungen Null setzen, dann Gleichungssystem lösen."
    ),

    Hm2Card.Flashcard(
        id = "hm2_005", topic = Hm2Topic.EXTREMA, difficulty = 2,
        front = """\(f(x,y) = x^3 - 3x + y^2 - 4y\). Bestimme alle stationären Punkte und klassifiziere sie.""",
        back = """Ableitungen gleich Null:
\(f_x = 3x^2 - 3 = 0 \Rightarrow x = \pm 1\)
\(f_y = 2y - 4 = 0 \Rightarrow y = 2\)

Stationäre Punkte: \(P_1=(1,2)\) und \(P_2=(-1,2)\)

\(H_f = \begin{pmatrix} 6x & 0 \\ 0 & 2 \end{pmatrix}\)

\(P_1\): \(\det H = 12 > 0\), \(f_{xx} = 6 > 0\) \(\Rightarrow\) MINIMUM
\(P_2\): \(\det H = -12 < 0\) \(\Rightarrow\) SATTELPUNKT"""
    ),

    Hm2Card.MC(
        id = "hm2_006", topic = Hm2Topic.EXTREMA, difficulty = 2,
        question = """Die Hesse-Matrix \(H_f\) hat Eigenwerte \(\lambda_1 = -3\) und \(\lambda_2 = 5\). Was ist der Punkt?""",
        options = listOf("Lokales Minimum", "Lokales Maximum", "Sattelpunkt", "Keine Aussage möglich"),
        correctIndex = 2,
        explanation = """Verschiedene Vorzeichen der Eigenwerte → \(H_f\) ist indefinit.

\(\det(H_f) = \lambda_1 \cdot \lambda_2 = (-3)\cdot 5 = -15 < 0\) → Sattelpunkt.

Merkregel:
- Alle EW positiv → positiv definit → Minimum
- Alle EW negativ → negativ definit → Maximum
- Gemischte Vorzeichen → indefinit → Sattelpunkt"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_026", topic = Hm2Topic.EXTREMA, difficulty = 1,
        front = """Was ist ein Sattelpunkt?""",
        back = """Ein stationärer Punkt (\(\nabla f = \mathbf{0}\)), der kein lokales Extremum ist.

Bei Sattelpunkt gilt: \(\det(H_f) < 0\)

In manchen Richtungen Minimum, in anderen Maximum.
Beispiel: \(f(x,y) = x^2 - y^2\) bei \((0,0)\):
- \(x\)-Richtung: lokales Minimum
- \(y\)-Richtung: lokales Maximum

Name kommt von der Pferdesattelform."""
    ),

    Hm2Card.Formula(
        id = "hm2_033", topic = Hm2Topic.EXTREMA, difficulty = 1,
        name = "Taylorentwicklung (2 Variablen)",
        formula = """\[f(x_0{+}h, y_0{+}k) \approx f_0 + \nabla f \cdot \binom{h}{k} + \tfrac{1}{2}(h,k)\,H_f\binom{h}{k}\]""",
        variables = listOf(
            "\\(\\nabla f\\)" to "Gradient an \\((x_0, y_0)\\)",
            "\\(H_f\\)" to "Hesse-Matrix an \\((x_0, y_0)\\)"
        ),
        back = """Bei stationärem Punkt (\(\nabla f = \mathbf{0}\)):
Nur der quadratische Term entscheidet über die Art des Extremums!

\[\frac{1}{2}(h,k)\,H_f\binom{h}{k} = \frac{1}{2}(f_{xx}h^2 + 2f_{xy}hk + f_{yy}k^2)\]

Positiv definit → Minimum · Negativ definit → Maximum · Indefinit → Sattelpunkt"""
    ),

    Hm2Card.MC(
        id = "hm2_037", topic = Hm2Topic.EXTREMA, difficulty = 1,
        question = """Für \(f(x) = x^3\) am Punkt \(x=0\): Was sagt die Hesse-Matrix?""",
        options = listOf(
            "H > 0, lokales Minimum",
            "H < 0, lokales Maximum",
            "H = 0, keine Aussage möglich",
            "det(H) < 0, Sattelpunkt"
        ),
        correctIndex = 2,
        explanation = """\(f''(0) = 6x\big|_{x=0} = 0 \Rightarrow H = 0\) → keine Aussage!

Höhere Terme: \(f'''(0) = 6 \neq 0\) → ungerade Ableitung → Wendepunkt (kein Extremum).

Merke: \(H = 0\) kann Minimum, Maximum oder Wendepunkt bedeuten. Immer höhere Ordnung prüfen!"""
    ),

    // ── EIGENWERTE ───────────────────────────

    Hm2Card.Formula(
        id = "hm2_007", topic = Hm2Topic.EIGENWERTE, difficulty = 1,
        name = "Charakteristisches Polynom & Eigenwerte",
        formula = """\[\det(A - \lambda I) = 0\]""",
        variables = listOf(
            "\\(A\\)" to "Quadratische \\(n \\times n\\)-Matrix",
            "\\(\\lambda\\)" to "Eigenwert",
            "\\(I\\)" to "Einheitsmatrix"
        ),
        back = """Vorgehen:
1. \(\det(A - \lambda I) = 0\) lösen → Eigenwerte \(\lambda_i\)
2. Für jedes \(\lambda_i\): \((A - \lambda_i I)\mathbf{x} = \mathbf{0}\) lösen → Eigenvektoren
3. Geometrische VF \(= \dim\!\ker(A - \lambda_i I)\)
4. Algebraische VF \(=\) Vielfachheit als Nullstelle

Diagonalisierbar \(\Leftrightarrow\) geom. VF \(=\) alg. VF für alle \(\lambda_i\)"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_008", topic = Hm2Topic.EIGENWERTE, difficulty = 2,
        front = """\(A = \begin{pmatrix} 3 & 1 \\ 0 & 3 \end{pmatrix}\). Eigenwerte, Eigenräume, Diagonalisierbarkeit?""",
        back = """Char. Poly.: \(\det(A - \lambda I) = (3-\lambda)^2 = 0 \Rightarrow \lambda = 3\) (alg. VF = 2)

Eigenraum zu \(\lambda=3\):
\((A-3I) = \begin{pmatrix} 0 & 1 \\ 0 & 0 \end{pmatrix} \Rightarrow x_2 = 0\)
\(E_3 = \mathrm{span}\{(1,0)^T\}\), geom. VF \(= 1\)

alg. VF \(= 2 \neq\) geom. VF \(= 1\)
→ A ist NICHT diagonalisierbar!""",
        klausurTipp = "Dreiecksmatrizen haben Eigenwerte auf der Hauptdiagonale!"
    ),

    Hm2Card.Flashcard(
        id = "hm2_009", topic = Hm2Topic.EIGENWERTE, difficulty = 2,
        front = """Was bedeutet Diagonalisierbarkeit und wie konstruiert man \(D = S^{-1}AS\)?""",
        back = """\(A\) diagonalisierbar \(\Leftrightarrow\) geom. VF = alg. VF für alle EW
\(\Leftrightarrow\) \(n\) lin. unabhängige Eigenvektoren existieren

Konstruktion:
1. Eigenwerte \(\lambda_i\) berechnen
2. Eigenvektoren \(\mathbf{v}_i\) berechnen
3. \(S = [\mathbf{v}_1 \mid \mathbf{v}_2 \mid \cdots \mid \mathbf{v}_n]\)
4. \(D = S^{-1}AS = \mathrm{diag}(\lambda_1, \ldots, \lambda_n)\)

Anwendung: \(A^n = S\,D^n\,S^{-1}\) — einfach berechenbar!"""
    ),

    Hm2Card.MC(
        id = "hm2_010", topic = Hm2Topic.EIGENWERTE, difficulty = 3,
        question = """Welche Matrix ist sicher diagonalisierbar (ohne Rechnung)?""",
        options = listOf(
            "\\(\\begin{pmatrix}1&1\\\\0&1\\end{pmatrix}\\) (Dreiecksmatrix, ein EW)",
            "\\(\\begin{pmatrix}0&-1\\\\1&0\\end{pmatrix}\\) (Rotationsmatrix)",
            "\\(\\begin{pmatrix}2&0\\\\0&-3\\end{pmatrix}\\) (Diagonalmatrix)",
            "\\(\\begin{pmatrix}1&2\\\\2&0\\end{pmatrix}\\) (symmetrisch)"
        ),
        correctIndex = 2,
        explanation = """Diagonalmatrizen sind immer diagonalisierbar (EV = Einheitsvektoren).

A: Ein EW mit alg. VF=2, geom. VF=1 → nicht diagonalisierbar.
B: Komplexe Eigenwerte \(\pm i\) → in \(\mathbb{R}\) nicht diagonalisierbar.
D: Wäre auch diagonalisierbar (Spektralsatz), aber C ist ohne Rechnung klarer."""
    ),

    Hm2Card.TrueFalse(
        id = "hm2_024", topic = Hm2Topic.EIGENWERTE, difficulty = 2,
        statement = """Eigenvektoren zu verschiedenen Eigenwerten sind immer linear unabhängig.""",
        correct = true,
        explanation = """RICHTIG. Annahme: \(\mathbf{v}_1 = \alpha\mathbf{v}_2\). Dann:
\(A\mathbf{v}_1 = \lambda_1\alpha\mathbf{v}_2\), aber auch \(\alpha A\mathbf{v}_2 = \alpha\lambda_2\mathbf{v}_2\).
Da \(\lambda_1 \neq \lambda_2\): Widerspruch! \(\Rightarrow\) lin. unabhängig. ✓

Folge: \(n\) verschiedene EW → \(n\) lin. unabh. EV → diagonalisierbar!"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_028", topic = Hm2Topic.EIGENWERTE, difficulty = 3,
        front = """Was ist algebraische vs. geometrische Vielfachheit? Wie hängen sie zusammen?""",
        back = """Algebraische VF: Vielfachheit von \(\lambda\) als Nullstelle von \(\det(A-\lambda I)\)

Geometrische VF: \(\dim\ker(A - \lambda I)\) = Anzahl lin. unabh. Eigenvektoren

Zusammenhang:
\[1 \leq \text{geom. VF} \leq \text{alg. VF}\]

Diagonalisierbar \(\Leftrightarrow\) geom. VF = alg. VF für ALLE Eigenwerte"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_030", topic = Hm2Topic.EIGENWERTE, difficulty = 2,
        front = """Berechne die Eigenwerte von \(A = \begin{pmatrix}2&1\\1&2\end{pmatrix}\).""",
        back = """Char. Poly.:
\[\det\begin{pmatrix}2-\lambda&1\\1&2-\lambda\end{pmatrix} = (2-\lambda)^2 - 1 = 0\]
\[\lambda^2 - 4\lambda + 3 = (\lambda-1)(\lambda-3) = 0\]
\[\lambda_1 = 1,\quad \lambda_2 = 3\]

Eigenvektoren:
\(\lambda_1 = 1\): \(\mathbf{v}_1 = \tfrac{1}{\sqrt{2}}(1,-1)^T\)
\(\lambda_2 = 3\): \(\mathbf{v}_2 = \tfrac{1}{\sqrt{2}}(1,1)^T\)

\(A\) symmetrisch → Eigenvektoren orthogonal! ✓"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_038", topic = Hm2Topic.EIGENWERTE, difficulty = 2,
        front = """\(A\mathbf{v} = \lambda\mathbf{v}\). Was sind die Eigenwerte von \(A^2\), \(A^{-1}\) und \(A + \mu I\)?""",
        back = """\(A^2\mathbf{v} = \lambda^2\mathbf{v}\) → EW: \(\lambda^2\)

\(A^{-1}\mathbf{v} = \dfrac{1}{\lambda}\mathbf{v}\) (falls \(\lambda \neq 0\)) → EW: \(\dfrac{1}{\lambda}\)

\((A + \mu I)\mathbf{v} = (\lambda + \mu)\mathbf{v}\) → EW: \(\lambda + \mu\)

Der Eigenvektor \(\mathbf{v}\) bleibt in allen Fällen gleich!"""
    ),

    Hm2Card.TrueFalse(
        id = "hm2_042", topic = Hm2Topic.EIGENWERTE, difficulty = 1,
        statement = """Jede symmetrische Matrix \(A = A^T\) hat nur reelle Eigenwerte.""",
        correct = true,
        explanation = """RICHTIG. Spektralsatz für symmetrische Matrizen:
1. Alle Eigenwerte sind reell
2. Eigenvektoren zu verschiedenen EW sind orthogonal
3. \(A\) ist diagonalisierbar durch orthogonale Matrix \(Q\)

\[A = Q\,D\,Q^T,\quad Q^T = Q^{-1},\quad D = \mathrm{diag}(\lambda_1,\ldots,\lambda_n)\]

Anwendung: Definitheitsprüfung — alle EW \(> 0 \Rightarrow\) positiv definit."""
    ),

    // ── GAUSS ────────────────────────────────

    Hm2Card.Formula(
        id = "hm2_011", topic = Hm2Topic.GAUSS, difficulty = 2,
        name = "Gaußscher Divergenzsatz",
        formula = """\[\oiint_{\partial B} \mathbf{v} \cdot \mathbf{n}\,do = \iiint_B \mathrm{div}(\mathbf{v})\,dV\]""",
        variables = listOf(
            "\\(B\\)" to "Volumen (kompakter Bereich mit glattem Rand)",
            "\\(\\partial B\\)" to "Oberfläche von \\(B\\)",
            "\\(\\mathbf{n}\\)" to "Aussengerichteter Einheitsnormalenvektor",
            "\\(\\mathrm{div}(\\mathbf{v})\\)" to "\\(= \\partial_x v_1 + \\partial_y v_2 + \\partial_z v_3\\)"
        ),
        back = """Idee: Oberflächen-Integral = Volumen-Integral der Divergenz

Wann benutzen:
- Flächenintegral über geschlossene Fläche schwer → Divergenz berechnen!
- Besonders effektiv wenn div(v) konstant oder einfach

Richtung \(\mathbf{n}\): immer NACH AUSSEN zeigend!""",
        klausurTipp = "Wenn Flächenintegral über geschlossene Fläche: Gauss oft einfacher!"
    ),

    Hm2Card.Formula(
        id = "hm2_013", topic = Hm2Topic.GAUSS, difficulty = 2,
        name = "Divergenz berechnen",
        formula = """\[\mathrm{div}(\mathbf{v}) = \nabla \cdot \mathbf{v} = \frac{\partial v_1}{\partial x} + \frac{\partial v_2}{\partial y} + \frac{\partial v_3}{\partial z}\]""",
        variables = listOf(
            "\\(\\mathbf{v}\\)" to "Vektorfeld \\((v_1, v_2, v_3)\\)",
            "\\(\\mathrm{div}(\\mathbf{v})\\)" to "Skalarfeld; physikalisch: Quellstärke"
        ),
        back = """Beispiele:
\(\mathrm{div}(x^2, y^2, z^2) = 2x + 2y + 2z\)
\(\mathrm{div}(xy, yz, xz) = y + z + x\)
\(\mathrm{div}(x, y, -2z) = 1 + 1 - 2 = 0\) (quellenfrei!)

Physik: \(\mathrm{div} > 0\): Quelle · \(< 0\): Senke · \(= 0\): quellenfrei"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_012", topic = Hm2Topic.GAUSS, difficulty = 3,
        front = """Berechne mit Gauss: \(\displaystyle\oiint_{\partial W}(x,y,z)\cdot\mathbf{n}\,do\) über die Oberfläche des Würfels \([0,1]^3\).""",
        back = """div\((x,y,z) = 1+1+1 = 3\) (konstant!)

Gauss:
\[\oiint_{\partial W}(x,y,z)\cdot\mathbf{n}\,do = \iiint_{[0,1]^3} 3\,dV = 3\cdot 1 = \boxed{3}\]

Direkte Rechnung hätte 6 Flächenintegrale (6 Seiten) erfordert!
Gauss reduziert auf ein einfaches Volumenintegral."""
    ),

    Hm2Card.TrueFalse(
        id = "hm2_025", topic = Hm2Topic.GAUSS, difficulty = 2,
        statement = """Der Gaußsche Divergenzsatz gilt nur für kugelförmige Volumina.""",
        correct = false,
        explanation = """FALSCH. Der Gaußsche Divergenzsatz gilt für beliebige kompakte Volumina \(B\) mit stückweise glattem orientierbarem Rand \(\partial B\).

Typische Volumina: Quader, Zylinder, Kugeln, Halbkugeln, konvexe Körper usw.

Bedingung: \(\mathbf{v}\) stetig differenzierbar in \(B\)."""
    ),

    Hm2Card.MC(
        id = "hm2_031", topic = Hm2Topic.GAUSS, difficulty = 3,
        question = """Mit Gauss: \(\displaystyle\oiint_{\partial K}\mathbf{v}\cdot\mathbf{n}\,do\) mit \(\mathbf{v}=(x,0,0)\) über Einheitskugel \(K\)?""",
        options = listOf("\\(0\\)", "\\(\\dfrac{4\\pi}{3}\\)", "\\(4\\pi\\)", "\\(\\dfrac{8\\pi}{3}\\)"),
        correctIndex = 1,
        explanation = """\(\mathrm{div}(x,0,0) = 1\) (konstant!)

\[\oiint_{\partial K}(x,0,0)\cdot\mathbf{n}\,do = \iiint_K 1\,dV = \mathrm{Vol}(K) = \frac{4}{3}\pi\]

Direkte Berechnung über Kugeloberfläche wäre viel aufwändiger."""
    ),

    // ── LAGRANGE ─────────────────────────────

    Hm2Card.Formula(
        id = "hm2_014", topic = Hm2Topic.LAGRANGE, difficulty = 2,
        name = "Lagrange-Multiplikatoren (1 Nebenbedingung)",
        formula = """\[\nabla f = \lambda \cdot \nabla h,\quad h(x,y,z) = 0\]""",
        variables = listOf(
            "\\(f\\)" to "Zielfunktion (minimieren/maximieren)",
            "\\(h\\)" to "Nebenbedingung \\(h(x,y,z)=0\\)",
            "\\(\\lambda\\)" to "Lagrange-Multiplikator (neue Unbekannte)"
        ),
        back = """System aufstellen:
\(f_x = \lambda h_x\), \(f_y = \lambda h_y\), \(f_z = \lambda h_z\), \(h = 0\)

\(n+1\) Gleichungen für \(n\) Unbekannte + \(\lambda\)

Tipp: \(\lambda\) oft eliminierbar durch Division der Gradienten-Gleichungen.""",
        klausurTipp = "λ muss nicht berechnet werden! Oft aus Gleichungen eliminieren."
    ),

    Hm2Card.Flashcard(
        id = "hm2_015", topic = Hm2Topic.LAGRANGE, difficulty = 3,
        front = """Maximiere \(f(x,y) = xy\) unter der Nebenbedingung \(x + y = 1\).""",
        back = """Lagrange mit \(h = x + y - 1\):
\(\nabla f = (y, x)\), \(\nabla h = (1, 1)\)

\(y = \lambda\) und \(x = \lambda\) → \(x = y\)

Einsetzen in \(h\): \(2x = 1 \Rightarrow x = y = \tfrac{1}{2}\)

\[f\!\left(\tfrac{1}{2},\tfrac{1}{2}\right) = \tfrac{1}{4}\]

Randvergleich: \(f(0,1) = f(1,0) = 0 < \tfrac{1}{4}\)

Ergebnis: Maximum \(f = \dfrac{1}{4}\) bei \(\left(\tfrac{1}{2},\tfrac{1}{2}\right)\)"""
    ),

    Hm2Card.MC(
        id = "hm2_016", topic = Hm2Topic.LAGRANGE, difficulty = 2,
        question = """Warum muss man bei Lagrange-Multiplikatoren auch singuläre Punkte untersuchen?""",
        options = listOf(
            "Das Lagrange-System liefert falsche Lösungen",
            "Auf dem Rand könnten andere Extrema liegen",
            "λ muss positiv sein",
            "An Punkten mit ∇h = 0 gilt die Lagrange-Bedingung nicht"
        ),
        correctIndex = 3,
        explanation = """Die Bedingung \(\nabla f = \lambda\nabla h\) gilt nur an regulären Punkten (\(\nabla h \neq \mathbf{0}\)).

An singulären Punkten mit \(\nabla h = \mathbf{0}\) kann ein Extremum liegen, das das Lagrange-System nicht findet.

Daher: Alle Lagrange-Lösungen + singuläre Punkte vergleichen!"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_032", topic = Hm2Topic.LAGRANGE, difficulty = 2,
        front = """Wie geht man mit zwei Nebenbedingungen \(h_1=0\) und \(h_2=0\) vor?""",
        back = """Zwei Multiplikatoren \(\lambda_1\) und \(\lambda_2\):
\[\nabla f = \lambda_1\nabla h_1 + \lambda_2\nabla h_2\]
\[h_1(x,y,z) = 0,\quad h_2(x,y,z) = 0\]

5 Gleichungen für 5 Unbekannte \((x,y,z,\lambda_1,\lambda_2)\) im 3D-Fall.

Achtung: \(\nabla h_1\) und \(\nabla h_2\) müssen linear unabhängig sein!"""
    ),

    Hm2Card.TrueFalse(
        id = "hm2_034", topic = Hm2Topic.LAGRANGE, difficulty = 2,
        statement = """Der Lagrange-Multiplikator \(\lambda\) muss stets positiv sein.""",
        correct = false,
        explanation = """FALSCH. \(\lambda\) kann jeden reellen Wert annehmen: positiv, negativ oder Null.

Das Vorzeichen hat keine direkte Bedeutung für die Art des Extremums.

Einfach berechnen und alle Kandidaten in \(f\) einsetzen, um Maximum und Minimum zu vergleichen!"""
    ),

    // ── FLÄCHENINTEGRAL ──────────────────────

    Hm2Card.Formula(
        id = "hm2_017", topic = Hm2Topic.FLAECHENINTEGRAL, difficulty = 3,
        name = "Flächenintegral (Parametrisierung)",
        formula = """\[\iint_F f\,do = \iint_D f(\varphi(u,v))\cdot|\varphi_u \times \varphi_v|\,du\,dv\]""",
        variables = listOf(
            "\\(\\varphi(u,v)\\)" to "Parametrisierung der Fläche",
            "\\(\\varphi_u, \\varphi_v\\)" to "Partielle Ableitungen nach \\(u\\) bzw. \\(v\\)",
            "\\(|\\varphi_u \\times \\varphi_v|\\)" to "Flächenelement (Länge des Kreuzprodukts)"
        ),
        back = """Schritte:
1. Parametrisierung \(\varphi(u,v)\) bestimmen
2. \(\varphi_u\) und \(\varphi_v\) berechnen
3. Kreuzprodukt \(\mathbf{N} = \varphi_u \times \varphi_v\) berechnen
4. \(|\mathbf{N}|\) = skalares Flächenelement
5. \(f(\varphi(u,v)) \cdot |\mathbf{N}|\) über \(D\) integrieren

Für Vektorflusintegral: \(\mathbf{N}\) muss orientiert (nach außen) sein!""",
        klausurTipp = "Zylinder: φ=(R cos t, R sin t, z) · Kugel: φ=(R sin θ cos φ, R sin θ sin φ, R cos θ)"
    ),

    Hm2Card.Flashcard(
        id = "hm2_018", topic = Hm2Topic.FLAECHENINTEGRAL, difficulty = 3,
        front = """Parametrisiere die Einheitssphäre und berechne \(|\varphi_\theta \times \varphi_\phi|\).""",
        back = """Parametrisierung:
\(\varphi(\theta,\phi) = (\sin\theta\cos\phi,\; \sin\theta\sin\phi,\; \cos\theta)\)
\(\theta \in [0,\pi]\), \(\phi \in [0,2\pi]\)

Ableitungen:
\(\varphi_\theta = (\cos\theta\cos\phi,\; \cos\theta\sin\phi,\; -\sin\theta)\)
\(\varphi_\phi = (-\sin\theta\sin\phi,\; \sin\theta\cos\phi,\; 0)\)

\[|\varphi_\theta \times \varphi_\phi| = \sin\theta \quad \text{(sehr wichtig!)}\]

Oberfläche der Einheitssphäre:
\[\int_0^\pi\!\int_0^{2\pi}\sin\theta\,d\phi\,d\theta = 4\pi\]"""
    ),

    Hm2Card.Formula(
        id = "hm2_029", topic = Hm2Topic.FLAECHENINTEGRAL, difficulty = 3,
        name = "Vektorflusintegral",
        formula = """\[\iint_F \mathbf{v}\cdot\mathbf{n}\,do = \iint_D \mathbf{v}(\varphi(u,v))\cdot(\varphi_u\times\varphi_v)\,du\,dv\]""",
        variables = listOf(
            "\\(\\mathbf{v}\\)" to "Vektorfeld",
            "\\(\\mathbf{n}\\)" to "Einheitsnormalenvektor",
            "\\(\\mathbf{N} = \\varphi_u \\times \\varphi_v\\)" to "Normalenvektor (nicht normiert)"
        ),
        back = """Achtung: Vorzeichen des Kreuzprodukts entscheidet über Orientierung!
\(\mathbf{N} = \varphi_u \times \varphi_v\) muss nach AUSSEN zeigen.

Physikalisch: Fluss durch die Fläche (Strom, Wärmestrom, ...)

Vergleich:
- Skalares Flächenintegral: \(do = |\mathbf{N}|\,du\,dv\)
- Vektorflusintegral: \(\mathbf{n}\,do = \mathbf{N}\,du\,dv\)"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_041", topic = Hm2Topic.FLAECHENINTEGRAL, difficulty = 2,
        front = """Parametrisiere einen Zylinder (Radius \(R\), Höhe \(h\)) und berechne den Mantelflächeninhalt.""",
        back = """Parametrisierung:
\(\varphi(t,z) = (R\cos t,\; R\sin t,\; z),\quad t\in[0,2\pi],\; z\in[0,h]\)

\(\varphi_t = (-R\sin t, R\cos t, 0)\), \(\varphi_z = (0,0,1)\)
\(\varphi_t\times\varphi_z = (R\cos t, R\sin t, 0)\), \(|\mathbf{N}| = R\)

\[\text{Mantelfläche} = \int_0^h\!\int_0^{2\pi} R\,dt\,dz = R\cdot 2\pi\cdot h = 2\pi Rh\]"""
    ),

    // ── KOORDINATEN ──────────────────────────

    Hm2Card.Formula(
        id = "hm2_019", topic = Hm2Topic.KOORDINATEN, difficulty = 2,
        name = "Zylinderkoordinaten",
        formula = """\[x=r\cos\phi,\quad y=r\sin\phi,\quad z=z;\quad dV = r\,dr\,d\phi\,dz\]""",
        variables = listOf(
            "\\(r\\)" to "Radialer Abstand \\([0,\\infty)\\)",
            "\\(\\phi\\)" to "Azimutalwinkel \\([0,2\\pi)\\)",
            "\\(z\\)" to "Höhenkoordinate"
        ),
        back = """\(r^2 = x^2 + y^2\), Jacobi: \(|J| = r\)

Wann benutzen: Zylindrische oder rotationssymmetrische Bereiche.

Beispiel Volumen Zylinder \(r\leq R\), \(0\leq z\leq h\):
\[\int_0^h\!\int_0^{2\pi}\!\int_0^R r\,dr\,d\phi\,dz = \pi R^2 h\]"""
    ),

    Hm2Card.Formula(
        id = "hm2_020", topic = Hm2Topic.KOORDINATEN, difficulty = 2,
        name = "Kugelkoordinaten",
        formula = """\[x=r\sin\theta\cos\phi,\; y=r\sin\theta\sin\phi,\; z=r\cos\theta;\quad dV=r^2\sin\theta\,dr\,d\theta\,d\phi\]""",
        variables = listOf(
            "\\(r\\)" to "Abstand \\([0,\\infty)\\)",
            "\\(\\theta\\)" to "Polwinkel (vom Nordpol) \\([0,\\pi]\\)",
            "\\(\\phi\\)" to "Azimutalwinkel \\([0,2\\pi)\\)"
        ),
        back = """Jacobi: \(|J| = r^2\sin\theta\)

Wann benutzen: Kugelförmige Bereiche.

Volumen Kugel \(r\leq R\):
\[\int_0^R\!\int_0^\pi\!\int_0^{2\pi} r^2\sin\theta\,d\phi\,d\theta\,dr = \frac{4}{3}\pi R^3\]"""
    ),

    Hm2Card.MC(
        id = "hm2_021", topic = Hm2Topic.KOORDINATEN, difficulty = 2,
        question = """Warum muss man bei Koordinatentransformationen die Jacobi-Determinante benutzen?""",
        options = listOf(
            "Um die Integrationsgrenzen zu bestimmen",
            "Das Volumenelement ändert sich bei Koordinatenwechsel",
            "Um die Funktionswerte zu transformieren",
            "Nur bei nichtlinearen Transformationen nötig"
        ),
        correctIndex = 1,
        explanation = """Das Volumenelement \(dx\,dy\) beschreibt ein infinitesimales Stück des Bereichs.

Bei Koordinatenwechsel \((u,v)\mapsto(x,y)\):
\[dx\,dy = \left|\det(J)\right|\,du\,dv\]

Die Jacobi-Matrix \(J = \begin{pmatrix}\partial_u x & \partial_v x\\\partial_u y & \partial_v y\end{pmatrix}\) beschreibt die lokale Verzerrung.

Ohne Jacobi: falsches Integral!"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_022", topic = Hm2Topic.KOORDINATEN, difficulty = 3,
        front = """Berechne \(|\det J|\) für die Polarkoordinaten-Transformation \((r,\phi)\mapsto(r\cos\phi, r\sin\phi)\).""",
        back = """\[J = \begin{pmatrix}\cos\phi & -r\sin\phi\\\sin\phi & r\cos\phi\end{pmatrix}\]

\[\det J = r\cos^2\phi + r\sin^2\phi = r\]

Da \(r\geq 0\): \(|\det J| = r\)

\[dA = dx\,dy = r\,dr\,d\phi\] ✓"""
    ),

    Hm2Card.Formula(
        id = "hm2_039", topic = Hm2Topic.KOORDINATEN, difficulty = 2,
        name = "Polarkoordinaten in 2D",
        formula = """\[x = r\cos\phi,\quad y = r\sin\phi;\quad dA = r\,dr\,d\phi\]""",
        variables = listOf(
            "\\(r\\)" to "Abstand \\([0,\\infty)\\)",
            "\\(\\phi\\)" to "Winkel \\([0,2\\pi)\\)",
            "\\(dA\\)" to "Flächenelement"
        ),
        back = """Nützlich bei kreissymmetrischen Integrationsbereichen!

\[\iint_{x^2+y^2\leq R^2} f\,dA = \int_0^{2\pi}\!\int_0^R f(r\cos\phi,r\sin\phi)\cdot r\,dr\,d\phi\]

Fläche des Kreises: \(\int_0^{2\pi}\!\int_0^R r\,dr\,d\phi = \pi R^2\)"""
    ),

    Hm2Card.Flashcard(
        id = "hm2_035", topic = Hm2Topic.KOORDINATEN, difficulty = 2,
        front = """Berechne das Volumen der Kugel mit Radius \(R\) mittels Kugelkoordinaten.""",
        back = """Kugelkoordinaten: \(dV = r^2\sin\theta\,dr\,d\theta\,d\phi\)

\[V = \int_0^{2\pi}\!d\phi \cdot \int_0^\pi\sin\theta\,d\theta \cdot \int_0^R r^2\,dr\]
\[= 2\pi \cdot \big[-\cos\theta\big]_0^\pi \cdot \frac{R^3}{3} = 2\pi \cdot 2 \cdot \frac{R^3}{3} = \frac{4}{3}\pi R^3\] ✓"""
    )
)

// ════════════════════════════════════════════
//  Helper: filtered card list
// ════════════════════════════════════════════

private fun hm2GetCards(topic: Hm2Topic?): List<Hm2Card> =
    if (topic == null) hm2Cards else hm2Cards.filter { it.topic == topic }

// ════════════════════════════════════════════
//  Main Screen
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Hm2TrainerScreen(onBackClick: () -> Unit) {
    var topicSelected by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf<Hm2Topic?>(null) }
    var cards by remember { mutableStateOf<List<Hm2Card>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var selectedMC by remember { mutableIntStateOf(-1) }
    var selectedTF by remember { mutableStateOf<Boolean?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }

    fun loadCards(topic: Hm2Topic?) {
        selectedTopic = topic
        cards = hm2GetCards(topic).shuffled()
        currentIndex = 0; revealed = false; selectedMC = -1; selectedTF = null
        score = 0; answered = 0; topicSelected = true
    }

    fun nextCard() {
        currentIndex = if (currentIndex < cards.size - 1) currentIndex + 1 else 0
        revealed = false; selectedMC = -1; selectedTF = null
    }

    fun prevCard() {
        currentIndex = if (currentIndex > 0) currentIndex - 1 else cards.size - 1
        revealed = false; selectedMC = -1; selectedTF = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (!topicSelected) "HM2 Trainer" else selectedTopic?.displayName ?: "Alle Themen",
                            fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 17.sp
                        )
                        if (topicSelected && cards.isNotEmpty()) {
                            Text(
                                "Karte ${currentIndex + 1} / ${cards.size}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (topicSelected) {
                            topicSelected = false; selectedTopic = null; cards = emptyList()
                        } else onBackClick()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
                actions = {
                    if (topicSelected && answered > 0) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                            Text(
                                "$score/$answered",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { padding ->
        if (!topicSelected) {
            Hm2TopicSelection(onTopicSelected = { loadCards(it) }, modifier = Modifier.padding(padding))
        } else if (cards.isNotEmpty()) {
            Hm2CardView(
                card = cards[currentIndex],
                revealed = revealed,
                selectedMC = selectedMC,
                selectedTF = selectedTF,
                onReveal = { revealed = true },
                onMCAnswer = { i ->
                    if (selectedMC < 0) {
                        selectedMC = i; revealed = true; answered++
                        if ((cards[currentIndex] as? Hm2Card.MC)?.correctIndex == i) score++
                    }
                },
                onTFAnswer = { tf ->
                    if (selectedTF == null) {
                        selectedTF = tf; revealed = true; answered++
                        if ((cards[currentIndex] as? Hm2Card.TrueFalse)?.correct == tf) score++
                    }
                },
                onNext = { nextCard() },
                onPrev = { prevCard() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ════════════════════════════════════════════
//  Topic Selection
// ════════════════════════════════════════════

@Composable
private fun Hm2TopicSelection(onTopicSelected: (Hm2Topic?) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("HM2 — Thema wählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Höhere Mathematik II · ETIT · KIT · Klausur 2026-08-10",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        Surface(
            onClick = { onTopicSelected(null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column {
                    Text("Alle Themen gemischt", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${hm2Cards.size} Karten aus allen Gebieten", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text(
            "Einzelne Themen", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
        )

        Hm2Topic.entries.forEach { topic ->
            val count = hm2Cards.count { it.topic == topic }
            Surface(
                onClick = { onTopicSelected(topic) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = topic.color.copy(alpha = 0.1f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(topic.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(topic.icon, null, Modifier.size(24.dp), tint = Color.White)
                    }
                    Column {
                        Text(topic.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = topic.color)
                        Text("$count Karten", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Basierend auf: HM2 ETIT KIT · Formelsammlung erlaubt",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

// ════════════════════════════════════════════
//  Card Dispatcher
// ════════════════════════════════════════════

@Composable
private fun Hm2CardView(
    card: Hm2Card,
    revealed: Boolean,
    selectedMC: Int,
    selectedTF: Boolean?,
    onReveal: () -> Unit,
    onMCAnswer: (Int) -> Unit,
    onTFAnswer: (Boolean) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (card) {
                is Hm2Card.Flashcard -> Hm2FlashcardView(card, revealed, onReveal, Modifier.fillMaxSize())
                is Hm2Card.Formula   -> Hm2FormulaView(card, revealed, onReveal, Modifier.fillMaxSize())
                is Hm2Card.MC        -> Hm2MCView(card, selectedMC, onMCAnswer, Modifier.fillMaxSize())
                is Hm2Card.TrueFalse -> Hm2TrueFalseView(card, selectedTF, onTFAnswer, Modifier.fillMaxSize())
            }
        }

        // Navigation row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onPrev, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Zurück")
            }
            Button(
                onClick = onNext, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = card.topic.color)
            ) {
                Text("Weiter")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

// ════════════════════════════════════════════
//  Flashcard View
// ════════════════════════════════════════════

@Composable
private fun Hm2FlashcardView(
    card: Hm2Card.Flashcard, revealed: Boolean, onReveal: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Hm2TopicBadge(card.topic)

        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Frage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Hm2Text(card.front, fontSize = 16, fontWeight = FontWeight.Medium)
            }
        }

        if (card.hint != null && !revealed) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFF9800).copy(alpha = 0.1f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp), tint = Color(0xFFFF9800))
                    Hm2Text("Tipp: ${card.hint}", fontSize = 13, color = Color(0xFFE65100), modifier = Modifier.weight(1f))
                }
            }
        }

        if (!revealed) {
            Button(
                onClick = onReveal, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = card.topic.color)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text("Antwort anzeigen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.08f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color(0xFF4CAF50))
                        Text("Antwort", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Hm2Text(card.back, fontSize = 14)
                }
            }
            if (card.klausurTipp != null) {
                Hm2KlausurTippBox(card.klausurTipp)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ════════════════════════════════════════════
//  Formula View
// ════════════════════════════════════════════

@Composable
private fun Hm2FormulaView(
    card: Hm2Card.Formula, revealed: Boolean, onReveal: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Hm2TopicBadge(card.topic)

        Text(card.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = card.topic.color.copy(alpha = 0.08f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Hm2LatexText(card.formula, fontSize = 15, fontWeight = "bold", color = card.topic.color)
            }
        }

        if (card.variables.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Variablen", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    card.variables.forEach { (key, value) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Hm2Text(key, fontSize = 13, fontWeight = FontWeight.SemiBold, color = card.topic.color, modifier = Modifier.widthIn(min = 70.dp))
                            Hm2Text(": $value", fontSize = 13)
                        }
                    }
                }
            }
        }

        if (!revealed) {
            Button(
                onClick = onReveal, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = card.topic.color)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text("Herleitung / Erklärung anzeigen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.08f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = Color(0xFF2196F3))
                        Text("Erklärung", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Hm2Text(card.back, fontSize = 14)
                }
            }
            if (card.klausurTipp != null) {
                Hm2KlausurTippBox(card.klausurTipp)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ════════════════════════════════════════════
//  Multiple Choice View
// ════════════════════════════════════════════

@Composable
private fun Hm2MCView(
    card: Hm2Card.MC, selectedMC: Int, onMCAnswer: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val answered = selectedMC >= 0
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Hm2TopicBadge(card.topic)

        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Multiple Choice", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Hm2Text(card.question, fontSize = 16, fontWeight = FontWeight.Medium)
            }
        }

        card.options.forEachIndexed { index, option ->
            val isCorrect = index == card.correctIndex
            val isSelected = index == selectedMC
            val bgColor = when {
                answered && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                answered && isSelected && !isCorrect -> Color(0xFFF44336).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
            }
            val borderColor = when {
                answered && isCorrect -> Color(0xFF4CAF50)
                answered && isSelected && !isCorrect -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Surface(
                onClick = { if (!answered) onMCAnswer(index) },
                modifier = Modifier.fillMaxWidth().border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = bgColor
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(
                            when {
                                answered && isCorrect -> Color(0xFF4CAF50)
                                answered && isSelected -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            answered && isCorrect -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            answered && isSelected -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            else -> Text("${'A' + index}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp)
                        }
                    }
                    Hm2Text(option, Modifier.weight(1f), fontSize = 14)
                }
            }
        }

        if (answered) {
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp), tint = Color(0xFF2196F3))
                        Text("Erklärung", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Hm2Text(card.explanation, fontSize = 13)
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ════════════════════════════════════════════
//  True / False View
// ════════════════════════════════════════════

@Composable
private fun Hm2TrueFalseView(
    card: Hm2Card.TrueFalse, selectedTF: Boolean?, onTFAnswer: (Boolean) -> Unit, modifier: Modifier = Modifier
) {
    val answered = selectedTF != null
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Hm2TopicBadge(card.topic)

        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Wahr oder Falsch?", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Hm2Text(card.statement, fontSize = 16, fontWeight = FontWeight.Medium)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val trueCorrect = card.correct
            val trueSelected = selectedTF == true
            Button(
                onClick = { if (!answered) onTFAnswer(true) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        answered && trueSelected && trueCorrect -> Color(0xFF4CAF50)
                        answered && trueSelected && !trueCorrect -> Color(0xFFF44336)
                        answered && !trueSelected && trueCorrect -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                        else -> Color(0xFF4CAF50)
                    }
                )
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("WAHR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            val falseCorrect = !card.correct
            val falseSelected = selectedTF == false
            Button(
                onClick = { if (!answered) onTFAnswer(false) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        answered && falseSelected && falseCorrect -> Color(0xFF4CAF50)
                        answered && falseSelected && !falseCorrect -> Color(0xFFF44336)
                        answered && !falseSelected && falseCorrect -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                        else -> Color(0xFFF44336)
                    }
                )
            ) {
                Icon(Icons.Default.Close, null, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("FALSCH", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (answered) {
            val isCorrect = selectedTF == card.correct
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFF44336).copy(alpha = 0.15f)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCorrect) Icons.Default.Check else Icons.Default.Close, null,
                        Modifier.size(20.dp), tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Text(
                        if (isCorrect) "Richtig! Die Aussage ist ${if (card.correct) "wahr" else "falsch"}."
                        else "Falsch! Die Aussage ist ${if (card.correct) "wahr" else "falsch"}.",
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp), tint = Color(0xFF2196F3))
                        Text("Erklärung", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Hm2Text(card.explanation, fontSize = 13)
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ════════════════════════════════════════════
//  Shared Small Composables
// ════════════════════════════════════════════

@Composable
private fun Hm2TopicBadge(topic: Hm2Topic) {
    Surface(shape = RoundedCornerShape(8.dp), color = topic.color.copy(alpha = 0.15f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(topic.icon, null, Modifier.size(14.dp), tint = topic.color)
            Text(topic.displayName, fontSize = 11.sp, color = topic.color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Hm2KlausurTippBox(tipp: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2196F3).copy(alpha = 0.1f)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.School, null, Modifier.size(18.dp), tint = Color(0xFF1565C0))
            Hm2Text("Klausur-Tipp: $tipp", fontSize = 13, color = Color(0xFF0D47A1), modifier = Modifier.weight(1f))
        }
    }
}
