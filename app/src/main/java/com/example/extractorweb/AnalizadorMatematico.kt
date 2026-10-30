package com.example.extractorweb

import kotlin.math.pow

data class ResultadoMatematico(
    val correcto: Boolean,
    val resultado: String,
    val procedimiento: List<String>,
    val variables: Map<String, Double>
)

class AnalizadorMatematico {

    private data class Token(
        val tipo: String,
        val valor: String,
        val linea: Int
    )

    private lateinit var tokens: List<Token>
    private var posicion = 0
    private val variables = linkedMapOf<String, Double>()
    private val procedimiento = mutableListOf<String>()

    fun analizar(codigo: String): ResultadoMatematico {
        variables.clear()
        procedimiento.clear()

        return try {
            tokens = tokenizar(codigo)
            posicion = 0

            while (!esTipo("FIN")) {
                if (esTipo("SEPARADOR")) {
                    avanzar()
                    continue
                }

                analizarInstruccion()

                if (!esTipo("FIN") && !esTipo("SEPARADOR")) {
                    error("Se esperaba un salto de línea o punto y coma.")
                }
            }

            ResultadoMatematico(
                correcto = true,
                resultado = variables.entries.lastOrNull()?.let {
                    "${it.key} = ${formatear(it.value)}"
                } ?: "Código analizado correctamente.",
                procedimiento = procedimiento.toList(),
                variables = variables.toMap()
            )

        } catch (e: Exception) {
            ResultadoMatematico(
                correcto = false,
                resultado = e.message ?: "Error matemático desconocido.",
                procedimiento = procedimiento.toList(),
                variables = variables.toMap()
            )
        }
    }

    private fun tokenizar(codigo: String): List<Token> {
        val lista = mutableListOf<Token>()
        var i = 0
        var linea = 1

        while (i < codigo.length) {
            val c = codigo[i]

            when {
                c == ' ' || c == '\t' || c == '\r' -> i++

                c == '\n' -> {
                    lista.add(Token("SEPARADOR", "\n", linea))
                    linea++
                    i++
                }

                c == ';' -> {
                    lista.add(Token("SEPARADOR", ";", linea))
                    i++
                }

                c == '/' && i + 1 < codigo.length &&
                    codigo[i + 1] == '/' -> {
                    while (i < codigo.length && codigo[i] != '\n') i++
                }

                c.isDigit() || (c == '.' && i + 1 < codigo.length &&
                    codigo[i + 1].isDigit()) -> {
                    val inicio = i
                    var puntos = 0

                    while (i < codigo.length &&
                        (codigo[i].isDigit() || codigo[i] == '.')) {
                        if (codigo[i] == '.') puntos++
                        i++
                    }

                    if (puntos > 1) error("Número decimal inválido en línea $linea.")

                    lista.add(Token("NUMERO", codigo.substring(inicio, i), linea))
                }

                c.isLetter() || c == '_' -> {
                    val inicio = i
                    while (i < codigo.length &&
                        (codigo[i].isLetterOrDigit() || codigo[i] == '_')) i++

                    lista.add(Token("IDENTIFICADOR", codigo.substring(inicio, i), linea))
                }

                c in "+-*/%^=()," -> {
                    lista.add(Token("SIMBOLO", c.toString(), linea))
                    i++
                }

                else -> error("Carácter no permitido '$c' en línea $linea.")
            }
        }

        lista.add(Token("FIN", "", linea))
        return lista
    }

    private fun analizarInstruccion() {
        var declaracion = false

        if (actual().valor in listOf("let", "var", "const")) {
            declaracion = true
            avanzar()
        }

        val nombre = esperar("IDENTIFICADOR").valor

        if (nombre in listOf("let", "var", "const")) {
            error("Se esperaba el nombre de una variable.")
        }

        esperarValor("=")

        val valor = expresion()

        if (!valor.isFinite()) {
            error("El resultado no es un número válido.")
        }

        if (declaracion && variables.containsKey(nombre)) {
            error("La variable '$nombre' ya fue declarada.")
        }

        if (!declaracion && !variables.containsKey(nombre)) {
            error("La variable '$nombre' no ha sido declarada. Usa let, var o const.")
        }

        variables[nombre] = valor

        procedimiento.add(
            "$nombre = ${formatear(valor)}"
        )
    }

