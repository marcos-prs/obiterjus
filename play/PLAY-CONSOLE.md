# Play Console — respostas prontas para o teste fechado

Documento de trabalho: cada seção corresponde a um formulário da Play Console.
O que está escrito aqui pode ser copiado como está. O que depende de decisão sua
está marcado com **▶ VOCÊ**.

Pacote: `com.obiterjus` · versionCode `1` · versionName `1.0` · minSdk 26 · targetSdk 36
Arquivo gerado: `app/build/outputs/bundle/release/app-release.aab` (assinado com a chave de upload).

---

## 1. Ficha da loja (Store listing)

**Nome do app** (até 30 caracteres)

```
ObiterJus
```

Alternativa, se quiser palavra-chave no nome (24 caracteres):

```
ObiterJus: prazos e DJEN
```

**Descrição breve** (até 80 caracteres) — 75:

```
Publicações do DJEN, prazos calculados e processos organizados num só lugar.
```

**Descrição completa** (até 4000 caracteres):

```
O ObiterJus acompanha as publicações oficiais em nome do advogado, calcula os prazos
e mantém o acervo organizado no próprio aparelho.

MONITORAMENTO DAS PUBLICAÇÕES
Informe sua inscrição na OAB e o aplicativo consulta diariamente o Diário de Justiça
Eletrônico Nacional (DJEN), do Conselho Nacional de Justiça. As publicações novas
chegam por notificação, já vinculadas ao processo correspondente.

PRAZOS CALCULADOS, NÃO ADIVINHADOS
Cada publicação passa por uma leitura que identifica o prazo, a contagem em dias úteis
ou corridos e o termo inicial. O cálculo considera feriados nacionais, feriados locais
e os períodos de suspensão do tribunal. Todo prazo fica como sugestão até você conferir
e confirmar.

AGENDA E CALENDÁRIO
Os prazos confirmados aparecem numa agenda própria e podem ser enviados para o Google
Agenda, se você autorizar.

PROCESSOS E CLIENTES
Os dados de cada processo são completados com a base pública do DataJud (CNJ): classe,
assunto, órgão julgador, data de distribuição. Os clientes ficam cadastrados uma vez e
podem ser vinculados a vários processos.

RASCUNHO DE PEÇAS
A partir do PDF de uma peça recebida, o aplicativo extrai a estrutura do documento e
sugere um rascunho da peça de resposta. O texto gerado é ponto de partida: precisa ser
lido, corrigido e conferido antes de qualquer uso. As citações vêm marcadas para
conferência justamente porque não são verificadas automaticamente.

SEUS DADOS FICAM COM VOCÊ
O acervo é guardado no aparelho, em banco de dados cifrado. A sincronização em nuvem é
opcional e só funciona se você ligar. Não há anúncios, não há rastreamento e o
aplicativo não usa identificador de publicidade.

IMPORTANTE
O ObiterJus é ferramenta de apoio. Não é sistema oficial de tribunal, não substitui a
consulta aos autos nem a conferência do prazo pelo advogado responsável. A
responsabilidade profissional continua inteira sua.
```

**Categoria do app**: `Produtividade`

**Recursos gráficos**

| Item | Situação |
| --- | --- |
| Ícone 512×512 | Pronto: `play/graficos/icone-512.png` |
| Gráfico de destaque 1024×500 | Pronto: `play/graficos/destaque-1024x500.png` |
| Capturas de tela de celular (mín. 2, máx. 8) | **▶ VOCÊ** — veja a seção 9 |

**Dados de contato**

- E-mail: `obiterjus@xablauinitiative.com.br`
- Site: `https://obiterjus.web.app`
- Telefone: opcional

---

## 2. Segurança dos dados (Data safety)

Levantado do código: as permissões declaradas são apenas `INTERNET` e `POST_NOTIFICATIONS`;
não há SDK de anúncios, de analytics ou de relatório de falhas; tráfego em texto claro está
bloqueado por `network_security_config`; `allowBackup` é `false`.

**O app coleta ou compartilha algum dos tipos de dados exigidos?** → **Sim**

**Todos os dados em trânsito são criptografados?** → **Sim**

**Você oferece um jeito de o usuário pedir a exclusão dos dados?** → **Sim**

URL de exclusão de conta: `https://obiterjus.web.app/exclusao-de-conta`

Declarar que a exclusão existe **no aplicativo e por solicitação** — os dois caminhos
existem (Perfil → menu ⋮ → Excluir conta).

