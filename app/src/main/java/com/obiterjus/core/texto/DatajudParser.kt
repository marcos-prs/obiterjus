package com.obiterjus.core.texto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DatajudParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parsearDescricao(raw: String?): String? {
        if (raw == null) return null
        if (!raw.trim().startsWith("[")) return raw

        return try {
            val root = json.parseToJsonElement(raw).jsonArray
            root.mapNotNull { 
                it.jsonObject["nome"]?.jsonPrimitive?.content 
            }.joinToString(", ").ifBlank { raw }
        } catch (e: Exception) {
            raw
        }
    }
}
