# Odontoart Rotas Android (Kotlin)

Este diretorio contem o app Android nativo em Kotlin.
Ele nao carrega a versao web e nao usa `WebView` para navegar.

## Arquitetura
- `app/`: modulo Android
- `app/src/main/java/com/odontoart/rotas`: login, fluxo de rotas e cliente Supabase nativos
- Compartilhamento com a versao web: somente banco Supabase (Auth + tabelas)

## Configuracao de ambiente
Defina as variaveis abaixo para o modulo Android:
- `VITE_SUPABASE_URL` ou `SUPABASE_URL`
- `VITE_SUPABASE_ANON_KEY` ou `SUPABASE_ANON_KEY`

O build injeta esses valores em `BuildConfig`.

## Build release
Versionamento do release:
- `ODONTOART_APP_VERSION_CODE` e `ODONTOART_APP_VERSION_NAME` em `gradle.properties`
- Exemplo: `ODONTOART_APP_VERSION_CODE=2` e `ODONTOART_APP_VERSION_NAME=1.0.1`

1. Opcionalmente defina as variaveis de assinatura:
   - `ODONTOART_KEYSTORE_PASSWORD`
   - `ODONTOART_KEY_ALIAS`
   - `ODONTOART_KEY_PASSWORD`
2. Execute:
   - `./gradlew.bat releasePlayReady`
3. Artefatos gerados:
   - `app/build/outputs/apk/release/app-release.apk`
   - `app/build/outputs/apk/release-versioned/odontoart-rotas-v<versionName>-b<versionCode>-<timestamp>.apk`
   - `app/build/outputs/bundle/release/app-release.aab`
   - `app/build/outputs/bundle/release-versioned/odontoart-rotas-v<versionName>-b<versionCode>-<timestamp>.aab`

Notas:
- `.aab` e o formato padrao para upload no Google Play.
- `.apk` e para instalacao direta/sideload.
- O build Android sincroniza automaticamente `../dist` para `app/src/main/assets` e remove assets antigos.
- Se `../dist` nao existir, o release falha com instrucao para rodar `npm run build` na raiz.
