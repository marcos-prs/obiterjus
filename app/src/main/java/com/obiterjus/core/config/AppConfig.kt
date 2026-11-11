package com.obiterjus.core.config

data class AppConfig(
    val djenBaseUrl: String = DEFAULT_DJEN_BASE_URL,
    val dataJudBaseUrl: String = DEFAULT_DATAJUD_BASE_URL,
    val dataJudApiKey: String = DEFAULT_DATAJUD_API_KEY,
    val djenDefaultItemsPerPage: Int = DEFAULT_DJEN_ITEMS_PER_PAGE,
    val syncLookbackDays: Long = DEFAULT_SYNC_LOOKBACK_DAYS,
    val djenToken: String = "",
    val dataJudEnabled: Boolean = true,
    val djenEnabled: Boolean = true,
    /**
     * Liga o registro no servidor de push. **Nasce desligado**: o app vai para
     * a loja antes do servidor existir, e registrar contra um endpoint
     * inexistente só geraria falha silenciosa a cada abertura. Vira `true` no
     * console do Remote Config quando o cron estiver no ar.
     */
    val pushDiarioEnabled: Boolean = false,
    /**
     * Expõe no Perfil o opt-in da pré-checagem no servidor (Fase 2 do push).
     *
     * Flag separado de [pushDiarioEnabled] de propósito: a Fase 1 pode entrar
     * em produção sozinha, enquanto a Fase 2 depende de a política de
     * privacidade registrar que um servidor nosso passa a guardar a OAB e o
     * nome do advogado e a consultar o DJEN por ele. Oferecer o toggle antes
     * disso seria pedir autorização para algo que a política não conta. Ver
     * `docs/tratamento-de-dados.md`.
     */
    val pushPreChecagemDisponivel: Boolean = false,
    val requestTimeoutSeconds: Long = DEFAULT_REQUEST_TIMEOUT_SECONDS,
    val source: AppConfigSource = AppConfigSource.FALLBACK,
) {
    companion object {
        const val DEFAULT_DJEN_BASE_URL = "https://comunicaapi.pje.jus.br/api/v1"
        const val DEFAULT_DATAJUD_BASE_URL = "https://api-publica.datajud.cnj.jus.br/"
        const val DEFAULT_DATAJUD_API_KEY = "cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw=="
        const val DEFAULT_DJEN_ITEMS_PER_PAGE = 100
        const val DEFAULT_SYNC_LOOKBACK_DAYS = 15L
        const val DEFAULT_REQUEST_TIMEOUT_SECONDS = 30L
    }
}

enum class AppConfigSource {
    REMOTE,
    CACHED,
    FALLBACK,
}
