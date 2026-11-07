# Termos e Condições de Uso — ObiterJus

**Versão 1.0** · Vigente a partir de `{{DATA_VIGENCIA}}` · Aplicável à versão `1.0` do aplicativo

---

## Leia isto antes de qualquer coisa

O ObiterJus é uma **ferramenta de apoio ao trabalho do advogado**. Ele lê publicações
já divulgadas em bases públicas do Conselho Nacional de Justiça, organiza-as, sugere
prazos e rascunha textos.

Ele **não** é o sistema oficial do tribunal, **não** substitui a consulta aos autos,
**não** substitui o Domicílio Judicial Eletrônico e **não** exerce juízo profissional.

Todo prazo, toda publicação e todo texto exibidos pelo aplicativo são **sugestões
sujeitas a erro** e devem ser conferidos pelo usuário na fonte oficial antes de
qualquer decisão profissional. A responsabilidade pela perda de prazo, pelo ato
processual e pelo conteúdo de qualquer peça é, sempre e integralmente, do advogado.

Se você não concorda com isso, não use o aplicativo.

---

## Cláusula 1 — Definições

Para os fins destes Termos:

| Termo | Significado |
| --- | --- |
| **Aplicativo** ou **ObiterJus** | O programa de computador para dispositivos Android distribuído sob esse nome, incluindo suas atualizações e os serviços de retaguarda operados pelo Desenvolvedor. |
| **Desenvolvedor** | **ObiterJus**, aplicativo de titularidade de `{{NOME_TITULAR}}`. |
| **Usuário** | A pessoa física que instala e utiliza o Aplicativo em nome próprio ou no exercício da advocacia. |
| **Conta** | O cadastro individual do Usuário, autenticado pelo serviço Firebase Authentication. |
| **Dispositivo** | O aparelho Android em que o Aplicativo está instalado. |
| **DJEN** | Diário de Justiça Eletrônico Nacional, base pública do CNJ (Resolução CNJ nº 455/2022), acessada por meio da API Comunica (`comunicaapi.pje.jus.br`). |
| **DataJud** | Base Nacional de Dados do Poder Judiciário (Resolução CNJ nº 331/2020), acessada pela API Pública (`api-publica.datajud.cnj.jus.br`). |
| **API CalendárioForense** | Serviço de retaguarda do Desenvolvedor responsável, com exclusividade, pelo cálculo das datas-limite exibidas no Aplicativo. |
| **API Obiter-Minuta** | Serviço de retaguarda do Desenvolvedor responsável pela extração de conteúdo de peças em PDF e pela geração de rascunhos por inteligência artificial. |
| **Sugestão de prazo** | Data-limite calculada e exibida pelo Aplicativo, com o grau de confiança indicado na interface. |
| **Minuta** | Rascunho de peça processual gerado por inteligência artificial a pedido do Usuário. |
| **Fonte oficial** | O sistema processual eletrônico do tribunal competente, os autos do processo e as intimações recebidas pelas vias legalmente previstas. |

---

## Cláusula 2 — Objeto e natureza jurídica do Aplicativo

**2.1.** Estes Termos regem a licença de uso do Aplicativo, concedida ao Usuário em
caráter pessoal, intransferível, não exclusivo e revogável.

**2.2.** O Aplicativo é **software de apoio à organização do trabalho advocatício**.
Sua finalidade é reunir, em um único lugar, informações já publicadas em bases
públicas do Poder Judiciário e informações inseridas pelo próprio Usuário,
apresentando-as de forma organizada, com sugestões de prazo e rascunhos de texto.

**2.3.** O Aplicativo **não é**, e não deve ser tratado como:

a) sistema oficial de intimação, publicação ou comunicação de atos processuais;
b) substituto do Domicílio Judicial Eletrônico, do portal do tribunal, do PJe, do
   e-SAJ, do eproc, do Projudi ou de qualquer sistema processual eletrônico;
c) serviço de consultoria, assessoria ou orientação jurídica;
d) serviço de escritório virtual, de controle de escritório ou de custódia de autos;
e) sistema de peticionamento, assinatura digital ou protocolo eletrônico;
f) serviço de backup, arquivamento legal ou preservação probatória de documentos;
g) sistema de alarme, monitoramento crítico ou serviço com nível de disponibilidade
   assegurado.

**2.4.** O uso do Aplicativo **não cria relação de mandato, de patrocínio, de
consultoria ou de qualquer outra natureza profissional** entre o Desenvolvedor e o
Usuário, nem entre o Desenvolvedor e os clientes, partes ou terceiros cujos dados
sejam tratados pelo Usuário no Aplicativo.

**2.5.** O Desenvolvedor não exerce a advocacia por meio do Aplicativo e não emite
opinião jurídica. As classificações, inferências, sugestões de recurso cabível e
rascunhos apresentados são resultado de processamento automatizado, não de análise
profissional.

---

## Cláusula 3 — Quem pode usar

**3.1.** O Aplicativo destina-se a advogados regularmente inscritos na Ordem dos
Advogados do Brasil e a profissionais e estagiários que atuem sob supervisão de
advogado inscrito.

