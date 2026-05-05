# ObiterJus MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** levar o ObiterJus a um MVP funcional, com login/cadastro, tela de perfil realmente operante, feedback de acoes, e os fluxos principais sem fios soltos.

**Architecture:** manter a arquitetura atual orientada a estado com Compose, ViewModels e Koin, mas fechar os contratos entre UI, eventos e persistencia. O MVP vai priorizar os fluxos que o advogado usa todo dia: entrar ou criar conta, configurar OAB, sincronizar, ler publicacoes, acompanhar prazos e navegar sem telas mortas.

**Tech Stack:** Jetpack Compose, Navigation Compose, Material 3, Koin, Room, DataStore, Firebase Auth, WorkManager, SnackbarHostState.

---

## Assumptions

- MVP = conjunto minimo que permite uso real do app sem telas mortas.
- O fluxo de autenticacao aceitavel no MVP inclui `anonymously`, email/senha e cadastro novo.
- Google/Outlook calendar continuam como integracao posterior, mas a UI precisa refletir o estado real da sync.
- O que nao for necessario para uso diario fica fora do MVP.

## MVP Scope

### Must have

- Perfil funcional com sync, logout, entrar e feedback visual.
- Cadastro de novo usuario com validacao, loading e erro.
- Snackbars globais para sucesso, erro e retry.
- Estados consistentes de loading, empty e error em telas principais.
- Navegacao sem itens mortos.
- Persistencia de preferencias basicas e estado de autenticacao.

### Should have

- Estados vazios e de erro com tratamento por tela.
- Conteudo de dashboard e listas sem CTA quebrado.
- Acoes criticas com resposta imediata ao usuario.

### Out of scope for MVP

- Refatoracao visual completa.
- Funcionalidades experimentais.
- Automacoes adicionais nao ligadas ao fluxo diario.

## Decision Log

- Decidido: tratar o MVP como "fluxo principal completo" e nao como "acabou de compilar".
  - Alternativas: continuar empilhando features; parar e polir o essencial.
  - Motivo: o problema atual e de coesao, nao de volume.
- Decidido: usar o stack ja existente em vez de reescrever a base.
  - Alternativas: trocar arquitetura; adicionar novas camadas complexas.
  - Motivo: risco e custo altos, ganho baixo para o estadio atual.
- Decidido: centralizar feedback de acao em snackbars compartilhadas.
  - Alternativas: Toast disperso por tela; dialogs para tudo.
  - Motivo: consistencia e menos ruido para o usuario.

---

### Task 1: Fechar o contrato de feedback global

**Files:**
- Modify: `app/src/main/java/com/obiterjus/presentation/componentes/EstadosInterface.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/principal/TelaPrincipal.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/perfil/TelaPerfil.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/prazos/TelaPrazos.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/publicacoes/TelaPublicacoes.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/processos/TelaProcessos.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/detalheprocesso/TelaDetalheProcesso.kt`

**Step 1: Mapear eventos de UI**

- Identificar cada acao critica que precisa virar snackbar.
- Separar mensagens de sucesso, erro e retry por caso de uso.

**Step 2: Criar/usar um canal unico de snackbar**

- Usar `SnackbarHostState` no ponto mais alto da UI.
- Expor eventos de tela como estado observavel, nao como efeitos perdidos.

**Step 3: Conectar telas criticas**

- Perfil, prazos, publicacoes e processos precisam emitir feedback claro.
- O usuario precisa saber quando algo deu certo, falhou ou ficou pendente.

**Step 4: Validar**

- Rodar `./gradlew.bat --no-daemon :app:compileDebugKotlin`
- Esperado: compile sem erros.

### Task 2: Tornar o Perfil realmente funcional

**Files:**
- Modify: `app/src/main/java/com/obiterjus/presentation/perfil/ModeloPerfil.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/perfil/TelaPerfil.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/principal/TelaPrincipal.kt`
- Modify: `app/src/main/java/com/obiterjus/di/ObiterModules.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Step 1: Ligar o toggle de sincronizacao automatica**

- Persistir a alteracao no repositorio de preferencias.
- Refletir o novo valor imediatamente na UI.

**Step 2: Ligar os botoes de acao**

- `Forcar sincronizacao` precisa executar a sincronizacao real e retornar estado.
- `Logout` precisa sair de forma previsivel.
- `Entrar` precisa abrir o fluxo de autenticacao.

**Step 3: Expor loading e erro**

- Enquanto a acao roda, mostrar estado de carregamento.
- Em falha, disparar snackbar com retry quando fizer sentido.

**Step 4: Validar**

- Testar manualmente entrar, sair, forcar sync e alternar toggles.
- Rodar `./gradlew.bat --no-daemon :app:lint`.

### Task 3: Criar a tela de cadastro/login novo usuario

**Files:**
- Create: `app/src/main/java/com/obiterjus/presentation/autenticacao/ModeloAutenticacao.kt`
- Create: `app/src/main/java/com/obiterjus/presentation/autenticacao/TelaAutenticacao.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/navegacao/NavGraph.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/principal/TelaPrincipal.kt`
- Modify: `app/src/main/java/com/obiterjus/di/ObiterModules.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Step 1: Definir o estado da tela**

