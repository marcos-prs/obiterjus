# Política de Privacidade — ObiterJus

**Versão 1.1** · Vigente a partir de `{{DATA_VIGENCIA_V11}}` · Aplicável a partir da versão `0.0002` do aplicativo

---

## 1. Do que trata esta Política

Esta Política descreve como o ObiterJus trata dados pessoais: o que coleta, onde
guarda, para quem envia, por quanto tempo mantém e quais direitos podem ser exercidos.

Ela integra os [Termos e Condições de Uso](termos-de-uso.md) e usa os mesmos
conceitos definidos lá.

Um ponto merece destaque desde já: o ObiterJus trata dados de **duas categorias de
pessoas com situações muito diferentes** — o advogado que usa o aplicativo, e as
partes, clientes e terceiros que aparecem nas publicações e nos processos. Estes
últimos não têm relação com o aplicativo e não consentiram com nada a ele. A Seção 3
explica quem responde por cada um.

---

## 2. Quem trata os dados

| Papel | Quem | Contato |
| --- | --- | --- |
| **Controlador** dos dados de cadastro, autenticação e funcionamento | **ObiterJus** | `obiterjus@xablauinitiative.com.br` |
| **Operador** dos dados processuais e de clientes inseridos pelo Usuário | **ObiterJus** | `obiterjus@xablauinitiative.com.br` |
| **Controlador** dos dados processuais e de clientes inseridos pelo Usuário | O **advogado usuário** | — |
| **Encarregado (DPO)** | `{{NOME_TITULAR}}` | `obiterjus@xablauinitiative.com.br` |

---

## 3. Divisão de papéis — leia antes do resto

**3.1.** Quanto aos **dados do próprio advogado** (nome, e-mail, inscrição na OAB,
escritório, áreas de atuação, preferências, registros de acesso e de sincronização),
o Desenvolvedor é **controlador**: decide as finalidades e os meios do tratamento.

**3.2.** Quanto aos **dados de clientes, partes, representantes e terceiros** que o
Usuário insere no aplicativo ou que chegam pelas consultas ao DJEN e ao DataJud, quem
decide a finalidade é o advogado, no exercício do mandato. Nesse recorte:

- **o advogado é o controlador**;
- **o Desenvolvedor é o operador**, e trata esses dados apenas conforme as instruções
  do advogado, materializadas nas escolhas que ele faz no aplicativo (ativar a
  sincronização, enviar uma peça para geração de minuta, exportar um relatório).

**3.3.** Esta Política, no que toca ao item 3.2, vale como **acordo de tratamento de
dados** entre controlador e operador (art. 39 da LGPD). O Desenvolvedor obriga-se a:

a) tratar os dados somente para executar as funcionalidades solicitadas;
b) não usar esses dados para finalidade própria, não os comercializar, não os cruzar
   com outras bases e não os utilizar para treinar modelos de inteligência artificial
   próprios ou de terceiros;
c) manter as medidas de segurança da Seção 9;
d) auxiliar o advogado no atendimento a pedidos de titulares e da autoridade;
e) comunicar ao advogado, sem demora injustificada, qualquer incidente de segurança
   que envolva os dados sob sua responsabilidade;
f) eliminar os dados ao fim do tratamento, na forma da Seção 8.

**3.4.** Titular que não seja o advogado usuário — cliente, parte contrária,
representante — e queira exercer direitos sobre dados que estejam no ObiterJus deve,
em regra, dirigir-se ao **advogado responsável pelo processo**, que é quem detém a
relação jurídica e o contexto. Pedidos recebidos diretamente pelo Desenvolvedor serão
encaminhados ao advogado e respondidos no que couber ao operador.

---

## 4. Dados tratados

### 4.1. Dados do advogado usuário