**3.2.** O Aplicativo **não verifica, não valida e não confirma** a inscrição do
Usuário na OAB. O número de inscrição e a seccional informados são usados apenas
como critério de busca no DJEN, exatamente como digitados. Informar dados de
inscrição de terceiro é de responsabilidade exclusiva de quem o faz.

**3.3.** O Usuário declara ter capacidade civil plena e utilizar o Aplicativo no
exercício de sua atividade profissional, como insumo de sua atividade-fim.

**3.4.** É vedado o uso por menores de 18 anos.

**3.5.** O Aplicativo **não possui gestão de escritório**: não há perfis de equipe,
compartilhamento de acervo entre contas, hierarquia de permissões ou trilha de
acesso por colaborador. Cada Conta é individual e enxerga apenas os próprios dados.
Se mais de uma pessoa usa a mesma Conta, o Desenvolvedor não tem como distinguir
quem praticou cada ato, e o Usuário titular responde por todos eles.

---

## Cláusula 4 — Conta, autenticação e segurança de acesso

**4.1.** O acesso completo às funcionalidades exige Conta autenticada por e-mail e
senha ou por conta Google, via Firebase Authentication.

**4.2.** O Aplicativo admite sessão anônima transitória. Nessa condição, **a
sincronização em nuvem e a geração de minutas ficam indisponíveis**, porque o
identificador anônimo é efêmero e desaparece na reinstalação, o que deixaria dados
sem titular identificável.

**4.3.** A senha é de guarda exclusiva do Usuário. O Desenvolvedor não tem acesso a
senhas e não as recupera; a redefinição ocorre exclusivamente por e-mail.

**4.4.** O Usuário deve comunicar imediatamente ao Desenvolvedor qualquer suspeita de
acesso não autorizado à sua Conta, pelo canal da Cláusula 24.

**4.5.** Ao sair da Conta no Aplicativo, **todos os dados locais são apagados**:
banco de dados, preferências, token de calendário e cache de certidões. O mesmo
ocorre quando uma Conta diferente da que gravou a base entra no mesmo Dispositivo.
Se a sincronização em nuvem não estiver ativa, esses dados **não são recuperáveis**.

---

## Cláusula 5 — Bases de dados que o Aplicativo consulta

O Aplicativo consulta, exclusivamente, as fontes descritas abaixo. Nenhuma outra.

| Fonte | Endereço | O que o Aplicativo envia | O que recebe | Natureza |
| --- | --- | --- | --- | --- |
| **DJEN / API Comunica (CNJ)** | `comunicaapi.pje.jus.br` | Número e UF da inscrição na OAB **ou** nome do advogado; intervalo de datas | Comunicações processuais divulgadas no DJEN e certidões de publicação em PDF | Base pública do CNJ, de terceiro |
| **DataJud (CNJ)** | `api-publica.datajud.cnj.jus.br` | Número do processo no padrão CNJ e sigla do tribunal | Metadados processuais e movimentos | Base pública do CNJ, de terceiro |
| **API CalendárioForense** | `calendario-forense-br.onrender.com` | Sigla do tribunal, data de disponibilização, quantidade e unidade do prazo, natureza (cível/penal), artigo do CPC quando aplicável | Data-limite e estado do cálculo | Serviço do Desenvolvedor |
| **API Obiter-Minuta** | `obiterjus.duckdns.org` | Arquivo PDF da peça enviada pelo Usuário, texto extraído, número do processo e vara quando informados | Estrutura extraída da peça e rascunho gerado por IA | Serviço do Desenvolvedor, com uso de provedor de IA de terceiro |
| **ViaCEP** | `viacep.com.br` | Apenas o CEP digitado | Logradouro, bairro, município e UF | Serviço público de terceiro |
| **Google Calendar** | `googleapis.com` | Título, descrição e data do prazo confirmado | Identificador do evento criado | Serviço de terceiro, **somente se o Usuário autorizar** |
| **Google Firebase** | Infraestrutura Google | Credenciais de autenticação; dados de acervo **somente se a sincronização em nuvem for ativada**; parâmetros de configuração remota | Autenticação, configuração e réplica dos dados sincronizados | Infraestrutura de terceiro |

**5.1.** As chaves de acesso às bases do CNJ são de titularidade do Desenvolvedor ou
públicas, conforme o caso, e são enviadas apenas para endereços sob o domínio
`jus.br`, em conexão cifrada — restrição verificada pelo próprio Aplicativo antes de
cada chamada.

**5.2.** O Desenvolvedor **não tem ingerência** sobre o conteúdo, a completude, a
pontualidade ou a disponibilidade das bases do CNJ e do ViaCEP. Erro, atraso, omissão
ou indisponibilidade nessas fontes repercute diretamente no Aplicativo.

---

## Cláusula 6 — Bases de dados e sistemas que o Aplicativo NÃO consulta

Esta cláusula é parte essencial do objeto contratado. O Usuário declara ciência de
que o Aplicativo **não acessa, não lê, não monitora e não recebe informação** de:

**6.1. Sistemas processuais e canais oficiais de intimação**

a) **Domicílio Judicial Eletrônico** (CNJ) — citações e intimações dirigidas ao
   Usuário nesse canal **não** aparecem no Aplicativo;
b) portais de intimação eletrônica dos tribunais (art. 5º da Lei nº 11.419/2006),
   incluindo o prazo de consulta de 10 dias corridos ali previsto;
