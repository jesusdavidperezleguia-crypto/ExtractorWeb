package com.example.extractorweb

data class ErrorHTML(
    val tipo: String,
    val mensaje: String,
    val linea: Int,
    val columna: Int
)

class AnalizadorHTML {

    private val etiquetasVacias = setOf(
        "area", "base", "br", "col", "embed", "hr",
        "img", "input", "link", "meta", "param",
        "source", "track", "wbr"
    )

    private val patronEtiqueta =
        Regex("""<!--[\s\S]*?-->|<![^>]*>|<\?[^>]*\?>|</?([A-Za-z][A-Za-z0-9:-]*)(?:\s[^<>]*?)?\s*/?>""")

    fun analizar(html: String): List<ErrorHTML> {
        val errores = mutableListOf<ErrorHTML>()
        val pila = mutableListOf<Triple<String, Int, Int>>()

        for (coincidencia in patronEtiqueta.findAll(html)) {
            val etiqueta = coincidencia.value

            // Ignorar comentarios, declaraciones y etiquetas de procesamiento.
            if (etiqueta.startsWith("<!--") ||
                etiqueta.startsWith("<!") ||
                etiqueta.startsWith("<?")) {
                continue
            }

            val nombre = coincidencia.groupValues[1].lowercase()
            val cierre = etiqueta.startsWith("</")
            val autocierre = etiqueta.endsWith("/>")

            val antes = html.substring(0, coincidencia.range.first)
            val linea = antes.count { it == '\n' } + 1
            val columna = (antes.length - (antes.lastIndexOf('\n') + 1)) + 1

            if (cierre) {
                val ultima = pila.lastOrNull()

                if (ultima == null) {
                    errores.add(
                        ErrorHTML(
                            "ERROR",
                            "La etiqueta </$nombre> no tiene una apertura correspondiente",
                            linea,
                            columna
                        )
                    )
                } else if (ultima.first != nombre) {
                    errores.add(
                        ErrorHTML(
                            "ERROR",
                            "Se encontró </$nombre>, pero se esperaba </${ultima.first}>",
                            linea,
                            columna
                        )
                    )
                } else {
                    pila.removeAt(pila.lastIndex)
                }
            } else if (!autocierre && nombre !in etiquetasVacias) {
                pila.add(Triple(nombre, linea, columna))
            }
        }

        for ((nombre, linea, columna) in pila) {
            errores.add(
                ErrorHTML(
                    "ERROR",
                    "La etiqueta <$nombre> no tiene cierre",
                    linea,
                    columna
                )
            )
        }

        return errores.sortedWith(
            compareBy<ErrorHTML> { it.linea }
                .thenBy { it.columna }
        )
    }
}