| Dado | Origem | Para quê |
| --- | --- | --- |
| Nome, e-mail | Cadastro ou conta Google | Identificar a conta, autenticar, comunicar |
| Senha | Cadastro | Autenticação — armazenada e verificada exclusivamente pelo Firebase Authentication; o Desenvolvedor não tem acesso a ela |
| Número e UF da inscrição na OAB | Cadastro | Buscar comunicações no DJEN |
| Escritório, áreas de atuação | Cadastro | Personalizar a experiência |
| Preferências (tema, notificações, intervalo de busca, modo de busca) | Uso do aplicativo | Funcionamento |
| Registros de sincronização (data, duração, fonte, resultado, erro) | Execução | Diagnóstico e auditoria, exibidos ao Usuário na tela de Auditoria |
| Registro de aceite dos Termos (versão, data, identificador) | Aceite | Prova do consentimento e do dever de informação |
| Token de acesso ao Google Calendar | Autorização do Usuário | Criar eventos de prazo |

### 4.2. Dados de terceiros (clientes, partes, representantes)

| Dado | Origem |
| --- | --- |
| Nome, CPF/CNPJ, endereço, telefone, e-mail | Cadastro pelo Usuário, ViaCEP, DJEN, DataJud |
| Qualificação (nacionalidade, estado civil, profissão) e representante legal | Cadastro pelo Usuário |
| Inteiro teor das comunicações processuais | DJEN |
| Metadados do processo, movimentos, valor da causa, nível de sigilo | DataJud e cadastro |
| Conteúdo de peças processuais enviadas para geração de minuta | Envio deliberado pelo Usuário |

**Atenção:** o teor de comunicações e de peças processuais pode conter **dados
pessoais sensíveis** (art. 5º, II, da LGPD) — saúde, filiação sindical, convicção
religiosa, dado biométrico, orientação sexual — e dados relativos a processos
criminais. O aplicativo não filtra nem classifica esse conteúdo.

### 4.3. Dados que o aplicativo NÃO coleta

O ObiterJus **não coleta e não usa**:

a) localização, GPS, rede móvel ou Wi-Fi para fins de posicionamento;
b) contatos, agenda pessoal, fotos, arquivos além do PDF que o Usuário escolhe enviar;
c) microfone, câmera ou sensores;
d) identificadores de publicidade, cookies de rastreamento ou perfis comportamentais;
e) telemetria de uso, analytics de comportamento ou relatórios automáticos de falha —
   **não há Firebase Analytics nem Crashlytics** nesta versão;
f) histórico de navegação ou dados de outros aplicativos;
g) dados de pagamento — não há cobrança nesta versão.

O aplicativo declara duas permissões Android no próprio manifesto: **internet** e
**notificações**. As bibliotecas que ele usa acrescentam as permissões técnicas
necessárias para receber mensagens de push, manter o aparelho acordado durante a
busca e verificar o estado da rede. Nenhuma delas dá acesso a conteúdo do aparelho,
a localização ou a dados de outros aplicativos.

---

## 5. Bases legais

| Tratamento | Base legal (LGPD) |
| --- | --- |
| Cadastro, autenticação e prestação das funcionalidades ao advogado | Execução de contrato — art. 7º, V |
| Consultas ao DJEN e ao DataJud com a OAB do Usuário | Execução de contrato — art. 7º, V |
| Tratamento de dados de partes e terceiros contidos em publicações e processos | Exercício regular de direitos em processo judicial — art. 7º, VI, e, quanto a dados sensíveis, art. 11, II, "d" |
| Cadastro de clientes e qualificação | Execução de contrato ou procedimento preliminar entre o cliente e o advogado — art. 7º, V (base do advogado, na condição de controlador) |
| Sincronização em nuvem | **Consentimento** específico e destacado do Usuário — art. 7º, I — revogável a qualquer tempo |
| Envio de peça para geração de minuta | **Consentimento** específico, manifestado a cada envio — art. 7º, I |
| Integração com Google Calendar | **Consentimento** específico — art. 7º, I |
| Aviso diário no horário escolhido, sem identificação (item 6.3) | Execução de contrato — art. 7º, V |
| Pré-checagem no servidor, com OAB e nome do advogado (item 6.3) | **Consentimento** específico e destacado do Usuário — art. 7º, I — revogável a qualquer tempo |
| Registros de sincronização e de aceite dos Termos | Legítimo interesse em segurança, auditoria e prova do dever de informação — art. 7º, IX; cumprimento de obrigação legal — art. 7º, II |
| Segurança da aplicação e prevenção a fraude | Legítimo interesse — art. 7º, IX |