c) PJe, e-SAJ, eproc, Projudi, SEEU, PROJUDI, Themis, Tucujuris ou qualquer outro
   sistema processual eletrônico, em qualquer instância;
d) painéis, caixas de mensagem ou "push" de tribunais;
e) sistemas de tribunais administrativos, conselhos profissionais, CARF, tribunais
   de contas, juntas comerciais ou câmaras arbitrais.

**6.2. Publicações fora do DJEN**

a) diários de justiça estaduais, regionais ou seccionais que não estejam integrados
   ao DJEN;
b) Diário Oficial da União, diários oficiais estaduais e municipais;
c) editais publicados em jornais de grande circulação ou em sítios de tribunais;
d) intimações pessoais, por carga, por mandado, por carta com aviso de recebimento,
   por oficial de justiça, por meio eletrônico direto ou em audiência.

**6.3. Conteúdo dos autos**

a) petições, decisões, sentenças, acórdãos, laudos, documentos e mídias juntados aos
   autos — o Aplicativo só conhece o texto da comunicação divulgada no DJEN, os
   metadados do DataJud e os arquivos que o próprio Usuário enviar;
b) processos que tramitam em segredo de justiça, cujo inteiro teor não é divulgado
   nas bases públicas;
c) processos físicos.

**6.4. Bases de terceiros de consulta e verificação**

a) cadastro da OAB — o Aplicativo não valida inscrição, situação, suspensão ou
   licenciamento de advogado;
b) Receita Federal, cadastros de CPF/CNPJ, situação cadastral ou quadro societário;
c) bureaus de crédito, protestos, cartórios, Detran, Juntas Comerciais, INSS,
   registros de imóveis;
d) bases de jurisprudência, doutrina, súmulas, temas repetitivos ou legislação — o
   Aplicativo **não faz pesquisa jurídica**;
e) tabelas de custas, guias, honorários ou cálculos de atualização monetária;
f) qualquer base de dados pessoais para enriquecimento cadastral.

**6.5. Atos que o Aplicativo não pratica**

a) não peticiona, não protocola, não assina digitalmente e não se conecta a
   certificado digital, token, e-CAC ou gov.br;
b) não pratica atos em nome do Usuário perante qualquer órgão;
c) não interpõe recursos, não requer prazos, não junta documentos;
d) não emite alvarás, guias, certidões negativas ou requerimentos.

**6.6.** A ausência de determinada informação no Aplicativo **não permite concluir**
que ela não exista. Ver Cláusula 9.

---

## Cláusula 7 — Captura de publicações: alcance e limites conhecidos

**7.1. Como funciona.** O Aplicativo consulta o DJEN por número e UF de inscrição na
OAB ou, se o Usuário optar, apenas pelo nome do advogado, em um intervalo de datas
retroativo cujo padrão é de 15 dias.

**7.2. Vínculo por OAB.** Uma comunicação só é encontrada se a inscrição do Usuário
constar do registro no DJEN. Publicações que omitam a OAB, que a grafem com erro, que
mencionem apenas outro patrono do mesmo processo ou que estejam associadas a
inscrição diversa **não serão capturadas**.

**7.3. Busca por nome e homônimos.** A busca por nome retorna publicações de qualquer
advogado com nome semelhante em todo o país. O Aplicativo classifica cada item em
confiança **alta** (a OAB do Usuário aparece no corpo do texto), **média** (o nome
aparece em forma estrutural compatível) ou **baixa** (só houve aproximação, ou o
corpo do texto não confirma o vínculo). **Nenhum item é descartado**, e a
classificação é indicativa: item de confiança baixa pode ser do Usuário, e item de
confiança alta pode não ser.

**7.4. Corte por volume.** Se a consulta retornar mais de 500 resultados no período,
o Aplicativo **interrompe a busca e não importa nenhum item**, por presunção de
homonímia de alta abrangência. Nessa hipótese o ciclo termina sem publicações, e o
Usuário deve estreitar o critério de busca. A paginação também é limitada a 100
páginas por ciclo.

**7.5. Duplicidade.** O Aplicativo identifica e marca comunicações repetidas pelo
identificador da fonte. Divergências de formatação na origem podem produzir itens
aparentemente duplicados ou, ao contrário, ocultar repetições legítimas.

**7.6. Sigilo.** O Aplicativo marca como sigilosa a comunicação cujo nível de sigilo
informado pela fonte seja maior que zero **ou** cujo texto contenha marcadores
textuais conhecidos. Trata-se de **detecção heurística**: pode marcar como sigiloso o
que não é, e **pode deixar de marcar o que é**. Antes de ativar a sincronização em
nuvem, de enviar peça para geração de minuta ou de exportar relatório, cabe ao
Usuário verificar o regime de sigilo de cada processo.

**7.7. Sincronização diária.** A busca automática é agendada uma vez por dia, por
volta das 7h, e depende de conexão de rede e da execução concedida pelo sistema
operacional. Otimizações de bateria, modo econômico, restrições do fabricante,
desligamento do aparelho ou falta de rede **podem atrasar ou impedir a execução sem
aviso**. A execução manual está sempre disponível na tela de Perfil.

---

## Cláusula 8 — Cálculo de prazos: falibilidade declarada