- Campos: nome, email, senha, confirmacao, loading, erro, modo login/cadastro.
- Incluir acoes de entrar com e-mail, criar conta e modo anonimo.

**Step 2: Implementar o fluxo visual**

- Tela unica com alternancia entre login e cadastro.
- Validacao basica antes de chamar o repositorio.

**Step 3: Ligar ao AuthRepository**

- Usar `signInWithEmail`, `signUpWithEmail` e `signInAnonymously`.
- Expor resultado via snackbar e navegacao apos sucesso.

**Step 4: Validar**

- Usuario novo consegue criar conta sem depender de outra tela.
- Rodar `./gradlew.bat --no-daemon :app:compileDebugKotlin`.

### Task 4: Fechar loading, empty e error nas telas principais

**Files:**
- Modify: `app/src/main/java/com/obiterjus/presentation/componentes/EstadosInterface.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/inicio/TelaInicio.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/publicacoes/TelaPublicacoes.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/prazos/TelaPrazos.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/processos/TelaProcessos.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/detalheprocesso/TelaDetalheProcesso.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/auditoria/TelaAuditoria.kt`

**Step 1: Padronizar loading**

- Cada tela precisa de indicador central ou skeleton utilitario.
- Evitar telas vazias sem explicacao durante carga.

**Step 2: Padronizar empty state**

- Titulo, icone e texto curto por contexto.
- Nao usar o mesmo empty para tudo.

**Step 3: Padronizar error state**

- Em erro, exibir snackbar e manter a tela recuperavel.
- Retry precisa apontar para a acao original.

**Step 4: Validar**

- Abrir cada tela com lista vazia, lista populada e falha simulada.
- Rodar `./gradlew.bat --no-daemon :app:lint`.

### Task 5: Remover fios mortos e alinhar navegação

**Files:**
- Modify: `app/src/main/java/com/obiterjus/MainActivity.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/navegacao/ObiterRota.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/navegacao/NavGraph.kt`
- Modify: `app/src/main/java/com/obiterjus/presentation/principal/TelaPrincipal.kt`
- Modify: `app/src/main/java/com/obiterjus/di/ObiterModules.kt`
- Delete: `app/src/main/java/com/obiterjus/presentation/agenda/TelaAgendaPrazos.kt`
- Delete: `app/src/main/java/com/obiterjus/presentation/agenda/AgendaPrazosViewModel.kt`
- Delete: `app/src/main/java/com/obiterjus/presentation/autenticacao/TelaAutenticacao.kt`
- Delete: `app/src/main/java/com/obiterjus/presentation/autenticacao/ModeloAutenticacao.kt`

**Step 1: Revisar rotas e bottom bar**

- Garantir que nao existe item navegavel sem tela funcional.
- Manter rotas de auditoria, perfil, prazos, processos, publicacoes e inicio.

**Step 2: Eliminar codigo obsoleto**

- Remover telas antigas substituidas.
- Atualizar o grafo de navegação e o DI para os novos fluxos.

**Step 3: Validar**

- Nao pode haver rota morta na navegação principal.
- Rodar `./gradlew.bat --no-daemon :app:assembleDebug`.

### Task 6: Fechar a entrega do MVP

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/obiterjus/ui/theme/*`
- Modify: `app/src/main/java/com/obiterjus/presentation/componentes/*`
- Modify: `docs/plans/2026-05-05-obiterjus-mvp.md`

**Step 1: Revisar microcopy**

- Remover texto vago, duplicado ou morto.
- Ajustar labels para refletirem o que realmente acontece.

**Step 2: Revisar acessibilidade**

- Verificar content descriptions e touch targets.
- Garantir contraste minimo nas telas mais usadas.

**Step 3: Rodar pacote final**

- `./gradlew.bat --no-daemon :app:compileDebugKotlin`
- `./gradlew.bat --no-daemon :app:lint`
- `./gradlew.bat --no-daemon :app:assembleDebug`

**Step 4: Commit**

- Commitar cada bloco concluido com mensagem curta e objetiva.

## Exit Criteria

- Usuario pode entrar ou cadastrar conta.
- Perfil responde aos botoes e toggles.
- Snackbar mostra resultado de acoes criticas.
- Telas principais tratam loading, empty e error.
- Nao existem rotas mortas visiveis no fluxo principal.
- Build debug e lint passam.
