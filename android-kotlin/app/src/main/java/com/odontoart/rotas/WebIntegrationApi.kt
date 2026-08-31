package com.odontoart.rotas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Gateways client-side equivalentes aos usados pelo app web.
 *
 * Nenhuma service-role key ou segredo de cron/sincronizacao e lido aqui. O APK usa
 * somente a anon key publica e o JWT do usuario para acessar Edge Functions protegidas.
 */
class WebIntegrationApi(
    supabaseUrlRaw: String = BuildConfig.SUPABASE_URL,
    supabaseAnonKeyRaw: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val supabaseUrl = supabaseUrlRaw.trim().trimEnd('/')
    private val supabaseAnonKey = supabaseAnonKeyRaw.trim()

    suspend fun fetchOdontoartEmpresa(session: UserSession, empresaId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val codigo = empresaId.trim()
            require(codigo.isNotBlank()) { "Informe o codigo da empresa." }
            val payload = invokeFunctionBlocking(
                session = session,
                functionName = "odontoart-empresa-proxy",
                body = JSONObject().put("empresaId", codigo),
            )
            extractFirstCompany(payload)
        }

    suspend fun invokeErpSync(
        session: UserSession,
        action: String,
        payload: JSONObject,
    ): Any? = withContext(Dispatchers.IO) {
        invokeFunctionBlocking(
            session = session,
            functionName = "erp-sync-manual",
            body = JSONObject()
                .put("action", action)
                .put("payload", payload),
        )
    }

    suspend fun invokeManageUsers(
        session: UserSession,
        action: String,
        payload: JSONObject,
    ): Any? = withContext(Dispatchers.IO) {
        invokeFunctionBlocking(
            session = session,
            functionName = "manage-users",
            body = JSONObject()
                .put("action", action)
                .put("payload", payload),
        )
    }

    suspend fun fetchCep(cepRaw: String): CepLookup = withContext(Dispatchers.IO) {
        val cep = cepRaw.filter(Char::isDigit).take(8)
        require(cep.length == 8) { "Informe um CEP valido com 8 digitos." }

        // Clientes.tsx usa ViaCEP diretamente. Mantemos o mesmo contrato no Android.
        val request = Request.Builder()
            .url("https://viacep.com.br/ws/$cep/json/")
            .header("Accept", "application/json")
            .get()
            .build()
        val json = JSONObject(execute(request))
        if (json.optBoolean("erro", false)) error("CEP nao encontrado.")
        CepLookup(
            cep = json.stringOrNull("cep"),
            endereco = json.stringOrNull("logradouro"),
            complemento = json.stringOrNull("complemento"),
            bairro = json.stringOrNull("bairro"),
            cidade = json.stringOrNull("localidade"),
            uf = json.stringOrNull("uf"),
        )
    }

    suspend fun fetchCnpj(cnpjRaw: String): CnpjLookup = withContext(Dispatchers.IO) {
        val digits = cnpjRaw.filter(Char::isDigit).take(14)
        require(isValidCnpj(digits)) { "CNPJ invalido." }
        val request = Request.Builder()
            .url("https://publica.cnpj.ws/cnpj/$digits")
            .header("Accept", "application/json")
            .get()
            .build()
        val json = JSONObject(execute(request))
        val estabelecimento = json.optJSONObject("estabelecimento")
        val estado = estabelecimento?.opt("estado")
        val cidade = estabelecimento?.opt("cidade")
        CnpjLookup(
            razaoSocial = json.stringOrNull("razao_social"),
            logradouro = estabelecimento?.stringOrNull("logradouro"),
            numero = estabelecimento?.stringOrNull("numero"),
            bairro = estabelecimento?.stringOrNull("bairro"),
            cep = estabelecimento?.stringOrNull("cep"),
            uf = when (estado) {
                is JSONObject -> estado.stringOrNull("sigla")
                is String -> estado.trim().ifBlank { null }
                else -> null
            },
            cidade = when (cidade) {
                is JSONObject -> cidade.stringOrNull("nome")
                is String -> cidade.trim().ifBlank { null }
                else -> null
            },
        )
    }

    private fun invokeFunctionBlocking(
        session: UserSession,
        functionName: String,
        body: JSONObject,
    ): Any? {
        ensureConfigured()
        val request = Request.Builder()
            .url("$supabaseUrl/functions/v1/$functionName")
            .header("apikey", supabaseAnonKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val raw = execute(request)
        if (raw.isBlank()) return null
        return JSONTokener(raw).nextValue()
    }

    private fun extractFirstCompany(payload: Any?): JSONObject? {
        return when (payload) {
            is JSONObject -> {
                val dados = payload.optJSONArray("dados")
                when {
                    dados != null && dados.length() > 0 -> dados.optJSONObject(0)
                    payload.has("Id") || payload.has("Cnpj") || payload.has("CNPJ") -> payload
                    else -> null
                }
            }
            is JSONArray -> if (payload.length() > 0) payload.optJSONObject(0) else null
            else -> null
        }
    }

    private fun execute(request: Request): String {
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = extractServerError(body)
                when (response.code) {
                    401 -> error("Sua sessao foi encerrada. Faca login novamente.")
                    404 -> error(message.ifBlank { "Recurso nao encontrado." })
                    429 -> error("Limite de consultas atingido. Aguarde alguns segundos.")
                    else -> error(message.ifBlank { "Falha na integracao (${response.code})." })
                }
            }
            return body
        }
    }

    private fun extractServerError(body: String): String {
        if (body.isBlank()) return ""
        return runCatching {
            val json = JSONObject(body)
            json.stringOrNull("error")
                ?: json.stringOrNull("message")
                ?: json.stringOrNull("detail")
                ?: body
        }.getOrDefault(body)
    }

    private fun ensureConfigured() {
        check(supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) {
            "Configuracao do Supabase ausente no aplicativo."
        }
    }

    private fun isValidCnpj(digits: String): Boolean {
        if (!Regex("\\d{14}").matches(digits)) return false
        if (digits.all { it == digits.first() }) return false
        fun digit(base: String, weights: IntArray): Int {
            val sum = weights.indices.sumOf { index -> base[index].digitToInt() * weights[index] }
            val remainder = sum % 11
            return if (remainder < 2) 0 else 11 - remainder
        }
        val first = digit(digits.take(12), intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
        if (first != digits[12].digitToInt()) return false
        val second = digit(digits.take(13), intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
        return second == digits[13].digitToInt()
    }

    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().ifBlank { null }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class CepLookup(
    val cep: String?,
    val endereco: String?,
    val complemento: String?,
    val bairro: String?,
    val cidade: String?,
    val uf: String?,
)

data class CnpjLookup(
    val razaoSocial: String?,
    val logradouro: String?,
    val numero: String?,
    val bairro: String?,
    val cep: String?,
    val uf: String?,
    val cidade: String?,
)
