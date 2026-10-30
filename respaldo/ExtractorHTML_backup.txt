package com.example.extractorweb

data class ResultadoWeb(
    val titulos: List<String>,
    val enlaces: List<String>,
    val imagenes: List<String>,
    val textoVisible: String
)

class ExtractorHTML {

    private val menor = 60.toChar()
    private val mayor = 62.toChar()

    fun extraer(html: String): ResultadoWeb {
        val titulos = mutableListOf<String>()
        val enlaces = mutableListOf<String>()
        val imagenes = mutableListOf<String>()
        val textos = mutableListOf<String>()

        var posicion = 0

        while (posicion < html.length) {
            val inicio = html.indexOf(menor, posicion)
            if (inicio < 0) {
                textos.add(html.substring(posicion))
                break
            }

            if (inicio > posicion) {
                textos.add(html.substring(posicion, inicio))
            }

            val fin = html.indexOf(mayor, inicio + 1)
            if (fin < 0) break

            val etiqueta = html.substring(inicio + 1, fin).trim()
            val etiquetaMin = etiqueta.lowercase()

            if (etiquetaMin.startsWith("a ") || etiquetaMin == "a") {
                buscarAtributo(etiqueta, "href")?.let { enlaces.add(it) }
            }

            if (etiquetaMin.startsWith("img ") || etiquetaMin == "img") {
                buscarAtributo(etiqueta, "src")?.let { imagenes.add(it) }
            }

            if (etiquetaMin.startsWith("title") ||
                etiquetaMin.startsWith("h1")) {
                val nombre = etiquetaMin.takeWhile { it.isLetterOrDigit() }
                val cierre = menor.toString() + "/" + nombre + mayor
                val finTexto = html.indexOf(cierre, fin + 1, ignoreCase = true)

                if (finTexto >= 0) {
                    val titulo = limpiarTexto(html.substring(fin + 1, finTexto))
                    if (titulo.isNotBlank()) titulos.add(titulo)
                }
            }

            if (etiquetaMin.startsWith("script") ||
                etiquetaMin.startsWith("style")) {
                val nombre = etiquetaMin.takeWhile { it.isLetterOrDigit() }
                val cierre = menor.toString() + "/" + nombre + mayor
                val finBloque = html.indexOf(cierre, fin + 1, ignoreCase = true)

                if (finBloque >= 0) {
                    posicion = finBloque + cierre.length
                    continue
                }
            }

            posicion = fin + 1
        }

        val textoVisible = limpiarTexto(
            textos.joinToString(" ")
                .replace("&nbsp;", " ", ignoreCase = true)
                .replace("&amp;", "&", ignoreCase = true)
                .replace("&lt;", "<", ignoreCase = true)
                .replace("&gt;", ">", ignoreCase = true)
        )

        return ResultadoWeb(
            titulos = titulos.distinct(),
            enlaces = enlaces.distinct(),
            imagenes = imagenes.distinct(),
            textoVisible = textoVisible
        )
    }

    private fun buscarAtributo(etiqueta: String, nombre: String): String? {
        val indice = etiqueta.indexOf(nombre, ignoreCase = true)
        if (indice < 0) return null

        val anterior = if (indice > 0) etiqueta[indice - 1] else ' '
        if (!anterior.isWhitespace()) return null

        var i = indice + nombre.length

        while (i < etiqueta.length && etiqueta[i].isWhitespace()) i++
        if (i >= etiqueta.length || etiqueta[i] != '=') return null
        i++

        while (i < etiqueta.length && etiqueta[i].isWhitespace()) i++
        if (i >= etiqueta.length) return null

        val comilla = etiqueta[i]
        if (comilla == '"' || comilla == '\'') {
            i++
            val fin = etiqueta.indexOf(comilla, i)
            if (fin < 0) return null
            return etiqueta.substring(i, fin)
        }

        val inicio = i
        while (i < etiqueta.length &&
            !etiqueta[i].isWhitespace() &&
            etiqueta[i] != mayor) {
            i++
        }

        return etiqueta.substring(inicio, i).ifBlank { null }
    }

    private fun limpiarTexto(texto: String): String {
        return texto
            .replace(Regex("[\\s]+"), " ")
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .trim()
    }
}