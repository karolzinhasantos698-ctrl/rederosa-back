# Guia genérico: Spring Boot, MySQL e frontend estático em uma VM Azure

Guia reutilizável para publicar um projeto Java com banco MySQL e frontend HTML/CSS/JavaScript em uma VM Ubuntu dedicada.

Este é um roteiro de configuração, não um registro de implantação executada. Os exemplos não contêm credenciais reais. As opções do portal, cotas, preços, versões de pacotes e disponibilidade devem ser conferidas na assinatura no momento da criação.

## 1. Arquitetura proposta

```text
Internet
   |
   | HTTPS :443
   v
Nginx na VM Ubuntu
   |-- frontend estático: /var/www/meuprojeto
   +-- /api/ --> Spring Boot em 127.0.0.1:8080
                     |-- MySQL local
                     +-- serviços externos, se houver
```

Uma VM separada oferece isolamento de recursos e configuração em relação aos demais projetos. Se as VMs pertencem à mesma assinatura, seus custos continuam descontando do mesmo saldo de créditos.

O frontend deste guia não precisa de Node em produção. Se o projeto utiliza React/Vue com build estático, publique o diretório de saída do build. Next.js com renderização no servidor ou outro backend de frontend exige configuração adicional.

## 2. Convenções e valores que precisam ser definidos

Os comandos usam `meuprojeto` como nome de exemplo. É possível mantê-lo ou substituí-lo consistentemente. Não usar espaços, acentos ou hífens no nome SQL do banco ou do usuário neste roteiro.

| Informação | Exemplo ou marcador |
|---|---|
| Grupo de recursos | `rg-meuprojeto` |
| VM | `vm-meuprojeto` |
| Usuário Ubuntu | `azureuser` |
| Banco MySQL | `meuprojeto` |
| Usuário MySQL | `meuprojeto_app` |
| Serviço systemd | `meuprojeto` |
| Diretório da aplicação | `/opt/meuprojeto` |
| Configuração privada | `/etc/meuprojeto/backend.env` |
| Frontend publicado | `/var/www/meuprojeto` |
| IP público | `IP_PUBLICO` — substituir pelo IP real |
| Domínio | `DOMINIO` — substituir pelo nome completo real |
| Chave no Windows | `C:\Users\SEU_USUARIO\Downloads\vm-meuprojeto_key.pem` |
| Backend no Windows | `C:\CAMINHO\backend` |
| Frontend no Windows | `C:\CAMINHO\frontend` |

**Não executar comandos com `IP_PUBLICO`, `DOMINIO`, `C:\CAMINHO` ou senhas de exemplo sem substituir os valores.** Os marcadores não são variáveis criadas automaticamente.

Confirme no projeto antes de começar:

- Versão do Java em `pom.xml` ou no build Gradle.
- Nome do JAR executável gerado.
- Configuração do MySQL e necessidade de importar dados.
- Rotas reais da API e do frontend.
- Variáveis exigidas por integrações externas.
- Existência de migrações de banco e comandos de build/teste.

Este guia exemplifica Java 17 e Maven. Use a versão exigida pelo projeto.

## 3. Segurança de configurações no código

Não colocar senha real ou chave de serviço externo no `application.properties` versionado. Exemplo de configuração orientada por ambiente:

```properties
spring.application.name=meuprojeto

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/meuprojeto}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:meuprojeto_app}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

server.address=${SERVER_ADDRESS:127.0.0.1}
server.port=${SERVER_PORT:8080}
server.forward-headers-strategy=framework

spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# Somente se o código do projeto utiliza esta propriedade:
# groq.api.key=${GROQ_API_KEY:}
```

Desativar `open-in-view` pode exigir ajuste em código que acessa relações lazy fora de transações. Validar os fluxos antes de alterar um projeto existente.

### Criação de tabelas

Defina explicitamente a estratégia do projeto:

- Preferível para evolução controlada: Flyway/Liquibase e Hibernate em `validate`.
- Para um protótipo que já depende do Hibernate criando tabelas: `update`, com backup antes de cada mudança de entidades.

Não usar `create` ou `create-drop` no banco persistente de produção: essas opções podem recriar ou apagar a estrutura. H2 em memória com `create-drop` pode ser adequado aos testes isolados, não ao banco real.

