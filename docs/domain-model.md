# Modelo de domínio e banco de dados

## Objetivo

Este documento define as entidades principais do Crazy Noisy Quiz e seus relacionamentos para o MVP.

O modelo deve refletir três conceitos diferentes:

1. A sala persistente onde os jogadores permanecem.
2. As partidas sequenciais executadas dentro da sala.
3. As rodadas, perguntas e respostas de cada partida.

## Entidades principais

### User

Representa uma conta de jogador.

Responsabilidades:

- Autenticação.
- Identidade do jogador.
- Participação em salas e partidas.
- Consulta de histórico.

### QuizRoom

Representa uma sala persistente criada por um jogador.

Uma sala pode conter várias partidas sequenciais.

### RoomParticipant

Representa a participação de um usuário em uma sala.

É uma entidade própria porque a relação entre usuário e sala possui dados, como data de entrada e saída.

### Match

Representa uma partida específica dentro de uma sala.

Uma sala pode possuir várias partidas, mas cada partida pertence a uma única sala.

### MatchParticipant

Representa a participação de um jogador em uma partida.

Essa tabela mantém o conjunto de jogadores daquela partida e permite armazenar presença, pontuação e posição.

### Category

Representa uma categoria de perguntas, como História, Ciência ou Esportes.

### Question

Representa uma pergunta cadastrada no catálogo do sistema.

### QuestionOption

Representa uma alternativa de uma pergunta.

### MatchCategory

Registra as categorias escolhidas para uma partida. Essa relação funciona como um snapshot da configuração da partida.

### MatchRound

Representa a ocorrência de uma pergunta dentro de uma partida. A mesma pergunta pode existir no catálogo e ser utilizada em várias partidas, mas cada utilização possui uma rodada própria.

### PlayerAnswer

Representa a resposta de um jogador em uma rodada específica.

## Relacionamentos

```text
User 1 ─── N QuizRoom                 dono da sala
User N ─── N QuizRoom                 através de RoomParticipant
QuizRoom 1 ─── N Match
Match N ─── N User                    através de MatchParticipant
Match N ─── N Category                através de MatchCategory
Category 1 ─── N Question
Question 1 ─── N QuestionOption
Match 1 ─── N MatchRound
MatchRound N ─── 1 Question
MatchRound 1 ─── N PlayerAnswer
User 1 ─── N PlayerAnswer
QuestionOption 1 ─── N PlayerAnswer
```

## Tabelas do MVP

```dbml
Table users {
  id uuid [pk]
  username varchar(30) [not null, unique]
  email varchar(255) [not null, unique]
  password_hash varchar(255) [not null]
  avatar_key varchar(20)
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table quiz_rooms {
  id uuid [pk]
  code varchar(6) [not null, unique]
  owner_id uuid [not null]
  status varchar(20) [not null]
  max_players int [not null, default: 8]
  created_at timestamp [not null]
  closed_at timestamp
}

Table room_participants {
  id uuid [pk]
  room_id uuid [not null]
  user_id uuid [not null]
  joined_at timestamp [not null]
  left_at timestamp

  Indexes {
    (room_id, user_id) [unique]
  }
}

Table matches {
  id uuid [pk]
  room_id uuid [not null]
  status varchar(20) [not null]
  total_rounds int [not null, default: 10]
  current_round_number int
  started_at timestamp
  finished_at timestamp
  created_at timestamp [not null]
}

Table match_participants {
  id uuid [pk]
  match_id uuid [not null]
  user_id uuid [not null]
  total_points int [not null, default: 0]
  correct_answers int [not null, default: 0]
  position int
  joined_at timestamp [not null]
  disconnected_at timestamp

  Indexes {
    (match_id, user_id) [unique]
  }
}

Table categories {
  id uuid [pk]
  name varchar(80) [not null, unique]
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table questions {
  id uuid [pk]
  category_id uuid [not null]
  statement text [not null]
  difficulty varchar(20) [not null]
  time_limit_seconds int [not null, default: 10]
  active boolean [not null, default: true]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table question_options {
  id uuid [pk]
  question_id uuid [not null]
  option_text varchar(500) [not null]
  option_order int [not null]
  is_correct boolean [not null, default: false]

  Indexes {
    (question_id, option_order) [unique]
  }
}

Table match_categories {
  match_id uuid [not null]
  category_id uuid [not null]

  Indexes {
    (match_id, category_id) [pk]
  }
}

Table match_rounds {
  id uuid [pk]
  match_id uuid [not null]
  question_id uuid [not null]
  round_number int [not null]
  status varchar(20) [not null]
  started_at timestamp
  ended_at timestamp

  Indexes {
    (match_id, round_number) [unique]
  }
}

Table player_answers {
  id uuid [pk]
  match_round_id uuid [not null]
  user_id uuid [not null]
  option_id uuid
  answered_at timestamp
  is_correct boolean [not null, default: false]
  response_time_ms int
  points_earned int [not null, default: 0]

  Indexes {
    (match_round_id, user_id) [unique]
  }
}

Ref: quiz_rooms.owner_id > users.id
Ref: room_participants.room_id > quiz_rooms.id
Ref: room_participants.user_id > users.id
Ref: matches.room_id > quiz_rooms.id
Ref: match_participants.match_id > matches.id
Ref: match_participants.user_id > users.id
Ref: questions.category_id > categories.id
Ref: question_options.question_id > questions.id
Ref: match_categories.match_id > matches.id
Ref: match_categories.category_id > categories.id
Ref: match_rounds.match_id > matches.id
Ref: match_rounds.question_id > questions.id
Ref: player_answers.match_round_id > match_rounds.id
Ref: player_answers.user_id > users.id
Ref: player_answers.option_id > question_options.id
```

## Decisões importantes

### Por que existe MatchParticipant?

`RoomParticipant` registra quem está na sala. `MatchParticipant` registra quem efetivamente participou de uma partida. Isso permite que um jogador permaneça na sala, mas não participe de uma partida específica.

### Por que existe MatchRound?

`Question` é parte do catálogo geral. `MatchRound` registra quando aquela pergunta foi sorteada para uma partida, em qual posição e durante qual intervalo de tempo.

### Por que armazenar a resposta correta em PlayerAnswer?

O campo `is_correct` preserva o resultado calculado no momento da resposta. Assim, alterações futuras no catálogo de perguntas não modificam o histórico de partidas.

### Por que MatchCategory é necessário?

A escolha de categorias pertence à partida, não à sala. Cada nova partida pode usar categorias diferentes sem alterar partidas anteriores.

## Restrições recomendadas

- Uma sala não pode ter dois participantes ativos com o mesmo usuário.
- Uma partida não pode ter o mesmo usuário duas vezes.
- Uma partida não pode ter duas rodadas com o mesmo número.
- Uma rodada só pode pertencer a uma partida.
- Uma pergunta deve possuir exatamente uma alternativa correta.
- Uma pergunta deve possuir quatro alternativas no MVP.
- Um jogador só pode responder uma vez por rodada.
- Uma resposta deve pertencer a uma alternativa da pergunta da rodada.
- Uma sala só pode iniciar uma partida se possuir pelo menos dois participantes.
