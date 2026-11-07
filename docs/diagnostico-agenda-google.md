# Agenda do Google — diagnóstico (06/11/2026)

## O que estava errado

### 1. O `google-services.json` do repositório não tinha nenhum cliente OAuth Android

O arquivo versionado trazia **só o cliente `client_type: 3`** (web, usado pelo login com
Google). Nenhum `client_type: 1`. Ou seja: o arquivo foi baixado antes de qualquer
impressão digital SHA ser registrada no projeto Firebase e nunca mais foi atualizado.

### 2. O certificado de release nunca foi registrado

No projeto `obiterjus` só estavam registradas as impressões do **keystore de debug**:

```
SHA-1   0D:DD:60:BA:C0:B8:41:A2:F8:E3:41:0E:E8:06:79:06:B4:0C:99:C2
SHA-256 91:A9:A2:B1:97:E1:A8:4A:BE:83:A9:67:FC:57:D1:C8:11:E4:7D:60:6B:5E:97:C3:A7:4A:3B:3A:3A:68:67:B1
```

A chave de upload (`Build aab`, alias `key0`) não estava lá.

Isso importa porque `Identity.getAuthorizationClient().authorize()` **não lê o
`google-services.json`**: o Google resolve o pedido no servidor pelo par
*nome do pacote + certificado que assinou o APK em execução*. Sem um cliente OAuth
Android correspondente, a chamada falha com `ApiException` código 10 (`DEVELOPER_ERROR`)
— que era exatamente o que a tela engolia.

### 3. Qualquer falha virava a mesma frase

Todos os caminhos de erro terminavam em `snackbarHostState.showSnackbar(feedbackErro)` —
"Não foi possível confirmar o prazo agora." Erro de configuração do Google Cloud, falta de
rede e cancelamento do usuário ficavam indistinguíveis.

### 4. Um 403 apagava um token válido

`CalendarSyncRepositoryImpl` limpava o access token em **qualquer** 401 ou 403. O 403 mais
provável aqui é `accessNotConfigured` — a Calendar API não habilitada no projeto —, que não
tem nada a ver com o token. O efeito era um laço: autoriza, toma 403, perde o token,
autoriza de novo, para sempre, sem nunca dizer o motivo.

## O que foi corrigido nesta sessão

- Registradas no Firebase as impressões SHA-1 e SHA-256 da chave de upload. O projeto passou
  a ter dois clientes OAuth Android (debug e upload).
- `app/google-services.json` baixado de novo. Passou de 1 para 3 clientes OAuth.
- `GoogleCalendarAuthorizationRepository` agora traduz o `ApiException` em mensagem
  acionável, registra em log (`CalendarAuth`) e devolve `FalhaAutorizacaoCalendar` com o
  código numérico.
- `CalendarSyncRepositoryImpl` só descarta o token quando o problema é mesmo de credencial
  (401, `authError`, `insufficientPermissions`); para `accessNotConfigured` diz que a API
  não está habilitada.
- As duas telas que autorizam a agenda (`PrazosScreen` e `DetalhePublicacaoScreen`) passaram
  a mostrar a mensagem real em vez da frase genérica.

## O que falta — exige o Google Cloud Console

Com a conta dona do projeto `obiterjus` (número 529769973008):

1. **Habilitar a Google Calendar API**
   <https://console.cloud.google.com/apis/library/calendar-json.googleapis.com?project=obiterjus>
   Não foi possível confirmar desta sessão se já está habilitada: a API recusa chave de API,
   então a sondagem não distingue "desabilitada" de "sem credencial".

2. **Tela de consentimento OAuth**
   <https://console.cloud.google.com/auth/overview?project=obiterjus>
   - O escopo `https://www.googleapis.com/auth/calendar` é **sensível**.
   - Com o app em `Testing`, só contas na lista de usuários de teste conseguem autorizar, e
     no máximo 100. Adicione ali as contas dos testadores do teste fechado.
   - Para sair de `Testing` o Google exige verificação do app, com vídeo de demonstração e
     página de política acessível — mais um motivo para publicar `/privacidade` antes.

3. **Depois do primeiro envio à Play**: copiar o SHA-1 da *chave de assinatura do app* em
   Play Console → **Testar e lançar** → **Configuração** → **Assinatura de apps** (em
   versões anteriores da interface a página aparece como *Integridade do app*; a busca
   do próprio console por "assinatura" leva direto). A página mostra **dois**
   certificados: pegar o da **chave de assinatura do app**, não o da chave de upload —
   este último começa com `00:2A:3B:2A:13:F1`. Registrar no Firebase:

   ```
   firebase apps:android:sha:create 1:529769973008:android:cc6f1248e5790b76381195 <SHA1-DA-PLAY> --project obiterjus
   ```

   Sem isso a agenda funciona no build local e falha no build distribuído pela Play, porque
   o app instalado é assinado pelo Google, com outro certificado.

## Como confirmar no aparelho

Com um build novo instalado, ao tocar em confirmar prazo com o Google:

```
adb logcat -s CalendarAuth:W CalendarSync:W
```

A mensagem agora nomeia a causa. Em particular, `código 10` significa que ainda falta
cliente OAuth Android para a assinatura daquele build.