### Tipos de dados a declarar

| Tipo | Coletado | Compartilhado | Obrigatório | Finalidades |
| --- | --- | --- | --- | --- |
| Informações pessoais › Nome | Sim | Não | Opcional | Funcionalidade do app; Gerenciamento da conta |
| Informações pessoais › Endereço de e-mail | Sim | Não | Obrigatório | Funcionalidade do app; Gerenciamento da conta |
| Informações pessoais › IDs do usuário | Sim | Não | Obrigatório | Funcionalidade do app; Gerenciamento da conta |
| Informações pessoais › Outras informações | Sim | Não | Obrigatório | Funcionalidade do app |
| Arquivos e documentos › Arquivos e documentos | Sim | Não | Opcional | Funcionalidade do app |
| IDs do dispositivo ou outros IDs | Sim | Não | Obrigatório | Funcionalidade do app |

Observações para preencher cada linha:

- **Outras informações**: número de inscrição na OAB, números de processo e o teor das
  publicações — que contém nomes de partes, advogados e representantes. É o acervo.
- **Arquivos e documentos**: só os PDFs que o usuário manda, deliberadamente, para gerar
  rascunho de peça. Não há varredura de armazenamento.
- **IDs do dispositivo ou outros IDs**: identificador de instalação usado pelo Firebase
  Remote Config. Não é identificador de publicidade — o app não declara `AD_ID`.
- **Coletado, não compartilhado**: os dados saem do aparelho apenas para serviços que
  processam em nome do desenvolvedor (Firebase, API Obiter-Minuta e o provedor de IA que
  ela contrata), o que a Play trata como operador, não como terceiro. O envio ao Google
  Agenda é ação iniciada pelo usuário, também fora da definição de compartilhamento.

**▶ VOCÊ — decida antes de enviar:** se preferir declarar o acervo como *opcional* em vez
de *obrigatório*, a leitura também se sustenta, já que a sincronização em nuvem só funciona
se o usuário ligar. Escolha uma e mantenha coerente com a Política de Privacidade.

---

## 3. Classificação de conteúdo (questionário IARC)

Categoria do questionário: **Utilitário, produtividade, comunicação ou outro**

| Pergunta | Resposta |
| --- | --- |
| Contém violência? | Não |
| Contém conteúdo sexual ou nudez? | Não |
| Contém linguagem imprópria? | Não |
| Contém referência a drogas, álcool ou tabaco? | Não |
| Contém jogos de azar ou simulação de apostas? | Não |
| Permite comunicação entre usuários? | Não |
| Permite compartilhar a localização do usuário? | Não |
| Permite compartilhar conteúdo gerado pelo usuário? | Não |
| Permite compras no app? | Não |
| Contém conteúdo assustador ou perturbador? | Não |

Observação: o aplicativo exibe textos de publicações judiciais, que podem tratar de
matéria criminal ou de família. É conteúdo oficial, factual e dirigido a profissional do
Direito — não é "conteúdo violento" no sentido do questionário.

---

## 4. Público-alvo e conteúdo

- **Faixas etárias**: apenas **18 anos ou mais**
- **O app é direcionado a crianças?** Não
- **Pode atrair crianças involuntariamente?** Não — é ferramenta de trabalho para advogados
- **Política para Famílias**: não se aplica

---

## 5. Acesso ao app (App access)

A revisão precisa ver o app funcionando. Hoje a maior parte das telas abre sem login, mas
monitoramento e geração de minuta não funcionam sem conta.

Resposta: **Todas ou algumas funcionalidades são restritas**

**▶ VOCÊ — crie uma conta de demonstração** (e-mail e senha dedicados, com algum processo e
alguma publicação já carregados) e preencha assim:

```
Nome das instruções: Conta de demonstração

Instruções:
1. Abra o aplicativo e toque na aba Perfil.
2. Toque em Entrar e use as credenciais informadas nos campos de usuário e senha.
3. A aba Publicações traz comunicações de exemplo vinculadas a processos.
4. A aba Prazos mostra os prazos calculados a partir dessas publicações.

Observação: o monitoramento consulta o Diário de Justiça Eletrônico Nacional (DJEN)
do CNJ pela inscrição na OAB cadastrada no perfil. A conta acima já está configurada.
```

---

## 6. Declarações restantes do painel "Políticas"