## 4. Preparar a assinatura e escolher a VM

### 4.1 Crédito, política e cota

No Azure, verifique separadamente:

1. Saldo e vencimento dos créditos no painel Educação, se a assinatura for estudantil.
2. Regiões permitidas em Política → Atribuições → política de regiões → Parâmetros.
3. Cota regional de vCPUs e cota da família de VM em Cotas → Compute.
4. Disponibilidade efetiva do tamanho na região e na assinatura.
5. Preço final na revisão da criação.

Uma cota positiva não garante disponibilidade. Um modelo listado com preço pode continuar bloqueado. `NotAvailableForSubscription` não é o mesmo erro que cota insuficiente.

### 4.2 Dimensionamento inicial

| Uso | Ponto de partida para avaliar |
|---|---|
| Protótipo com pouco acesso | 2 vCPUs e 4 GiB |
| Mais folga para Java e MySQL juntos | 2 vCPUs e 8 GiB |
| Evento ou muitos acessos simultâneos | Definir após teste de carga |

Esses tamanhos são pontos de partida, não garantias de capacidade. Memória, CPU, consultas, índices, polling, conexões e chamadas externas influenciam o resultado.

Modelos da série B usam créditos de CPU e podem reduzir desempenho sob carga sustentada. Compare o preço com modelos de CPU sustentada que estejam disponíveis. Evite selecionar uma família antiga sem verificar os avisos de retirada e alternativas.

### 4.3 Campos do portal

| Campo | Configuração sugerida |
|---|---|
| Assinatura | A que contém os créditos/orçamento |
| Grupo de recursos | Criar grupo exclusivo para o projeto |
| Nome | `vm-meuprojeto` |
| Região | Uma permitida, com cota e modelo implantável |
| Disponibilidade | Sem redundância de infraestrutura, se o projeto aceitar VM única |
| Sistema | Ubuntu Server 24.04 LTS, x64 |
| Tamanho | Conforme cota, preço e carga esperada |
| Spot | Desativado para evitar interrupção por retomada de capacidade |
| Autenticação | Chave pública SSH |
| Usuário | `azureuser` |
| Disco | SSD Standard LRS; cerca de 30–32 GiB para iniciar se o volume de dados permitir |
| Disco efêmero | Não utilizar para o banco persistente |
| IP público | IPv4 estático para acesso público |
| NSG | Associar à interface ou sub-rede e conferir regras efetivas |
| Balanceador | Nenhum para esta arquitetura simples |

VM única não fornece alta disponibilidade. Uma falha ou manutenção afeta site, backend e banco ao mesmo tempo.

Ative exclusão de disco/IP/NIC junto com a VM apenas se esse for o ciclo de vida desejado; faça backup externo antes de excluir recursos que contêm dados.

### 4.4 Regras de entrada

| Porta | Protocolo | Origem | Uso |
|---|---|---|---|
| 22 | TCP | IP público do administrador | SSH |
| 80 | TCP | Internet | HTTP e validação do certificado |
| 443 | TCP | Internet | HTTPS |

Não publicar 3306 ou 8080. Cada regra deve ter prioridade livre, antes da negação padrão. Exemplos: 300, 310 e 320.

Se o IP administrativo mudar, atualize a regra SSH no portal. Confirme o acesso antes de encerrar uma sessão existente.

## 5. Conectar pelo Windows

No **PowerShell do Windows**:

```powershell
ssh -i "$env:USERPROFILE\Downloads\vm-meuprojeto_key.pem" azureuser@IP_PUBLICO
```

Substitua o IP e o nome do arquivo. No primeiro acesso, verifique a impressão digital do host por um canal confiável do Azure antes de aceitá-la.

### Erro de permissões da chave

Se o OpenSSH informar `UNPROTECTED PRIVATE KEY FILE`, restrinja o arquivo à sua conta no PowerShell:

