package com.obiterjus.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.obiterjus.MainActivity
import com.obiterjus.R

/**
 * Helper centralizado para emissão de notificações do ObiterJus.
 *
 * Canais disponíveis:
 * - [CHANNEL_PUBLICACOES]   → novas publicações genéricas
 * - [CHANNEL_PRAZO]         → publicação com prazo identificado
 * - [CHANNEL_SIGILOSA]      → publicação marcada como sigilosa
 * - [CHANNEL_PROCESSO_NOVO] → processo inédito descoberto via DJEN
 * - [CHANNEL_PRAZO_VENCENDO]→ prazo próximo do vencimento (PrazosWorker)
 */
class PublicacaoNotificationHelper(
    private val context: Context,
) {
    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    fun notificarNovasPublicacoes(quantidade: Int) {
        if (quantidade <= 0 || !hasPermission()) return
        ensureAllChannels()

        notify(
            channelId = CHANNEL_PUBLICACOES,
            notificationId = ID_NOVAS_PUBLICACOES,
            title = context.getString(R.string.notification_publicacoes_title),
            body = context.resources.getQuantityString(
                R.plurals.notification_publicacoes_body,
                quantidade,
                quantidade,
            ),
        )
    }

    @SuppressLint("MissingPermission")
    fun notificarPublicacaoComPrazo() {
        if (!hasPermission()) return
        ensureAllChannels()

        notify(
            channelId = CHANNEL_PRAZO,
            notificationId = ID_PUBLICACAO_COM_PRAZO,
            title = context.getString(R.string.notification_prazo_title),
            body = context.getString(R.string.notification_prazo_body),
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    @SuppressLint("MissingPermission")
    fun notificarPublicacaoSigilosa() {
        if (!hasPermission()) return
        ensureAllChannels()

        notify(
            channelId = CHANNEL_SIGILOSA,
            notificationId = ID_PUBLICACAO_SIGILOSA,
            title = context.getString(R.string.notification_sigilosa_title),
            body = context.getString(R.string.notification_sigilosa_body),
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    @SuppressLint("MissingPermission")
    fun notificarProcessosNovos(quantidade: Int) {
        if (quantidade <= 0 || !hasPermission()) return
        ensureAllChannels()

        notify(
            channelId = CHANNEL_PROCESSO_NOVO,
            notificationId = ID_PROCESSO_NOVO,
            title = context.resources.getQuantityString(
                R.plurals.notification_processo_novo_title,
                quantidade,
                quantidade,
            ),
            body = context.getString(R.string.notification_processo_novo_body),
        )
    }

    @SuppressLint("MissingPermission")
    fun notificarPrazosVencendo(quantidade: Int) {
        if (quantidade <= 0 || !hasPermission()) return
        ensureAllChannels()

        notify(
            channelId = CHANNEL_PRAZO_VENCENDO,
            notificationId = ID_PRAZO_VENCENDO,
            title = context.getString(R.string.notification_prazo_vencendo_title),
            body = context.resources.getQuantityString(
                R.plurals.notification_prazo_vencendo_body,
                quantidade,
                quantidade,
            ),
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun ensureAllChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        listOf(
            Triple(
                CHANNEL_PUBLICACOES,
                context.getString(R.string.notification_channel_publicacoes),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            Triple(
                CHANNEL_PRAZO,
                context.getString(R.string.notification_channel_prazo),
                NotificationManager.IMPORTANCE_HIGH,
            ),
            Triple(
                CHANNEL_SIGILOSA,
                context.getString(R.string.notification_channel_sigilosa),
                NotificationManager.IMPORTANCE_HIGH,
            ),
            Triple(
                CHANNEL_PROCESSO_NOVO,
                context.getString(R.string.notification_channel_processo_novo),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            Triple(
                CHANNEL_PRAZO_VENCENDO,
                context.getString(R.string.notification_channel_prazo_vencendo),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        ).forEach { (id, name, importance) ->
            manager.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_MAIN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun notify(
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(buildPendingIntent())
            .setAutoCancel(true)
            .setPriority(priority)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private companion object {
        const val CHANNEL_PUBLICACOES = "publicacoes"
        const val CHANNEL_PRAZO = "prazo_identificado"
        const val CHANNEL_SIGILOSA = "publicacao_sigilosa"
        const val CHANNEL_PROCESSO_NOVO = "processo_novo"
        const val CHANNEL_PRAZO_VENCENDO = "prazo_vencendo"

        const val REQUEST_CODE_MAIN = 101

        const val ID_NOVAS_PUBLICACOES = 20260430
        const val ID_PUBLICACAO_COM_PRAZO = 20260431
        const val ID_PUBLICACAO_SIGILOSA = 20260432
        const val ID_PROCESSO_NOVO = 20260433
        const val ID_PRAZO_VENCENDO = 20260434
    }
}
