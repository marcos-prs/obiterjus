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
}
