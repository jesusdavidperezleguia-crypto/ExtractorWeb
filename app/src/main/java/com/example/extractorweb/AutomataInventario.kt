package com.example.extractorweb

data class Producto(
    val nombre: String,
    val stock: Int,
    val precio: Double,
    val valorTotal: Double
)

data class ResultadoInventario(
    val productos: List<Producto>,
    val tokens: List<String>,
    val estados: List<String>,
    val valorAcumulado: Double
)

class AutomataInventario {

    private enum class Estado {
        INICIO, NOMBRE, STOCK, PRECIO, CIERRE
    }

    fun analizar(texto: String): ResultadoInventario {
        val productos = mutableListOf<Producto>()
        val tokens = mutableListOf<String>()
        val estados = mutableListOf<String>()

        var posicion = 0
        var acumulador = 0.0
        var estado = Estado.INICIO
        var nombre = ""
        var stock = 0
        var precio = 0.0

        fun consumir(token: String) {
            if (!texto.startsWith(token, posicion)) {
                throw IllegalArgumentException(
                    "Posición $posicion: se esperaba $token"
                )
            }
            tokens.add(token)
            posicion += token.length
        }

        fun leerHasta(delimitador: String): String {
            val fin = texto.indexOf(delimitador, posicion)
            if (fin < 0) {
                throw IllegalArgumentException(
                    "No se encontró $delimitador"
                )
            }
            val valor = texto.substring(posicion, fin).trim()
            posicion = fin
            return valor
        }

        estados.add("INICIO")

        while (posicion < texto.length) {
            when (estado) {
                Estado.INICIO -> {
                    while (posicion < texto.length &&
                        texto[posicion].isWhitespace()) {
                        posicion++
                    }

                    if (posicion >= texto.length) break

                    consumir("<prod>")
                    estado = Estado.NOMBRE
                    estados.add("INICIO -> NOMBRE")
                }

                Estado.NOMBRE -> {
                    consumir("Nombre:")
                    nombre = leerHasta("; Stock:")

                    if (nombre.isBlank()) {
                        throw IllegalArgumentException(
                            "El nombre está vacío"
                        )
                    }

                    consumir("; Stock:")
                    estado = Estado.STOCK
                    estados.add("NOMBRE -> STOCK")
                }

                Estado.STOCK -> {
                    val valor = leerHasta("; Precio:")
                    stock = valor.toIntOrNull()
                        ?: throw IllegalArgumentException(
                            "Stock inválido: $valor"
                        )

                    if (stock < 0) {
                        throw IllegalArgumentException(
                            "El stock no puede ser negativo"
                        )
                    }

                    consumir("; Precio:")
                    estado = Estado.PRECIO
                    estados.add("STOCK -> PRECIO")
                }

                Estado.PRECIO -> {
                    val valor = leerHasta("</prod>")
                    precio = valor.toDoubleOrNull()
                        ?: throw IllegalArgumentException(
                            "Precio inválido: $valor"
                        )

                    if (!precio.isFinite() || precio < 0) {
                        throw IllegalArgumentException(
                            "El precio debe ser válido y no negativo"
                        )
                    }

                    estado = Estado.CIERRE
                    estados.add("PRECIO -> CIERRE")
                }

                Estado.CIERRE -> {
                    consumir("</prod>")

                    val total = stock * precio
                    productos.add(
                        Producto(nombre, stock, precio, total)
                    )
                    acumulador += total

                    estados.add(
                        "CIERRE: $nombre, total=$total, " +
                        "acumulado=$acumulador"
                    )

                    estado = Estado.INICIO
                }
            }
        }

        if (estado != Estado.INICIO) {
            throw IllegalArgumentException(
                "El producto está incompleto"
            )
        }

        if (productos.isEmpty()) {
            throw IllegalArgumentException(
                "No se encontraron productos"
            )
        }

        estados.add("FINAL: análisis completado")

        return ResultadoInventario(
            productos, tokens, estados, acumulador
        )
    }
}