**5.1.** O consentimento para sincronização em nuvem, para envio de peças, para o
Google Calendar e para a pré-checagem no servidor é **independente**: recusar um não
impede o uso dos demais nem do restante do aplicativo. Recusar a pré-checagem não
desliga o aviso diário — ele continua funcionando sem identificação, na forma do item
6.3.

---

## 6. Onde os dados ficam

### 6.1. No aparelho — o padrão

O banco de dados local `obiterjus.db` é **cifrado com SQLCipher**. A chave é gerada
aleatoriamente e guardada no **Android Keystore**, fora do alcance de outros
aplicativos.

Ficam nele: publicações (inclusive as sigilosas), processos, participantes com dados
de qualificação, clientes, prazos, minutas, movimentos e registros de sincronização.
As preferências e a identidade profissional ficam no armazenamento de preferências do
aplicativo; o token do Google Calendar fica cifrado.

O **backup automático do Android está desativado** e a transferência entre aparelhos
é expressamente excluída. Nada disso sai por backup da conta Google.

**Ao sair da conta**, todos esses dados são apagados do aparelho — banco, preferências,
token de calendário e cache de certidões. Entrar com uma conta diferente da que gravou
a base dispara a mesma limpeza antes de qualquer restauração.

### 6.2. Na nuvem — apenas com autorização

Somente se o Usuário ativar a sincronização (que **nasce desativada**), os dados do
acervo são replicados no **Firebase Firestore**, em documentos vinculados ao
identificador da conta.

O acesso é restrito pelas regras de segurança do projeto: cada conta lê e escreve
apenas os próprios documentos, e **sessão anônima é recusada**. Publicações marcadas
como sigilosas e os prazos a elas vinculados são **filtrados no envio** — observada a
limitação da detecção heurística descrita nos Termos de Uso (cláusula 7.6).

### 6.3. O aviso da manhã — como o aplicativo é acordado

O Android não garante horário para tarefas em segundo plano: em certos estados de
economia de bateria a rede em segundo plano é desligada, e a busca da manhã escorrega
até o Usuário abrir o aplicativo. Por isso quem acorda o aplicativo é um servidor do
Desenvolvedor (**API Obiter-Push**), por mensagem do Firebase Cloud Messaging.

**Aviso sem identificação — o padrão.** O aparelho se inscreve em um tópico do
Firebase cujo nome carrega apenas o horário e o fuso (`sync-diaria-h07-utcm180`).
Não há identificação do aparelho nem do advogado, não há cadastro e não há banco de
dados: **nenhum dado sai do aparelho para servidor do Desenvolvedor** neste caminho, e
o servidor não sabe quantos aparelhos escutam nem de quem são. A mensagem não carrega
conteúdo — ela apenas manda o aplicativo consultar o DJEN por conta própria.

**Pré-checagem no servidor — apenas com autorização.** Opcional, **nasce desativada** e
só é ligada por escolha expressa no Perfil. Com ela ativa, o aparelho envia à API
Obiter-Push, a cada abertura do aplicativo:

a) um identificador aleatório da instalação, gerado no próprio aparelho e sem relação
   com a conta, com o e-mail ou com qualquer identificador do dispositivo;
b) o número e a UF da OAB e o nome do advogado;
c) o horário e o fuso em que o aviso deve sair, e o modo de busca escolhido;
d) um número inteiro: o maior identificador de publicação que aquele aparelho já viu
   no DJEN.

Com isso, o servidor consulta o DJEN pela OAB **em nome do Usuário** e só envia o
aviso quando há publicação nova. É o que evita avisar em dias sem movimento.

**O que isso muda, dito sem rodeio:**

1. Um servidor do Desenvolvedor passa a guardar OAB, UF e nome do advogado. São dados
   que já são enviados ao DJEN hoje, mas que até aqui não ficavam em servidor do
   Desenvolvedor — e que agora dizem **de quem** é cada registro.
2. Esse servidor passa a saber **em que dias aquela OAB tem publicação**. É informação
   sobre a rotina do escritório, ainda que qualquer pessoa possa obter a mesma coisa
   consultando o Diário, que é público.
3. A resposta do DJEN é lida apenas para extrair o maior identificador e **descartada**.
   Só o número inteiro é gravado. Nenhum conteúdo de publicação é armazenado no
   servidor.
