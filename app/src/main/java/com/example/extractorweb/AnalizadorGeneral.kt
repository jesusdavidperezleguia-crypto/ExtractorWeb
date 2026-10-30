package com.example.extractorweb

data class TokenAnalizado(
    val texto: String,
    val tipo: String,
    val linea: Int,
    val columna: Int
)

data class ResultadoAnalisis(
    val tokens: List<TokenAnalizado>,
    val estados: List<String>,
    val palabras: Int,
    val numeros: Int,
    val etiquetas: Int,
    val simbolos: Int,
    val lineas: Int
)

class AnalizadorGeneral {

    private val patron = Regex(
        """</?[A-Za-z][^>]*>|\d+(?:[.,]\d+)?|[\p{L}\p{M}_]+|\s+|."""
    )

    private val operadores = setOf(
        "+", "-", "*", "/", "=", "==", "!=", "<=", ">=",
        "&&", "||", "!", "%", "++", "--", "->", "=>"
    )

    fun analizar(texto: String): ResultadoAnalisis {
        val tokens = mutableListOf<TokenAnalizado>()
        val estados = mutableListOf<String>()

        var linea = 1
        var columna = 1

        var palabras = 0
        var numeros = 0
        var etiquetas = 0
        var simbolos = 0

        estados.add("INICIO: texto recibido")

        for (coincidencia in patron.findAll(texto)) {
            val elemento = coincidencia.value
            val tipo: String

            tipo = when {
                elemento.all { it.isWhitespace() } -> "ESPACIO"
                elemento.startsWith("<") && elemento.endsWith(">") ->
                    "ETIQUETA"
                elemento.first().isDigit() -> "NUMERO"
                elemento.first().isLetter() ||
                    elemento.first() == '_' ||
                    Character.getType(elemento.first()) ==
                    Character.NON_SPACING_MARK.toInt() ->
                    "PALABRA"
                elemento in operadores -> "OPERADOR"
                else -> "SIMBOLO"
            }

            if (tipo != "ESPACIO") {
                tokens.add(
                    TokenAnalizado(
                        texto = elemento,
                        tipo = tipo,
                        linea = linea,
                        columna = columna
                    )
                )

                when (tipo) {
                    "PALABRA" -> palabras++
                    "NUMERO" -> numeros++
                    "ETIQUETA" -> etiquetas++
                    "SIMBOLO", "OPERADOR" -> simbolos++
                }

                estados.add(
                    "LECTURA -> $tipo: '$elemento' (línea $linea)"
                )
            }

            for (caracter in elemento) {
                if (caracter == '\n') {
                    linea++
                    columna = 1
                } else {
                    columna++
                }
            }
        }

        estados.add("FINAL: análisis completado")

        return ResultadoAnalisis(
            tokens = tokens,
            estados = estados,
            palabras = palabras,
            numeros = numeros,
            etiquetas = etiquetas,
            simbolos = simbolos,
            lineas = if (texto.isEmpty()) 0 else texto.count { it == '\n' } + 1
        )
    }
}
