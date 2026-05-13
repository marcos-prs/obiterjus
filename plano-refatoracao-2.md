# Plano de Refatoração — TelaDetalheProcesso
> DI: Koin

---

## Bloco 1 — Parsing de descrições da timeline

**Arquivo:** `core/texto/DatajudParser.kt` (novo) + mapper de domínio

**Problema:** `item.descricao` chega como JSON cru do DataJud, ex:
`[{"codigo":4,"valor":107,"nome":"Certidão","descricao":"tipo_de_documento"}]`

**Plano:**
1. Criar objeto ou classe `DatajudParser` em `core/texto/DatajudParser.kt`:
   ```kotlin
   object DatajudParser {
       fun parsearDescricao(raw: String?): String? { ... }
   }
   ```
2. Tentar deserializar como array JSON; se bem-sucedido, mapear campo `"nome"` de cada elemento e juntar com `", "`
3. Se falhar o parse (texto livre legível), retornar o próprio texto
4. Como é um `object` utilitário puro (sem dependências externas), **não precisa ser registrado no módulo Koin**
5. Aplicar no mapper de domínio que converte a resposta da API em `TimelineProcessoItem` — **nunca na UI nem no ViewModel**

---

## Bloco 2 — Cores dos pontos da timeline

**Arquivo:** mapper de domínio + `CorPontoTimeline.kt`

**Problema:** A lógica atual só distingue `isSigiloso`, `isImportante` e `MOVIMENTO_DATAJUD`, resultando em cor uniforme.

**Plano:**
1. Adicionar ao enum `CorPontoTimeline` (se necessário): `DANGER`, `WARNING`, `PRIMARY`, `SUCCESS`, `ACCENT`, `MUTED`
2. Criar mapeamento por palavras-chave do `titulo` no mapper de domínio:

| Palavras-chave | Cor |
|---|---|
| `"liminar"`, `"tutela"`, `"antecipada"` | `DANGER` — vermelho |
| `"audiência"` | `WARNING` — dourado |
| `"contestação"`, `"manifestação"` | `PRIMARY` — azul escuro |
| `"citação"`, `"intimação"`, `"despacho"` | `MUTED` — cinza |
| `"petição inicial"`, `"distribuída"` | `SUCCESS` — verde escuro |
| outros movimentos DataJud | `ACCENT` — laranja |

3. Expor `corPonto: CorPontoTimeline` já resolvida no `TimelineProcessoItem`
4. O mapper onde isso é aplicado já deve estar registrado no módulo Koin correspondente (ex: `processoModule`); nenhuma alteração de registro é necessária, apenas lógica interna do mapper

---

## Bloco 3 — Layout do ItemTimeline (ponto sozinho na coluna esquerda)

**Arquivo:** `presentation/componentes/timeline/ItemTimeline.kt`

**Problema:** O ponto e a linha vertical ficam desalinhados ou sozinhos sem conteúdo visível ao lado.

**Plano:**
1. Revisar o layout de `ItemTimeline`: usar `Row` com o ponto + linha em uma `Column` fixa à esquerda (largura fixa, ex `24.dp`) e o conteúdo textual em `Column` com `Modifier.weight(1f)` à direita
2. Garantir que o ponto esteja alinhado ao topo da primeira linha de texto (`Alignment.Top`)
3. A linha vertical deve crescer até o próximo item — verificar se `mostrarLinha = true` está correto para todos exceto o último item
4. Composable puro, sem dependências injetadas — **nenhuma alteração em módulos Koin**

---

## Bloco 4 — Formato de data e layout de cada item da timeline

**Arquivo:** `core/time/FormatadorData.kt` + `ConteudoTimeline` + `ItemTimeline.kt`

**Problema:** Data truncada e descrição sem hierarquia visual clara.

