# Consent wall — avisos por tela e por ação

**Versão 1.0** · Acompanha os [Termos de Uso](termos-de-uso.md) v1.0 e a
[Política de Privacidade](politica-de-privacidade.md) v1.0

Este documento define **o que o aplicativo exibe, onde, quando e como registra** a
ciência do usuário. É a ponte entre o texto jurídico e a interface.

---

## 1. Princípio de desenho

O objetivo é duplo e as duas metades se atrapalham se mal calibradas:

1. **Provar a ciência** — que o advogado foi informado, de forma específica e
   destacada, das limitações de cada funcionalidade antes de usá-la.
2. **Não inutilizar o aplicativo** — um modal em toda tela, toda vez, é ignorado em
   dois dias e vira prova de que ninguém leu.

A solução adotada é uma escala de três intensidades:

| Nível | Comportamento | Onde se usa |
| --- | --- | --- |
| **A — Portão** | Modal bloqueante com rolagem até o fim e caixa de marcação obrigatória. Não avança sem marcar. | Aceite geral e ações de risco alto |
| **B — Primeira visita** | Modal bloqueante **uma única vez por tela, por versão do texto**. Depois some. | Telas principais |
| **C — Faixa** | Faixa fixa e discreta no topo ou rodapé da tela, sempre visível, tocável para abrir o texto completo. | Todas as telas, permanentemente |

Regra: **toda tela do nível B ganha, depois do primeiro aceite, a faixa do nível C.**
O aviso nunca desaparece — muda de forma.

**Reexibição.** O portão e a primeira visita voltam a bloquear quando: (a) a versão do
texto daquela tela muda; (b) o usuário troca de conta; (c) o usuário pede, no Perfil,
"rever avisos"; (d) passam 12 meses desde o aceite.

---

## 2. Modelo de dados do aceite

Cada aviso é identificado por uma chave estável e uma versão própria — o texto da tela
de prazos pode mudar sem forçar reaceite da tela de clientes.

```
AceiteAviso
├── chave           : String   // "prazos", "minuta.envio", "sync.nuvem"
├── versaoTexto     : Int      // versão daquele aviso específico
├── versaoTermos    : String   // versão dos Termos vigente no aceite ("1.0")
├── hashTexto       : String   // SHA-256 do texto exibido, normalizado
├── aceitoEm        : Instant
├── uid             : String   // conta que aceitou
└── origem          : String   // "portao" | "primeira_visita" | "acao"
```

**Onde grava.** Localmente no DataStore (funciona offline e é o que governa a
interface) e, quando houver conta não anônima, replicado em
`users/{uid}/aceites/{chave}` — **independentemente de a sincronização em nuvem estar
ativa**, porque este registro é prova do dever de informação, não dado de acervo.
Registrar isso na Política (Seção 8) já está feito.

**O hash é o ponto crítico.** Guardar só "aceitou a v2" não prova nada se o texto da
v2 se perdeu. O hash do texto exibido, com o texto versionado em `res/values/`,
permite reconstruir exatamente o que a pessoa leu.

**Tela de Perfil.** Um item "Avisos e termos aceitos" listando chave, versão, data e
ação para reler. Isso cumpre a cláusula 24.3 dos Termos e é, na prática, a melhor
defesa disponível.

---

## 3. Portão de entrada (nível A)

**Quando:** primeira execução, e sempre que a versão dos Termos ou da Política mudar
em ponto material.
**Onde:** antes da primeira tela útil, depois da autenticação.
**Chave:** `termos.geral` · **Bloqueio:** rolagem completa + 5 caixas de marcação
independentes.