```powershell
$chave = "$env:USERPROFILE\Downloads\vm-meuprojeto_key.pem"
$usuario = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$acl = Get-Acl -LiteralPath $chave
$acl.SetAccessRuleProtection($true, $false)
foreach ($regra in @($acl.Access)) {
    [void]$acl.RemoveAccessRuleSpecific($regra)
}
$permissao = [System.Security.AccessControl.FileSystemAccessRule]::new(
    $usuario, "Read", "Allow"
)
$acl.AddAccessRule($permissao)
Set-Acl -LiteralPath $chave -AclObject $acl
```

Não compartilhe o `.pem`, nem o coloque no repositório ou no diretório público do site.

### Identificar onde executar

- `PS C:\...>`: Windows; usar `ssh`, `scp` e build local.
- `azureuser@...:~$`: Ubuntu; instalar pacotes e configurar serviços.
- `mysql>`: SQL no cliente MySQL.

Copie somente o comando, não o prefixo. Se a colagem misturar linhas, execute uma por vez. Se o SSH cair, reconecte antes de continuar. No Windows PowerShell, use `curl.exe` se precisar do curl real; `curl` pode ser um alias de outra ferramenta.

## 6. Instalar dependências na VM

No **Ubuntu**, uma linha por vez:

```bash
sudo apt update
```

```bash
sudo apt install -y openjdk-17-jre-headless mysql-server nginx python3
```

```bash
sudo systemctl enable --now mysql nginx
```

```bash
java -version
```

```bash
sudo systemctl is-active mysql nginx
```

Esperado: Java na versão do projeto e `active` para os dois serviços. Atualizar o catálogo com `apt update` não instala os pacotes por si só.

## 7. Configurar o banco MySQL

### 7.1 Criar banco

No Ubuntu:

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS meuprojeto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 7.2 Criar usuário exclusivo

```bash
sudo env MYSQL_HISTFILE=/dev/null mysql
```

No prompt `mysql>`, substitua a senha de exemplo:

```sql
CREATE USER 'meuprojeto_app'@'localhost' IDENTIFIED BY 'SENHA_FORTE_EXCLUSIVA';
GRANT ALL PRIVILEGES ON meuprojeto.* TO 'meuprojeto_app'@'localhost';
EXIT;
```

As permissões acima permitem gerenciar a estrutura somente desse banco, útil para protótipos com Hibernate `update`. Em uma configuração com migrações separadas, reduzir o usuário de execução aos privilégios necessários e usar outro usuário para migrações.

Não habilitar o acesso externo do `root` e não alterar a autenticação administrativa do Ubuntu apenas para o backend conectar.

### 7.3 Testar

```bash
mysql -u meuprojeto_app -p -D meuprojeto -e "SELECT 1;"
```

Digite a senha no prompt. Ela não aparece. Esperado: retorno `1`.

### 7.4 Banco vazio ou importação

Criar o banco não importa os dados do computador. Se for necessário migrar dados, exportar um dump da origem, transferir de forma privada e importar antes de liberar o site. Verificar compatibilidade de versões e o banco selecionado. Não importar um dump desconhecido sobre um banco em uso sem backup.

## 8. Gerar o JAR no computador

No **PowerShell**:

```powershell
cd C:\CAMINHO\backend
.\mvnw.cmd clean package
```

Só prossiga após `BUILD SUCCESS`. Não ignorar testes automaticamente para contornar erro de configuração.

Se um teste de contexto depende de MySQL local, uma opção é configurar H2 isolado em testes, quando compatível com o comportamento testado. Isso não substitui testes de compatibilidade do banco real quando necessários.

Identifique o JAR executável em `target`. Para Spring Boot, use o JAR reempacotado, não `.jar.original`.

Enviar com um nome simples:

```powershell
scp -i "$env:USERPROFILE\Downloads\vm-meuprojeto_key.pem" "C:\CAMINHO\backend\target\NOME_REAL_DO_ARQUIVO.jar" azureuser@IP_PUBLICO:/home/azureuser/backend-next.jar
```

## 9. Criar usuário Linux e diretórios da aplicação

Os próximos comandos destinam-se à primeira configuração de uma VM nova. Se o usuário ou diretório já existir, conferir o estado antes de repetir.

No Ubuntu:

```bash
sudo useradd --system --user-group --home-dir /opt/meuprojeto --shell /usr/sbin/nologin meuprojeto
```

```bash
sudo install -d -o root -g meuprojeto -m 0750 /opt/meuprojeto
```

