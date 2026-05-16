package com.obiterjus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.obiterjus.data.datajud.local.MovimentoDao
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.processo.local.ProcessoDao
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.data.publicacao.local.PublicacaoEntity

import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.data.agenda.local.PrazoSugeridoDao

@Database(
    entities = [
        PublicacaoEntity::class,
        ProcessoEntity::class,
        MovimentoEntity::class,
        ParticipanteEntity::class,
        SyncLogEntity::class,
        PrazoSugeridoEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
@TypeConverters(CnjTypeConverters::class)
abstract class ObiterDatabase : RoomDatabase() {
    abstract fun publicacaoDao(): PublicacaoDao
    abstract fun processoDao(): ProcessoDao
    abstract fun movimentoDao(): MovimentoDao
    abstract fun participanteDao(): ParticipanteDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun prazoSugeridoDao(): PrazoSugeridoDao
}