> ### Antes de começar
>
> O ObiterJus organiza publicações do DJEN, sugere prazos e rascunha textos. Ele
> ajuda — mas erra. Marque cada item para confirmar que entendeu:
>
> - [ ] **Não é o sistema do tribunal.** O ObiterJus não substitui o Domicílio
>   Judicial Eletrônico, o PJe, o e-SAJ, o eproc nem a consulta aos autos.
> - [ ] **Os prazos são sugestões.** A data exibida pode estar errada. Conferir na
>   fonte oficial continua sendo meu dever.
> - [ ] **Falta de aviso não é aviso de que não há nada.** Lista vazia, prazo sem data
>   ou notificação não recebida não significam que não existe publicação ou prazo.
> - [ ] **Os textos gerados por IA são rascunhos.** Podem citar lei e jurisprudência
>   que não existem. Reviso tudo antes de usar.
> - [ ] **A responsabilidade profissional é minha.** Pelo prazo, pelo ato e pela peça
>   protocolada.
>
> [Ler os Termos de Uso na íntegra] · [Ler a Política de Privacidade]
>
> **[Não aceito]** **[Aceito e quero continuar]**

*Recusar encerra o aplicativo com a mensagem: "Sem o aceite não é possível usar o
ObiterJus. Você pode reabrir o aplicativo quando quiser."*

---

## 4. Avisos por tela (nível B → C)

O texto do nível B é o modal da primeira visita. O texto do nível C é a faixa
permanente — uma linha, tocável, que abre o modal de novo.

---

### 4.1. Início · `ObiterRota.Inicio`

**Chave:** `inicio` · **v1**

> **O painel resume, não confere.**
>
> Os números desta tela vêm do que o aplicativo conseguiu capturar até agora. Uma
> sincronização que falhou, um processo que não está no DJEN ou uma publicação sem a
> sua OAB no texto simplesmente não aparecem aqui — sem erro visível.
>
> Use este painel para se orientar, nunca para concluir que está tudo em dia.

**Faixa:** `Resumo do que foi capturado. Não é conferência de acervo.`

---

### 4.2. Publicações · `ObiterRota.Publicacoes`

**Chave:** `publicacoes` · **v1**

> **De onde vêm estas publicações — e o que falta aqui.**
>
> **Fonte única:** DJEN, a base pública do CNJ, consultada pela sua inscrição na OAB
> ou pelo seu nome.
>
> **Não aparece nesta lista:**
> - intimação pelo Domicílio Judicial Eletrônico ou pelo portal do tribunal;
> - intimação pessoal, por carga, mandado, AR ou em audiência;
> - publicação em diário não integrado ao DJEN;
> - publicação que não traga a sua OAB, ou que a traga com erro;
> - processo em segredo de justiça, cujo teor não é divulgado.
>
> **Homônimos:** na busca por nome, cada item recebe confiança alta, média ou baixa.
> É indicativo. Confiança baixa pode ser seu; confiança alta pode não ser.
>
> **Corte por volume:** acima de 500 resultados no período, a busca é interrompida e
> **nada é importado**. Se sua lista está estranhamente vazia, estreite o critério no
> Perfil.

**Faixa:** `Só DJEN. Não inclui Domicílio Judicial Eletrônico nem portal do tribunal.`

---

### 4.3. Detalhe da publicação · `ObiterRota.DetalhePublicacao`

**Chave:** `publicacao.detalhe` · **v1**

> **Este texto é cópia do que o DJEN divulgou.**
>
> O aplicativo limpa a formatação e destaca trechos, mas não interpreta o ato, não
> avalia cabimento de recurso e não confere se o teor corresponde ao que consta dos
> autos. Divergências entre o diário e os autos existem — e prevalecem os autos.
>
> A marca de sigilo é detectada automaticamente por marcadores no texto. **Pode
> falhar nos dois sentidos.** Antes de compartilhar ou enviar este conteúdo para
> fora, verifique o regime de sigilo do processo.

**Faixa:** `Teor conforme o DJEN. Confira nos autos.`

---

### 4.4. Prazos e agenda · `ObiterRota.Prazos`

**Chave:** `prazos` · **v2** — *a mais importante do aplicativo; texto longo é
justificado aqui*

