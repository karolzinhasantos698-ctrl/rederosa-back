# Check-in Seguro: acompanhamento por horário e localização

## Uso

1. Faça login novamente depois desta atualização. O login agora gera uma sessão de acesso para proteger os acompanhamentos.
2. Em **Check-in Seguro**, escolha 15, 30 ou 60 minutos e toque em **Me acompanhe**.
3. Envie o link por **Compartilhar pelo WhatsApp** ou **Copiar link**. O envio é manual e a usuária escolhe a pessoa.
4. Permita o GPS. A pessoa abre o link sem cadastro e vê a localização compartilhada no mapa, com atualizações a cada poucos segundos enquanto as páginas estiverem visíveis.
5. A usuária pode confirmar que está bem, adicionar 15 minutos ou cancelar sem confirmar. O mesmo link reflete as alterações.

Não existem mensagens automáticas por WhatsApp, SMS ou e-mail nesta versão. Combine com a pessoa de conferir o link no horário. O estado **ATRASADO** significa apenas que o horário passou sem confirmação.

## Persistência e acesso

- Os prazos e o histórico ficam no banco. Fechar o navegador não encerra nem reinicia o acompanhamento.
- O relógio do backend determina o atraso; não há dependência de um temporizador aberto no navegador.
- Cada conta pode ter apenas um acompanhamento em andamento, incluindo os atrasados. Um bloqueio de banco evita inícios simultâneos da mesma conta.
- A sessão do login dura sete dias e somente seu hash é salvo no banco. As rotas privadas exigem `Authorization: Bearer <token>`.
- O link público possui um identificador aleatório de 256 bits e permite apenas consulta. Não divulga e-mail, ID da conta ou credenciais.
- O telefone é opcional. Se informado, fica disponível para ligação a quem tiver o link.
- A posição mais recente (latitude, longitude, precisão e horários de coleta/recebimento) fica no acompanhamento e é compartilhada pelo link. Não é armazenado um histórico de todos os deslocamentos.
- Somente a dona da conta pode enviar, pausar ou retomar o GPS. Uma posição mais antiga não substitui uma mais nova; amostras anteriores a uma pausa não reativam o compartilhamento.
- O link expira 24 horas após o prazo ou encerramento, limitado a 48 horas desde o início. Extensões são limitadas a 24 horas de duração total.
- O histórico mostra os últimos 20 acompanhamentos. Os registros locais antigos são preservados em uma seção separada.

## Abrir em outro aparelho

`localhost` e `127.0.0.1` só funcionam no próprio computador. A interface impede compartilhar esse endereço pelo WhatsApp para evitar enviar um link inacessível.

Para consultar o estado na mesma rede Wi-Fi, abra o frontend pelo IP de rede do computador, por exemplo `http://192.168.1.10:5500/checkin.html`. O frontend usará esse mesmo host na porta 8080 para a API. O backend e o Live Server precisam aceitar conexões dessa rede; as portas devem estar acessíveis no firewall. **O GPS do celular exige HTTPS: abrir um IP da rede por HTTP não habilita a geolocalização.**

Para acesso pela internet, hospede o backend em HTTPS e configure:

```properties
# Variável de ambiente usada pelo application.properties
REDE_ROSA_PUBLIC_BASE_URL=https://api.seu-dominio.example
```

A página de acompanhamento é servida pelo próprio backend em `/acompanhar/{token}`, com seus arquivos em `/acompanhamento/`. Não depende do Live Server da usuária.

Se o frontend estiver em um domínio diferente do backend, declare a URL da API antes de carregar `js/api.js`:

```html
<meta name="api-url" content="https://api.seu-dominio.example">
```

Sem configuração explícita, páginas HTTPS usam sua própria origem para a API; páginas HTTP de desenvolvimento usam o mesmo host na porta 8080. Em produção com proxy, configure a URL pública explicitamente para evitar gerar links internos.

## Mapa e GPS

- O mapa interativo usa Leaflet 1.9.4, servido localmente com sua licença, e mapas do OpenStreetMap. O marcador se move sem recarregar um iframe.
- `watchPosition` acompanha as mudanças do GPS; uma consulta adicional a cada dez segundos busca uma posição fresca mesmo quando o aparelho está parado. As atualizações são enviadas no máximo uma vez a cada três segundos.
- A página pública consulta o backend a cada três segundos; a página da usuária recupera o estado a cada cinco segundos. Esses intervalos não garantem entrega instantânea quando a conexão está lenta.
- A idade da posição e a precisão ficam visíveis. Depois de 30 segundos sem posição recente, a tela informa que a página ou o GPS pode estar pausado. Falhas de conexão não transformam a última posição em uma transmissão ativa.
- **Pausar compartilhamento** pausa somente o GPS; o prazo continua. **Retomar localização** reinicia a captura. Ao confirmar ou cancelar, o backend rejeita novas posições e o mapa público identifica a última posição como pertencente a um acompanhamento encerrado.
- Ao sair da página ou colocá-la em segundo plano, o frontend tenta registrar a pausa. Se o sistema encerrar a página antes do envio, o horário da última posição continua permitindo identificar a interrupção.
- Para o GPS funcionar, use HTTPS (ou localhost no computador), permita a localização e mantenha a página aberta e visível. Navegadores não garantem GPS contínuo com a tela bloqueada ou o aplicativo em segundo plano. Esse tipo de rastreamento exige implementação nativa para o aparelho.

Referências: [Geolocation API / W3C](https://www.w3.org/TR/geolocation/), [Leaflet](https://leafletjs.com/reference).

## API

| Método e rota | Acesso | Uso |
| --- | --- | --- |
| `POST /login` | E-mail e senha | Retorna ID, nome, e-mail e token; não retorna a senha |
| `POST /api/acompanhamentos` | Sessão | Inicia com `minutos` (15, 30 ou 60) e `telefone` opcional |
| `GET /api/acompanhamentos/ativo` | Sessão | Retorna `{ "acompanhamento": ... }` ou `null` nesse campo |
| `GET /api/acompanhamentos/historico` | Sessão | Últimos 20 registros da própria conta |
| `POST /api/acompanhamentos/{id}/confirmar` | Dona do registro | Confirma que está bem |
| `POST /api/acompanhamentos/{id}/estender` | Dona do registro | Adiciona `minutos` (15, 30 ou 60) |
| `POST /api/acompanhamentos/{id}/cancelar` | Dona do registro | Encerra sem confirmação |
| `PUT /api/acompanhamentos/{id}/localizacao` | Dona do registro | Recebe `latitude`, `longitude`, `precisao` e `coletadaEm` (ISO UTC) |
| `POST /api/acompanhamentos/{id}/localizacao/pausar` | Dona do registro | Interrompe o compartilhamento sem encerrar o prazo |
| `GET /api/acompanhamentos/publico/{token}` | Link temporário | Nome de chamada, telefone autorizado, estado, horários e última localização compartilhada |

## Verificação

```powershell
.\mvnw.cmd '-Dtest=AcompanhamentoIntegrationTests,UsuariaControllerTests' test
```

Os testes de integração usam H2 em memória, sem criar ou alterar contas no MySQL. Cobrem login, persistência, extensão, prazo vencido, cancelamento, expiração, acesso entre contas, coordenadas inválidas, amostras fora de ordem, pausa do GPS e interrupção após encerrar.

O fluxo também foi verificado com duas instâncias do Chrome, API e banco isolados e GPS simulado nativamente: os marcadores se movem nas duas telas sem atualização manual. Foram conferidos pausa, retomada, saída e retorno à página, interrupção ao encerrar e telas de 1366, 390 e 320 pixels.