Esta cláusula descreve o que a calculadora de prazos faz, o que ela não faz e o que
ela pode errar. O Usuário declara tê-la lido integralmente.

### 8.1. Fonte única e estados do cálculo

**8.1.1.** O Aplicativo **não implementa regras próprias de contagem**. Toda data-limite
é calculada pela API CalendárioForense, a partir da sigla do tribunal, da data de
disponibilização, da quantidade e da unidade do prazo.

**8.1.2.** Cada sugestão de prazo é exibida em um destes estados:

| Estado | Significado exato |
| --- | --- |
| **Confiável** | A API reconheceu o tribunal e devolveu a data sem ressalva. **Não é garantia de acerto** — significa apenas que o cálculo foi concluído sem sinalização de ambiguidade. |
| **Incerto** | A API sinalizou ambiguidade, ou o prazo foi convertido de meses para dias, ou a data decorre de inferência do Aplicativo. Exige conferência. |
| **Pendente** | Não houve cálculo: o tribunal não foi identificado, a API estava indisponível ou a resposta não permitiu concluir. **Não há data.** O Aplicativo tentará novamente no próximo ciclo, sem aviso ao Usuário. |

### 8.2. Termo inicial presumido

**8.2.1.** O Aplicativo presume, **em todos os casos**, que a intimação se deu pela
publicação no DJEN e adota como data-base a data de disponibilização informada pela
fonte, deixando à API a aplicação da regra dos §§ 3º e 4º do art. 4º da Lei nº
11.419/2006 e do art. 224, §§ 2º e 3º, do CPC.

**8.2.2.** Essa presunção **é falsa** sempre que a intimação tenha ocorrido por outra
via — pessoal, por carga, por mandado, por AR, pelo Domicílio Judicial Eletrônico,
pelo portal do tribunal ou em audiência. Nesses casos o termo inicial real é outro e
**a data exibida estará errada**.

### 8.3. O que a calculadora NÃO aplica

O Usuário declara ciência expressa de que o Aplicativo **não identifica e não aplica**:

a) **prazo em dobro** de qualquer espécie — Fazenda Pública (art. 183 do CPC),
   Ministério Público (art. 180), Defensoria Pública (art. 186), litisconsortes com
   procuradores distintos em autos físicos (art. 229) ou escritórios de prática
   jurídica. O pedido enviado à API declara sempre parte comum, com multiplicador 1;
b) prazos próprios de processo penal por tipo de ato, além da contagem em dias
   corridos (art. 798 do CPP): o Aplicativo **não infere** prazo penal a partir do
   tipo de comunicação;
c) suspensão ou interrupção do processo (arts. 313, 314 e 921 do CPC), sobrestamento
   por tema repetitivo ou repercussão geral, suspensão por convenção das partes
   (art. 190 e art. 313, II);
d) portarias, provimentos e decisões locais que suspendam expediente forense ou
   prazos em determinado juízo, comarca, foro ou tribunal;
e) feriados locais, pontos facultativos e recessos que não constem do calendário da
   API para o tribunal informado;
f) devolução, restituição, prorrogação ou dilação de prazo deferida no caso concreto;
g) impedimento por obstáculo criado pela parte contrária, indisponibilidade do
   sistema ou justa causa (art. 223 do CPC);
h) prazos contratuais, administrativos, prescricionais, decadenciais, trabalhistas
   próprios da CLT, eleitorais ou de qualquer natureza que não decorra da comunicação
   processual analisada.

### 8.4. Como o prazo é extraído do texto

**8.4.1.** O Aplicativo procura no texto expressões numéricas de prazo — "prazo de 15
dias", "no prazo de 5 dias úteis", "prazo de 48 horas". **Não reconhece** prazos por
extenso ("quinze dias"), remissões genéricas ("no prazo legal", "no prazo do art.
1.003"), prazos em quíntuplo, prazos condicionais ou prazos indicados apenas em
tabela ou imagem.

**8.4.2.** Prazo em **meses** é convertido em blocos de 30 dias corridos. É
aproximação, e por isso o resultado é sempre rebaixado a **Incerto**.

**8.4.3.** Quando o texto diz apenas "prazo de N dias", sem qualificar, o Aplicativo
adota dias **úteis** no cível (art. 219 do CPC) e dias **corridos** no penal (art.
798 do CPP). A natureza vem do processo cadastrado; **se não for possível determiná-la,
o Aplicativo assume cível**. Em processo penal não identificado como tal, a data
sairá errada.

### 8.5. Prazos inferidos, não declarados

**8.5.1.** Quando a comunicação não declara prazo, o Aplicativo pode sugerir um a
partir de dois mecanismos, ambos restritos ao cível:

a) **classificação da decisão** — decisões monocráticas denegatórias em tribunal
   ("não conheço do agravo", "nego seguimento ao recurso") geram sugestão de agravo
   interno (arts. 1.021 e 1.070 do CPC);
b) **tipo da comunicação** — citação, agravo, embargos de declaração, apelação e
   recurso inominado têm prazos padrão do CPC associados.

**8.5.2.** Toda sugestão desse tipo nasce **Incerta e não confirmada**. É hipótese de
trabalho sobre o recurso cabível, não parecer sobre cabimento, tempestividade,
preparo ou interesse recursal.

