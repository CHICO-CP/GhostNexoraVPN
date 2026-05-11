package com.ghostnexora.vpn.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.ghostnexora.vpn.data.model.ProxyConfig
import com.ghostnexora.vpn.data.model.VpnProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ══════════════════════════════════════════════════════════════════════════
// GHOST NEXORA VPN — GESTOR DE JSON
// Importación y exportación de perfiles en formato JSON estructurado
// ══════════════════════════════════════════════════════════════════════════

@Singleton
class JsonManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    // ══════════════════════════════════════════════════════════════════════
    // IMPORTACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Importa perfiles desde un URI (seleccionado con SAF / file picker).
     * @return [ImportResult] con los perfiles parseados o el error.
     */
    fun importFromUri(uri: Uri): ImportResult {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("No se pudo abrir el archivo")

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            parseJson(jsonString)
        } catch (e: Exception) {
            ImportResult.Error("Error al leer el archivo: ${e.message}")
        }
    }

    /**
     * Importa perfiles desde un String JSON directamente.
     */
    fun importFromString(jsonString: String): ImportResult =
        parseJson(jsonString)

    /**
     * Parsea el JSON y devuelve la lista de perfiles o un error detallado.
     */
    private fun parseJson(jsonString: String): ImportResult {
        if (jsonString.isBlank()) {
            return ImportResult.Error("El archivo está vacío")
        }

        return try {
            // Intentar parsear como documento completo (formato Ghost Nexora)
            val document = gson.fromJson(jsonString, VpnProfileDocument::class.java)

            if (document?.profiles.isNullOrEmpty()) {
                // Intentar parsear como array simple de perfiles
                val profiles = gson.fromJson(jsonString, Array<VpnProfileJson>::class.java)
                    ?.toList()
                    ?: return ImportResult.Error("Formato JSON no reconocido")

                val mapped = profiles.mapNotNull { it.toVpnProfile() }
                if (mapped.isEmpty()) ImportResult.Error("No se encontraron perfiles válidos")
                else ImportResult.Success(mapped, document?.appName ?: "Importación externa")
            } else {
                val mapped = document.profiles!!.mapNotNull { it.toVpnProfile() }
                ImportResult.Success(mapped, document.appName ?: "Ghost Nexora VPN")
            }
        } catch (e: JsonSyntaxException) {
            ImportResult.Error("JSON malformado: ${e.message?.take(80)}")
        } catch (e: Exception) {
            ImportResult.Error("Error inesperado: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // EXPORTACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Exporta una lista de perfiles a un archivo JSON en el directorio
     * de archivos internos de la app, listo para compartir con FileProvider.
     * @return El [File] generado o null si hubo error.
     */
    fun exportToFile(profiles: List<VpnProfile>): File? {
        return try {
            val document = VpnProfileDocument(
                appName  = "Ghost Nexora VPN",
                version  = "1.0.0",
                exportedAt = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.getDefault()
                ).format(Date()),
                profiles = profiles.map { it.toJson() }
            )

            val jsonString = gson.toJson(document)
            val fileName   = "ghost_nexora_export_${System.currentTimeMillis()}.json"
            val outputDir  = File(context.filesDir, "exports").apply { mkdirs() }
            val outputFile = File(outputDir, fileName)

            outputFile.writeText(jsonString)
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convierte los perfiles a JSON String sin crear archivo.
     * Útil para compartir directamente o para previsualización.
     */
    fun exportToString(profiles: List<VpnProfile>): String {
        val document = VpnProfileDocument(
            appName    = "Ghost Nexora VPN",
            version    = "1.0.0",
            exportedAt = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.getDefault()
            ).format(Date()),
            profiles   = profiles.map { it.toJson() }
        )
        return gson.toJson(document)
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si un JSON tiene el formato correcto sin importar.
     * Útil para la previsualización en la pantalla de importación.
     */
    fun validateJson(jsonString: String): ValidationResult {
        if (jsonString.isBlank()) return ValidationResult(false, "Archivo vacío", 0)

        return try {
            val doc = gson.fromJson(jsonString, VpnProfileDocument::class.java)
            val count = doc?.profiles?.size ?: 0
            if (count > 0) {
                ValidationResult(true, "Formato válido", count)
            } else {
                // Intentar como array plano
                val arr = gson.fromJson(jsonString, Array<VpnProfileJson>::class.java)
                val arrCount = arr?.size ?: 0
                if (arrCount > 0) ValidationResult(true, "Formato array válido", arrCount)
                else ValidationResult(false, "No se encontraron perfiles", 0)
            }
        } catch (e: JsonSyntaxException) {
            ValidationResult(false, "JSON malformado", 0)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// MODELOS DE TRANSFERENCIA JSON
// DTOs separados del modelo de Room para mayor flexibilidad
// ══════════════════════════════════════════════════════════════════════════

/** Documento completo de exportación Ghost Nexora */
data class VpnProfileDocument(
    val appName: String?    = "Ghost Nexora VPN",
    val version: String?    = "1.0.0",
    val exportedAt: String? = null,
    val profiles: List<VpnProfileJson>? = null
)

/** DTO de perfil para serialización JSON */
data class VpnProfileJson(
    val id: String?         = null,
    val name: String?       = null,
    val host: String?       = null,
    val port: Int?          = 443,
    val username: String?   = "",
    val password: String?   = "",
    val method: String?     = "ssh",
    val sslEnabled: Boolean? = true,
    val sni: String?        = "",
    val proxy: ProxyJson?   = null,
    val tags: List<String>? = emptyList(),
    val notes: String?      = "",
    val enabled: Boolean?   = true,
    val lastUsed: String?   = ""
) {
    /** Convierte el DTO al modelo de Room, con validación básica */
    fun toVpnProfile(): VpnProfile? {
        val resolvedHost = host?.trim() ?: return null
        if (resolvedHost.isEmpty()) return null

        return VpnProfile(
            id         = if (id.isNullOrBlank()) UUID.randomUUID().toString() else id,
            name       = name?.trim()?.ifEmpty { resolvedHost } ?: resolvedHost,
            host       = resolvedHost,
            port       = port?.takeIf { it in 1..65535 } ?: 443,
            username   = username ?: "",
            password   = password ?: "",
            method     = method ?: "ssh",
            sslEnabled = sslEnabled ?: true,
            sni        = sni ?: "",
            proxy      = ProxyConfig(
                host = proxy?.host ?: "",
                port = proxy?.port ?: 0,
                type = proxy?.type ?: ""
            ),
            tagsRaw    = tags?.joinToString(",") ?: "",
            notes      = notes ?: "",
            enabled    = enabled ?: true,
            lastUsed   = lastUsed ?: "",
            createdAt  = System.currentTimeMillis()
        )
    }
}

data class ProxyJson(
    val host: String? = "",
    val port: Int?    = 0,
    val type: String? = ""
)

/** Extensión: convierte VpnProfile → VpnProfileJson para exportar */
fun VpnProfile.toJson() = VpnProfileJson(
    id         = id,
    name       = name,
    host       = host,
    port       = port,
    username   = username,
    password   = password,
    method     = method,
    sslEnabled = sslEnabled,
    sni        = sni,
    proxy      = ProxyJson(
        host = proxy.host,
        port = proxy.port,
        type = proxy.type
    ),
    tags       = tags,
    notes      = notes,
    enabled    = enabled,
    lastUsed   = lastUsed
)

// ══════════════════════════════════════════════════════════════════════════
// RESULTADOS
// ══════════════════════════════════════════════════════════════════════════

sealed class ImportResult {
    data class Success(
        val profiles: List<VpnProfile>,
        val sourceName: String = ""
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String,
    val profileCount: Int
)
