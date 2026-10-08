# Checkpoint: envio de respostas

O jogador envia uma alternativa por rodada. A API confirma o registro sem revelar
o gabarito ou calcular pontos nesta etapa.

## Testes

Execute no IntelliJ:

- `MatchAnswerServiceTest`: participação, vínculo da rodada e alternativa, prazo e duplicidade.
- `MatchAnswerControllerTest`: contrato HTTP, validação do body e respostas de erro.
- `MatchAnswerRepositoryTest`: persistência e restrição de duplicidade em Postgres temporário.

O teste de repository precisa do Docker funcionando. O Flyway aplica a V13 no banco
temporário; o banco de desenvolvimento não é alterado pelos testes.

## Postman

1. Reinicie a aplicação para aplicar `V13__create_match_answers_table.sql`.
2. Prepare uma nova sala com dois participantes e inicie a partida com o token do criador.
3. Copie o `id` retornado ao iniciar a partida: ele é o `matchId`.
4. Abra a primeira rodada: `POST /api/matches/{matchId}/rounds/next`.
5. Copie o `id` da rodada para `roundId` e um `options[].id` para `optionId`.
6. Com o token de um participante, envie dentro do prazo:
   `POST /api/matches/{matchId}/rounds/{roundId}/answers`.

Body JSON (substitua pelo UUID da alternativa retornada):

```json
{
  "optionId": "30000000-0000-0000-0000-000000000001"
}
```

Sucesso retorna `201` com `id`, `roundId`, `optionId` e `answeredAt`.
Prepare a requisição com antecedência: o prazo padrão é de 10 segundos.
Uma rodada antiga já estará fora do prazo; ainda não implementamos seu encerramento
nem a reabertura de partidas nesta etapa.

Um segundo envio dentro do prazo retorna `409` por duplicidade. Depois do prazo,
retorna `409` por tempo esgotado. Não participantes recebem `403`; uma alternativa
de outra pergunta recebe `400`; recursos inexistentes recebem `404`.

Sugestão de commit após os testes passarem:
`add player answer submission and tests`

Próxima etapa: encerrar rodadas e calcular pontuação.
