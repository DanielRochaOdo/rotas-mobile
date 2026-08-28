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
      update/
        AppUpdateInfo.kt
        AppUpdateManager.kt
        AppUpdateRepository.kt
      ui/
        RotasApp.kt
        theme/RotasTheme.kt
```

### Responsabilidades

- `MainActivity.kt`: hospeda a UI Compose e aciona a checagem de atualização do canal direto.
- `ui/`: telas, componentes e identidade visual Android.
- `update/`: consulta metadata pública, baixa e encaminha APK para o instalador Android.
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

## Versionamento

Versionamento opcional em `gradle.properties`:

```properties
ODONTOART_APP_VERSION_CODE=2
ODONTOART_APP_VERSION_NAME=1.0.1
```

Toda atualização distribuída precisa ter `ODONTOART_APP_VERSION_CODE` maior que a versão instalada no aparelho.

Para release, configure também:

- `ODONTOART_KEYSTORE_PASSWORD`
- `ODONTOART_KEY_ALIAS`
- `ODONTOART_KEY_PASSWORD`

A mesma chave de assinatura deve ser mantida entre as versões para que o Android aceite a atualização sobre o app já instalado.

## Canal normal / Google Play

Execute:

```bash
./gradlew releasePlayReady
```

Esse canal não recebe `REQUEST_INSTALL_PACKAGES` e não usa o atualizador de APK direto.

O build Android não executa Node/NPM e não precisa que o projeto web seja compilado antes.

## Canal direto com autoatualização

O canal `direct` segue o mesmo princípio usado no Venda+: o aplicativo consulta um JSON público, compara o `versionCode` e, quando encontra uma versão superior, oferece o download e abre o instalador do Android.

Somente o build `direct` recebe a permissão:

```text
android.permission.REQUEST_INSTALL_PACKAGES
```

O build normal permanece sem essa permissão.

### Endereços padrão

Por padrão, o Rotas usa:

```text
https://odontoart.com/rotas/updates/android-update.json
https://odontoart.com/rotas/updates/<apk-versionado>.apk
https://odontoart.com/rotas/updates/index.html
```

É possível alterar a pasta e a URL da metadata sem mudar código:

```properties
ROTAS_UPDATE_BASE_URL=https://odontoart.com/rotas/updates
ROTAS_UPDATE_METADATA_URL=https://odontoart.com/rotas/updates/android-update.json
```

Esses valores podem ser informados por variável de ambiente, `local.properties` ou `.env`.

### Gerar pacote para HostGator

Execute:

```bash
./gradlew directDistributionReady
```

Os arquivos ficam em:

```text
android-kotlin/app/build/outputs/direct-distribution/
```

O diretório contém:

- `odontoart-rotas-direct-v<versao>-b<build>.apk`
- `android-update.json`
- `android-update-v<versao>.json`
- `index.html`

O `android-update.json` é gerado automaticamente no formato:

```json
{
  "versionCode": 2,
  "versionName": "1.0.1",
  "apkUrl": "https://odontoart.com/rotas/updates/odontoart-rotas-direct-v1.0.1-b2.apk",
  "notes": "Atualizacao da versao 1.0.1"
}
```

### Publicar na HostGator

Crie ou utilize esta pasta pública:

```text
public_html/rotas/updates/
```

Envie para ela os arquivos gerados por `directDistributionReady`.

A regra é manter esta URL sempre fixa:

```text
https://odontoart.com/rotas/updates/android-update.json
```

A cada nova versão:

1. aumente `ODONTOART_APP_VERSION_CODE` e ajuste `ODONTOART_APP_VERSION_NAME`;
2. gere `./gradlew directDistributionReady`;
3. envie o novo APK para `public_html/rotas/updates/`;
4. substitua `android-update.json` pelo novo arquivo;
5. substitua `index.html` pela página gerada.

O HTML público apresenta a versão atual e um botão de download manual do APK. O aplicativo não depende do HTML para atualizar: a autoatualização depende apenas do `android-update.json` e do APK estarem acessíveis por HTTPS.

### Fluxo no aparelho

1. O app `direct` consulta `android-update.json` ao abrir.
2. Se `versionCode` remoto for maior que `BuildConfig.VERSION_CODE`, mostra "Atualização disponível".
3. Ao confirmar, baixa o APK para o cache privado do aplicativo.
4. O arquivo é validado antes da instalação.
5. Se necessário, o Android abre a tela "Instalar apps desconhecidos".
6. Ao voltar para o Rotas com a permissão liberada, o instalador é aberto automaticamente.

## Paridade com o web

A migração funcional deve ser acompanhada pelo documento `../MIGRACAO_ANDROID_NATIVO.md`.

Quando uma regra for alterada no web, deve-se avaliar se ela também afeta o Android. A regra pode ser reimplementada em Kotlin ou compartilhada por banco/RPC/API, mas nunca por reutilização de tela TypeScript dentro do APK.