### 8.6. Disponibilidade da calculadora

**8.6.1.** A API CalendárioForense é executada em infraestrutura de plano gratuito,
que hiberna quando ociosa. A primeira chamada do dia pode demorar dezenas de segundos
e falhas esporádicas são esperadas. O Aplicativo repete a tentativa uma vez.

**8.6.2.** Falhando, o prazo fica **Pendente**, **sem data e sem notificação ao
Usuário**. Uma publicação com prazo real pode, portanto, permanecer no Aplicativo sem
qualquer data-limite exibida por tempo indeterminado.

### 8.7. Prazo cadastrado manualmente

O Usuário pode informar quantidade de dias e regime de contagem. A data-limite
continua sendo calculada pela API, e a confiança exibida reflete a declaração do
próprio Usuário. O cadastro manual substitui a sugestão automática e cancela, em
regime de melhor esforço, o evento de calendário anteriormente criado.

---

## Cláusula 9 — O silêncio do Aplicativo não é informação

**9.1.** O Usuário declara ciência de que **nenhuma das situações abaixo significa
que não há publicação, movimentação ou prazo**:

a) a lista de publicações estar vazia;
b) o processo não exibir movimentos novos;
c) a agenda não exibir prazo para determinada data;
d) uma publicação aparecer sem data-limite;
e) nenhuma notificação ter sido recebida.

**9.2.** Cada uma dessas situações é igualmente compatível com falha de captura,
indisponibilidade de fonte, divergência de cadastro, corte por volume, ausência do
órgão no DJEN, restrição do sistema operacional à execução em segundo plano ou erro
do próprio Aplicativo.

**9.3.** O acompanhamento de prazos por fonte oficial permanece **integralmente** a
cargo do Usuário, independentemente do uso do Aplicativo.

---

## Cláusula 10 — Notificações e alertas

**10.1.** O Aplicativo pode emitir notificações sobre novas publicações, publicações
com prazo, publicações sigilosas, processos novos, movimentações e prazos a vencer.

**10.2.** O alerta de prazo a vencer considera **apenas os dois dias seguintes** e só
alcança prazos que tenham data-limite calculada e registrada. Prazos em estado
Pendente **não geram alerta**.

**10.3.** A entrega de notificações depende de permissão do sistema, de conexão, de
o aparelho estar ligado e das políticas de economia de energia do fabricante. O
Desenvolvedor **não assegura** entrega, pontualidade ou visualização de qualquer
notificação.

**10.4.** O Aplicativo não é serviço de alarme. Não use notificação como único
controle de prazo.

---

## Cláusula 11 — Agenda e integração com Google Calendar

**11.1.** A integração é opcional e depende de autorização expressa do Usuário na
conta Google, com o escopo solicitado no momento da conexão.

**11.2.** Ao confirmar um prazo, o Usuário escolhe registrá-lo apenas no Aplicativo
ou também no Google Calendar. O evento criado contém o número do processo e o texto
descritivo do prazo — informação que passa a existir na conta Google do Usuário e a
se sujeitar aos termos e à política de privacidade do Google.

**11.3.** A sincronização é **unidirecional**: do Aplicativo para o calendário.
Alterar, mover ou excluir o evento no Google Calendar **não altera** o prazo no
Aplicativo.

**11.4.** Falhas de criação de evento são tratadas em regime de melhor esforço, com
nova tentativa em segundo plano. O prazo permanece registrado no Aplicativo ainda que
o evento externo nunca seja criado.

**11.5.** A integração com Microsoft Outlook **não está disponível** nesta versão,
ainda que o nome do provedor apareça em partes da interface.

---

## Cláusula 12 — Minutas geradas por inteligência artificial

Esta cláusula rege a funcionalidade de maior risco do Aplicativo.

**12.1. Como funciona.** A pedido do Usuário, o Aplicativo envia à API Obiter-Minuta
o arquivo PDF de uma peça processual. O serviço extrai a estrutura do documento e,
em etapa seguinte, gera um rascunho de peça de resposta por meio de **modelo de
inteligência artificial operado por terceiro**.

**12.2. Envio deliberado.** Nenhum documento é enviado automaticamente. O envio
depende de ação afirmativa do Usuário, arquivo por arquivo, limitado a 20 MB.

**12.3. Bloqueio por sigilo.** O Aplicativo recusa o envio de peça vinculada a
processo cadastrado com nível de sigilo maior que zero. **Esse bloqueio depende do
cadastro estar correto** e não substitui a verificação do Usuário.

**12.4. Natureza do resultado.** O rascunho é **texto gerado por máquina**. Ele pode,
entre outros defeitos:

a) citar legislação, súmula, tema repetitivo ou precedente **inexistente, revogado
   ou impertinente**;
b) atribuir a decisões conteúdo que elas não têm;
c) inverter polos, confundir partes, trocar valores, datas e números de processo;
d) omitir preliminar, prejudicial, prescrição, decadência ou tese decisiva;
e) formular pedido incompatível com o rito, com a competência ou com o interesse do
   cliente;
f) reproduzir vieses e imprecisões do material de origem;
g) apresentar índice de confiança elevado em trecho materialmente errado.