4. O aviso enviado ao aparelho **não carrega conteúdo nem contagem**. Quem lê a
   publicação, classifica e decide o que notificar continua sendo o aplicativo, no
   aparelho.
5. O nome do tópico de entrega é derivado da OAB por função criptográfica
   (HMAC-SHA256), de modo que o número da OAB não transita nos metadados do Firebase.

**Revogação.** Desligar a opção no Perfil, desligar a sincronização automática ou
excluir a conta apaga o registro no servidor: o aparelho pede a exclusão da linha
correspondente, identificada pelo identificador aleatório da instalação. A partir daí
o servidor volta a não guardar nada a respeito do Usuário, e o aviso sem identificação
continua funcionando.

---

## 7. Com quem os dados são compartilhados

O ObiterJus **não vende, não aluga e não cede** dados pessoais. O compartilhamento
ocorre apenas com os operadores e fontes abaixo, na medida necessária.

| Destinatário | O que recebe | Quando | País |
| --- | --- | --- | --- |
| **CNJ — DJEN / API Comunica** | Número e UF da OAB, ou nome do advogado, e intervalo de datas | A cada sincronização | Brasil |
| **CNJ — DataJud** | Número do processo e sigla do tribunal | Ao sincronizar processos | Brasil |
| **API CalendárioForense** (Desenvolvedor, hospedada na Render) | Sigla do tribunal, data de disponibilização, prazo, natureza. **Sem identificação de parte, de processo ou de advogado** | A cada cálculo de prazo | Exterior — Estados Unidos |
| **API Obiter-Minuta** (Desenvolvedor, hospedada na Oracle Cloud) | PDF da peça, texto integral extraído, número do processo e vara quando informados, token de autenticação | Somente a cada envio deliberado pelo Usuário | `{{REGIAO_OBITER_MINUTA}}` |
| **Google — Gemini API** (provedor de IA contratado pela API Obiter-Minuta) | Texto da peça, para extração e geração do rascunho | No processamento do envio | Exterior — infraestrutura global do Google (Estados Unidos) |
| **API Obiter-Push** (Desenvolvedor, hospedada na Oracle Cloud) | Identificador aleatório da instalação, número e UF da OAB, nome do advogado, horário e fuso, modo de busca e o maior identificador de publicação já visto pelo aparelho | Somente com a pré-checagem ativada (item 6.3), a cada abertura do aplicativo | `{{REGIAO_OBITER_PUSH}}` |
| **Google — Firebase Cloud Messaging** | A inscrição do aparelho em tópicos: horário, fuso e, com a pré-checagem ativa, um código derivado da OAB por função criptográfica. **Não recebe o número da OAB, o nome do advogado nem conteúdo de publicação** | Enquanto o aviso diário estiver ativo | Exterior — infraestrutura global do Google (Estados Unidos) |
| **Google — Firebase Authentication** | E-mail, credenciais, registros de acesso | Sempre que há conta | Exterior — infraestrutura global do Google (Estados Unidos) |
| **Google — Firebase Firestore** | Acervo sincronizado | Somente com sincronização ativa | Brasil — `southamerica-east1` (São Paulo) |
| **Google — Firebase Remote Config** | Nenhum dado pessoal; apenas recebe parâmetros | Na inicialização | Exterior |
| **Google — Google Calendar** | Título, descrição e data do prazo | Somente com autorização e a cada prazo confirmado com esse destino | Exterior |
| **ViaCEP** | Apenas o CEP digitado | Ao preencher endereço | Brasil |
| **Aplicativo escolhido pelo Usuário** | Conteúdo do relatório exportado | Ao compartilhar | Depende da escolha |
| **Autoridades** | O que for exigido | Mediante ordem judicial ou requisição legal | Brasil |

**7.1. Transferência internacional.** Parte da infraestrutura está no exterior. As
transferências se apoiam no art. 33, II, "d" (necessidade para execução de contrato) e
IX (consentimento específico e destacado, no caso da sincronização e da geração de
minutas) da LGPD, com adoção de cláusulas contratuais e garantias oferecidas pelos
respectivos provedores.

