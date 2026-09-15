# Escopo do MVP

## Objetivo

Entregar uma primeira versão jogável do Crazy Noisy Quiz, permitindo que amigos criem uma sala, entrem por código e disputem partidas sequenciais em tempo real.

## Funcionalidades incluídas

- Cadastro e login.
- Perfil básico do jogador.
- Criação de sala privada.
- Entrada em sala por código.
- Lobby com lista de participantes.
- Até 8 jogadores por sala.
- Mínimo de 2 jogadores para iniciar uma partida.
- Seleção de uma ou mais categorias.
- Dez perguntas por partida.
- Perguntas de múltipla escolha com quatro alternativas.
- Sorteio de perguntas conforme as categorias selecionadas.
- Uma resposta por jogador em cada pergunta.
- Cronômetro de 10 segundos por pergunta.
- Pontuação por acerto e velocidade.
- Resultado final da partida.
- Histórico das partidas da sala.
- Várias partidas sequenciais na mesma sala.
- Eventos da partida em tempo real via WebSocket.
- Perguntas iniciais cadastradas por seed.

## Fora do MVP

- Matchmaking automático.
- Equipes.
- Chat.
- Criação de perguntas por usuários comuns.
- Painel administrativo completo.
- Notificações push.
- Reconexão durante partida ativa.
- Temporadas e ligas.
- Conquistas.
- Ranking global avançado.

## Fluxo principal

1. O jogador faz login.
2. O jogador cria uma sala ou entra utilizando um código.
3. Os participantes aguardam no lobby.
4. O dono da sala escolhe as categorias.
5. O dono inicia a partida.
6. O servidor sorteia as perguntas.
7. Os jogadores respondem em tempo real.
8. O servidor calcula a pontuação.
9. O sistema exibe o resultado.
10. Os jogadores permanecem na sala para uma nova partida.