**Plano:**
1. Adicionar função `formatarDataPorExtenso(data: LocalDateTime): String` em `FormatadorData` → ex: `"29 abr 2026"` (locale pt-BR, mês minúsculo)
2. `FormatadorData` provavelmente já é `object` utilitário — **sem alteração em módulos Koin**; se for classe injetada, o `single { FormatadorData() }` existente no módulo já cobre
3. Em `ItemTimeline`, estruturar o conteúdo como:
   - Linha 1: data (`bodySmall`, `textMuted`)
   - Linha 2: título (`bodyMedium`, bold)
   - Linha 3 (opcional): descrição parseada (`bodySmall`, `textMuted`)
4. Substituir a chamada `FormatadorData::formatarDataHora` por `formatarDataPorExtenso` em `ConteudoTimeline`

---

## Bloco 5 — Remover card/borda do HeaderDetalhe

**Arquivo:** `TelaDetalheProcesso.kt` → `HeaderDetalhe`

**Problema:** `Surface` com `BorderStroke` e `RoundedCornerShape` cria card visual desnecessário.

**Plano:**
1. Substituir `Surface(shape=..., border=..., color=surfacePergaminho)` por `Column` simples
2. Manter o `padding` interno existente
3. Composable puro — **sem impacto em módulos Koin**

---

## Bloco 6 — Aba "Informações" → "Info" + abas roláveis

**Arquivo:** `TelaDetalheProcesso.kt` → `AbaDetalhe.rotulo()` + `SecondaryTabRow`

**Plano:**
1. Em `rotulo()`, mudar `AbaDetalhe.INFORMACOES` para retornar `stringResource(R.string.detalhe_aba_info)` (criar string `"Info"`)
2. Substituir `SecondaryTabRow` por `ScrollableTabRow`
3. Definir `edgePadding = 0.dp` para manter alinhamento à esquerda
4. **Sem impacto em módulos Koin**

---

## Bloco 7 — Rodapé com fonte dos dados

**Arquivo:** `ConteudoTimeline` + `EstadoDetalheProcesso.kt` + `ModeloDetalheProcesso.kt`

**Problema:** Rodapé ausente ou string vazia.

**Plano:**
1. Garantir que `EstadoDetalheProcesso` exponha `ultimaAtualizacao: LocalDateTime?`
2. O `ModeloDetalheProcesso` é registrado via `viewModel { ModeloDetalheProcesso(get()) }` no módulo Koin — verificar se o repositório já retorna esse campo; se não, adicionar ao repositório e ao estado
3. Passar `ultimaAtualizacao` como parâmetro para `ConteudoTimeline`
4. No final da `LazyColumn`, renderizar:
   ```kotlin
   item {
       Surface(color = colors.surfacePergaminho.copy(alpha = 0.5f), ...) {
           Text("Fonte: DataJud (CNJ) · Sync: ${FormatadorData.formatarDataHora(ultimaAtualizacao)}")
       }
   }
   ```

---

## Bloco 8 — Texto de publicação não deve aparecer na timeline

**Arquivo:** mapper de domínio que popula `TimelineProcessoItem`

**Problema:** Itens de publicação têm seu texto completo exposto no campo `descricao` da timeline.

**Plano:**
1. No mapper, identificar itens do tipo `PUBLICACAO`
2. Para esses itens, setar `descricao = null` (ou `"Ver em Publicações"`)
3. O texto completo permanece disponível apenas na aba Publicações via `Publicacao.textoLimpo`
4. Mapper já registrado no módulo Koin existente — **sem nova entrada necessária**

---

## Ordem de execução sugerida

| # | Bloco | Impacto | Risco | Toca módulo Koin? |
|---|---|---|---|---|
| 1 | Parser JSON descricao | Alto | Baixo | Não |
| 2 | Mapper — ocultar texto de publicação na timeline | Alto | Baixo | Não |
| 3 | Formato de data por extenso | Médio | Baixo | Não |
| 4 | Layout ItemTimeline (ponto + conteúdo) | Alto | Médio | Não |
| 5 | Cores dos pontos | Médio | Baixo | Não |
| 6 | Remover card HeaderDetalhe | Baixo | Baixo | Não |
| 7 | Aba Info + ScrollableTabRow | Baixo | Baixo | Não |
| 8 | Rodapé fonte de dados | Baixo | Baixo | Só se `ultimaAtualizacao` não existir no estado |
