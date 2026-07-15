package com.obiterjus.presentation.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Catálogo centralizado de ícones do ObiterJus.
 *
 * Todos os ícones usados na UI devem ser referenciados por aqui.
 * Facilita auditoria visual, troca de iconografia e consistência.
 */
object ObiterIcones {

    // ── Navegação (filled / outlined) ────────────────────────
    val InicioAtivo: ImageVector get() = Icons.Default.GridView
    val InicioInativo: ImageVector get() = Icons.Outlined.GridView
    val PublicacoesAtivo: ImageVector get() = Icons.Default.Description
    val PublicacoesInativo: ImageVector get() = Icons.Outlined.Description
    val PrazosAtivo: ImageVector get() = Icons.Default.Schedule
    val PrazosInativo: ImageVector get() = Icons.Outlined.Schedule
    val ProcessosAtivo: ImageVector get() = Icons.AutoMirrored.Filled.FormatListBulleted
    val ProcessosInativo: ImageVector get() = Icons.AutoMirrored.Outlined.FormatListBulleted
    val PerfilAtivo: ImageVector get() = Icons.Default.Person
    val PerfilInativo: ImageVector get() = Icons.Outlined.Person
    val Voltar: ImageVector get() = Icons.AutoMirrored.Rounded.ArrowBack

    // ── Ações ────────────────────────────────────────────────
    val Buscar: ImageVector get() = Icons.Default.Search
    val Fechar: ImageVector get() = Icons.Default.Close
    val Limpar: ImageVector get() = Icons.Default.Clear
    val Compartilhar: ImageVector get() = Icons.Default.Share
    val Filtrar: ImageVector get() = Icons.Default.FilterList
    val Sincronizar: ImageVector get() = Icons.Default.Sync
    val Confirmar: ImageVector get() = Icons.Default.Check
    val ExpandirAbaixo: ImageVector get() = Icons.Default.KeyboardArrowDown
    val Adicionar: ImageVector get() = Icons.Default.Add
    val Editar: ImageVector get() = Icons.Default.Edit
    val Excluir: ImageVector get() = Icons.Default.Delete
    val MaisOpcoes: ImageVector get() = Icons.Default.MoreVert

    // ── Status ───────────────────────────────────────────────
    val Sucesso: ImageVector get() = Icons.Default.CheckCircle
    val Aviso: ImageVector get() = Icons.Default.Warning
    val Erro: ImageVector get() = Icons.Default.Error
    val Info: ImageVector get() = Icons.Default.Info
    val Favorito: ImageVector get() = Icons.Default.Star
    val Sigilo: ImageVector get() = Icons.Default.Lock

    // ── Contextuais ──────────────────────────────────────────
    val Documento: ImageVector get() = Icons.Default.Description
    val Artigo: ImageVector get() = Icons.AutoMirrored.Filled.Article
    val Fonte: ImageVector get() = Icons.Default.Source
    val Evento: ImageVector get() = Icons.Default.Event
    val Agenda: ImageVector get() = Icons.Default.Event
    val Historico: ImageVector get() = Icons.Default.History
    val Notificacao: ImageVector get() = Icons.Default.Notifications
    val Conta: ImageVector get() = Icons.Default.AccountCircle
    val Login: ImageVector get() = Icons.AutoMirrored.Filled.Login
    val Pasta: ImageVector get() = Icons.Default.Folder
    val Email: ImageVector get() = Icons.Default.Email
    val Visibilidade: ImageVector get() = Icons.Default.Visibility
    val VisibilidadeOculta: ImageVector get() = Icons.Default.VisibilityOff
    val Seguranca: ImageVector get() = Icons.Default.Security

    // ── UI Helpers ───────────────────────────────────────────
    val Expandir: ImageVector get() = Icons.Default.Add
    val Recolher: ImageVector get() = Icons.Default.Close
    val MoverCima: ImageVector get() = Icons.Default.KeyboardArrowUp
    val MoverBaixo: ImageVector get() = Icons.Default.KeyboardArrowDown
    val Anterior: ImageVector get() = Icons.AutoMirrored.Filled.KeyboardArrowLeft
    val Proximo: ImageVector get() = Icons.AutoMirrored.Filled.KeyboardArrowRight

    // ── Sincronização na nuvem ───────────────────────────────
    val NuvemSincronizando: ImageVector get() = Icons.Default.CloudSync
    val NuvemConcluida: ImageVector get() = Icons.Default.CloudDone
}
