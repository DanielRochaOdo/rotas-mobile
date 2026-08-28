# Migração do Rotas Mobile para Android nativo

## 1. Objetivo

Transformar o `rotas-mobile` em um aplicativo Android genuinamente nativo, mantendo o repositório `DanielRochaOdo/Odontoart-rotas` como referência funcional, visual e de regras de negócio, sem alterar o repositório web e sem embarcar sua aplicação React dentro do APK.

## 2. Regra principal

### O web é fonte de referência, não fonte de UI para o APK

Pode ser usado para entender:

- regras de negócio;
- permissões por perfil;
- campos e validações;
- comportamento dos fluxos;
- dados consumidos do Supabase/RPCs/APIs;
- identidade e hierarquia visual do produto.

Não pode ser usado no Android por meio de:

- WebView;
- cópia de `dist`;
- JavaScript bridge;
- injeção de CSS;
- componentes React;
- TypeScript executado ou empacotado no aplicativo;
- HashRouter ou qualquer navegação web dentro do APK.

## 3. Arquitetura alvo

```text
app/src/main/java/com/odontoart/rotas/
  core/
    designsystem/
    network/
    session/
    util/
  data/
    auth/
    routes/
    visits/
    clients/
    queue/
    kpi/
  domain/
    model/
    repository/
    usecase/
  feature/
    login/
    dashboard/
    agenda/
    visits/
    clients/
    acceptance/
    queue/
    kpi/
    news/
    settings/
    logs/
  navigation/
  ui/theme/
```

### Camadas

**UI / feature**
- Jetpack Compose e Material 3.
- Cada funcionalidade possui sua tela e ViewModel.
- Estado exposto por `StateFlow`.

**Domain**
- Modelos e regras independentes da interface.
- Regras do web devem ser traduzidas para casos de uso Kotlin quando não estiverem centralizadas no banco/API.

**Data**
- Repositórios responsáveis por Auth, Supabase REST, RPCs e APIs externas.
- A tela não conhece URLs, SQL, JSON ou detalhes de autenticação.

**Core**
- sessão, cliente HTTP, design system, datas, erros e componentes comuns.

## 4. Direção de UX

O Android deve preservar a identidade Odontoart, mas não copiar a composição desktop.

### Padrões principais

- `NavigationBar` para áreas de uso frequente;
- `TopAppBar` contextual;
- `LazyColumn`/`LazyRow` para listas;
- cards em lugar de tabelas densas;
- `FloatingActionButton` para ação primária;
- bottom sheets/dialogs para ações rápidas;
- filtros em chips;
- busca com campo nativo;
- feedback de carregamento e erro dentro da tela;
- Intent nativo para mapas, telefone e links externos;
- suporte a tema claro/escuro do Android;
- alvos de toque adequados para uso em campo.

### Identidade

- verde Odontoart como cor primária;
- superfícies claras e hierarquia Material 3;
- tipografia do sistema;
- menos elementos simultâneos que no desktop;
- informação priorizada por contexto de uso.

## 5. Paridade funcional

Rotas identificadas no web atual:

| Módulo web | Rota web | Android nativo | Situação |
|---|---|---|---|
| Login | `/login` | Login Compose | Base implementada |
| Dashboard | `/` e `/dashboard` | Início | Base implementada |
| Agenda / Rotas | `/agenda` | Agenda | Base implementada para rotas/paradas |
| Visitas | `/visitas` | Visitas | Portar regras e fluxo |
| Aceite Digital | `/aceite-digital` | Aceite | Portar |
| Clientes | `/clientes` | Clientes | Portar |
| Fila | `/fila` | Fila | Portar |
| KPI | `/kpi` | Indicadores | Portar e redesenhar para mobile |
| Novidades | `/novidades` | Novidades | Portar |
| Configurações | `/configuracoes` | Configurações | Portar |
| Logs | `/logs` | Logs | Portar apenas para perfis autorizados |

A rota web `/rotas` atualmente redireciona para `/agenda`; no Android deve existir apenas o conceito nativo de Agenda/Rotas, sem reproduzir redirecionamento web.

## 6. Regras que precisam ser revisadas durante a migração

O web recebeu evoluções depois da última atualização do mobile, incluindo:

- melhorias de UX;
- sistema de notificações;
- KPI;
- regra de visita;
- exibição da regra de visita no card da empresa;
- modo ação;
- mudanças em cadastro/estado de usuários e empresas;
- campo de observação e redesign de telas.

Cada item deve ser rastreado até a implementação Android correspondente.

## 7. Estratégia de migração

### Fase 1 — romper dependência web

- remover WebView do fluxo do app;
- remover bridge JavaScript e injeção de CSS;
- remover sincronização de `dist` no Gradle;
- remover dependências WebKit usadas apenas pelo shell web;
- criar tema Material 3;
- criar shell Compose;
- manter autenticação e acesso Supabase em Kotlin.

### Fase 2 — arquitetura por feature

- separar `MainViewModel` em ViewModels por feature;
- separar `SupabaseApi` em repositórios por domínio;
- persistir sessão com armazenamento Android apropriado;
- padronizar tratamento de erros;
- criar navegação tipada do app.

### Fase 3 — operação de campo

Prioridade:

1. Agenda/Rotas completa;
2. Visitas;
3. Clientes;
4. notificações;
5. Aceite Digital;
6. Fila;
7. KPI;
8. demais telas administrativas.

### Fase 4 — paridade e otimização

- comparar tela a tela e regra a regra com o web;
- testes de permissões por perfil;
- testes offline/de rede instável onde fizer sentido;
- performance de listas grandes;
- acessibilidade;
- validação em aparelhos Android reais.

## 8. Política para evitar nova divergência

Toda alteração relevante no `Odontoart-rotas` deve responder no PR/ticket:

> Esta mudança afeta o aplicativo Android?

Se sim, deve existir uma das opções:

1. regra compartilhada no banco/RPC/API e consumida pelos dois clientes; ou
2. tarefa Android vinculada para implementar a mesma regra em Kotlin.

Mudanças de UI desktop não precisam ser copiadas literalmente. O Android deve manter equivalência de objetivo e regra, usando padrões mobile.

## 9. Critérios de aceite do Android nativo

Uma funcionalidade só é considerada nativa quando:

- a tela é renderizada por Compose;
- não existe WebView no fluxo;
- não depende de HTML/CSS/JS/TS para funcionar;
- dados são acessados pela camada Kotlin;
- navegação ocorre pelo aplicativo Android;
- permissões e validações equivalem ao comportamento esperado do web;
- a experiência é utilizável em tela de celular sem reproduzir layout desktop.

## 10. Estado desta branch

Branch: `refactor/android-native-kotlin`

Nesta primeira etapa já foram iniciados:

- remoção da dependência de `dist` no build;
- remoção das dependências WebKit do shell;
- substituição da `MainActivity` por Compose;
- tema Material 3 próprio;
- login nativo;
- dashboard nativo inicial;
- agenda nativa inicial com seleção de rota e paradas;
- abertura de endereço via Intent de mapa;
- navegação inferior Android.

Os demais módulos permanecem fora do escopo da Fase 1 e devem ser portados progressivamente, sem reintroduzir conteúdo web no APK.