```bash
sudo install -d -o root -g root -m 0700 /etc/meuprojeto
```

```bash
sudo install -o root -g meuprojeto -m 0640 /home/azureuser/backend-next.jar /opt/meuprojeto/backend.jar
```

Essa configuração executa o Java com um usuário dedicado, separado do administrador SSH. O usuário da aplicação pode ler o JAR, mas não modificá-lo.

Se o projeto grava arquivos, criar um diretório próprio com permissão para esse usuário, por exemplo `/var/lib/meuprojeto`, e configurar o caminho no projeto. Não abrir permissões gerais no diretório do código.

## 10. Criar arquivo de ambiente privado

```bash
sudo nano /etc/meuprojeto/backend.env
```

Inserir, com valores reais:

```dotenv
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/meuprojeto
SPRING_DATASOURCE_USERNAME=meuprojeto_app
SPRING_DATASOURCE_PASSWORD="SENHA_REAL_DO_USUARIO_MYSQL"
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8080
SERVER_FORWARD_HEADERS_STRATEGY=framework
SPRING_JPA_SHOW_SQL=false
```

Se o projeto já depende de `update` para criar tabelas, incluir conscientemente:

```dotenv
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

Se há migrações, usar a configuração correspondente do projeto em vez de adicionar `update`.

Para serviços externos, acrescentar as variáveis que o código efetivamente lê. Exemplos, somente quando aplicáveis:

```dotenv
GROQ_API_KEY=CHAVE_REAL
```

Para links públicos, usar a propriedade esperada pelo projeto; não há um nome universal:

```dotenv
# Exemplo, se o código lê APP_PUBLIC_BASE_URL:
APP_PUBLIC_BASE_URL=https://DOMINIO
```

O arquivo do systemd não é um script shell: não usar `export`, substituição `$(...)` ou referências a outras variáveis esperando expansão automática. Para senha entre aspas duplas, aspas e barras invertidas internas precisam ser escapadas corretamente. Uma senha aleatória longa em hexadecimal ou alfanumérica reduz erros de transcrição; guardar em gerenciador de senhas.

Salvar no nano com Ctrl+O, Enter e sair com Ctrl+X. Se um prompt secundário estiver aberto, cancelar esse prompt antes de salvar. Não enviar capturas com segredos visíveis.

Aplicar permissões:

```bash
sudo chmod 600 /etc/meuprojeto/backend.env
```

O systemd iniciado por root lê o arquivo antes de executar o processo como `meuprojeto`. Não é necessário tornar esse arquivo público nem copiá-lo para o frontend.

## 11. Criar o serviço systemd

No Ubuntu:

```bash
sudo nano /etc/systemd/system/meuprojeto.service
```

Conteúdo:

```ini
[Unit]
Description=Backend Meu Projeto
After=network.target mysql.service
Requires=mysql.service

[Service]
User=meuprojeto
Group=meuprojeto
WorkingDirectory=/opt/meuprojeto
EnvironmentFile=/etc/meuprojeto/backend.env
ExecStart=/usr/bin/java -Xms128m -Xmx1g -jar /opt/meuprojeto/backend.jar
Restart=on-failure
RestartSec=5
UMask=0077
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

O heap máximo de 1 GiB é um ponto de partida, não um limite total de memória da JVM. Ajustar após medição e deixar espaço para MySQL, sistema e Nginx.

Ativar, uma linha por vez:

```bash
sudo systemctl daemon-reload
```

```bash
sudo systemctl enable --now meuprojeto
```

Aguardar a inicialização e conferir:

```bash
sudo systemctl status meuprojeto --no-pager -l
```

```bash
sudo journalctl -u meuprojeto -n 80 --no-pager
```

Teste um endpoint real, substituindo o caminho:

```bash
curl -i http://127.0.0.1:8080/api/ENDPOINT_REAL
```

Um serviço `active` pode ainda estar iniciando. Validar uma resposta da aplicação, não apenas a existência do processo. `404` pode significar rota errada, e não backend parado.

## 12. Preparar e enviar o frontend

### 12.1 Endereço da API

O frontend deve chamar o mesmo domínio do site por caminhos relativos, por exemplo:

```javascript
fetch('/api/usuarios');
```

