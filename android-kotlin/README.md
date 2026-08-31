# Odontoart Rotas Android

Aplicativo Android da Odontoart Agenda+.

## Fonte de verdade

O produto oficial é o repositório:

```text
DanielRochaOdo/Odontoart-rotas
```

A prioridade do Android é **fidelidade visual e funcional ao web**. Para evitar duas implementações divergentes, o APK usa o mesmo React + CSS/Tailwind + TypeScript do web como runtime de interface, hospedado por um `WebView` Android seguro através de `WebViewAssetLoader`.

O Kotlin permanece responsável por recursos de plataforma:

- ciclo de vida Android;
- empacotamento e assinatura do APK;
- autoatualização do canal `direct`;
- abertura de Waze, Google Maps, Uber, 99 e demais intents;
- seletor de arquivos;
- botão Voltar e integração com o sistema;
- isolamento dos assets em origem HTTPS local (`appassets.androidplatform.net`).

Não deve existir uma segunda versão manual das telas em Compose para competir com o web. Alterações de layout, modal, filtro, regra ou fluxo devem ser feitas primeiro no `Odontoart-rotas`; o próximo build Android incorpora o mesmo código.

## Como o build funciona

O Gradle procura o repositório web nesta ordem:

1. caminho informado em `ODONTOART_WEB_REPO_PATH`;
2. `rotas-mobile/web-reference` (usado pelo CI);
3. repositório irmão `../Odontoart-rotas` na pasta GitHub local.

O fonte web **não é modificado**. O Gradle copia os arquivos necessários para:

```text
android-kotlin/app/build/web-runtime/
```

Na cópia temporária ele executa:

```text
npm ci
npm run build
```

Depois sincroniza o `dist` gerado para os assets do APK.

## Estrutura local recomendada

```text
GitHub/
├── rotas-mobile/
└── Odontoart-rotas/
```

Com essa estrutura não é necessário configurar `ODONTOART_WEB_REPO_PATH`.

Se o web estiver em outro local:

```powershell
$env:ODONTOART_WEB_REPO_PATH="C:\caminho\Odontoart-rotas"
```

## Variáveis de ambiente

O `.env` da raiz do `rotas-mobile` fornece somente configurações de cliente necessárias ao bundle:

```properties
VITE_SUPABASE_URL=
VITE_SUPABASE_ANON_KEY=
VITE_CEP_API_URL=
VITE_NOMINATIM_PROXY_URL=
VITE_ODONTOART_PROXY_URL=
VITE_ODONTOART_TOKEN=
VITE_DASHBOARD_URL=
VITE_DASHBOARD_ANON_KEY=
```

Aliases `SUPABASE_URL`, `PRIMARY_SUPABASE_URL`, `DASHBOARD_URL` e `DASHBOARD_ANON_KEY` continuam aceitos pelo Gradle quando aplicável.

### Nunca entram no APK

Os valores abaixo são exclusivos de backend/scripts e não devem ser transformados em `BuildConfig` nem copiados para `.env.local` do runtime web:

```text
SUPABASE_SERVICE_ROLE_KEY
DASHBOARD_SERVICE_ROLE_KEY
CRON_SECRET
DASH_SYNC_MODE
DASH_SYNC_BATCH_SIZE
DASH_SYNC_SAFETY_LAG_SECONDS
DASH_SYNC_LOCK_TTL_SECONDS
DASH_SYNC_TABLES
```

## Gerar APK de teste

Na raiz do `rotas-mobile`:

```powershell
git checkout main
git pull
cd android-kotlin
.\gradlew.bat clean assembleDebug
```

O build agora também compila o web. O APK fica em:

```text
android-kotlin\app\build\outputs\apk\debug\app-debug.apk
```

## Versionamento

Em `gradle.properties` ou como propriedade Gradle:

```properties
ODONTOART_APP_VERSION_CODE=2
ODONTOART_APP_VERSION_NAME=1.0.1
```

Toda atualização precisa ter `ODONTOART_APP_VERSION_CODE` superior ao instalado.

Para release configure:

- `ODONTOART_KEYSTORE_PASSWORD`
- `ODONTOART_KEY_ALIAS`
- `ODONTOART_KEY_PASSWORD`

A chave de assinatura deve ser a mesma em todas as versões.

## Canal normal / Google Play

```powershell
.\gradlew.bat releasePlayReady
```

## Canal direto com autoatualização

Somente o build `direct` recebe `REQUEST_INSTALL_PACKAGES`.

```powershell
.\gradlew.bat directDistributionReady
```

Saída:

```text
android-kotlin\app\build\outputs\direct-distribution\
```

Arquivos:

- `odontoart-rotas-direct-v<versao>-b<build>.apk`
- `android-update.json`
- `android-update-v<versao>.json`
- `index.html`

Publicação manual recomendada:

```text
public_html/rotas/updates/
```

A URL fixa de metadata continua:

```text
https://odontoart.com/rotas/updates/android-update.json
```

## Regra de paridade

O critério de aceite do Android passa a ser simples:

> A mesma revisão do `Odontoart-rotas` usada no build deve produzir no APK os mesmos módulos, layouts responsivos, filtros, modais, permissões, consultas e regras de negócio do sistema web.

O Android não deve recriar uma versão alternativa dessas telas.
