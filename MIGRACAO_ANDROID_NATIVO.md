# Rotas Mobile — arquitetura de paridade com o web

## 1. Objetivo

O `rotas-mobile` deve entregar no Android o mesmo produto existente em `DanielRochaOdo/Odontoart-rotas`, com paridade visual e funcional.

O repositório web é a **fonte de verdade** para:

- layout e responsividade;
- módulos e navegação;
- permissões;
- filtros;
- formulários e modais;
- regras de negócio;
- consultas Supabase/RPC;
- Edge Functions e integrações;
- estados de loading/erro/vazio;
- comportamento de Agenda/Visitas/Rotas.

O repositório `Odontoart-rotas` permanece somente leitura durante o build Android.

## 2. Decisão arquitetural

As tentativas de reimplementar o sistema em Jetpack Compose geraram divergências visuais, perda de funcionalidades e consultas incompatíveis com o backend.

Para garantir paridade, o Android passa a usar o mesmo bundle React/CSS/TypeScript do web dentro de um `WebView` hospedado pelo Kotlin.

O Kotlin continua responsável por recursos específicos da plataforma:

- Activity e ciclo de vida;
- empacotamento/assinatura do APK;
- autoatualização;
- intents para mapas e aplicativos externos;
- seletor de arquivos;
- botão Voltar;
- armazenamento/cache do WebView;
- distribuição Google Play ou canal direto.

## 3. Fonte web sem alteração

O Gradle localiza `Odontoart-rotas`, copia os arquivos necessários para uma pasta temporária em `build/`, gera somente variáveis públicas `VITE_*`, executa o build Vite e empacota o `dist` no APK.

Nenhum arquivo do repositório web é sobrescrito.

Estrutura recomendada:

```text
GitHub/
├── rotas-mobile/
└── Odontoart-rotas/
```

Caminho alternativo:

```powershell
$env:ODONTOART_WEB_REPO_PATH="C:\caminho\Odontoart-rotas"
```

## 4. Segurança

Podem ser incluídas no bundle apenas configurações de cliente já expostas pelo web, como:

```text
VITE_SUPABASE_URL
VITE_SUPABASE_ANON_KEY
VITE_DASHBOARD_URL
VITE_DASHBOARD_ANON_KEY
VITE_CEP_API_URL
VITE_NOMINATIM_PROXY_URL
VITE_ODONTOART_PROXY_URL
VITE_ODONTOART_TOKEN
```

Nunca devem ser empacotados:

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

## 5. Regra de paridade

Não deve existir uma segunda implementação manual da interface em Compose competindo com o web.

Para cada revisão utilizada no build, o APK deve reproduzir os mesmos:

- módulos;
- menus;
- permissões;
- filtros;
- dados;
- modais;
- botões e ações;
- validações;
- regras de negócio;
- identidade visual;
- comportamento responsivo.

## 6. Agenda e Visitas

Agenda/Visitas são consideradas áreas críticas. O Android deve usar o mesmo `Visitas.tsx`, APIs e helpers do web.

Exemplo de divergência que motivou a mudança: a implementação Kotlin consultava `visits.route_stop_id`, porém o web deriva a parada consultando `route_stops` por `route_id + cliente_id`. A reimplementação manual criou um contrato inexistente no banco.

Com o runtime compartilhado, esse tipo de divergência deixa de existir porque o mesmo código de consulta é executado no web e no APK.

## 7. Build

APK de teste:

```powershell
cd android-kotlin
.\gradlew.bat clean assembleDebug
```

O Gradle executa automaticamente a preparação e compilação do web antes de compilar o APK.

Saída:

```text
android-kotlin\app\build\outputs\apk\debug\app-debug.apk
```

Canal direto:

```powershell
.\gradlew.bat directDistributionReady
```

## 8. Critério de aceite

Uma versão Android somente deve ser considerada pronta quando:

1. o build web usado no APK vem diretamente do `Odontoart-rotas`;
2. o APK abre e autentica normalmente;
3. Agenda, Visitas e Rotas exibem os mesmos dados e regras do web;
4. filtros, modais e ações principais funcionam no aparelho;
5. links externos são encaminhados para intents Android quando aplicável;
6. nenhuma credencial de servidor foi incorporada ao APK;
7. o CI compila tanto o web quanto o Android e valida os assets gerados.

## 9. Evolução futura

Se algum recurso necessitar comportamento específico do dispositivo, ele pode receber uma integração Kotlin/JavaScript dedicada. A interface e regra central, porém, permanecem no código compartilhado do web para evitar nova divergência.