Ou utilizar uma base configurável:

```javascript
const API_BASE = window.APP_API_URL || location.origin;
fetch(API_BASE + '/api/usuarios');
```

O nome `APP_API_URL` só funciona se o código tiver sido implementado para lê-lo. Não adicionar uma variável arbitrária esperando que todas as páginas a utilizem.

Pesquisar referências a `localhost`, `127.0.0.1`, `:8080` e URLs HTTP nos arquivos. No aparelho de um visitante, `localhost` é o aparelho dele. Em HTTPS, chamadas HTTP também podem ser bloqueadas como conteúdo misto.

Corrigir no código fonte e testar antes de enviar. Evitar substituições globais sem revisão, pois textos e URLs de outros serviços podem exigir tratamento diferente.

### 12.2 Enviar

Para um frontend estático simples, no PowerShell:

```powershell
scp -i "$env:USERPROFILE\Downloads\vm-meuprojeto_key.pem" -r "C:\CAMINHO\frontend" azureuser@IP_PUBLICO:/home/azureuser/frontend-inicial
```

Se essa pasta de destino já existir, usar outro nome e conferir a estrutura para não criar uma subpasta inesperada.

No Ubuntu:

```bash
find /home/azureuser/frontend-inicial -maxdepth 3 -name index.html
```

Após confirmar que `index.html` está diretamente na pasta esperada:

```bash
sudo install -d -m 0755 /var/www/meuprojeto
```

```bash
sudo cp -a /home/azureuser/frontend-inicial/. /var/www/meuprojeto/
```

```bash
sudo chown -R root:root /var/www/meuprojeto
```

```bash
sudo find /var/www/meuprojeto -type d -exec chmod 755 {} +
```

```bash
sudo find /var/www/meuprojeto -type f -exec chmod 644 {} +
```

**A pasta pública deve conter somente arquivos publicáveis.** Não enviar `.git`, `.env`, dumps, chaves, configurações privadas ou a pasta inteira de um backend. Para frameworks, publicar somente a saída estática do build.

## 13. Criar DNS gratuito do Azure

Na VM, abrir o recurso de IP público → Configuração → rótulo de nome DNS. Escolher um nome disponível e salvar.

Copiar o nome completo exibido. Ele pode conter região e outros componentes; não deduzir o endereço somente pelo rótulo.

No Ubuntu, substituir `DOMINIO`:

```bash
getent ahostsv4 DOMINIO
```

Conferir se aponta para o IP público correto. Esperar a propagação antes de emitir o certificado.

Também é possível usar um domínio próprio com registro DNS apontando ao IP. Este roteiro não exige comprar um domínio.

## 14. Configurar Nginx

### 14.1 Conferir rotas

O exemplo abaixo encaminha **somente `/api/`** ao backend. Se o projeto usa `/login`, `/chat`, `/usuarios` ou outras rotas sem esse prefixo, adicionar locais correspondentes conforme descrito adiante.

Não reutilizar cegamente as rotas de outro projeto. Uma rota `/chat` pode ser API, enquanto `/chat.html` é página do frontend.

### 14.2 Arquivo do site

```bash
sudo nano /etc/nginx/sites-available/meuprojeto
```

Substituir `DOMINIO` no conteúdo:

```nginx
server {
    listen 80;
    server_name DOMINIO;

    root /var/www/meuprojeto;
    index index.html;
    client_max_body_size 5m;

    location / {
        try_files $uri $uri/ =404;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

Sem barra após `8080`, `proxy_pass` preserva `/api/` no caminho enviado ao Spring Boot. Acrescentar uma barra nessa diretiva pode mudar a rota que chega ao backend.

Se existirem outras rotas de API, adicionar um bloco no mesmo `server`, adaptando a lista:

```nginx
location ~ ^/(login|usuarios|chat)(/|$) {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 60s;
}
```

Para SPA com roteamento no navegador, avaliar `try_files $uri $uri/ /index.html` no bloco estático. Não aplicar essa mudança automaticamente a sites com páginas HTML independentes ou à localização da API.

WebSocket, SSE e uploads grandes exigem ajustes próprios de proxy, timeout e buffering; este exemplo não cobre essas integrações automaticamente.

### 14.3 Ativar e testar

```bash
sudo ln -sfn /etc/nginx/sites-available/meuprojeto /etc/nginx/sites-enabled/meuprojeto
```

```bash
sudo nginx -t
```

Somente se o teste passar:

```bash
sudo systemctl reload nginx
```

```bash
curl -I -H 'Host: DOMINIO' http://127.0.0.1/
```

```bash
curl -i -H 'Host: DOMINIO' http://127.0.0.1/api/ENDPOINT_REAL
```

Testar também externamente em `http://DOMINIO`, após liberar a porta 80. Não é necessário apagar configurações de outros sites. Em uma VM nova, o site padrão pode continuar como fallback se não houver conflito de nomes; sempre testar com o Host correto.