**12.5. Dever de revisão.** O rascunho é ponto de partida. O Usuário obriga-se a
**revisar integralmente** o texto, conferir toda citação normativa e jurisprudencial
na fonte primária e adequá-lo ao caso antes de qualquer uso. Nenhum texto gerado deve
ser protocolado sem revisão.

**12.6. Responsabilidade profissional.** A peça protocolada é ato privativo de
advogado e da exclusiva responsabilidade de quem a subscreve, inclusive quanto a
litigância de má-fé, dever de lealdade, veracidade e responsabilidade disciplinar
perante a OAB. O Desenvolvedor não responde, em nenhuma hipótese, pelo conteúdo de
peça protocolada.

**12.7. Sigilo profissional.** O envio de peça a serviço externo implica trânsito de
informação coberta por sigilo profissional por infraestrutura de terceiros. Cabe ao
Usuário, antes de cada envio, avaliar a compatibilidade com seus deveres de sigilo
(art. 7º, XIX, e art. 34, VII, da Lei nº 8.906/1994 e Código de Ética e Disciplina da
OAB) e a necessidade de anuência do cliente. **O Aplicativo não faz essa avaliação
pelo Usuário.**

**12.8. Tratamento pelo serviço.** O serviço informa descartar o arquivo PDF após o
processamento e devolve, para cada resultado, o modelo de IA utilizado, a versão do
conversor e o resumo de auditoria. O resultado da extração expira após a primeira
leitura ou em duas horas. As condições de retenção pelo provedor de IA constam da
Política de Privacidade.

**12.9. Autoria e titularidade.** O Usuário é titular do texto que produzir a partir
do rascunho. O Desenvolvedor não reivindica direitos sobre peças, minutas ou
documentos do Usuário. O Usuário declara ter direito de enviar o material que
submeter.

---

## Cláusula 13 — Cadastro de clientes e qualificação por extenso

**13.1.** Os dados de clientes, partes e representantes são inseridos ou confirmados
pelo Usuário, ou importados das bases descritas na Cláusula 5. O Aplicativo **não
valida** CPF, CNPJ, endereço, estado civil, profissão ou poderes de representação.

**13.2.** A qualificação por extenso é montada mecanicamente a partir dos campos
preenchidos e **omite silenciosamente** os campos vazios. Conferir o texto antes de
usá-lo em peça é ônus do Usuário.

**13.3.** O Usuário declara ter base legal para inserir no Aplicativo dados pessoais
de terceiros e responde perante esses titulares por esse tratamento, na forma da
Política de Privacidade.

---

## Cláusula 14 — Sincronização em nuvem

**14.1.** A sincronização com o Firebase Firestore **nasce desativada** e só ocorre
mediante ativação expressa do Usuário, em cada Dispositivo.

**14.2.** Ativada, sobem para a nuvem processos, publicações, movimentos,
participantes, clientes, vínculos, prazos e minutas, vinculados ao identificador da
Conta. Publicações marcadas como sigilosas e os prazos a elas vinculados **são
filtrados e não sobem** — observada a limitação da heurística descrita em 7.6.

**14.3.** A sincronização é recurso de continuidade entre dispositivos, **não é
serviço de backup**. O Desenvolvedor não assegura integridade, completude,
disponibilidade ou possibilidade de restauração dos dados replicados.

**14.4.** Sem sincronização ativa, os dados existem apenas no Dispositivo. Desinstalar
o Aplicativo, apagar seus dados, sair da Conta, perder ou trocar o aparelho implica
**perda definitiva**. O backup do sistema Android está desativado por decisão de
segurança, e os dados do Aplicativo não são transferidos entre aparelhos por essa via.

---

## Cláusula 15 — Exportação e compartilhamento

**15.1.** O relatório exportado é texto simples, gerado a pedido do Usuário, que pode
conter dados pessoais de terceiros, teor de publicações e datas-limite.

**15.2.** Publicações marcadas como sigilosas têm o texto suprimido no relatório,
permanecendo apenas a identificação e a marca de sigilo — sujeito à mesma limitação
de 7.6.

**15.3.** A partir do compartilhamento, o conteúdo passa ao aplicativo de destino
escolhido pelo Usuário e sai do controle do Desenvolvedor. A adequação do
compartilhamento é responsabilidade exclusiva do Usuário.

---

## Cláusula 16 — Obrigações do Usuário

O Usuário obriga-se a:

a) **conferir na fonte oficial** toda publicação, movimentação e data-limite antes de
   praticar ou deixar de praticar qualquer ato processual;
b) manter controle de prazos independente do Aplicativo;
c) manter corretos os dados de inscrição na OAB e o cadastro dos processos,
   especialmente tribunal, natureza e nível de sigilo, dos quais dependem o cálculo e
   os bloqueios de segurança;
d) revisar integralmente qualquer texto gerado pelo Aplicativo antes de utilizá-lo;
e) avaliar, antes de cada envio a serviço externo ou de cada compartilhamento, a
   compatibilidade com o sigilo profissional e com a legislação de proteção de dados;
f) manter o Dispositivo e a Conta seguros;
g) responder pelos dados pessoais de terceiros que inserir, na condição de
   controlador desse tratamento;
h) usar o Aplicativo em conformidade com a lei, com o Estatuto da Advocacia e com o
   Código de Ética e Disciplina da OAB.

