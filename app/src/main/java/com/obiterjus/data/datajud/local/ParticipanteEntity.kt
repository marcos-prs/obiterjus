package com.obiterjus.data.datajud.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "participantes",
    indices = [
        Index("numeroProcesso"),
    ],
)
data class ParticipanteEntity(
    @PrimaryKey
    val idLocal: String,
    val numeroProcesso: String,
    val polo: String?,
    val nome: String?,
    val tipoPessoa: String?,
    val tipoParticipacao: String?,
    // Qualificação completa
    val cpfCnpj: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val endereco: String? = null,
    val contatos: String? = null,
    // Endereço estruturado
    val cep: String? = null,
    val logradouro: String? = null,
    val numeroEndereco: String? = null,
    // Contatos estruturados
    val telefone: String? = null,
    val email: String? = null,
)