## 15. Configurar HTTPS

Antes de emitir:

- DNS precisa apontar à VM.
- Nginx deve responder na porta 80 pelo domínio.
- Regras de rede devem permitir 80 e 443.
- `server_name` deve conter o domínio exato.

No Ubuntu:

```bash
sudo apt install -y certbot python3-certbot-nginx
```

Guardar cópia da configuração antes da alteração automática:

```bash
sudo cp /etc/nginx/sites-available/meuprojeto /etc/nginx/sites-available/meuprojeto.before-https
```

Emitir, substituindo `DOMINIO`:

```bash
sudo certbot --nginx -d DOMINIO --redirect
```

Informar e-mail de contato, avaliar e aceitar os termos se estiver de acordo. A opção de compartilhamento de e-mail é independente da emissão.

Validar:

```bash
sudo nginx -t
```

```bash
curl -I https://DOMINIO/
```

```bash
sudo certbot renew --dry-run
```

Conferir a tarefa de renovação disponibilizada pelo pacote:

```bash
systemctl list-timers --all | grep -i certbot
```

O Certbot altera o Nginx para carregar o certificado e redirecionar HTTP. Não substituir depois o arquivo por uma configuração antiga sem TLS. Não publicar a chave privada do certificado.

GPS no navegador requer contexto seguro e permissão do usuário. HTTPS é necessário, mas não garante rastreamento contínuo em segundo plano.

## 16. Diagnosticar problemas de HTTPS

Teste local preservando domínio e validação do certificado:

```bash
curl -I --resolve DOMINIO:443:127.0.0.1 https://DOMINIO/
```

Se funcionar localmente e der timeout externamente, conferir regras do Azure, IP/DNS e firewall do Ubuntu:

```bash
sudo ss -lntp
```

```bash
sudo ufw status
```

Se o UFW estiver ativo, verificar as regras antes de alterar. Não habilitar um firewall novo remotamente sem permitir SSH, pois isso pode bloquear o administrador.

Um certificado emitido não abre automaticamente a porta 443 no Azure.

## 17. Validar integrações externas

Uma chave presente no arquivo não significa chave válida. Após mudar uma variável:

```bash
sudo systemctl restart meuprojeto
```

Testar o endpoint real do backend e consultar logs. Não imprimir chaves, arquivo `.env` ou cabeçalhos Authorization na conversa ou em capturas.

Para um projeto que implementa um `POST /chat` com corpo de texto, um exemplo de teste seria:

```bash
curl --max-time 40 -sS -H 'Content-Type: text/plain' --data 'Responda apenas: teste funcionando' http://127.0.0.1:8080/chat
```

Esse comando só se aplica se a API tiver esse contrato. Não interpretar um `GET` que retorna texto fixo como validação de credencial de IA.

Tratar separadamente credencial inválida, limites de uso, indisponibilidade e timeout do provedor. Créditos do Azure não cobrem automaticamente cobranças de APIs externas.

## 18. Teste funcional antes de divulgar

Usar dados fictícios e verificar:

1. Página inicial, estilos, imagens e navegação.
2. Cadastro e login, se existirem.
3. Chamadas no navegador sem `localhost` e sem conteúdo misto.
4. Autorização: uma conta não deve acessar dados privados de outra.
5. CRUD e persistência após reiniciar o backend.
6. Integrações externas, limites e mensagens de erro.
7. GPS e links compartilhados em um segundo aparelho, se existirem.
8. Recuperação de senha com comprovação de identidade, se existir.
9. Reconexão do site após fechar a sessão SSH.
10. Comportamento sob a carga prevista.