---

## Cláusula 17 — Vedações

É vedado ao Usuário:

a) descompilar, desmontar, aplicar engenharia reversa ou tentar extrair o
   código-fonte, chaves ou credenciais do Aplicativo;
b) acessar as APIs de retaguarda do Desenvolvedor por meio diverso do Aplicativo, ou
   automatizar chamadas em volume incompatível com o uso pessoal;
c) revender, sublicenciar, alugar ou disponibilizar o Aplicativo a terceiros, ou
   compartilhar credenciais de Conta;
d) usar o Aplicativo para coleta massiva de dados pessoais, formação de cadastros,
   enriquecimento de bases ou qualquer finalidade estranha à atividade advocatícia;
e) contornar os limites de consulta, de sigilo ou de volume implementados no
   Aplicativo;
f) empregar o Aplicativo para prática de ato ilícito, assédio processual ou violação
   de segredo de justiça;
g) remover, ocultar ou alterar avisos, marcas ou indicações de autoria.

O descumprimento autoriza a suspensão ou o encerramento imediato da Conta, sem
prejuízo das medidas cabíveis.

---

## Cláusula 18 — Propriedade intelectual

**18.1.** O Aplicativo, suas APIs, interfaces, marcas, textos e bases de regras são de
titularidade do Desenvolvedor, protegidos pela Lei nº 9.610/1998 e pela Lei nº
9.609/1998. Estes Termos concedem licença de uso, não transferem propriedade.

**18.2.** Os dados, documentos e textos produzidos pelo Usuário permanecem de sua
titularidade. O Desenvolvedor os acessa apenas na medida necessária à prestação das
funcionalidades contratadas, na forma da Política de Privacidade.

**18.3.** O conteúdo obtido das bases públicas do CNJ sujeita-se às condições de uso
dessas fontes.

---

## Cláusula 19 — Disponibilidade, versão e evolução

**19.1.** O Aplicativo encontra-se em **estágio inicial de desenvolvimento** e é
fornecido **no estado em que se encontra** (*as is*), sem garantia de funcionamento
ininterrupto, ausência de erros ou adequação a finalidade específica, no limite
permitido pela legislação aplicável.

**19.2.** Não há nível de serviço assegurado, janela de manutenção comunicada
previamente, tempo de resposta a incidentes ou compromisso de suporte.

**19.3.** O Desenvolvedor pode, a qualquer tempo, alterar, suspender ou descontinuar
funcionalidades, integrar ou remover fontes de dados e modificar critérios de cálculo,
classificação e sugestão. Alterações relevantes serão comunicadas no próprio
Aplicativo.

**19.4.** Em caso de descontinuação definitiva, o Desenvolvedor envidará esforços
razoáveis para avisar com **30 dias** de antecedência e disponibilizar meio de
exportação dos dados.

---

## Cláusula 20 — Preço

**20.1.** Nesta versão o Aplicativo é fornecido **sem cobrança**.

**20.2.** A eventual introdução de planos pagos será comunicada previamente e não
alcançará período já utilizado. A gratuidade atual não gera direito adquirido à
gratuidade futura.

---

## Cláusula 21 — Limitação de responsabilidade

**21.1.** O Usuário reconhece que o Aplicativo é **ferramenta auxiliar** e que a
decisão profissional, o controle de prazos e a prática dos atos processuais são
exclusivamente seus.

**21.2.** No limite máximo permitido pela legislação aplicável, o Desenvolvedor não
responde por:

a) **perda, decurso ou intempestividade de prazo**, qualquer que seja a causa,
   inclusive falha de captura, erro de cálculo, ausência de sugestão, estado
   Pendente, notificação não entregue ou indisponibilidade do Aplicativo ou de
   qualquer das fontes;
b) conteúdo, forma, tempestividade ou consequência de peça elaborada a partir de
   rascunho gerado pelo Aplicativo;
c) decisão tomada com base em informação exibida pelo Aplicativo sem conferência na
   fonte oficial;
d) erro, omissão, atraso ou indisponibilidade das bases do CNJ, do ViaCEP, dos
   serviços Google ou de qualquer terceiro;
e) restrições impostas pelo sistema operacional ou pelo fabricante do Dispositivo à
   execução em segundo plano e às notificações;
f) perda de dados decorrente de desinstalação, limpeza de dados, saída da Conta,
   troca de aparelho ou não ativação da sincronização;
g) uso do Aplicativo por quem não seja advogado habilitado, ou fora da finalidade
   prevista na Cláusula 2;
h) danos indiretos, lucros cessantes, perda de chance, dano à imagem ou dano
   reflexo.

**21.3.** Nas hipóteses em que a limitação do item 21.2 não for admitida pela
legislação aplicável, a responsabilidade do Desenvolvedor fica limitada ao total
efetivamente pago pelo Usuário pelo Aplicativo nos 12 meses anteriores ao fato.

**21.4.** As limitações desta cláusula **não alcançam** dolo, culpa grave, violação a
direitos da personalidade nem as hipóteses de responsabilidade que a lei declare
inafastáveis.

**21.5.** O Usuário declara utilizar o Aplicativo como insumo de sua atividade
profissional, não se qualificando como destinatário final. Caso, no caso concreto,
seja reconhecida a incidência do Código de Defesa do Consumidor, as disposições desta
cláusula aplicam-se apenas na extensão compatível com aquele diploma.

