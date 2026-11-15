# Timer 1 min — Galaxy Watch Ultra 2 (Wear OS)

Comportamento:
- Botão de ação abre o app → contagem regressiva de 1:00.
- Botão de ação de novo (ou toque na tela) → +1 min a cada clique.
- Zerou → vibra e fecha, voltando ao app anterior.
- Toque longo ou deslizar para a direita → cancela.

## 1. Compilar
1. Abra a pasta `QuickTimer` no Android Studio (Ladybug ou mais novo). Ele baixa o Gradle 8.9 e as dependências sozinho.
2. Build > Build App Bundle(s)/APK(s) > Build APK(s)
   → `app/build/outputs/apk/debug/app-debug.apk`

## 2. Ativar depuração no relógio
1. Configurações > Sobre o relógio > Software > toque 5x em "Versão do software".
2. Configurações > Opções do desenvolvedor > ative "Depuração ADB" e "Depuração por Wi-Fi".
3. Relógio e PC na mesma rede Wi-Fi.

## 3. Instalar
Em "Depuração por Wi-Fi" > "Parear novo dispositivo", anote IP:porta e código:

    adb pair 192.168.x.x:PORTA_PAREAMENTO   (digite o código)
    adb connect 192.168.x.x:PORTA_CONEXAO
    adb install -r app/build/outputs/apk/debug/app-debug.apk

(Ou, já pareado, basta apertar Run ▶ no Android Studio com o relógio selecionado.)

## 4. Atribuir ao botão de ação
Configurações > Recursos avançados > Botão rápido (Quick button) > escolha "Timer 1 min".
Se a lista do seu firmware não permitir apps de terceiros, use
Configurações > Recursos avançados > Personalizar teclas > Pressionar duas vezes (tecla Início) > "Timer 1 min".
