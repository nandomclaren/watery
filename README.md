# Watery

Widget de água para a tela inicial do Android, sincronizado com o Health Connect.

- Defina a meta diária de água e o tamanho do copo no app.
- Toque no widget (ou no botão "+") para registrar um copo — isso grava um registro de hidratação no Health Connect e também lê o total do dia de lá, então fica sincronizado com qualquer outro app conectado ao Health Connect.
- O badge no canto superior do widget mostra copos bebidos/meta (ex: `8/10`) e o número abaixo do copo mostra os litros bebidos hoje (ex: `2L`).
- O widget reinicia sozinho todo dia (o contador é zerado com base na data local).

## Como gerar o APK

Este repositório tem um workflow do GitHub Actions (`.github/workflows/build-apk.yml`) que compila um APK de debug a cada push. Depois que ele rodar, baixe o artefato `watery-debug-apk` na aba **Actions** do repositório e instale no celular (é necessário permitir instalação de fontes desconhecidas).

Para compilar localmente com o Android Studio, basta abrir a pasta do projeto e rodar `./gradlew assembleDebug` (requer Android SDK com `compileSdk 35`).

## Requisitos

- Android 8.0 (API 26) ou superior.
- App **Health Connect** instalado (nativo a partir do Android 14; em versões anteriores, disponível na Play Store). O app oferece um atalho para instalar/atualizar caso necessário.
