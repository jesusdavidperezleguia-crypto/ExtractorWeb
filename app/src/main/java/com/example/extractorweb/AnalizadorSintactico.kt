package com.example.extractorweb

data class Diagnostico(
    val tipo: String,
    val mensaje: String,
    val linea: Int,
    val columna: Int
)

class AnalizadorSintactico {

    fun analizar(texto: String): List<Diagnostico> {
        val diagnosticos = mutableListOf<Diagnostico>()
        val pila = mutableListOf<Triple<Char, Int, Int>>()

        var linea = 1
        var columna = 1
        var comilla: Char? = null
        var escape = false
        var comentarioLinea = false

        val aperturas = setOf('(', '[', '{')
        val cierres = mapOf(')' to '(', ']' to '[', '}' to '{')

        for (caracter in texto) {
            if (comentarioLinea) {
                if (caracter == '\n') {
                    comentarioLinea = false
                }
            } else if (comilla != null) {
                if (escape) {
                    escape = false
                } else if (caracter == '\\') {
                    escape = true
                } else if (caracter == comilla) {
                    comilla = null
                }
            } else {
                when {
                    caracter == '"' || caracter == '\'' || caracter == '`' ->
                        comilla = caracter

                    caracter == '/' && columna > 1 -> {
                        // El reconocimiento de comentarios se deja para
                        // una mejora posterior, dependiente del lenguaje.
                    }

                    caracter in aperturas ->
                        pila.add(Triple(caracter, linea, columna))

                    caracter in cierres.keys -> {
                        val esperado = cierres[caracter]!!
                        val ultimo = pila.lastOrNull()

                        if (ultimo == null || ultimo.first != esperado) {
                            diagnosticos.add(
                                Diagnostico(
                                    "ERROR",
                                    "El símbolo '$caracter' no tiene una apertura compatible",
                                    linea,
                                    columna
                                )
                            )
                        } else {
                            pila.removeAt(pila.lastIndex)
                        }
                    }
                }
            }

            if (caracter == '\n') {
                linea++
                columna = 1
                comentarioLinea = false
            } else {
                columna++
            }
        }

        for ((caracter, lineaApertura, columnaApertura) in pila) {
            diagnosticos.add(
                Diagnostico(
                    "ERROR",
                    "Falta cerrar el delimitador '$caracter'",
                    lineaApertura,
                    columnaApertura
                )
            )
        }

        // Comprobar expresiones sencillas que terminan con un operador.
        val operadoresFinales =
            Regex("""(?:[=+\-*/%]|&&|\|\||==|!=|<=|>=|->|=>)\s*$""")

        val asignacionVacia = Regex("""^[A-Za-z_]\w*\s*=\s*$""")

        texto.lines().forEachIndexed { indice, contenido ->
            val limpia = contenido.trim()

            if (asignacionVacia.matches(limpia)) {
                diagnosticos.add(
                    Diagnostico(
                        "ERROR",
                        "La asignación no tiene un valor a la derecha del '='",
                        indice + 1,
                        contenido.indexOf('=').coerceAtLeast(0) + 1
                    )
                )
            } else if (
                limpia.isNotEmpty() &&
                operadoresFinales.containsMatchIn(limpia) &&
                !limpia.startsWith("//")
            ) {
                diagnosticos.add(
                    Diagnostico(
                        "ADVERTENCIA",
                        "La línea termina con un operador; comprueba si falta un valor",
                        indice + 1,
                        contenido.trimEnd().length.coerceAtLeast(1)
                    )
                )
            }
        }

        return diagnosticos.sortedWith(
            compareBy<Diagnostico> { it.linea }
                .thenBy { it.columna }
        )
    }
}
