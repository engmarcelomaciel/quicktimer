# Timer — Galaxy Watch Ultra 2 (Wear OS) · v2.0

## Uso
| Ação | Resultado |
|---|---|
| Tecla Início 2x (sem timer rodando) | Inicia nova série com o tempo inicial |
| Tecla Início 2x com o timer na tela | Soma o incremento (padrão +1 min) |
| Toque na tela | Soma o incremento |
| Deslizar para baixo | Desfaz um incremento |
| Toque longo | Cancela |
| Deslizar para a direita / tecla Início 1x | Sai; a contagem continua em segundo plano |
| Tocar no indicador do mostrador | Volta para a tela do timer |

- Nos 10 s finais: vibração dupla e os números pulsam (negrito + laranja) a cada segundo.
- Ao zerar: vibra 3x e fecha, voltando ao app anterior.
- Mostra a série atual e a frequência cardíaca (pede permissão na 1ª vez).

## Ajustes (ícone "Timer Ajustes" na lista de apps)
Cada item muda de opção a cada toque:
- Tempo inicial: 15 s … 5 min
- Incremento por clique: 15 s, 30 s, 1 min, 2 min
- Frequência cardíaca: mostrar/ocultar
- Zerar séries automaticamente: após 1/2/4/8/12 h sem uso, à meia-noite, ou nunca
- Zerar séries agora

## Compilar
Push no GitHub → aba Actions → "Build APK" → Artifacts → QuickTimer-apk.

## Instalar (PowerShell em C:\platform-tools)
    .\adb pair 192.168.1.98:PORTA_PAREAMENTO
    .\adb connect 192.168.1.98:PORTA_CONEXAO
    .\adb install -r app-debug.apk