**7.2. O que nunca é enviado sem ação do Usuário.** Nenhum documento, nenhuma peça e
nenhum dado de cliente sai do aparelho automaticamente. A sincronização em nuvem exige
ativação; o envio para geração de minuta exige seleção de arquivo e confirmação; a
integração de calendário exige autorização na conta Google; o envio da OAB e do nome
à API Obiter-Push exige ativar a pré-checagem no Perfil, que nasce desativada.

---

## 8. Por quanto tempo os dados ficam

| Dado | Prazo |
| --- | --- |
| Certidões de publicação em PDF no cache | 24 horas; apagadas integralmente ao sair da conta |
| Resultado da extração de peça no serviço de minutas | Expira na primeira leitura ou em 2 horas |
| Arquivo PDF enviado ao serviço de minutas | Descartado após o processamento, conforme informado pelo serviço |
| Registros de sincronização | Podados automaticamente, mantendo janela recente para auditoria |
| Banco de dados local | Até o Usuário excluir o registro, sair da conta, excluir a conta ou desinstalar |
| Dados no Firestore | Enquanto a conta existir e a sincronização estiver ativa; apagados na hora na exclusão feita no aplicativo, ou em até 15 dias no pedido por escrito |
| Dados de cadastro e de autenticação | Enquanto a conta existir; registros de acesso mantidos pelo prazo legal aplicável |
| Registro da pré-checagem na API Obiter-Push | Enquanto a opção estiver ativa; apagado quando o Usuário a desliga, desliga a sincronização automática ou exclui a conta |
| Registro de aceite dos Termos | Mantido enquanto puder ser exigido como prova do dever de informação, ainda que a conta seja excluída |

**8.1.** Desativar a sincronização interrompe novos envios, mas **não apaga** o que já
foi replicado. Para apagar o que está na nuvem, exclua a conta na forma do item 10.3.

---

## 9. Segurança

Medidas adotadas:

- banco de dados local **cifrado** (SQLCipher), com chave no Android Keystore;
- **backup do sistema desativado** e transferência entre aparelhos bloqueada;
- todo tráfego em **HTTPS**, com política de segurança de rede declarada no aplicativo;
- credenciais das bases do CNJ enviadas **exclusivamente** para endereços sob o
  domínio `jus.br`, verificado a cada chamada — mesmo que a configuração remota seja
  alterada;
- regras de acesso na nuvem restritas ao próprio identificador da conta, com recusa de
  sessão anônima;
- limpeza integral dos dados locais na saída da conta e na troca de titular no aparelho;
- bloqueio de envio de peça vinculada a processo cadastrado como sigiloso;
- filtragem de publicações sigilosas antes da sincronização em nuvem;
- código ofuscado e minificado na versão de distribuição.

**9.1.** Nenhuma medida elimina o risco. Não há sistema absolutamente seguro. O
Usuário contribui mantendo o aparelho com bloqueio de tela, sistema atualizado e sem
modificações de root.

**9.2. Incidentes.** Havendo incidente de segurança com risco relevante, o
Desenvolvedor comunicará os titulares afetados e a Autoridade Nacional de Proteção de
Dados em prazo razoável, na forma do art. 48 da LGPD, e — quanto aos dados de que é
operador — comunicará o advogado controlador sem demora injustificada.

---

## 10. Direitos do titular

Nos termos do art. 18 da LGPD, o titular pode requerer:

a) confirmação da existência de tratamento;
b) acesso aos dados;
c) correção de dados incompletos, inexatos ou desatualizados;
d) anonimização, bloqueio ou eliminação de dados desnecessários, excessivos ou
   tratados em desconformidade;
e) portabilidade;
f) eliminação dos dados tratados com base no consentimento;
g) informação sobre com quem os dados foram compartilhados;
h) informação sobre a possibilidade de não consentir e as consequências da recusa;
i) revogação do consentimento.

**10.1. Como exercer.** Pelo e-mail `obiterjus@xablauinitiative.com.br`. A resposta é dada em
até **15 dias**. Pode ser necessário confirmar a identidade do requerente antes do
atendimento.