| Formulário | Resposta |
| --- | --- |
| Anúncios | **Não**, o app não contém anúncios |
| ID de publicidade | **Não usa** (a permissão `AD_ID` não é declarada) |
| App de notícias | Não |
| App de empréstimo pessoal | Não |
| Apps de saúde | Não |
| Recursos financeiros | Não |
| Apps governamentais | Não |
| COVID-19 / rastreamento de contato | Não |
| Conteúdo gerado por IA | **Sim** — o app gera rascunho de peça por IA generativa |
| URL da Política de Privacidade | `https://obiterjus.web.app/privacidade` |

Sobre a declaração de IA: a política da Play para apps com IA generativa pede um caminho
para o usuário reportar conteúdo impróprio. Hoje o canal é o e-mail de contato da ficha da
loja; se a revisão pedir algo dentro do app, vale acrescentar um item "Reportar problema"
na tela de resultado da minuta.

---

## 7. Teste fechado

1. **Criar o app**: Todos os apps → Criar app. Nome `ObiterJus`, idioma padrão
   `português (Brasil)`, tipo `App`, `Gratuito`.
2. **Testes → Teste fechado** → criar uma faixa (a padrão `alpha` serve).
3. **Enviar** `app/build/outputs/bundle/release/app-release.aab`.
4. **Lista de testadores**: e-mails de Contas Google, um por testador.
5. **URL de feedback**: `mailto:obiterjus@xablauinitiative.com.br` serve.
6. **Notas da versão**:

```
Primeira versão em teste fechado.

- Monitoramento diário das publicações do DJEN pela inscrição na OAB
- Cálculo de prazos com feriados e suspensões do tribunal
- Agenda de prazos, com envio opcional ao Google Agenda
- Dados do processo completados pela base pública do DataJud
- Cadastro de clientes vinculados a processos
- Rascunho de peça a partir do PDF da peça recebida
- Acervo guardado no aparelho em banco cifrado; sincronização em nuvem opcional
```

**▶ VOCÊ — regra dos 12 testadores:** contas de desenvolvedor pessoais criadas depois de
novembro de 2023 precisam de **12 testadores participando por 14 dias seguidos** no teste
fechado antes de poder pedir acesso à produção. Vale montar a lista com folga.

---

## 8. O que precisa acontecer antes de enviar os formulários

As páginas ficam no site do app, `https://obiterjus.web.app` — Next.js com exportação
estática, publicado no Firebase Hosting do mesmo projeto `obiterjus`.

Estado em 06/11/2026, conferido em produção:

| Rota | Situação |
| --- | --- |
| `/exclusao-de-conta` | **No ar** (200) — atende à Segurança dos dados |
| `/privacidade` | 200, mas ainda com o **texto curto antigo**, de 06/05/2026 |
| `/termos` | 404 — nunca existiu |

Duas coisas foram corrigidas junto: o `firebase.json` tinha um `rewrite` de `**` para
`/index.html`, então **qualquer** rota inexistente devolvia a home com 200 — inclusive
`/termos` e `/exclusao-de-conta`. Numa revisão da Play isso é pior do que um 404 honesto,
porque a URL "funciona" sem entregar a página. Agora 404 é 404.

Para trocar a política antiga pela versão longa e publicar os Termos:

1. Preencher `web/dados-do-controlador.json` — falta **1 campo**, `COMARCA_FORO`; o
   gerador recusa rodar enquanto houver lacuna.
2. `python web/build.py` — escreve as duas páginas dentro do site.
3. No site: `npm run build && firebase deploy --only hosting`.

---

## 9. O que depende de você

| Item | Por quê |
| --- | --- |
| Comarca de eleição de foro (Cláusula 25.7 dos Termos) | É dado seu; não cabe inventar em documento jurídico. O resto da identificação já está resolvido: o controlador, na tabela da Política, é o próprio **ObiterJus** — sem razão social, sem CNPJ/CPF e sem endereço — e o titular/Encarregado é Marcos Paulo Rocha de Souza |
| Capturas de tela | Exigem o app rodando com acervo real; telas vazias fazem ficha ruim |
| Conta de demonstração para a revisão | Precisa de e-mail e senha criados por você |
| Criar o app na Play Console e enviar o AAB | Exige sua conta de desenvolvedor |
| Google Cloud: habilitar a Calendar API e configurar a tela de consentimento | Exige o console, com sua conta |
| Registrar no Firebase o SHA-1 da **chave de assinatura do app** da Play | Esse certificado só existe depois do primeiro envio |