    private fun expresion(): Double = sumaResta()

    private fun sumaResta(): Double {
        var resultado = multiplicacionDivision()

        while (actual().valor == "+" || actual().valor == "-") {
            val operador = avanzar().valor
            val segundo = multiplicacionDivision()

            resultado = if (operador == "+") {
                resultado + segundo
            } else {
                resultado - segundo
            }
        }

        return resultado
    }

    private fun multiplicacionDivision(): Double {
        var resultado = potencia()

        while (actual().valor in listOf("*", "/", "%")) {
            val operador = avanzar().valor
            val segundo = potencia()

            resultado = when (operador) {
                "*" -> resultado * segundo
                "/" -> {
                    if (segundo == 0.0) error("No se puede dividir entre cero.")
                    resultado / segundo
                }
                "%" -> {
                    if (segundo == 0.0) error("No se puede calcular módulo entre cero.")
                    resultado % segundo
                }
                else -> resultado
            }
        }

        return resultado
    }

    private fun potencia(): Double {
        val base = unario()

        if (actual().valor == "^") {
            avanzar()
            val exponente = potencia()
            return base.powSeguro(exponente)
        }

        return base
    }

    private fun unario(): Double {
        return when (actual().valor) {
            "-" -> {
                avanzar()
                -unario()
            }
            "+" -> {
                avanzar()
                unario()
            }
            else -> primario()
        }
    }

    private fun primario(): Double {
        val token = actual()

        if (token.tipo == "NUMERO") {
            avanzar()
            return token.valor.toDouble()
        }

        if (token.valor == "(") {
            avanzar()
            val valor = expresion()
            esperarValor(")")
            return valor
        }

        if (token.tipo == "IDENTIFICADOR") {
            val nombre = avanzar().valor

            if (actual().valor == "(") {
                avanzar()
                val argumento = expresion()
                esperarValor(")")

                return when (nombre) {
                    "sqrt" -> {
                        if (argumento < 0) error("No existe raíz real de un número negativo.")
                        kotlin.math.sqrt(argumento)
                    }
                    "abs" -> kotlin.math.abs(argumento)
                    "round" -> kotlin.math.round(argumento)
                    else -> error("Función '$nombre' no reconocida.")
                }
            }

            return variables[nombre]
                ?: error("La variable '$nombre' no existe.")
        }

        error("Expresión inválida en línea ${token.linea}.")
    }

    private fun actual(): Token = tokens[posicion]

    private fun avanzar(): Token {
        val token = actual()
        if (posicion < tokens.lastIndex) posicion++
        return token
    }

    private fun esTipo(tipo: String) = actual().tipo == tipo

    private fun esperar(tipo: String): Token {
        if (!esTipo(tipo)) {
            error("Se esperaba $tipo en línea ${actual().linea}.")
        }
        return avanzar()
    }

    private fun esperarValor(valor: String) {
        if (actual().valor != valor) {
            error("Se esperaba '$valor' en línea ${actual().linea}.")
        }
        avanzar()
    }

    private fun formatear(numero: Double): String {
        return if (numero == numero.toLong().toDouble()) {
            numero.toLong().toString()
        } else {
            numero.toString()
        }
    }

    private fun error(mensaje: String): Nothing {
        throw IllegalArgumentException(mensaje)
    }

    private fun Double.powSeguro(exponente: Double): Double {
        val resultado = this.pow(exponente)
        if (!resultado.isFinite()) {
            error("La potencia produce un resultado fuera de rango.")
        }
        return resultado
    }
}