Um retorno HTTP 200 do servidor não confirma todos esses fluxos.

## 19. Atualizar o backend sem sobrescrever o JAR em uso

Compilar/testar no Windows e enviar para `/home/azureuser/backend-next.jar`, como no primeiro envio.

Antes de uma atualização que altera dados ou entidades, fazer backup do banco.

No Ubuntu, guardar o JAR anterior:

```bash
sudo cp /opt/meuprojeto/backend.jar /opt/meuprojeto/backend-previous.jar
```

```bash
sudo systemctl stop meuprojeto
```

```bash
sudo install -o root -g meuprojeto -m 0640 /home/azureuser/backend-next.jar /opt/meuprojeto/backend.jar
```

```bash
sudo systemctl start meuprojeto
```

Validar serviço, logs e endpoint. Se necessário restaurar o JAR anterior com o serviço parado. Rollback do JAR não desfaz migrações ou mudanças no banco.

Alterar apenas o arquivo `.env` exige reinício do serviço. Alterar `.service` exige `sudo systemctl daemon-reload` e reinício.

## 20. Atualizar o frontend

Enviar a versão nova para uma pasta intermediária nova. Conferir o conteúdo e guardar backup da versão publicada antes de copiar.

Não reenviar para uma pasta existente sem verificar como o `scp` organiza os diretórios; isso pode criar uma pasta adicional e causar 404.

Preferir preparar uma pasta completa nova com as permissões certas e trocar a versão de forma controlada. Uma cópia por cima mantém arquivos antigos que foram removidos do código fonte; revisar esse caso, em especial se há arquivos que deixaram de poder ser públicos.

Arquivos estáticos normalmente não exigem reinício do backend. Recarregar o navegador sem cache e verificar as chamadas de API após a atualização.

## 21. Backup

### 21.1 Exportar banco

No Ubuntu:

```bash
mkdir -p ~/backups
chmod 700 ~/backups
```

```bash
(umask 077; sudo mysqldump --single-transaction --no-tablespaces --databases meuprojeto > ~/backups/meuprojeto-$(date +%Y%m%d-%H%M%S).sql)
```

Verificar saída, tamanho e conteúdo esperado sem divulgar dados pessoais. O snapshot transacional é adequado às tabelas InnoDB; não executar alterações de estrutura simultaneamente ao dump.

Copiar o backup para fora da VM. Um arquivo no mesmo disco não protege contra exclusão ou perda da máquina. Guardar também configurações privadas, certificados quando necessário, frontend e versão do JAR, com controle de acesso adequado.

### 21.2 Restauração

Testar a restauração em ambiente isolado antes de depender do backup. Um dump com `--databases` pode conter comandos para selecionar e criar o banco original; revisar antes de restaurar em outra base.

Não executar `DROP DATABASE`, `TRUNCATE` ou uma importação destrutiva sem confirmar o recurso, o banco, o escopo e a existência de backup verificável.

Definir periodicidade e retenção conforme importância e volume dos dados. Dumps e logs podem conter dados pessoais e segredos.

## 22. Comandos de manutenção

### Serviços e logs

```bash
sudo systemctl status meuprojeto --no-pager -l
sudo systemctl restart meuprojeto
sudo systemctl is-active mysql nginx
sudo journalctl -u meuprojeto -n 100 --no-pager
sudo journalctl -u meuprojeto -f
sudo tail -n 50 /var/log/nginx/error.log
```

Ctrl+C sai do acompanhamento de logs e não encerra o serviço.

### Recursos

```bash
free -h
df -h /
sudo du -sh /var/lib/mysql /var/www/meuprojeto /opt/meuprojeto
sudo journalctl --disk-usage
sudo ss -lntp
```

Monitorar crescimento do banco, logs, backups e uso de CPU. Não prometer número de usuários com base apenas no tamanho do JAR ou RAM livre em repouso.

### Configuração e certificado

```bash
sudo nginx -t
sudo certbot certificates
sudo certbot renew --dry-run
```

`sudo nginx -T` mostra a configuração efetiva; revisar antes de compartilhar porque configurações de outros serviços podem conter informações privadas.

