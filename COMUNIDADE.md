# Salas de conversa da Rede Rosa

## O que está disponível

As três salas são criadas automaticamente no primeiro início do backend e reutilizadas nos próximos:

- **Apoio emocional:** conversar, ouvir e acolher.
- **Direitos das mulheres:** dúvidas, informações e experiências.
- **Segurança:** experiências e dicas de cuidado.

Na página `chat.html`, o chat da Rosa aparece primeiro e já aberto. Os cartões das salas ficam abaixo; a caixa de mensagens da comunidade só aparece depois de clicar em uma sala. Nenhuma sala abre automaticamente ao carregar ou recarregar a página. O botão **Fechar conversa** volta aos cartões preservando o rascunho.

Todas as usuárias autenticadas que abrirem a mesma sala podem ler e participar. Não há convite, mensagem direta ou sala restrita a participantes específicos nesta versão.

As mensagens ficam no banco e são atualizadas na tela a cada três segundos enquanto a página estiver visível. O histórico inicial contém as últimas 50 mensagens e o botão de mensagens anteriores permite continuar a leitura. Nome da autora, horário e indicação de mensagem própria são definidos pelo backend; e-mail, senha, credencial e ID da conta não são enviados às outras participantes.

**Enter** envia a mensagem; **Shift + Enter** insere uma linha. O limite é de 2.000 caracteres. Rascunhos são preservados por conta e sala na sessão do navegador quando o armazenamento estiver disponível.

A conversa com a assistente **Rosa** fica em uma seção separada e não publica mensagens nas salas. Ela continua dependendo da configuração existente do provedor de IA. As salas de pessoas não dependem desse provedor.

## Sessão e envio

Para ler ou enviar mensagens, é necessário o token retornado por `POST /login`, no cabeçalho `Authorization: Bearer <token>`. Se o navegador ainda tem apenas uma sessão antiga sem token, faça login novamente. O catálogo de nomes das salas pode ser consultado sem login.

Cada envio usa uma `chaveEnvio` UUID. Repetir uma tentativa com o mesmo texto e chave retorna a mensagem já salva. Reutilizar a chave com outro texto ou sala retorna conflito, evitando duplicações e perda de intenção após uma falha de rede.

O envio bloqueia a linha da sala até confirmar a transação. Assim, consultas por cursor não avançam além de uma mensagem cujo ID foi criado mas ainda não foi confirmado no banco. O frontend só avança o cursor após receber uma página de leitura; a resposta de um envio próprio não pode pular mensagens ainda não recebidas de outras pessoas.

## Denúncias

O botão **Denunciar** está nas mensagens recebidas. A usuária informa um motivo e a denúncia é salva em `denuncia_comunidade`, uma vez por mensagem e denunciante. O registro não apaga a mensagem automaticamente nem informa que houve análise humana.

Não há painel administrativo ou fila de moderação nesta implementação. A interface confirma apenas **Denúncia registrada**. Os registros ficam disponíveis no banco para uma futura ferramenta administrativa.

## API

| Método e rota | Acesso | Uso |
| --- | --- | --- |
| `GET /api/comunidade/salas` | Público | Lista as três salas |
| `GET /api/comunidade/salas/{sala}/mensagens` | Sessão | Últimas mensagens da sala |
| `GET /api/comunidade/salas/{sala}/mensagens?apos={id}` | Sessão | Mensagens posteriores ao cursor |
| `GET /api/comunidade/salas/{sala}/mensagens?antes={id}` | Sessão | Histórico anterior |
| `POST /api/comunidade/salas/{sala}/mensagens` | Sessão | Envia `{ "texto": "...", "chaveEnvio": "UUID" }` |
| `POST /api/comunidade/salas/{sala}/mensagens/{id}/denuncias` | Sessão | Registra `{ "motivo": "..." }` |

Os identificadores das salas são `apoio-emocional`, `direitos-das-mulheres` e `seguranca`. A leitura aceita `limite` de 1 a 100 (padrão 50). `apos` e `antes` são exclusivos entre si.

O frontend usa `js/api.js`, a mesma configuração da API dos acompanhamentos. Para conversar em aparelhos diferentes, cada aparelho precisa abrir um frontend e backend acessíveis pela rede ou internet; `localhost` aponta para o próprio aparelho.

## Verificação

```powershell
.\mvnw.cmd '-Dtest=ComunidadeIntegrationTests,AcompanhamentoIntegrationTests,UsuariaControllerTests' test
```

Os testes usam H2 em memória e cobrem as três salas, duas contas, autoria do servidor, paginação, repetição de envio, conteúdo literal, parâmetros inválidos, denúncias e envios simultâneos com leitura por cursor. Também foram executadas as verificações de regressão do acompanhamento, GPS e perfil.

Foi verificado no Chrome com duas contas fictícias e API real em banco isolado: login, envio por botão e Enter, recebimento automático, histórico por sala, rascunhos, denúncia, persistência após recarregar e layout em 1366, 390 e 320 pixels.
