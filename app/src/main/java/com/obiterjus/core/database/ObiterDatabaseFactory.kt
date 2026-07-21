package com.obiterjus.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ObiterDatabaseFactory {
    private const val DATABASE_NAME = "obiterjus.db"

    fun create(context: Context): ObiterDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            ObiterDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
            )
            .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN participantesJson TEXT")
            db.execSQL("ALTER TABLE processos ADD COLUMN assuntosJson TEXT")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN prazoQuantidade INTEGER")
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN prazoUnidade TEXT")
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN prazoDiasUteis INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN prazoTexto TEXT")
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN prazoDataLimite TEXT")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS participantes (
                    idLocal TEXT NOT NULL PRIMARY KEY,
                    numeroProcesso TEXT NOT NULL,
                    polo TEXT,
                    nome TEXT,
                    tipoPessoa TEXT,
                    tipoParticipacao TEXT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_participantes_numeroProcesso ON participantes(numeroProcesso)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    executadoEm INTEGER NOT NULL,
                    duracaoMs INTEGER,
                    fonte TEXT NOT NULL,
                    novasPublicacoes INTEGER NOT NULL,
                    processosSincronizados INTEGER NOT NULL,
                    sucesso INTEGER NOT NULL,
                    mensagemErro TEXT
                )
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS prazos_sugeridos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    publicacaoId INTEGER NOT NULL,
                    quantidade INTEGER NOT NULL,
                    unidade TEXT NOT NULL,
                    diasUteis INTEGER NOT NULL,
                    textoOriginal TEXT NOT NULL,
                    dataLimite TEXT,
                    isConfirmado INTEGER NOT NULL DEFAULT 0,
                    idExternoCalendario TEXT,
                    provedorCalendario TEXT,
                    FOREIGN KEY(publicacaoId) REFERENCES publicacoes(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_prazos_sugeridos_publicacaoId ON prazos_sugeridos(publicacaoId)")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN confiancaMatch TEXT NOT NULL DEFAULT 'ALTA'")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE processos ADD COLUMN dataJudTentativasRestantes INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN duplicataDe INTEGER")
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN totalDuplicatas INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_publicacoes_duplicataDe ON publicacoes(duplicataDe)")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE participantes ADD COLUMN cep TEXT")
            db.execSQL("ALTER TABLE participantes ADD COLUMN logradouro TEXT")
            db.execSQL("ALTER TABLE participantes ADD COLUMN numeroEndereco TEXT")
            db.execSQL("ALTER TABLE participantes ADD COLUMN telefone TEXT")
            db.execSQL("ALTER TABLE participantes ADD COLUMN email TEXT")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE publicacoes ADD COLUMN prazoConfianca TEXT")
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE processos ADD COLUMN natureza TEXT")
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE participantes ADD COLUMN ehCliente INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE prazos_sugeridos ADD COLUMN isCumprido INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS clientes (
                    id TEXT NOT NULL PRIMARY KEY,
                    tipoPessoa TEXT NOT NULL,
                    nome TEXT NOT NULL,
                    nomeNormalizado TEXT NOT NULL,
                    documento TEXT,
                    documentoNormalizado TEXT,
                    nacionalidade TEXT,
                    estadoCivil TEXT,
                    profissao TEXT,
                    cep TEXT,
                    logradouro TEXT,
                    numeroEndereco TEXT,
                    complemento TEXT,
                    bairro TEXT,
                    municipio TEXT,
                    uf TEXT,
                    telefone TEXT,
                    email TEXT,
                    observacoes TEXT,
                    rep_nome TEXT,
                    rep_documento TEXT,
                    rep_nacionalidade TEXT,
                    rep_estadoCivil TEXT,
                    rep_profissao TEXT,
                    rep_cargo TEXT,
                    criadoEm INTEGER NOT NULL,
                    atualizadoEm INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_clientes_documentoNormalizado ON clientes(documentoNormalizado)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_nomeNormalizado ON clientes(nomeNormalizado)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS clientes_processos (
                    clienteId TEXT NOT NULL,
                    numeroProcesso TEXT NOT NULL,
                    participanteIdLocal TEXT,
                    vinculadoEm INTEGER NOT NULL,
                    PRIMARY KEY(clienteId, numeroProcesso),
                    FOREIGN KEY(clienteId) REFERENCES clientes(id) ON DELETE CASCADE,
                    FOREIGN KEY(numeroProcesso) REFERENCES processos(numeroProcesso) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_processos_numeroProcesso ON clientes_processos(numeroProcesso)")
        }
    }
}