> **Como estes prazos são calculados — e o que eles ignoram.**
>
> A data vem da API CalendárioForense, a partir da sigla do tribunal e da data de
> disponibilização. O aplicativo presume **sempre** que a intimação se deu pela
> publicação no diário.
>
> **Os três estados:**
> - **Confiável** — o cálculo foi concluído sem ressalva. Não é garantia de acerto.
> - **Incerto** — houve ambiguidade, conversão de meses ou inferência. Confira.
> - **Pendente** — **não há data.** O tribunal não foi identificado ou o serviço
>   falhou. Será tentado de novo, sem aviso.
>
> **O cálculo NÃO aplica:**
> - prazo em dobro — Fazenda Pública, MP, Defensoria, litisconsortes com procuradores
>   distintos;
> - suspensão ou interrupção do processo, sobrestamento por tema repetitivo;
> - portaria local que suspenda expediente ou prazos;
> - feriado local fora do calendário do tribunal;
> - prorrogação ou devolução deferida no seu caso;
> - prazo penal por tipo de ato — no penal só conta em dias corridos.
>
> **Não lê prazo por extenso** ("quinze dias") nem remissão genérica ("no prazo
> legal"). Prazo em meses vira 30 dias corridos por mês, e sai como Incerto.
>
> **Sem natureza cadastrada, o processo é tratado como cível.** Em processo penal não
> identificado como tal, a data sairá errada.
>
> **O alerta cobre só os dois dias seguintes**, e apenas prazos com data calculada.
> Prazo Pendente nunca gera alerta.

**Faixa:** `Prazos são sugestões. Não aplicam prazo em dobro nem suspensões.`

---

### 4.5. Processos e clientes · `ObiterRota.Processos`

**Chave:** `acervo` · **v1**

> **Este acervo é seu, não do tribunal.**
>
> Os processos entram aqui porque apareceram numa publicação sua ou porque você os
> cadastrou. Os dados vêm do DataJud, a base pública do CNJ, que **não cobre todos os
> tribunais e nem sempre está atualizada**. Movimento que não aparece aqui pode
> perfeitamente existir nos autos.
>
> O aplicativo não valida CPF, CNPJ, endereço nem poderes de representação. O que
> você digitar é o que ele usará.

**Faixa:** `Metadados do DataJud. Podem estar incompletos ou atrasados.`

---

### 4.6. Detalhe do processo · `ObiterRota.DetalheProcesso`

**Chave:** `processo.detalhe` · **v1**

> **A linha do tempo é parcial por construção.**
>
> Ela reúne o que veio do DataJud e as publicações capturadas. Não é o extrato dos
> autos: não há petições, documentos, decisões na íntegra nem despachos que não
> tenham sido publicados.
>
> **O nível de sigilo cadastrado aqui governa dois bloqueios de segurança:** impede o
> envio de peças deste processo para geração de minuta e impede a subida das
> publicações vinculadas para a nuvem. Cadastrá-lo errado desliga essas proteções.

**Faixa:** `Linha do tempo parcial. Não substitui os autos.`

---

### 4.7. Adicionar / editar processo · `AdicionarProcesso`, `EditarProcesso`

**Chave:** `processo.edicao` · **v1**

> **Três campos mudam o comportamento do aplicativo:**
>
> - **Tribunal** — sem ele, nenhum prazo é calculado. Fica tudo Pendente.
> - **Natureza** — cível conta em dias úteis, penal em dias corridos. Em branco, o
>   aplicativo assume cível.
> - **Nível de sigilo** — maior que zero bloqueia envio de peças e sincronização em
>   nuvem deste processo.
>
> Preencha com cuidado. Estes campos não são conferidos por ninguém.

**Faixa:** `Tribunal, natureza e sigilo alteram cálculo e bloqueios.`

---

### 4.8. Clientes · `DetalheCliente`, `AdicionarCliente`, `EditarCliente`

**Chave:** `clientes` · **v1**

> **Dados de terceiros — a responsabilidade é sua.**
>
> Ao cadastrar cliente, parte ou representante, você trata dados pessoais de alguém
> que não é usuário deste aplicativo. Nessa relação **você é o controlador** e o
> ObiterJus é o operador: ele guarda e processa conforme as suas escolhas.
>
> A qualificação por extenso é montada mecanicamente com os campos preenchidos e
> **omite em silêncio** o que estiver em branco. Releia antes de colar numa peça.
>
> O aplicativo não valida CPF, CNPJ nem endereço.

**Faixa:** `Você é o controlador destes dados. Qualificação omite campos vazios.`

---

### 4.9. Minutas · `ObiterRota.Minuta`

**Chave:** `minuta` · **v1** — *tela; o envio tem portão próprio em 5.4*

> **O que esta tela produz é rascunho de máquina.**
>
> O texto é gerado por inteligência artificial a partir da peça que você enviar. Ele
> pode:
> - citar lei, súmula ou precedente **que não existe, foi revogado ou não se aplica**;
> - inverter partes, trocar valores, datas e números de processo;
> - omitir preliminar, prescrição ou a tese que decide o caso;
> - exibir índice de confiança alto num trecho materialmente errado.
>
> Nada aqui foi revisado por advogado. A peça que você protocolar é sua, com tudo o
> que isso implica perante o cliente, o juízo e a OAB.

**Faixa:** `Rascunho gerado por IA. Revise integralmente antes de usar.`

---

### 4.10. Perfil e configurações · `Perfil`, `EditarPerfil`

**Chave:** `perfil` · **v1**

> **O que você configura aqui define o que o aplicativo enxerga.**
>
> Sua inscrição na OAB é o critério de busca no DJEN — e **não é validada** contra o
> cadastro da Ordem. Digitada com erro, a busca não encontra nada e nada avisa.
>
> A sincronização automática roda uma vez por dia, por volta das 7h, e depende de
> rede e da permissão que o Android der ao aplicativo para rodar em segundo plano.
> Economia de bateria pode atrasá-la ou impedi-la sem aviso.

**Faixa:** `OAB não é validada. Sincronização automática pode falhar em silêncio.`

---

### 4.11. Auditoria · `ObiterRota.Auditoria`

**Chave:** `auditoria` · **v1**

> **Este registro é de diagnóstico, não de prova processual.**
>
> Ele mostra quando o aplicativo tentou sincronizar, quanto tempo levou e o que deu
> errado. Serve para você entender por que algo não apareceu.
>
> Uma execução marcada como bem-sucedida significa que a consulta foi concluída —
> **não** que tudo o que existia foi capturado.

**Faixa:** `Diagnóstico de execução. Sucesso não significa captura completa.`

---

## 5. Portões por ação (nível A)

Aqui o bloqueio é por ação, não por tela, e **não se dispensa com "não mostrar de
novo"** nos itens 5.3 a 5.5 — são atos que enviam dado para fora ou que criam
confiança sobre um prazo.

---

### 5.1. Confirmar prazo

**Chave:** `acao.prazo.confirmar` · Marcação obrigatória · Dispensável após 3 usos,
com faixa mantida no diálogo

> **Confirmar este prazo na agenda**
>
> Data sugerida: **{data}** · Confiança: **{estado}**
>
> {quando Incerto ou Pendente:}
> ⚠️ Este cálculo não é confiável. {motivo}
>
> Confirmar registra o prazo na sua agenda. **Não valida a data.** O aplicativo não
> aplicou prazo em dobro nem verificou suspensões, portarias locais ou prorrogações
> deferidas no seu caso.
>
> - [ ] Confirmo que verifiquei esta data na fonte oficial.
>
> **[Cancelar]** **[Confirmar prazo]**

---

### 5.2. Cadastrar prazo manualmente

**Chave:** `acao.prazo.manual` · Sem marcação; texto informativo no formulário

> A contagem continua sendo feita pelo serviço de cálculo, a partir dos dias e do
> regime que você informar. A confiança exibida passa a refletir a sua declaração,
> não uma verificação do aplicativo. Este prazo substitui a sugestão automática.

---

### 5.3. Enviar peça para geração de minuta

**Chave:** `acao.minuta.envio` · Marcação obrigatória · **Sempre exibido**

> **Enviar esta peça para fora do aparelho**
>
> Arquivo: **{nome}** · {tamanho}
>
> O PDF sai do seu aparelho e vai para o serviço Obiter-Minuta, onde é processado por
> um modelo de inteligência artificial operado por terceiro. O serviço informa
> descartar o arquivo após o processamento.
>
> Isto envolve informação coberta por **sigilo profissional**. Só você pode avaliar se
> este envio é compatível com seus deveres perante o cliente e a OAB, e se depende de
> anuência dele.
>
> Processos cadastrados com sigilo são bloqueados automaticamente — mas **o bloqueio
> depende do cadastro estar correto**.
>
> - [ ] Verifiquei que este processo não corre em segredo de justiça e que posso
>   enviar este documento.
>
> **[Cancelar]** **[Enviar]**

---

### 5.4. Ativar sincronização em nuvem

**Chave:** `acao.sync.nuvem` · Marcação obrigatória · **Sempre exibido ao ativar**

> **Ativar a cópia do acervo na nuvem**
>
> Passam a subir para servidores do Google (Firebase Firestore), vinculados à sua
> conta: processos, publicações, movimentos, participantes, clientes, prazos e
> minutas. Isso inclui **dados pessoais de terceiros** — nome, CPF/CNPJ, endereço e o
> inteiro teor das publicações.
>
> Publicações marcadas como sigilosas não sobem. **A marcação é automática e pode
> falhar.**
>
> Isto **não é backup**: não há garantia de integridade nem de restauração.
>
> Você pode desativar quando quiser — o que já subiu permanece até você pedir a
> exclusão.
>
> - [ ] Autorizo o envio destes dados e confirmo que posso fazê-lo em relação aos
>   dados de terceiros que constam do meu acervo.
>
> **[Agora não]** **[Ativar sincronização]**

---

### 5.5. Conectar Google Calendar

**Chave:** `acao.calendar.conectar` · Marcação obrigatória · **Sempre exibido**

> **Conectar sua agenda do Google**
>
> Ao confirmar um prazo com destino ao Google Calendar, o aplicativo cria um evento
> contendo **o número do processo** e o texto do prazo. Essa informação passa a
> existir na sua conta Google e se sujeita às políticas do Google.
>
> A sincronização é de mão única: alterar ou apagar o evento no Google **não muda** o
> prazo aqui.
>
> - [ ] Autorizo a criação de eventos com dados do processo na minha conta Google.
>
> **[Cancelar]** **[Conectar]**

---

### 5.6. Exportar / compartilhar relatório

**Chave:** `acao.exportar` · Marcação obrigatória · **Sempre exibido**

> **Compartilhar este relatório**
>
> O arquivo contém dados de processos e, conforme o caso, **dados pessoais de
> terceiros** e o teor de publicações. Publicações marcadas como sigilosas saem sem o
> texto — sujeito à mesma falibilidade da detecção automática.
>
> Depois de compartilhado, o conteúdo está no aplicativo de destino e fora do controle
> do ObiterJus.
>
> - [ ] Confirmo que posso compartilhar este conteúdo com o destinatário escolhido.
>
> **[Cancelar]** **[Compartilhar]**

---

### 5.7. Ativar busca apenas por nome

**Chave:** `acao.busca.nome` · Sem marcação; aviso no seletor

> A busca por nome traz publicações de **qualquer advogado homônimo no país**. Acima
> de 500 resultados no período, a busca é abortada e **nada é importado**. Prefira a
> busca por OAB sempre que possível.

---

## 6. Onde os avisos NÃO devem aparecer

Para preservar o peso dos que existem:

- não repetir o aviso de prazo dentro do cartão de cada publicação — a faixa da tela
  basta;
- não bloquear navegação entre abas já aceitas;
- não exibir modal em telas puramente de leitura já cobertas por faixa;
- não usar modal para erro de rede, campo obrigatório ou mensagem operacional — isso
  é snackbar.

---

## 7. Acessibilidade e forma

Requisitos que sustentam a validade do aceite:

1. **Legibilidade** — corpo mínimo equivalente a 14sp, contraste AA, respeito à fonte
   ampliada do sistema. Texto que o usuário não consegue ler não foi informado.
2. **Rolagem obrigatória até o fim** nos portões: o botão de confirmar só habilita
   depois que o conteúdo foi percorrido.
3. **Caixas independentes** no portão de entrada — nada de "marcar todas".
4. **Sem pré-marcação**. Caixa marcada por padrão não é manifestação (art. 8º, § 4º,
   da LGPD).
5. **Recusa possível e sem punição disfarçada** nos consentimentos opcionais — nuvem,
   minuta e calendário são independentes entre si e do resto do aplicativo.
6. **Leitor de tela**: modal anunciado como diálogo, foco inicial no título, ordem de
   leitura título → corpo → caixas → botões.
7. **Texto em `res/values/`**, nunca embutido em código, para que o hash da versão
   exibida seja reconstituível.

---

## 8. Implementação sugerida

Peças a criar:

| Componente | Responsabilidade |
| --- | --- |
| `core/consent/AvisoTela.kt` | Enum das chaves, com versão e id do recurso de texto |
| `core/consent/AceiteRepository.kt` | Ler e gravar aceites no DataStore; calcular o hash |
| `data/consent/AceiteFirestoreSync.kt` | Replicar o aceite em `users/{uid}/aceites/{chave}`, independente da flag de sincronização de acervo |
| `presentation/componentes/consent/PortaoAviso.kt` | Modal bloqueante com rolagem obrigatória e caixas |
| `presentation/componentes/consent/FaixaAviso.kt` | Faixa persistente, tocável |
| `presentation/componentes/consent/AvisoTelaEffect.kt` | Efeito que dispara a primeira visita conforme a chave da rota |
| `presentation/perfil/AceitesSection.kt` | Lista de aceites com data, versão e ação de reler |

Pontos de atenção na implementação:

- **A chave da rota governa o efeito**, não o composable — assim uma tela nova sem
  chave cadastrada falha visivelmente em desenvolvimento em vez de passar sem aviso.
- **O portão de entrada precede a restauração de estado**: nada de acervo deve ser
  carregado antes do aceite geral.
- **Sessão anônima** grava o aceite só localmente; ao vincular a conta, replicar.
- **Migração de versão de texto** exige incremento explícito da constante — um teste
  deve falhar se o texto mudar sem a versão mudar (comparação de hash em teste de
  unidade é suficiente e barata).

---

## 9. Rastreabilidade — de cada aviso à cláusula

| Chave | Cláusula dos Termos | Seção da Política |
| --- | --- | --- |
| `termos.geral` | 2, 6, 8, 9, 12, 21 e Declaração de ciência | 1 |
| `inicio` | 7, 9 | — |
| `publicacoes` | 5, 6, 7 | 4.2, 7 |
| `publicacao.detalhe` | 6.3, 7.6 | 4.2 |
| `prazos` | 8 (integral), 9, 10 | 12 |
| `acervo` | 5, 6.4, 13 | 3, 4.2 |
| `processo.detalhe` | 6.3, 7.6, 12.3 | 4.2 |
| `processo.edicao` | 8.4.3, 12.3, 16 c) | — |
| `clientes` | 13, 16 g) | 3, 4.2 |
| `minuta` | 12 | 4.2, 7, 12 |
| `perfil` | 3.2, 7.7, 10.3 | 4.1 |
| `auditoria` | 7.7, 9 | 4.1 |
| `acao.prazo.confirmar` | 8, 11 | — |
| `acao.prazo.manual` | 8.7 | — |
| `acao.minuta.envio` | 12.2, 12.3, 12.7 | 5, 7 |
| `acao.sync.nuvem` | 14 | 5, 6.2, 7 |
| `acao.calendar.conectar` | 11 | 5, 7 |
| `acao.exportar` | 15 | 7 |
| `acao.busca.nome` | 7.3, 7.4 | — |

---

### Histórico de versões

| Versão | Data | Alterações |
| --- | --- | --- |
| 1.0 | `[DD/MM/AAAA]` | Texto inicial. |
