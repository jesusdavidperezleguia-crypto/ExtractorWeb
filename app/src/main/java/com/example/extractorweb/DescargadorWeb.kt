package com.example.extractorweb

import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.nio.charset.Charset

data class PaginaDescargada(
    val urlFinal: String,
    val codigoHTTP: Int,
    val html: String
)

class DescargadorWeb {

    fun descargar(direccion: String): PaginaDescargada {
        val texto = direccion.trim()
        val urlNormalizada = if (
            texto.startsWith("https://", ignoreCase = true) ||
            texto.startsWith("http://", ignoreCase = true)
        ) {
            texto
        } else {
            "https://$texto"
        }

        val uri = URI(urlNormalizada).normalize()
        require(uri.scheme == "https" || uri.scheme == "http") {
            "Solo se permiten direcciones HTTP o HTTPS."
        }
        require(uri.host != null && uri.userInfo == null) {
            "La dirección web no es válida."
        }

        val conexion = URL(uri.toASCIIString()).openConnection()
            as HttpURLConnection

        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 10000
            conexion.readTimeout = 15000
            conexion.instanceFollowRedirects = true
            conexion.setRequestProperty(
                "User-Agent",
                "ExtractorWeb/1.0"
            )
            conexion.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml"
            )

            val codigo = conexion.responseCode
            val tipo = conexion.contentType.orEmpty()

            if (codigo !in 200..299) {
                throw Exception("El servidor respondió con HTTP $codigo.")
            }

            if (
                tipo.isNotBlank() &&
                !tipo.contains("text/html", true) &&
                !tipo.contains("application/xhtml+xml", true)
            ) {
                throw Exception(
                    "La dirección no devolvió HTML. Tipo recibido: $tipo"
                )
            }

            val charsetNombre = Regex(
                """charset\s*=\s*["']?([^;"'\s]+)""",
                RegexOption.IGNORE_CASE
            ).find(tipo)?.groupValues?.get(1)

            val charset = try {
                if (charsetNombre != null) {
                    Charset.forName(charsetNombre)
                } else {
                    Charsets.UTF_8
                }
            } catch (_: Exception) {
                Charsets.UTF_8
            }

            val html = conexion.inputStream.bufferedReader(charset).use {
                it.readText()
            }

            return PaginaDescargada(
                urlFinal = conexion.url.toString(),
                codigoHTTP = codigo,
                html = html
            )
        } finally {
            conexion.disconnect()
        }
    }
}
