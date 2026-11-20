# QuickTimer — Timer de descanso para Galaxy Watch (Wear OS)

Contagem regressiva rápida, pensada para o **descanso entre séries na academia**.
Abre com um atalho de botão, começa em 1 minuto e cada novo acionamento soma mais tempo.
Números grandes, legíveis sem óculos, frequência cardíaca na tela e vibração nos segundos finais.

Feito e testado no **Samsung Galaxy Watch Ultra 2**. Deve funcionar em qualquer relógio **Wear OS 3 ou mais novo** (Android 11+), mas outros modelos não foram testados.

---

## Sumário
1. [Recursos](#recursos)
2. [Como usar](#como-usar)
3. [Ajustes](#ajustes)
4. [Instalação (APK pronto)](#instalação-apk-pronto)
5. [Configurar o atalho do botão](#configurar-o-atalho-do-botão)
6. [Atualizar para uma versão nova](#atualizar-para-uma-versão-nova)
7. [Compilar a partir do código](#compilar-a-partir-do-código)
8. [Permissões e privacidade](#permissões-e-privacidade)
9. [Limitações conhecidas](#limitações-conhecidas)
10. [Solução de problemas](#solução-de-problemas)
11. [Estrutura do projeto](#estrutura-do-projeto)
12. [Licença](#licença)

---

## Recursos

- **Contagem regressiva** com tempo inicial configurável (padrão 1 min).
- **Somar tempo** a cada acionamento do atalho ou toque na tela (padrão +1 min).
- **Desfazer** um incremento deslizando para baixo.
- **Contador de séries** ("Série 3"), com zeramento automático configurável.
- **Frequência cardíaca** em tempo real, em fonte grande.
- **Alertas por vibração:**
  - vibração dupla aos 10 s restantes;
  - uma vibração curta por segundo de 00:05 a 00:01;
  - três vibrações longas no 00:00.
- **Pulso visual nos 10 s finais:** os números ficam laranja e em negrito a cada segundo, com animação suave.
- **Segundo plano:** saia do app e a contagem continua, com indicador no mostrador.
- **Fecha sozinho** ao terminar e volta para o app que estava aberto antes.

---

## Como usar

| Ação | Resultado |
|---|---|
| Atalho (ex.: tecla Início 2×) sem timer rodando | Inicia nova série com o tempo inicial |
| Atalho de novo com o timer na tela | Soma o incremento |
| Toque na tela | Soma o incremento |
| Deslizar para baixo | Desfaz um incremento (não deixa ficar abaixo de 1 s) |
| Toque longo | Cancela e fecha |
| Tecla Início 1× ou deslizar para a direita | Sai do app; a contagem continua em segundo plano |
| Atalho com o timer rodando em segundo plano | Só reabre a tela, sem somar |
| Tocar no indicador do mostrador | Reabre a tela do timer |

**Layout da tela**

```
        Série 3          ← série atual (ou "+1 min" ao somar)
        00:47            ← contagem, no centro
        ♥ 128            ← frequência cardíaca
```

O arco laranja na borda mostra a fração de tempo que ainda falta.

---

## Ajustes

Abra o ícone **"Timer Ajustes"** na lista de apps do relógio. Cada item muda de opção a cada toque.

| Ajuste | Opções |
|---|---|
| Tempo inicial | 15 s, 30 s, 45 s, 1 min, 1 min 30 s, 2 min, 3 min, 5 min |
| Incremento por clique | 15 s, 30 s, 1 min, 2 min |
| Frequência cardíaca | Mostrar / Ocultar |
| Zerar séries automaticamente | Após 1, 2, 4, 8 ou 12 h sem uso · À meia-noite · Nunca |
| Zerar agora | Volta a contagem de séries para zero |

---

## Instalação (APK pronto)

O app não está na Play Store. A instalação é feita do computador para o relógio, pela rede Wi-Fi, com o **adb** (ferramenta oficial do Android).

### 1. Baixe o APK
Na página do repositório, abra **Releases** (coluna da direita) e baixe o `QuickTimer-x.y.apk` da versão mais recente.

### 2. Baixe o adb (Platform-Tools)
Em <https://developer.android.com/tools/releases/platform-tools>, baixe o pacote do seu sistema e extraia:
- **Windows:** extraia em `C:\` (fica `C:\platform-tools`).
- **macOS / Linux:** extraia em qualquer pasta, por exemplo `~/platform-tools`.

Coloque o APK baixado dentro dessa pasta e renomeie para `quicktimer.apk`.

### 3. Ative a depuração no relógio
1. **Configurações > Sobre o relógio > Software** → toque 5× em **Versão do software** até aparecer "Modo desenvolvedor ativado".
2. **Configurações > Opções do desenvolvedor** → ative **Depuração ADB** e **Depuração por Wi-Fi**.
3. Deixe o Wi-Fi do relógio em **sempre ligado** (Configurações > Conexões > Wi-Fi). Sem isso, ele desliga o Wi-Fi quando está conectado ao celular por Bluetooth.
4. O relógio e o computador precisam estar **na mesma rede Wi-Fi**.

### 4. Abra o terminal na pasta do adb
- **Windows:** abra `C:\platform-tools` no Explorer, digite `powershell` na barra de endereço e tecle Enter.
- **macOS / Linux:** `cd ~/platform-tools`

> No **PowerShell** os comandos começam com `.\adb`. No **cmd** use `adb`. No **macOS/Linux** use `./adb`.

### 5. Pareie (só na primeira vez)
No relógio: **Depuração por Wi-Fi > Parear novo dispositivo**. A tela mostra um **código de 6 dígitos** e um **IP:porta**. Deixe essa tela aberta e rode, trocando pelos seus números:

```
.\adb pair 192.168.1.50:41235
```

Digite o código quando pedir. Deve aparecer `Successfully paired`.

### 6. Conecte e instale
Volte uma tela no relógio. A tela principal de **Depuração por Wi-Fi** mostra outro **IP:porta**: o IP é o mesmo, a porta é diferente. Use esse:

```
.\adb connect 192.168.1.50:37123
.\adb devices
.\adb -s 192.168.1.50:37123 install -r quicktimer.apk
```

Deve aparecer `Success`. O app surge na lista como **"Timer 1 min"**, e há também o ícone **"Timer Ajustes"**.

### 7. Primeira abertura
Abra o app e aceite as permissões de **sensores corporais** (frequência cardíaca) e **notificações** (indicador de segundo plano).

Depois de tudo instalado, desative a **Depuração por Wi-Fi** para economizar bateria. O app continua instalado.

---

## Configurar o atalho do botão

No relógio (ou no app **Galaxy Wearable** do celular):

**Configurações > Recursos avançados > Personalizar teclas > Tecla Início > Pressionar duas vezes > Timer 1 min**

Com isso, dois toques na tecla Início iniciam o timer, e dois toques de novo com o timer aberto somam tempo.

> Em relógios de outras marcas, procure nas configurações a opção de atalho de botão físico para abrir um app.

---

## Atualizar para uma versão nova

1. Baixe o APK novo em **Releases**.
2. Ligue a **Depuração por Wi-Fi** no relógio e confira a porta atual.
3. Rode:
   ```
   .\adb connect 192.168.1.50:PORTA_ATUAL
   .\adb -s 192.168.1.50:PORTA_ATUAL install -r quicktimer.apk
   ```

O `-r` atualiza mantendo ajustes, séries e permissões. O pareamento não precisa ser refeito.

---

## Compilar a partir do código

### Opção A — GitHub Actions (sem instalar nada)
1. Faça um **fork** deste repositório.
2. Na aba **Actions** do seu fork, habilite os workflows, se pedido.
3. Abra **Build APK > Run workflow**. Qualquer push também dispara o build.
4. Em 3–5 minutos, na execução com ✓ verde, baixe **QuickTimer-apk** em **Artifacts**.

### Opção B — Android Studio
1. Instale o [Android Studio](https://developer.android.com/studio) (Ladybug ou mais novo).
2. **File > Open** → selecione a pasta do projeto e espere o Gradle sincronizar.
3. **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

Com o relógio conectado pelo adb, o botão **Run ▶** compila e instala de uma vez.

### Opção C — Linha de comando
Requer JDK 17, Android SDK (API 35) e Gradle 8.9:
```
gradle assembleDebug
```

### Sobre a assinatura
O projeto inclui uma chave de assinatura fixa (`app/quicktimer.jks`, senha `quicktimer`). Assim, todo build sai com a mesma assinatura e o `install -r` sempre funciona como atualização. Ela serve só para instalação pessoal via adb. **Não use essa chave para publicar o app em loja.** Se for distribuir uma versão sua, gere sua própria chave:
```
keytool -genkeypair -keystore app/minha-chave.jks -alias minha -keyalg RSA -keysize 2048 -validity 10000
```
e ajuste o bloco `signingConfigs` em `app/build.gradle.kts`.

---

## Permissões e privacidade

| Permissão | Para quê |
|---|---|
| Sensores corporais (`BODY_SENSORS`) | Ler a frequência cardíaca, apenas enquanto a tela do timer está aberta |
| Notificações | Mostrar o indicador do timer rodando em segundo plano |
| Vibração | Alertas táteis |
| Serviço em primeiro plano / wake lock | Manter a contagem correndo com a tela apagada |

- O app **não usa internet**, não coleta nem envia dados.
- A frequência cardíaca é só exibida, nunca gravada.
- Os ajustes e o contador de séries ficam salvos apenas no relógio.
- Se você negar a permissão de sensores, o timer funciona normalmente e mostra "♥ --".

**Bateria:** a tela fica acesa enquanto o timer está aberto. Fora dele, o serviço em segundo plano só roda durante a contagem e se encerra ao terminar.

---

## Limitações conhecidas

- **Botão de ação (laranja) do Galaxy Watch Ultra:** a Samsung só permite funções próprias nesse botão (Cronômetro, Lanterna, etc.). Apps de terceiros não aparecem na lista, e o clique não é repassado a outros apps. Use o atalho da tecla Início.
- **Sem bloco (tile) nem complicação** de mostrador, por enquanto.
- Testado apenas no Galaxy Watch Ultra 2. Em telas menores, os números podem ficar justos.

---

## Solução de problemas

| Mensagem / sintoma | Causa e solução |
|---|---|
| `adb : O termo 'adb' não é reconhecido` | No PowerShell use `.\adb` em vez de `adb`. |
| `error: protocol fault (couldn't read status message)` no `pair` | Endereço digitado errado. Use o IP:porta exato da tela **Parear novo dispositivo**, com ela ainda aberta. |
| `failed to connect` / `recusou ativamente (10061)` | A porta de conexão mudou. Confira a porta atual na tela principal de Depuração por Wi-Fi. |
| `no devices/emulators found` | Não está conectado. Rode `adb connect IP:PORTA`. |
| `more than one device/emulator` | O relógio aparece duas vezes. Use `-s IP:PORTA` no comando (veja `adb devices`). |
| Dispositivo aparece como `offline` | Rode `adb disconnect` e `adb kill-server`, desligue e religue a Depuração por Wi-Fi e conecte de novo. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE ... signatures do not match` | O app instalado tem outra assinatura. Desinstale com `adb uninstall com.comntrol.quicktimer` e instale de novo (refaça o atalho da tecla Início). |
| "Timer 1 min" não aparece no Botão rápido | Esperado, veja [Limitações](#limitações-conhecidas). Use Personalizar teclas > Tecla Início. |
| Frequência cardíaca fica "--" | Ela leva de 5 a 10 s para aparecer. Confira se a permissão de sensores foi concedida e se o relógio está bem ajustado ao pulso. |
| O relógio desconecta toda hora | Deixe o Wi-Fi do relógio sempre ligado e a tela acesa durante a instalação. |

---

## Estrutura do projeto

```
.github/workflows/build.yml     Build automático do APK no GitHub Actions
app/build.gradle.kts            Dependências, versão e assinatura
app/quicktimer.jks              Chave de assinatura fixa (uso pessoal)
app/src/main/AndroidManifest.xml
app/src/main/java/com/comntrol/quicktimer/
    MainActivity.kt             Tela do timer, gestos, frequência cardíaca
    TimerController.kt          Estado do timer (somar, desfazer, cancelar) e vibrações
    TimerService.kt             Contagem em segundo plano, alertas e indicador no mostrador
    SettingsActivity.kt         Tela de ajustes
    Prefs.kt                    Preferências e contador de séries
app/src/main/res/               Ícones e textos
```

Stack: Kotlin, Jetpack Compose, Compose for Wear OS, Wear Ongoing Activity. `minSdk 30`, `targetSdk 34`.

---

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).
Uso por sua conta e risco; o app não é afiliado à Samsung nem ao Google.
