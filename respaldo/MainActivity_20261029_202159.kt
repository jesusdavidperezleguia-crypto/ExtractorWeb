package com.example.extractorweb

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var entradaTexto: EditText
    private lateinit var resultadoTexto: TextView
    private lateinit var entradaUrl: EditText
    private lateinit var estadoDescarga: TextView
    private lateinit var btnDescargar: Button
    private lateinit var galeriaImagenes: LinearLayout

    private val analizador = AnalizadorGeneral()
    private val analizadorSintactico = AnalizadorSintactico()
    private val analizadorHTML = AnalizadorHTML()
    private val extractorHTML = ExtractorHTML()
    private val descargadorWeb = DescargadorWeb()

    private val ejemplo = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Mi página de prueba</title>
        </head>
        <body>
            <h1>Bienvenido a Extractor Web</h1>
            <p>Esta es una página de ejemplo.</p>
            <a href="https://www.wikipedia.org/">Visitar Wikipedia</a>
            <img src="https://www.w3schools.com/html/img_girl.jpg" alt="Imagen de prueba">
        </body>
        </html>
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        entradaTexto = findViewById(R.id.entradaTexto)
        resultadoTexto = findViewById(R.id.resultadoTexto)
        entradaUrl = findViewById(R.id.entradaUrl)
        estadoDescarga = findViewById(R.id.estadoDescarga)
        btnDescargar = findViewById(R.id.btnDescargar)
        galeriaImagenes = findViewById(R.id.galeriaImagenes)

        findViewById<Button>(R.id.btnEjemplo).setOnClickListener {
            entradaTexto.setText(ejemplo)
            entradaUrl.setText("https://www.w3schools.com/html/")
            estadoDescarga.text = "Ejemplo local cargado."
            resultadoTexto.text = "Ejemplo cargado. Pulsa ANALIZAR TEXTO."
            galeriaImagenes.removeAllViews()
        }

        findViewById<Button>(R.id.btnAnalizar).setOnClickListener {
            analizarTexto()
        }

        btnDescargar.setOnClickListener {
            descargarPagina()
        }

        entradaUrl.setText("https://www.wikipedia.org/")
        entradaTexto.setText(ejemplo)
    }

    private fun descargarPagina() {
        val direccion = entradaUrl.text.toString().trim()

        if (direccion.isBlank()) {
            estadoDescarga.text = "Escribe una dirección web primero."
            entradaUrl.requestFocus()
            return
        }

        btnDescargar.isEnabled = false
        estadoDescarga.text = "Descargando página web..."
        resultadoTexto.text = "Conectando con el servidor. Espera un momento."
        galeriaImagenes.removeAllViews()

        Thread {
            try {
                val pagina = descargadorWeb.descargar(direccion)

                runOnUiThread {
                    entradaUrl.setText(pagina.urlFinal)
                    entradaTexto.setText(pagina.html)

                    estadoDescarga.text =
                        "Descarga completada. HTTP ${pagina.codigoHTTP}. " +
                        "Caracteres recibidos: ${pagina.html.length}."

                    analizarTexto()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    estadoDescarga.text =
                        "No se pudo descargar la página: " +
                        (e.message ?: "Error de conexión desconocido.")

                    resultadoTexto.text =
                        "Comprueba la URL y tu conexión a Internet.\n\n" +
                        "Detalle: ${e.message ?: "Error desconocido"}"
                }
            } finally {
                runOnUiThread {
                    btnDescargar.isEnabled = true
                }
            }
        }.start()
    }

    private fun analizarTexto() {
        try {
            val texto = entradaTexto.text.toString()

            val resultado = analizador.analizar(texto)
            val diagnosticos = analizadorSintactico.analizar(texto)
            val erroresHTML = analizadorHTML.analizar(texto)
            val extraccion = extractorHTML.extraer(texto)

            val salida = StringBuilder()

            salida.append("INFORME DE EXTRACTOR WEB\n")
            salida.append("========================\n\n")

            salida.append("1. ANÁLISIS LÉXICO\n")
            salida.append("------------------\n")
            salida.append("Líneas: ${resultado.lineas}\n")
            salida.append("Palabras: ${resultado.palabras}\n")
            salida.append("Números: ${resultado.numeros}\n")
            salida.append("Etiquetas: ${resultado.etiquetas}\n")
            salida.append("Símbolos y operadores: ${resultado.simbolos}\n")
            salida.append("Tokens: ${resultado.tokens.size}\n\n")

            salida.append("2. ANÁLISIS SINTÁCTICO BÁSICO\n")
            salida.append("----------------------------\n")

            if (diagnosticos.isEmpty()) {
                salida.append("Sin problemas en las reglas revisadas.\n")
            } else {
                diagnosticos.forEach {
                    salida.append(
                        "[${it.tipo}] Línea ${it.linea}, " +
                        "columna ${it.columna}: ${it.mensaje}\n"
                    )
                }
            }

            salida.append("\n3. VALIDACIÓN HTML BÁSICA\n")
            salida.append("------------------------\n")

            if (erroresHTML.isEmpty()) {
                salida.append("No se detectaron problemas de etiquetas.\n")
            } else {
                erroresHTML.forEach {
                    salida.append(
                        "[${it.tipo}] Línea ${it.linea}, " +
                        "columna ${it.columna}: ${it.mensaje}\n"
                    )
                }
            }

            salida.append("\n4. DATOS EXTRAÍDOS DEL HTML\n")
            salida.append("---------------------------\n")

            salida.append("\nTÍTULOS (${extraccion.titulos.size})\n")
            if (extraccion.titulos.isEmpty()) {
                salida.append("No se encontraron títulos.\n")
            } else {
                extraccion.titulos.forEach { salida.append("- $it\n") }
            }

            salida.append("\nENLACES (${extraccion.enlaces.size})\n")
            if (extraccion.enlaces.isEmpty()) {
                salida.append("No se encontraron enlaces.\n")
            } else {
                extraccion.enlaces.forEach { salida.append("- $it\n") }
            }

            salida.append("\nIMÁGENES (${extraccion.imagenes.size})\n")
            if (extraccion.imagenes.isEmpty()) {
                salida.append("No se encontraron imágenes.\n")
            } else {
                extraccion.imagenes.forEach { salida.append("- $it\n") }
            }

            salida.append("\nTEXTO EXTRAÍDO\n")
            salida.append("--------------\n")
            salida.append(
                extraccion.textoVisible.ifBlank {
                    "No se encontró texto visible."
                }
            )
            salida.append("\n\n5. TOKENS IDENTIFICADOS\n")
            salida.append("-----------------------\n")

            resultado.tokens.forEachIndexed { indice, token ->
                salida.append(
                    "${indice + 1}. [${token.tipo}] '${token.texto}' " +
                    "(línea ${token.linea}, columna ${token.columna})\n"
                )
            }

            resultadoTexto.text = salida.toString()

            mostrarImagenes(
                extraccion.imagenes,
                entradaUrl.text.toString().trim()
            )

        } catch (e: Exception) {
            resultadoTexto.text =
                "ERROR AL ANALIZAR\n\n${e.message ?: "Error desconocido"}"
        }
    }

    private fun mostrarImagenes(imagenes: List<String>, direccionBase: String) {
        galeriaImagenes.removeAllViews()

        if (imagenes.isEmpty()) {
            agregarMensajeGaleria("No se encontraron imágenes para mostrar.")
            return
        }

        val direcciones = imagenes.take(12)
        agregarMensajeGaleria(
            "Imágenes encontradas: ${imagenes.size}. " +
            "Mostrando hasta ${direcciones.size}."
        )

        direcciones.forEachIndexed { indice, origen ->
            val urlImagen = try {
                URI(direccionBase).resolve(origen).toString()
            } catch (_: Exception) {
                origen
            }

            val contenedor = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10, 10, 10, 16)
            }

            val etiqueta = TextView(this).apply {
                text = "${indice + 1}. $urlImagen"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
                setPadding(0, 4, 0, 8)
            }

            val imagen = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    220
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.rgb(32, 32, 32))
                contentDescription = "Vista previa de imagen ${indice + 1}"
            }

            contenedor.addView(etiqueta)
            contenedor.addView(imagen)
            galeriaImagenes.addView(contenedor)

            Thread {
                var conexion: HttpURLConnection? = null

                try {
                    conexion = URL(urlImagen).openConnection() as HttpURLConnection
                    conexion.connectTimeout = 10000
                    conexion.readTimeout = 15000
                    conexion.instanceFollowRedirects = true
                    conexion.setRequestProperty("User-Agent", "ExtractorWeb/1.0")
                    conexion.connect()

                    val tipo = conexion.contentType ?: ""
                    if (conexion.responseCode in 200..299 &&
                        tipo.startsWith("image/")) {

                        val bitmap = conexion.inputStream.use {
                            BitmapFactory.decodeStream(it)
                        }

                        runOnUiThread {
                            if (bitmap != null) {
                                imagen.setImageBitmap(bitmap)
                            } else {
                                etiqueta.text = "${indice + 1}. No se pudo interpretar la imagen."
                            }
                        }
                    } else {
                        runOnUiThread {
                            etiqueta.text = "${indice + 1}. Imagen no disponible."
                        }
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        etiqueta.text = "${indice + 1}. No se pudo cargar: $urlImagen"
                    }
                } finally {
                    conexion?.disconnect()
                }
            }.start()
        }
    }

    private fun agregarMensajeGaleria(mensaje: String) {
        val texto = TextView(this).apply {
            text = mensaje
            setTextColor(android.graphics.Color.LTGRAY)
            textSize = 14f
            setPadding(10, 12, 10, 12)
        }
        galeriaImagenes.addView(texto)
    }
}