## 23. Problemas comuns

| Sintoma | Causa provável ou checagem |
|---|---|
| Região recusada | Política de regiões da assinatura |
| Cota insuficiente | Limite regional ou da família, não saldo financeiro |
| `NotAvailableForSubscription` | Modelo restrito/indisponível; cota positiva não basta |
| Chave SSH ignorada | Permissões do arquivo no Windows |
| Java não encontrado | Pacote não instalado ou versão/caminho incorreto |
| Backend usa usuário errado no banco | Variáveis ausentes, arquivo errado ou falta de reinício |
| `Unable to determine Dialect` | Examinar a causa JDBC anterior; verificar conexão e credenciais antes de fixar dialeto |
| Serviço falha imediatamente | JAR, permissões, Java, `.env` e logs do systemd |
| Nginx 404 | Pasta, index, Host ou caminho estático incorreto |
| Nginx 502 | Backend parado, ainda iniciando ou porta errada |
| HTTPS timeout | Porta 443, regras efetivas, firewall ou listener |
| Certbot falha na validação | DNS, porta 80, Host e acesso externo |
| Frontend funciona só no computador | `localhost` no JavaScript ou acesso não publicado |
| API chama rota errada | Prefixo `/api`, configuração do proxy e contrato do projeto |
| Chave externa inválida | Substituir no arquivo privado e reiniciar; não expor o segredo |
| Falha depois de sair do SSH | Aplicação foi iniciada manualmente em vez de serviço |

## 24. Custos e encerramento de recursos

Calcular o custo somando:

- Processamento da VM por hora e período em execução.
- Disco provisionado, snapshots e backups.
- IP público e tráfego sujeito a cobrança.
- Monitoramento ou outros serviços ativados.
- Serviços externos, cobrados separadamente quando aplicável.

O preço exibido para o tamanho da VM não é necessariamente o custo total. Criar alertas e revisar consumo real no portal. Orçamentos e alertas não devem ser tratados como bloqueio automático de gastos.

Ao suspender o uso, usar a opção de parar/desalocar no Azure e confirmar o estado. Discos e IPs podem continuar cobrando. Ao encerrar definitivamente, fazer backup externo e conferir os recursos remanescentes no grupo antes de excluí-los.

## 25. Checklist de conclusão

- [ ] VM criada na região e família permitidas, preço revisado.
- [ ] SSH restrito e chave privada protegida.
- [ ] MySQL local, banco e usuário exclusivo testados.
- [ ] JAR gerado com testes passando.
- [ ] Backend executando como serviço com usuário dedicado.
- [ ] Arquivo de segredos privado e fora do Git.
- [ ] Frontend publicado sem segredos ou `localhost` indevido.
- [ ] Rotas reais do projeto configuradas no proxy.
- [ ] DNS correto e portas 80/443 acessíveis.
- [ ] Certificado instalado e renovação simulada validada.
- [ ] Links públicos gerados com HTTPS e domínio corretos.
- [ ] Fluxos funcionais e autorização testados.
- [ ] Credenciais expostas durante desenvolvimento revogadas/substituídas.
- [ ] Backup externo e restauração testados.
- [ ] Custos, vencimento de créditos e alertas revisados.
- [ ] Capacidade avaliada com carga representativa.

## 26. Referências oficiais para consulta

- Azure: https://portal.azure.com/
- VM Linux no portal: https://learn.microsoft.com/en-us/azure/virtual-machines/linux/quick-create-portal
- SSH em VM Linux: https://learn.microsoft.com/en-us/azure/virtual-machines/linux-vm-connect
- Cotas: https://learn.microsoft.com/en-us/azure/virtual-machines/quotas
- Calculadora Azure: https://azure.microsoft.com/en-us/pricing/calculator/
- Certbot: https://eff-certbot.readthedocs.io/en/stable/using.html
- Nginx proxy: https://nginx.org/en/docs/http/ngx_http_proxy_module.html
- Spring Boot: https://docs.spring.io/spring-boot/reference/

Consultar versões, políticas e custos atuais antes de uma nova implantação. Adaptar este guia ao contrato real da aplicação; ele não substitui a leitura das configurações do outro projeto.