**10.2. No próprio aplicativo.** O Usuário pode, sem pedido formal: editar seus dados
de perfil, excluir processos, clientes e minutas, desativar a sincronização em nuvem,
revogar a autorização do Google Calendar, desativar notificações, apagar todos os
dados locais saindo da conta e **excluir a própria conta**.

**10.3. Exclusão da conta.** Pode ser feita de duas formas:

a) **No aplicativo**, em *Perfil* → menu da conta → *Excluir conta*. A eliminação é
   **imediata e definitiva**: apaga o acervo guardado na nuvem, os dados deste
   aparelho e a própria conta de acesso. Exige a senha, para que não baste ter o
   aparelho destravado em mãos.

b) **Por escrito**, pelo e-mail `obiterjus@xablauinitiative.com.br` ou pela página
   `https://obiterjus.duckdns.org/exclusao-de-conta`, quando o Usuário não tiver mais
   acesso ao aplicativo. A eliminação dos dados na nuvem se dá em até **15 dias**, e
   pode ser necessário confirmar a identidade do requerente.

Em qualquer das duas formas fica ressalvada a retenção tratada no item 10.4 e na
Seção 8.

**10.4. Limites.** Alguns dados não podem ser eliminados a pedido: os necessários ao
cumprimento de obrigação legal, ao exercício regular de direitos em processo, e os
registros de aceite dos Termos, mantidos como prova.

**10.5. Reclamação.** O titular pode reclamar à Autoridade Nacional de Proteção de
Dados (ANPD).

---

## 11. Crianças e adolescentes

O aplicativo não se destina a menores de 18 anos e não coleta dados de crianças e
adolescentes deliberadamente. Dados de menores podem, contudo, constar de publicações
e processos tratados pelo advogado — hipótese em que o tratamento se dá no interesse
do próprio menor, sob a responsabilidade do advogado controlador, na forma do art. 14
da LGPD e da legislação processual.

---

## 12. Decisões automatizadas

**12.1.** O aplicativo executa processamentos automatizados: classificação de
publicações, cálculo de prazos, detecção de sigilo, classificação de confiança de
vínculo e geração de rascunhos por inteligência artificial.

**12.2.** Nenhum desses processamentos produz decisão com efeito jurídico sobre
titulares de dados. Todos geram **sugestões submetidas à decisão do advogado**, que
pode aceitá-las, alterá-las ou descartá-las.

**12.3.** Os critérios estão descritos nos Termos de Uso, cláusulas 7, 8 e 12. O
titular pode solicitar revisão e informações adicionais pelo canal da Seção 10,
observados o segredo comercial e industrial (art. 20, § 1º, da LGPD).

---

## 13. Alterações desta Política

**13.1.** Esta Política pode ser alterada. Cada versão recebe número e data.

**13.2.** Alteração que amplie as finalidades, inclua novo destinatário de dados ou
modifique a base legal de qualquer tratamento será comunicada no aplicativo e, quando
a base for o consentimento, dependerá de **nova manifestação** do Usuário.

**13.3.** O histórico de versões fica ao final deste documento e disponível na tela de
Perfil do aplicativo.

---

## 14. Contato

| Assunto | Canal |
| --- | --- |
| Encarregado pelo tratamento de dados (DPO) | `{{NOME_TITULAR}}` — `obiterjus@xablauinitiative.com.br` |
| Exercício de direitos do titular | `obiterjus@xablauinitiative.com.br` |
| Exclusão de conta e de dados | `obiterjus@xablauinitiative.com.br` |
| Incidentes de segurança | `obiterjus@xablauinitiative.com.br` |

---

### Histórico de versões

| Versão | Data | Alterações |
| --- | --- | --- |
| 1.0 | `{{DATA_VIGENCIA}}` | Texto inicial. |
| 1.1 | `{{DATA_VIGENCIA_V11}}` | Descreve o aviso diário por push (item 6.3): o caminho sem identificação, que é o padrão, e a pré-checagem no servidor, opcional e desativada de origem, em que a API Obiter-Push passa a receber OAB, UF e nome do advogado e a consultar o DJEN em nome do Usuário. Acrescenta a API Obiter-Push e o Firebase Cloud Messaging à tabela do item 7, a base legal do item 5 e o prazo de retenção do item 8. Precisa as permissões Android no item 4.3. |
