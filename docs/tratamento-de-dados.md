# Registro de tratamento de dados — ObiterJus

Documento de trabalho exigido pelo art. 37 da LGPD. Descreve o que o app coleta,
onde guarda e para quem envia. Atualizar junto com qualquer mudança que altere o
fluxo de dados.

Última revisão: 13/08/2026.

## Titulares

Duas categorias, com expectativas bem diferentes:

- **O advogado usuário** — nome, e-mail, OAB, escritório, áreas de atuação.
  Fornecidos por ele no cadastro.
- **Terceiros** — partes, clientes e representantes legais que aparecem nas
  publicações e nos processos. Não têm relação com o app e não consentiram nada
  a ele. O tratamento se apoia no exercício regular de direitos em processo
  judicial (art. 7º, VI, e art. 11, II, "d") e na atividade advocatícia do
  controlador.

## O que fica no aparelho

Banco `obiterjus.db`, cifrado com SQLCipher; a senha é aleatória e vive no
Android Keystore (`CofreLocal`).

| Tabela | Conteúdo sensível |
| --- | --- |
| `publicacoes` | Inteiro teor das publicações do DJEN, incluindo as sigilosas |
| `processos` | Metadados processuais, valor da causa, nível de sigilo |
| `participantes` | Nome, CPF/CNPJ, endereço, telefone, e-mail das partes |
| `clientes` | Qualificação completa e dados do representante legal |
| `prazos_sugeridos`, `minutas`, `movimentos`, `sync_logs` | Derivados dos acima |

Preferências e identidade do advogado ficam no DataStore `obiter_settings`. O
access token do Google Calendar fica cifrado no `CofreLocal`.

Backup do sistema está desligado (`allowBackup="false"`) e a transferência entre
aparelhos é excluída explicitamente em `data_extraction_rules.xml`: nada disso
sai por backup do Google.

**Ao sair da conta**, `SessaoLocalRepository.limparTudo()` apaga banco,
preferências, token de calendário e cache de certidões. Entrar com um uid
diferente do que gravou a base dispara a mesma limpeza antes de qualquer
restauração.

## O que sai do aparelho

### Firestore (`users/{uid}/…`) — opcional

Só com `sincronizacaoNuvemAutorizada` ligada no Perfil, que **nasce desligada**.
Sobem processos, publicações, movimentos, participantes, clientes, vínculos,
prazos e minutas.

Publicações marcadas como sigilosas são filtradas no envio, e os prazos ligados
a elas também.

Acesso restrito pelas regras em `firestore.rules`: só o próprio uid, e sessão
anônima é recusada. As regras são versionadas neste repositório — alterar pelo
console sem trazer para cá é regressão.

### API Obiter-Minuta (`obiterjus.duckdns.org`) — sob ação do usuário

Recebe o PDF da peça que o advogado escolhe enviar e, depois, o texto integral
extraído. Processo com `nivelSigilo > 0` é bloqueado antes do upload
(`ExtrairPecaUC`). Teto de 20 MB por arquivo.

Autenticação por Firebase ID token. **Pendência conhecida:** esse token dá
acesso ao Firestore do usuário; a troca por um token de escopo restrito depende
de mudança no backend. Enquanto não houver, o servidor de minutas é um ponto de
confiança equivalente ao próprio app.

**Pendência conhecida:** sem certificate pinning, e o host está em DNS dinâmico
gratuito. Depende de migrar para domínio próprio.

**A confirmar com o backend:** o DTO de auditoria recebe `pdf_descartado: true`,
mas isso é afirmação do servidor. Verificar retenção real, política de logs e
qual provedor de IA processa o texto — e se há contrato de tratamento de dados
com ele.

### Consultas a serviços públicos

DJEN (`comunicaapi.pje.jus.br`) e DataJud (`api-publica.datajud.cnj.jus.br`)
recebem a OAB e o número do processo. São fontes públicas do CNJ. A URL vem do
Remote Config, restrita a `https` sob o domínio `jus.br` para que a credencial
não possa ser redirecionada.

CalendárioForense (`calendario-forense-br.onrender.com`) recebe tribunal, data e
prazo — sem identificação de parte.

ViaCEP recebe apenas o CEP digitado.

### Google Calendar — opcional

Com autorização do usuário, cria eventos com o número do processo e o texto do
prazo. Escopo pedido hoje: `auth/calendar` (leitura e escrita de todos os
calendários). **Pendência:** `calendar.events` bastaria.

## Retenção

- Certidões em PDF no cache: 24 h (`CertidaoDjenRepositoryImpl.limparCache`).
- Logs de sincronização: podados por `SyncLogDao.descartarAntigos()`.
- Banco local: até o usuário excluir o registro ou sair da conta.
- Firestore: enquanto a conta existir. **Pendência:** não há fluxo de exclusão de
  conta que apague os documentos remotos.

## Pendências abertas

1. Fluxo de exclusão de conta que remova os dados do Firestore.
2. Certificate pinning e domínio próprio para a API de minutas.
3. Token de escopo restrito no lugar do Firebase ID token.
4. Reduzir o escopo do OAuth do Google Calendar.
5. Confirmar retenção e subcontratados do serviço de minutas.
