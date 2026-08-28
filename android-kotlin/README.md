# Odontoart Rotas Android

Aplicativo Android nativo do sistema de rotas da Odontoart.

## Princípio do projeto

O repositório web `DanielRochaOdo/Odontoart-rotas` é a referência de regras de negócio, fluxos e identidade do produto, mas **não é uma dependência de execução do Android**.

O aplicativo Android deve ser implementado com recursos nativos da plataforma:

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel + StateFlow
- OkHttp/Supabase para acesso aos dados
- Intents e APIs Android para integrações do dispositivo

Não fazem parte da arquitetura do aplicativo:

- WebView
- JavaScript injetado
- TypeScript
- CSS/Tailwind
- cópia do `dist` web para `assets`
- navegação por HashRouter/BrowserRouter dentro do APK

## Estrutura atual

```text
android-kotlin/
  app/
    src/main/java/com/odontoart/rotas/
      AppModels.kt
      MainActivity.kt
      MainViewModel.kt
      SupabaseApi.kt
      ui/
        RotasApp.kt
        theme/RotasTheme.kt
```

### Responsabilidades

- `MainActivity.kt`: hospeda exclusivamente a UI Compose.
- `ui/`: telas, componentes e identidade visual Android.
- `MainViewModel.kt`: estado e ações da interface.
- `SupabaseApi.kt`: comunicação com Auth/REST do Supabase.
- `AppModels.kt`: modelos usados pelo aplicativo.

## UX Android

O mobile não deve reproduzir literalmente o desktop. A versão Android usa:

- navegação inferior para áreas principais;
- top app bar contextual;
- cards para rotas, empresas e paradas;
- ações com área de toque adequada;
- FAB para ações primárias;
- listas verticais em vez de tabelas desktop;
- abertura de mapas com Intent nativo;
- Material 3 e suporte ao tema do sistema.

A identidade visual mantém a marca Odontoart, mas a composição segue padrões Android.

## Configuração

Configure no ambiente, `local.properties` ou `.env` na raiz do `rotas-mobile`:

```properties
SUPABASE_URL=https://SEU-PROJETO.supabase.co
SUPABASE_ANON_KEY=SUA_CHAVE_ANON
```

O build injeta os valores em `BuildConfig`.

## Build

Versionamento opcional em `gradle.properties`:

```properties
ODONTOART_APP_VERSION_CODE=2
ODONTOART_APP_VERSION_NAME=1.0.1
```

Para release, configure também:

- `ODONTOART_KEYSTORE_PASSWORD`
- `ODONTOART_KEY_ALIAS`
- `ODONTOART_KEY_PASSWORD`

Depois execute:

```bash
./gradlew releasePlayReady
```

O build Android não executa Node/NPM e não precisa que o projeto web seja compilado antes.

## Paridade com o web

A migração funcional deve ser acompanhada pelo documento `../MIGRACAO_ANDROID_NATIVO.md`.

Quando uma regra for alterada no web, deve-se avaliar se ela também afeta o Android. A regra pode ser reimplementada em Kotlin ou compartilhada por banco/RPC/API, mas nunca por reutilização de tela TypeScript dentro do APK.
