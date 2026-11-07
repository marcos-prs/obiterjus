# Justificativa do escopo OAuth — Google Agenda

Texto para o campo de justificativa de escopo sensível na Google Auth Platform
(console.cloud.google.com/auth, projeto `obiterjus`) e para a submissão de verificação.

Escopo pedido pelo build atual: `https://www.googleapis.com/auth/calendar`
(constante `GOOGLE_CALENDAR_SCOPE` em `GoogleCalendarAuthorizationRepository.kt`).

---

## O que o aplicativo realmente faz

Levantado do código, não da intenção — `CalendarDataSources.kt` expõe duas chamadas e
nada mais:

| Operação | Endpoint |
| --- | --- |
| Criar evento | `POST /calendar/v3/calendars/primary/events` |
| Excluir evento | `DELETE /calendar/v3/calendars/primary/events/{eventId}` |

Não há leitura, listagem, acesso a agendas secundárias, alteração de configurações nem
uso de `calendarList`. O corpo do evento criado tem quatro campos: `summary`,
`description`, `start` e `end`.

---

## Justificativa (português)

> O ObiterJus é um aplicativo de apoio ao trabalho do advogado. Ele monitora as
> publicações oficiais do Diário de Justiça Eletrônico Nacional (DJEN) e do DataJud,
> bases públicas do Conselho Nacional de Justiça, e calcula as datas-limite processuais
> decorrentes dessas publicações.
>
> O escopo do Google Agenda é usado para uma única finalidade. Quando o usuário toca em
> "confirmar prazo", o aplicativo cria na agenda principal dele um evento com a
> data-limite calculada, contendo o número do processo e a descrição da intimação.
> Quando o usuário desfaz a confirmação ou exclui o prazo dentro do aplicativo, o evento
> correspondente é removido pelo identificador que o aplicativo guardou localmente.
>
> São exatamente duas operações, ambas disparadas por ação explícita do usuário:
> criação de evento e exclusão de evento, sempre na agenda principal da conta
> autorizada.
>
> O aplicativo não lê a agenda do usuário, não lista eventos, não acessa agendas de
> terceiros, não altera configurações de agenda e não transmite esses dados a terceiros.
> Os identificadores dos eventos criados ficam apenas no dispositivo, para permitir a
> exclusão posterior.
>
> A funcionalidade é essencial ao propósito do aplicativo: a perda de um prazo
> processual é a falha de maior consequência na prática da advocacia, e levar a
> data-limite para a agenda que o advogado já usa é o que torna o monitoramento útil no
> dia a dia.

---

## Justification (English)

Submeter em inglês tende a ser mais rápido na revisão.

> ObiterJus is a productivity app for Brazilian lawyers. It monitors official court
> publications from DJEN and DataJud — public databases maintained by the Brazilian
> National Council of Justice — and computes the resulting procedural filing deadlines.
>
> The Google Calendar scope serves a single purpose. When the user taps "confirm
> deadline", the app creates an event on their primary calendar carrying the computed
> due date, the case number and the description of the court notice. When the user
> undoes that confirmation or deletes the deadline inside the app, the app deletes the
> corresponding event using the event id it stored locally.
>
> There are exactly two operations, both triggered by an explicit user action: insert an
> event and delete an event, always on the primary calendar of the authorized account.
>
> The app does not read the user's calendar, does not list events, does not access other
> people's calendars, does not change calendar settings and does not transmit this data
> to any third party. Event ids are kept on the device only, so that the app can delete
> the events it created.
>
> This feature is essential to the app's purpose: missing a procedural deadline is the
> most consequential failure in legal practice, and writing the due date into the
> calendar the lawyer already uses is what makes the monitoring useful day to day.

---

## Antes da verificação: estreitar o escopo

`.../auth/calendar` concede leitura e escrita em **todas** as agendas da conta. O
aplicativo usa duas operações na agenda principal. A regra que o Google aplica na
revisão é pedir o mínimo necessário, então esse escopo é maior do que a função exige e
é o tipo de desencontro que reprova verificação.

O escopo que corresponde ao que o código faz é
**`https://www.googleapis.com/auth/calendar.events.owned`** — "ver, criar, alterar e
excluir eventos em agendas que você possui". A agenda principal é do próprio usuário, e
a referência de `Events: insert` lista esse escopo entre os que autorizam a chamada.

`calendar.app.created` é ainda mais restrito, mas **não serve sem reescrever o código**:
ele dá acesso apenas a agendas secundárias criadas pelo próprio aplicativo, e as duas
chamadas hoje apontam para `primary`.

### Sequência recomendada

1. **Agora, para o teste fechado:** manter `.../auth/calendar` e usar a justificativa
   acima. Trocar o escopo exige build novo, e o AAB já enviado pede esse escopo.
2. **Antes de sair de `Testing`:** trocar a constante por `calendar.events.owned`,
   subir build novo e então pedir verificação. A justificativa fica mais forte porque o
   escopo passa a bater exatamente com as duas chamadas.

A troca é de uma linha:

```kotlin
// GoogleCalendarAuthorizationRepository.kt
const val GOOGLE_CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events.owned"
```

Atenção ao trocar: quem já autorizou com o escopo antigo precisa autorizar de novo, e o
token guardado deve ser descartado na atualização, senão o app segue usando a concessão
anterior até ela expirar.

---

## Fontes

- [Choose Google Calendar API scopes](https://developers.google.com/workspace/calendar/api/auth)
- [OAuth 2.0 Scopes for Google APIs](https://developers.google.com/identity/protocols/oauth2/scopes)
- [Events: insert](https://developers.google.com/workspace/calendar/api/v3/reference/events/insert)
- [Sensitive scope verification](https://developers.google.com/identity/protocols/oauth2/production-readiness/sensitive-scope-verification)