**21.6.** O Usuário obriga-se a manter o Desenvolvedor indene de pretensões de
terceiros — clientes, partes ou titulares de dados — decorrentes do uso que o Usuário
fizer do Aplicativo, do conteúdo que nele inserir e das peças que protocolar.

---

## Cláusula 22 — Proteção de dados

**22.1.** O tratamento de dados pessoais rege-se pela **Política de Privacidade do
ObiterJus**, que integra estes Termos para todos os efeitos.

**22.2.** Quanto aos dados pessoais de clientes, partes e terceiros inseridos ou
importados pelo Usuário, **o Usuário é o controlador** e o Desenvolvedor atua como
**operador**, nos termos do art. 5º, VI e VII, da Lei nº 13.709/2018. As condições
desse tratamento estão detalhadas na Política de Privacidade e valem como acordo de
tratamento entre as partes.

**22.3.** Quanto aos dados cadastrais da Conta e aos registros técnicos de
funcionamento, o Desenvolvedor é controlador.

---

## Cláusula 23 — Vigência, encerramento e exclusão de dados

**23.1.** Estes Termos vigem enquanto durar o uso do Aplicativo.

**23.2.** O Usuário pode encerrar a relação a qualquer tempo, saindo da Conta e
desinstalando o Aplicativo — o que apaga os dados locais.

**23.3.** A exclusão da Conta e dos dados eventualmente replicados na nuvem é
solicitada pelo canal da Cláusula 24 e atendida no prazo de **15 dias**, ressalvada a
retenção exigida por obrigação legal ou regulatória.

**23.4.** O Desenvolvedor pode suspender ou encerrar Conta em caso de violação destas
condições, de ordem judicial ou de risco à segurança da plataforma, com comunicação
ao Usuário sempre que possível.

**23.5.** Sobrevivem ao encerramento as cláusulas 17, 18, 21 e 25.

---

## Cláusula 24 — Alterações destes Termos e novo aceite

**24.1.** Estes Termos podem ser alterados. Cada versão recebe número e data de
vigência.

**24.2.** Alteração que modifique o objeto, as fontes consultadas, o funcionamento do
cálculo de prazos, o tratamento de dados ou o regime de responsabilidade exige
**novo aceite** do Usuário, que será solicitado no Aplicativo antes da continuidade
do uso.

**24.3.** O Aplicativo registra, para cada aceite, a versão aceita, a data e hora, o
identificador da Conta e a identificação do texto aceito. Esse registro serve como
prova do consentimento e fica disponível ao Usuário na tela de Perfil.

**24.4.** A recusa do novo aceite impede o uso das funcionalidades afetadas e faculta
ao Usuário solicitar a exclusão dos dados na forma da Cláusula 23.

---

## Cláusula 25 — Disposições finais

**25.1. Integralidade.** Estes Termos, a Política de Privacidade e os avisos exibidos
nas telas do Aplicativo formam o acordo integral entre as partes quanto ao objeto.

**25.2. Precedência.** Em caso de divergência, prevalece o texto destes Termos sobre
os avisos resumidos exibidos nas telas, que têm função informativa e de registro de
ciência.

**25.3. Independência das cláusulas.** A invalidade de uma disposição não afeta as
demais.

**25.4. Tolerância.** A tolerância quanto ao descumprimento não implica novação nem
renúncia.

**25.5. Cessão.** O Usuário não pode ceder sua posição. O Desenvolvedor pode cedê-la
em caso de reorganização societária, mediante comunicação prévia.

**25.6. Lei aplicável.** Aplica-se a legislação brasileira, em especial a Lei nº
13.709/2018, a Lei nº 12.965/2014, a Lei nº 9.609/1998 e o Código Civil.

---

## Cláusula 26 — Contato

| Assunto | Canal |
| --- | --- |
| Suporte, dúvidas e incidentes | `obiterjus@xablauinitiative.com.br` |
| Proteção de dados e direitos do titular | `obiterjus@xablauinitiative.com.br` |
| Exclusão de conta e de dados | `obiterjus@xablauinitiative.com.br` |

---

## Declaração de ciência do Usuário

Ao aceitar estes Termos, o Usuário declara, de forma específica e destacada, que:

1. compreendeu que o ObiterJus é **ferramenta de apoio sujeita a falhas**, não
   substituindo a conferência nos sistemas oficiais;
2. leu a Cláusula 6 e sabe **quais bases o Aplicativo não consulta**, em especial o
   Domicílio Judicial Eletrônico e os portais de intimação dos tribunais;
3. leu a Cláusula 8 e conhece as **limitações do cálculo de prazos**, inclusive a não
   aplicação de prazo em dobro e de suspensões processuais;
4. compreendeu a Cláusula 9: **a ausência de informação no Aplicativo não significa a
   inexistência do ato**;
5. assume a responsabilidade exclusiva pelo controle de prazos, pela revisão dos
   textos gerados por inteligência artificial e pelos atos processuais que praticar.

---

### Histórico de versões

| Versão | Data | Alterações |
| --- | --- | --- |
| 1.0 | `{{DATA_VIGENCIA}}` | Texto inicial. |
