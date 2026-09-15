# Regras de negócio

## Salas

- Um usuário pode criar uma sala privada.
- Cada sala possui um código único para entrada.
- O criador da sala é o dono da sala e também participa das partidas.
- Uma sala pode conter várias partidas sequenciais.
- A sala possui no mínimo 2 jogadores para iniciar uma partida.
- A sala possui no máximo 8 jogadores.
- O dono da sala inicia a partida e as partidas seguintes.
- Os jogadores permanecem na sala após o término de uma partida.
- O dono pode encerrar a sala.
- Nenhum jogador pode entrar em uma partida depois que ela foi iniciada.

## Estados da sala

```text
OPEN -> IN_GAME -> OPEN
  \____________> CLOSED
```

- `OPEN`: a sala aceita jogadores e aguarda a próxima partida.
- `IN_GAME`: existe uma partida em andamento.
- `CLOSED`: a sala não pode mais ser utilizada.

## Partidas

- Cada partida pertence a uma única sala.
- Uma sala pode possuir várias partidas.
- Cada partida possui 10 perguntas.
- Cada pergunta aparece uma única vez dentro da partida.
- Perguntas utilizadas em uma partida podem aparecer em partidas futuras.
- A partida é controlada pelo servidor.
- A partida não é pausada quando um jogador desconecta.

## Estados da partida

```text
WAITING -> IN_PROGRESS -> FINISHED
```

- `WAITING`: a partida foi configurada, mas ainda não começou.
- `IN_PROGRESS`: as perguntas estão sendo apresentadas aos jogadores.
- `FINISHED`: todas as rodadas terminaram e o resultado foi calculado.

## Categorias e perguntas

- O dono escolhe pelo menos uma categoria antes de iniciar a partida.
- As perguntas são sorteadas somente entre as categorias selecionadas.
- O sistema deve tentar distribuir as perguntas de forma equilibrada entre as categorias.
- Cada pergunta possui quatro alternativas.
- Cada pergunta possui exatamente uma alternativa correta.
- Cada pergunta possui uma dificuldade e um tempo limite.
- O MVP utilizará perguntas cadastradas por seed.
- Perguntas inativas não podem ser sorteadas.
- Usuários comuns não cadastram perguntas no MVP.

## Respostas

- Cada jogador pode responder uma vez por pergunta.
- Respostas recebidas depois do encerramento do tempo são inválidas.
- O servidor registra o momento em que a resposta foi recebida.
- O servidor valida se a alternativa pertence à pergunta atual.
- O cliente não pode definir sua própria pontuação.
- Respostas erradas recebem zero ponto.
- Perguntas não respondidas recebem zero ponto.

## Pontuação

- Apenas respostas corretas geram pontos.
- Respostas mais rápidas geram mais pontos.
- O tempo padrão de cada pergunta é de 10 segundos.
- A pontuação inicial utiliza faixas fixas:

  - Até 1 segundo: 100 pontos.
  - Até 3 segundos: 80 pontos.
  - Até 5 segundos: 60 pontos.
  - Até 8 segundos: 40 pontos.
  - Até 10 segundos: 20 pontos.
  - Resposta errada ou fora do tempo: 0 ponto.

- A pontuação é calculada e armazenada pelo servidor.
- O resultado final considera a soma dos pontos obtidos em todas as perguntas.
- Em caso de empate, vence quem tiver mais respostas corretas.
- Se o empate persistir, os jogadores compartilham a mesma posição.

## Desconexão

- Um jogador pode sair da sala antes do início da partida e entrar novamente.
- Durante uma partida, a desconexão não pausa o jogo.
- O jogador desconectado perde as perguntas cujo prazo expirar enquanto estiver ausente.
- O jogador não pode responder perguntas que já expiraram.
- As respostas enviadas antes da desconexão são preservadas.
- O MVP não terá recuperação do estado da partida após reconexão.

## Escopo futuro

Os seguintes recursos poderão ser adicionados posteriormente:

- Reconexão com recuperação de estado.
- Matchmaking automático.
- Partidas em equipes.
- Ranking global.
- Temporadas e ligas.
- Conquistas.
- Painel administrativo para perguntas.
- Criação e moderação de perguntas por usuários.

