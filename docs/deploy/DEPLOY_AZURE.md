# Publicação da Rede Rosa no Azure

Documento elaborado em 10/11/2026, com base na implantação acompanhada em 09 e 10/11/2026.

Este documento registra o ambiente criado, os comandos utilizados, os problemas resolvidos e os procedimentos de manutenção. Nenhuma senha ou chave privada é incluída. As seções de pendências e manutenção não significam que essas ações já foram executadas.

## 1. Resultado da implantação

A Rede Rosa foi publicada em uma máquina virtual Ubuntu no Azure. O frontend é servido pelo Nginx, que também encaminha as requisições da API para o Spring Boot. O backend utiliza o MySQL instalado na mesma máquina e chama a API externa da Groq para a assistente Rosa.

Endereço público:

**https://valdineyteste.eastus2.cloudapp.azure.com**

Página do chat:

**https://valdineyteste.eastus2.cloudapp.azure.com/chat.html**

Foi confirmado durante a sessão:

- VM criada e em execução.
- MySQL e Nginx ativos.
- Banco `rederosa` criado e acesso do usuário da aplicação validado com `SELECT 1`.
- Build Maven concluído com `BUILD SUCCESS`, após corrigir a configuração de um teste.
- Backend iniciado com sucesso e registrado como serviço do systemd.
- API de salas retornando HTTP 200 pelo Nginx, com as três salas iniciais.
- Frontend acessível publicamente por HTTP antes da configuração do certificado.
- Certificado HTTPS emitido e instalado pelo Certbot.
- Chave inválida da Groq substituída na configuração do serviço.
- Teste direto da IA retornando `teste funcionando`.
- Usuário confirmou que testou a IA no site e que funcionou.

Não foi registrado um teste completo de todas as funcionalidades, teste de carga, restauração de backup ou validação do GPS em vários aparelhos.

## 2. Arquitetura

```text
Navegador / celular
        |
        | HTTPS — porta 443
        v
IP público do Azure / DNS gratuito do Azure
        |
        v
Nginx
  |-- arquivos HTML, CSS, JavaScript e imagens
  |
  +-- rotas de API --> Spring Boot / Java 17
                         127.0.0.1:8080
                           |
                           +--> MySQL local / banco rederosa
                           |
                           +--> API externa da Groq
```

O acesso HTTP na porta 80 é redirecionado para HTTPS pelo Certbot. A porta 80 também é importante para a validação e renovação do certificado na configuração utilizada.

O Java está configurado para escutar em `127.0.0.1`, portanto a porta 8080 não precisa ser publicada na internet. O MySQL também não precisa de uma regra pública de entrada: apenas o backend local deve acessá-lo.

O modelo de IA não roda na VM. A VM executa o código que chama a Groq pela internet.

## 3. Recursos e custos

| Item | Configuração registrada |
|---|---|
| Assinatura | Azure for Students |
| Grupo de recursos | `rg-rederosa` |
| Máquina virtual | `vm-rederosa` |
| Região | East US 2 |
| Zona | Zona 1 |
| Tamanho final | `Standard_D2s_v3` |
| CPU | 2 vCPUs |
| Memória | 8 GiB |
| Sistema | Ubuntu Server 24.04 LTS, x64 |
| Segurança | Inicialização confiável, Secure Boot e vTPM ativados no resumo de criação |
| Compra | Sob demanda; Spot desativado |
| Disco do sistema | Tamanho padrão da imagem, 30 GiB; SSD Standard com redundância local |
| IP público | `135.237.160.112` |
| Atribuição do IP | Estática, conforme tela do recurso de IP |
| DNS | `valdineyteste.eastus2.cloudapp.azure.com` |
| Usuário Linux | `azureuser` |
| Autenticação | Chave SSH RSA |
| Balanceador de carga | Não utilizado |

Inicialmente foi planejada uma VM de 4 GiB. Os modelos econômicos pesquisados apresentaram restrições de assinatura, disponibilidade ou cota. O ambiente efetivamente criado tem **8 GiB**, e não 4 GiB.

A tela de criação mostrou **US$ 0,0960 por hora**, aproximadamente **US$ 70,08 por 730 horas**, apenas para processamento da VM. Disco, IP público, tráfego excedente e serviços adicionais podem gerar cobranças separadas.

Foi usada uma estimativa de planejamento de aproximadamente US$ 80–90 por mês para VM e recursos básicos. Esse intervalo não é uma fatura nem uma cotação garantida. Conferir o consumo real em Gerenciamento de Custos no portal.

O painel de Educação exibiu **US$ 538 de créditos disponíveis**, gasto de novembro zerado naquele momento e vencimento em **10/02/2027**. Esse saldo foi observado antes da operação contínua da VM; não representa o saldo atualizado.

Os créditos têm validade mesmo que sobre saldo. Desalocar a VM interrompe a cobrança de processamento, mas não necessariamente a de disco e IP. Parar apenas o Ubuntu não equivale necessariamente a desalocar o recurso no Azure.

## 4. Restrições encontradas na conta educacional

A política `Allowed resource deployment regions` apresentou as regiões:

- `northcentralus` — North Central US.
- `centralus` — Central US.
- `chilecentral` — Chile Central.
- `eastus2` — East US 2.
- `canadacentral` — Canada Central.

East US e Brazil South foram bloqueadas pela política da assinatura. East US 2 foi a região usada no resultado final.

Problemas encontrados:

| Modelo ou configuração | Resultado observado |
|---|---|
| B2s, 4 GiB, East US 2 | `NotAvailableForSubscription` |
| D2als_v6, 4 GiB | Falha de validação: cota zero para a família Dalsv6 em East US 2; precisava de 2 vCPUs |
| B2als_v2, 4 GiB | Indisponível para a assinatura, apesar de haver cota na família Basv2 |
| Localizar opções implantáveis para B2als_v2 | Nenhuma opção encontrada pelo portal |
| D2as_v4, 8 GiB | Indisponível na configuração consultada |
| D2s_v3, 8 GiB | Implantação concluída |

Ter créditos, cota de CPU e região autorizada são condições diferentes. Uma família pode ter cota positiva e continuar sem um modelo implantável. Não é correto afirmar que toda conta educacional proíbe banco gerenciado ou permite qualquer VM.

O portal classificou a série D v3 como fim de vida útil. Não registramos uma data de retirada; acompanhar avisos do Azure e planejar eventual troca de tamanho quando houver uma opção compatível disponível.

## 5. Localização dos projetos e arquivos

### Computador Windows

| Conteúdo | Caminho |
|---|---|
| Backend | `C:\Users\Valdiney\IdeaProjects\rederosa-back` |
| Frontend | `C:\Users\Valdiney\Documents\workspace\rederosa` |
| Configuração local | `src/main/resources/application.properties` no backend |
| JAR gerado | `target/rederosa-back-0.0.1-SNAPSHOT.jar` |
| Chave SSH | `C:\Users\Valdiney\Downloads\vm-rederosa_key.pem` |

A chave `.pem` autentica o acesso administrativo à VM. Não colocar no Git, enviar no chat ou copiar para a pasta pública do site.

### Máquina virtual

| Conteúdo | Caminho |
|---|---|
| Backend executável | `/home/azureuser/rederosa-back.jar` |
| Cópia enviada do frontend | `/home/azureuser/frontend` |
| Frontend servido pelo Nginx | `/var/www/rederosa` |
| Variáveis do backend | `/home/azureuser/rederosa-config/backend.env` |
| Serviço systemd | `/etc/systemd/system/rederosa.service` |
| Site Nginx | `/etc/nginx/sites-available/rederosa` |
| Link de ativação Nginx | `/etc/nginx/sites-enabled/rederosa` |
| Certificado público | `/etc/letsencrypt/live/valdineyteste.eastus2.cloudapp.azure.com/fullchain.pem` |
| Chave privada do certificado | `/etc/letsencrypt/live/valdineyteste.eastus2.cloudapp.azure.com/privkey.pem` |

## 6. Acesso SSH e permissões da chave

No PowerShell do Windows:

```powershell
ssh -i "$env:USERPROFILE\Downloads\vm-rederosa_key.pem" azureuser@135.237.160.112
```

No primeiro acesso foi apresentada uma impressão digital ED25519. A confirmação adicionou o host ao arquivo de hosts conhecidos do Windows. Em caso de mudança futura da impressão digital, verificar a identidade da VM antes de aceitar a alteração.

O primeiro acesso falhou com `UNPROTECTED PRIVATE KEY FILE` e `bad permissions`. O OpenSSH identificou permissões de leitura para outro grupo do Windows. O problema foi corrigido restringindo a chave à conta do usuário:

```powershell
$chave = "$env:USERPROFILE\Downloads\vm-rederosa_key.pem"
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

Esse procedimento altera as permissões somente da chave indicada.

### Diferenciar os terminais

- `PS C:\...>` indica PowerShell no computador Windows.
- `azureuser@vm-rederosa:~$` indica terminal Ubuntu na VM.
- `mysql>` indica o cliente interativo do MySQL.

Não copiar os prefixos dos terminais junto com os comandos. Durante a sessão, colagens de vários comandos foram cortadas ou misturadas. Quando isso ocorrer, executar uma linha por vez, pressionar Enter e aguardar o retorno do terminal.

Uma desconexão SSH devolve o usuário ao PowerShell. Nesse caso, comandos como `curl` podem passar a executar outra ferramenta: no Windows PowerShell, `curl` pode ser um alias de `Invoke-WebRequest`. Reconectar por SSH antes de usar comandos Linux; para usar o executável de curl no Windows, escrever `curl.exe`.

## 7. Instalação de Java, MySQL e Nginx

No Ubuntu:

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless mysql-server nginx
```

O `apt update` atualiza o catálogo; ele sozinho não instala o Java nem os serviços. Inicialmente apenas essa etapa havia sido executada, causando `java: command not found` e serviços inativos.

Validação e ativação:

```bash
java -version
sudo systemctl enable --now mysql nginx
sudo systemctl is-active mysql nginx
```

Resultado confirmado: Java 17 instalado e dois retornos `active` para os serviços. A saída da instalação apresentou MySQL 8.0 e Nginx 1.24; versões de atualização podem mudar com o tempo.

## 8. Banco de dados

Criação:

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS rederosa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Foi criado um usuário exclusivo da aplicação, sem utilizar `root` no backend:

```bash
sudo env MYSQL_HISTFILE=/dev/null mysql
```

No cliente MySQL:

```sql
CREATE USER 'rederosa_app'@'localhost' IDENTIFIED BY 'SUBSTITUIR_POR_SENHA_FORTE';
GRANT ALL PRIVILEGES ON rederosa.* TO 'rederosa_app'@'localhost';
EXIT;
```

O texto de senha acima é um exemplo e deve ser substituído. O usuário tem permissões no banco `rederosa`, sem privilégios globais. As permissões de criação e alteração de tabelas atendem ao uso atual de `spring.jpa.hibernate.ddl-auto=update`.

Teste executado:

```bash
mysql -u rederosa_app -p -D rederosa -e "SELECT 1;"
```

A senha é digitada no prompt, sem ficar visível. O retorno foi `1`.

O banco da VM foi criado vazio. Não foi feita importação do banco existente no computador. O backend criou sua estrutura e as salas iniciais ao iniciar.

Houve uma pergunta sobre apagar os dados. **Nenhum DROP ou limpeza de registros foi executado durante a sessão.** Uma exclusão futura exige definir o escopo e fazer backup antes.

## 9. Build do backend e correção do teste

O backend utiliza Spring Boot 3.5.0, Java 17 e Maven.

No PowerShell, dentro do projeto:

```powershell
cd C:\Users\Valdiney\IdeaProjects\rederosa-back
.\mvnw.cmd clean package
```

O primeiro build executou 30 testes e terminou com um erro em `RederosaBackApplicationTests.contextLoads`. O teste tentava iniciar o contexto usando a configuração de MySQL local e não conseguia obter os metadados JDBC. A mensagem final incluía `Unable to determine Dialect without JDBC metadata`.

Foi alterado somente o teste `src/test/java/com/example/rederosaback/RederosaBackApplicationTests.java`, adicionando as propriedades:

```java
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
```

O H2 já era dependência de teste e já era usado pelos outros testes de integração. Essa mudança não troca o MySQL usado na VM por H2.

O usuário executou o build novamente e confirmou `BUILD SUCCESS`. O JAR empacotado tinha aproximadamente 52 MB. O arquivo `.jar.original` é o artefato anterior ao reempacotamento; o arquivo que deve ser enviado é o `.jar` executável, sem `.original`.

## 10. Envio e primeira execução do backend

No PowerShell:

```powershell
scp -i "$env:USERPROFILE\Downloads\vm-rederosa_key.pem" "C:\Users\Valdiney\IdeaProjects\rederosa-back\target\rederosa-back-0.0.1-SNAPSHOT.jar" azureuser@135.237.160.112:/home/azureuser/rederosa-back.jar
```

Para a primeira execução manual, a senha foi lida sem eco no terminal Ubuntu:

```bash
read -s -p "Senha do banco: " SPRING_DATASOURCE_PASSWORD
```

Após digitar a senha e pressionar Enter, foram configuradas as variáveis:

```bash
echo
export SPRING_DATASOURCE_PASSWORD
export SPRING_DATASOURCE_USERNAME=rederosa_app
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/rederosa'
export SERVER_ADDRESS=127.0.0.1
java -Xmx1g -jar ~/rederosa-back.jar
```

Na primeira tentativa algumas linhas de `export` não foram executadas. A aplicação tentou conectar como `root`, conforme o `application.properties`, e falhou com `Access denied for user 'root'@'localhost'`. Após exportar todas as variáveis, iniciou com `Started RederosaBackApplication` e criou as salas iniciais.

`-Xmx1g` limita o heap Java a 1 GiB. Não é um limite da memória total do processo: outras áreas da JVM também consomem RAM.

## 11. Configuração persistente do serviço

As configurações foram salvas em:

`/home/azureuser/rederosa-config/backend.env`

Estrutura final esperada, com valores secretos omitidos:

```dotenv
SPRING_DATASOURCE_USERNAME=rederosa_app
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/rederosa
SPRING_DATASOURCE_PASSWORD="VALOR_SECRETO"
SERVER_ADDRESS=127.0.0.1
SPRING_JPA_SHOW_SQL=false
REDE_ROSA_PUBLIC_BASE_URL=https://valdineyteste.eastus2.cloudapp.azure.com
GROQ_API_KEY=VALOR_SECRETO
```

As variáveis de ambiente substituem as propriedades correspondentes empacotadas no JAR. Assim, `SPRING_DATASOURCE_USERNAME` substitui `spring.datasource.username`, e `GROQ_API_KEY` substitui `groq.api.key`.

A configuração inicial foi escrita com `umask 077`, para restringir o acesso ao arquivo. Para conferir as permissões sem ler segredos:

```bash
stat -c '%a %U:%G %n' /home/azureuser/rederosa-config/backend.env
```

Permissão esperada do arquivo: `600`, proprietário `azureuser`. Caso necessário:

```bash
chmod 700 /home/azureuser/rederosa-config
chmod 600 /home/azureuser/rederosa-config/backend.env
```

O serviço criado em `/etc/systemd/system/rederosa.service`:

```ini
[Unit]
Description=Backend Rede Rosa
After=network.target mysql.service
Requires=mysql.service

[Service]
User=azureuser
WorkingDirectory=/home/azureuser
EnvironmentFile=/home/azureuser/rederosa-config/backend.env
ExecStart=/usr/bin/java -Xmx1g -jar /home/azureuser/rederosa-back.jar
Restart=on-failure
RestartSec=5
UMask=0077

[Install]
WantedBy=multi-user.target
```

Ativação:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now rederosa
sudo systemctl status rederosa --no-pager -l
```

Foi confirmado `active (running)`, com consumo aproximado de 280 MB naquele instante. Esse valor foi medido com pouca carga e não representa consumo máximo ou capacidade de usuários.

O serviço continua executando quando a sessão SSH termina e é habilitado para iniciar com a VM. Alterações no arquivo `.env` exigem reiniciar o serviço; alterações no arquivo `.service` exigem também `daemon-reload`.

## 12. Publicação do frontend

O frontend é composto por arquivos HTML, CSS, JavaScript e imagens, sem necessidade de um servidor Node nesta implantação.

Envio no PowerShell:

```powershell
scp -i "$env:USERPROFILE\Downloads\vm-rederosa_key.pem" -r "C:\Users\Valdiney\Documents\workspace\rederosa" azureuser@135.237.160.112:/home/azureuser/frontend
```

Publicação no Ubuntu:

```bash
sudo mkdir -p /var/www/rederosa
sudo cp -a /home/azureuser/frontend/. /var/www/rederosa/
sudo chmod -R a+rX /var/www/rederosa
```

O primeiro teste retornou HTTP 404 porque a configuração do Nginx estava criada, mas `/var/www/rederosa` ainda não existia. Após copiar os arquivos, foi confirmado:

```text
/var/www/rederosa/index.html
```

### Ajustes de endereço da API na cópia publicada

Algumas páginas tinham `http://localhost:8080/` escrito diretamente. No navegador de um visitante, `localhost` aponta para o próprio aparelho, não para a VM.

Também foi identificado que `js/api.js` usa a origem atual em HTTPS, mas em HTTP de desenvolvimento pode usar a porta 8080. Para fixar a origem do site na cópia publicada, foi inserido:

```html
<script>window.REDE_ROSA_API_URL=location.origin;</script>
```

Foi usado um script Python para substituir `http://localhost:8080/` por `/` e adicionar esse script após `<head>` nas páginas publicadas. As modificações ocorreram em `/var/www/rederosa`, não nos arquivos originais do frontend no Windows.

**Ao reenviar o frontend original, esses ajustes precisam ser preservados ou reaplicados.** O procedimento de atualização abaixo evita inserir o script mais de uma vez.

## 13. Configuração do Nginx

Arquivo: `/etc/nginx/sites-available/rederosa`.

Configuração base antes das alterações automáticas do Certbot:

```nginx
server {
    listen 80;
    server_name valdineyteste.eastus2.cloudapp.azure.com;

    root /var/www/rederosa;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }

    location ~ ^/(api|login|usuarias|contatos|ocorrencias|recuperar-senha|chat|checkin|acompanhar|acompanhamento)(/|$) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

As páginas como `/chat.html` continuam sendo arquivos estáticos. A rota `/chat`, sem extensão, é encaminhada ao backend. O mesmo cuidado vale para `/checkin` e `/checkin.html`.

Ativação e validação:

```bash
sudo ln -sfn /etc/nginx/sites-available/rederosa /etc/nginx/sites-enabled/rederosa
sudo nginx -t
sudo systemctl reload nginx
```

Teste que confirmou as salas pelo proxy, antes do HTTPS:

```bash
curl -i -H "Host: 135.237.160.112" http://127.0.0.1/api/comunidade/salas
```

As salas retornadas foram `apoio-emocional`, `direitos-das-mulheres` e `seguranca`.

**Não sobrescrever o arquivo atual com a configuração base acima:** o Certbot adicionou configurações de TLS e redirecionamento. Para consultar a configuração real, usar `sudo nginx -T` na VM.

## 14. Rede e portas no Azure

Foi utilizado um grupo de segurança de rede básico associado à interface da VM, com nome `vm-rederosa-nsg`.

| Regra | Porta | Protocolo | Prioridade usada ou orientada | Finalidade |
|---|---|---|---|---|
| SSH | 22 | TCP | 300 | Administração |
| Allow-HTTP | 80 | TCP | 310 | HTTP e validação do certificado |
| Allow-HTTPS | 443 | TCP | 320 | Site com TLS |

A regra 80 foi confirmada em imagem. Quando o HTTPS apresentou timeout, a imagem das regras mostrou que ainda não existia entrada para 443; foi orientada a criação de `Allow-HTTPS`. Depois, o usuário conseguiu testar a IA no site. Não foi recebida uma nova exportação final das regras.

O SSH apareceu com origem `Qualquer`. Foi recomendada a restrição ao IP público do administrador, mas não houve confirmação de que essa alteração foi feita. Essa é uma pendência.

Não criar regras públicas para 3306/MySQL ou 8080/backend. Manter a administração de banco por SSH ou por acesso local na VM.

Também foi marcada durante a criação a opção de excluir o IP público e a NIC ao excluir a VM. Conferir essa opção antes de uma exclusão futura; recursos persistentes podem continuar gerando cobrança.

## 15. DNS e HTTPS

Foi escolhido o rótulo gratuito `valdineyteste` no recurso de IP público do Azure, resultando em:

`valdineyteste.eastus2.cloudapp.azure.com`

Esse endereço é um subdomínio fornecido pelo Azure, não um domínio registrado separadamente.

O `server_name` do Nginx foi alterado do IP público para esse nome DNS. Em seguida foram instalados os pacotes:

```bash
sudo apt install -y certbot python3-certbot-nginx
```

Emissão e instalação:

```bash
sudo certbot --nginx -d valdineyteste.eastus2.cloudapp.azure.com --redirect
```

O Certbot solicitou e-mail e aceitação dos termos e concluiu com `Successfully received certificate` e `Successfully deployed certificate`.

O certificado emitido tem vencimento informado em **07/02/2027**. O Certbot informou que configurou uma tarefa de renovação automática. A emissão foi confirmada; um teste de renovação em modo simulado ainda não foi registrado.

Comando recomendado para verificar a renovação:

```bash
sudo certbot renew --dry-run
```

Teste local de TLS, sem depender do tráfego de entrada do Azure:

```bash
curl -I --resolve valdineyteste.eastus2.cloudapp.azure.com:443:127.0.0.1 https://valdineyteste.eastus2.cloudapp.azure.com/
```

Se esse teste funcionar e o navegador externo der timeout, conferir NSG, porta 443, resolução DNS e eventual firewall do Ubuntu. Não é necessário reemitir o certificado apenas por existir timeout.

### Links públicos de acompanhamento

Foi configurada a variável:

```dotenv
REDE_ROSA_PUBLIC_BASE_URL=https://valdineyteste.eastus2.cloudapp.azure.com
```

Depois foi reiniciado o backend. Essa propriedade evita gerar links públicos com `localhost`, IP interno ou esquema incorreto atrás do proxy.

HTTPS permite que o navegador solicite geolocalização. Ele não garante GPS contínuo com tela bloqueada ou página em segundo plano. O projeto depende das permissões e limitações do navegador.

## 16. Correção da assistente Rosa / Groq

A IA falhou inicialmente. O teste direto no Ubuntu retornou:

```json
{"error":{"message":"Invalid API Key","type":"invalid_request_error","code":"invalid_api_key"}}
```

Isso confirmou falha de credencial na Groq. O banco e o Nginx não eram a causa desse erro.

O controlador `ChatIAController` lê a propriedade `groq.api.key`. A nova chave foi configurada no `backend.env` por meio da variável equivalente `GROQ_API_KEY`, sem recompilar o JAR:

```bash
sudo nano /home/azureuser/rederosa-config/backend.env
```

Adicionar ou substituir uma única linha:

```dotenv
GROQ_API_KEY=SUBSTITUIR_PELA_CHAVE_VALIDA
```

Para salvar no nano: Ctrl+O, Enter, Ctrl+X. Se aparecer `Command to execute`, cancelar esse prompt antes de salvar. Na sessão houve dificuldade com atalhos; ao voltar a `azureuser@vm-rederosa:~$`, o editor já estava fechado.

Verificação sem exibir o segredo:

```bash
sudo grep -q '^GROQ_API_KEY=.' /home/azureuser/rederosa-config/backend.env && echo "Chave salva" || echo "Chave não salva"
```

Esse comando verifica apenas se há um valor, não a validade da chave.

Aplicação da configuração:

```bash
sudo systemctl restart rederosa
```

Após aguardar a inicialização:

```bash
curl --max-time 40 -sS -H "Content-Type: text/plain" --data "Responda apenas: teste funcionando" http://127.0.0.1:8080/chat
```

Resultado confirmado: **`teste funcionando`**. Em seguida, o usuário confirmou que a IA funcionou no site.

A chamada é `POST /chat` com corpo de texto. O endpoint `GET /chat` retorna somente uma mensagem de teste do controlador; ele não valida a credencial da Groq.

## 17. Procedimento para atualizar o backend

Esta seção é um procedimento de manutenção, não uma ação já executada após a implantação.

1. Compilar e testar no Windows:

```powershell
cd C:\Users\Valdiney\IdeaProjects\rederosa-back
.\mvnw.cmd clean package
```

2. Somente após `BUILD SUCCESS`, enviar com um nome temporário:

```powershell
scp -i "$env:USERPROFILE\Downloads\vm-rederosa_key.pem" "C:\Users\Valdiney\IdeaProjects\rederosa-back\target\rederosa-back-0.0.1-SNAPSHOT.jar" azureuser@135.237.160.112:/home/azureuser/rederosa-back-next.jar
```

3. No Ubuntu, fazer backup do banco antes se houver alteração de entidades ou estrutura. O Hibernate está em modo `update`, portanto atualizar o JAR pode alterar tabelas.

4. Guardar o JAR anterior, parar o serviço e trocar o arquivo:

```bash
cp /home/azureuser/rederosa-back.jar /home/azureuser/rederosa-back-previous.jar
sudo systemctl stop rederosa
mv /home/azureuser/rederosa-back-next.jar /home/azureuser/rederosa-back.jar
sudo systemctl start rederosa
```

5. Aguardar e validar:

```bash
sudo systemctl status rederosa --no-pager -l
curl -i http://127.0.0.1:8080/api/comunidade/salas
```

Não enviar diretamente sobre o JAR em uso: o processo pode precisar ler partes do arquivo enquanto executa. Enviar como `next` e trocar com o serviço parado evita esse problema.

Se for necessário voltar ao código anterior, parar o serviço, restaurar `rederosa-back-previous.jar` e iniciar novamente. Voltar o JAR não desfaz alterações de schema ou dados; avaliar o backup antes de um rollback.

## 18. Procedimento para atualizar o frontend

Para evitar uma pasta `frontend/rederosa` inesperada quando o diretório de destino já existe, enviar para uma pasta intermediária nova:

```powershell
$destinoFrontend = 'frontend-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
scp -i "$env:USERPROFILE\Downloads\vm-rederosa_key.pem" -r "C:\Users\Valdiney\Documents\workspace\rederosa" "azureuser@135.237.160.112:/home/azureuser/$destinoFrontend"
```

No Ubuntu, usar o nome gerado no comando, conferir `index.html` e copiar para `/var/www/rederosa`. Guardar uma cópia da versão publicada antes de atualizar.

Após copiar, reaplicar os ajustes de API somente na pasta publicada. Script que evita duplicar a configuração:

```bash
sudo python3 - <<'PY'
from pathlib import Path

script = '<script>window.REDE_ROSA_API_URL=location.origin;</script>'
for arquivo in Path('/var/www/rederosa').rglob('*.html'):
    texto = arquivo.read_text(encoding='utf-8')
    texto = texto.replace('http://localhost:8080/', '/')
    if script not in texto:
        texto = texto.replace('<head>', '<head>' + script, 1)
    arquivo.write_text(texto, encoding='utf-8')
PY
```

Validar navegação, cadastro, login, chat e acompanhamento em janela privada ou após recarregamento sem cache. Essa transformação cobre os padrões observados na implantação; revisar novos arquivos e integrações adicionados posteriormente.

## 19. Comandos de operação e diagnóstico

Executar no Ubuntu, uma linha por vez quando houver dificuldade de colagem.

### Serviços

```bash
sudo systemctl status rederosa --no-pager -l
sudo systemctl status mysql --no-pager -l
sudo systemctl status nginx --no-pager -l
sudo systemctl restart rederosa
```

### Logs

```bash
sudo journalctl -u rederosa -n 100 --no-pager
sudo journalctl -u rederosa -f
sudo tail -n 50 /var/log/nginx/error.log
```

Ctrl+C encerra o acompanhamento de logs; isso não para o serviço. Os logs da aplicação podem conter conteúdo de conversas com a IA, pois o controlador atual registra respostas. Evitar compartilhar logs completos com dados pessoais.

### API, site e recursos

```bash
curl -i http://127.0.0.1:8080/api/comunidade/salas
curl -I https://valdineyteste.eastus2.cloudapp.azure.com/
sudo nginx -t
free -h
df -h /
sudo du -sh /var/lib/mysql /var/www/rederosa
sudo ss -lntp
```

O teste do domínio feito dentro da VM não substitui um teste externo: verificar também pelo navegador ou por outro aparelho.

### Diagnóstico rápido

| Sintoma | Verificação inicial |
|---|---|
| SSH rejeita chave por permissões | ACL do `.pem` no Windows |
| `Permission denied (publickey)` | Caminho da chave, usuário `azureuser`, permissões e chave correta |
| `java: command not found` | Instalação do pacote Java 17 |
| Backend tenta conectar como `root` | Variáveis do serviço e reinício após alterar `.env` |
| HTTP 404 na raiz | Existência de `/var/www/rederosa/index.html` e `root` do Nginx |
| HTTP 502 | Status do backend, logs e acesso a `127.0.0.1:8080` |
| HTTPS com timeout | Regra 443 no Azure, listener do Nginx e firewall |
| Groq `invalid_api_key` | Chave válida em `GROQ_API_KEY` e reinício do serviço |
| GPS indisponível | URL HTTPS, permissão do navegador e página visível |
| Comando de Linux falha no PowerShell | Reconectar por SSH e confirmar o prompt |

## 20. Backup e dados

O backup do Azure estava desativado no resumo de criação. Não foi configurado backup automático do MySQL durante esta sessão.

Antes de apagar dados, mudar entidades ou fazer uma atualização importante, criar um dump. Exemplo a executar no Ubuntu:

```bash
mkdir -p ~/backups
chmod 700 ~/backups
```

```bash
(umask 077; sudo mysqldump --single-transaction --no-tablespaces --databases rederosa > ~/backups/rederosa-$(date +%Y%m%d-%H%M%S).sql)
```

Verificar se o comando terminou sem erro e se o arquivo não está vazio. O dump contém dados pessoais e deve permanecer privado. Copiar uma versão para fora da VM: backup no mesmo disco não protege contra perda do recurso.

Fazer também cópia protegida das configurações do Nginx, systemd e variáveis de ambiente. O arquivo de ambiente contém segredos e precisa de tratamento diferente de um documento público.

Nenhum procedimento de restauração foi testado durante a implantação. Uma estratégia de backup só deve ser considerada validada após uma restauração em ambiente isolado.

## 21. Espaço e capacidade

Medições locais realizadas antes do deploy:

| Conteúdo | Tamanho aproximado |
|---|---|
| `src` do backend | 1,09 MB |
| Frontend | 1,87 MB |
| JAR empacotado enviado | 52 MB |

Esses valores não incluem o crescimento futuro do banco, sistema operacional, pacotes, logs ou backups. O login inicial da VM apresentou cerca de 28 GiB utilizáveis na partição raiz.

O código analisado trabalha principalmente com cadastros, contatos, mensagens, ocorrências e localização. Não foi identificado fluxo de upload de foto ou vídeo no backend analisado. O banco pode crescer com mensagens e registros, e os logs e backups também precisam de acompanhamento.

Não foi estimada uma quantidade garantida de usuários. O chat e o acompanhamento consultam o backend com frequência; acessos simultâneos pesam mais que o simples total de cadastros.

Antes da competição, executar teste de carga com comportamento parecido com o real e observar CPU, memória, conexões de banco e tempo de resposta. A VM D2s_v3 criada é diferente das opções B discutidas inicialmente; as observações sobre créditos de CPU da série B não se aplicam automaticamente a ela.

## 22. Pendências e melhorias recomendadas

1. **Rotacionar as credenciais expostas:** uma captura enviada durante a configuração mostrou a chave da Groq e a senha do MySQL. Não há confirmação de rotação após a exposição. Substituir a chave, revogar a antiga e alterar a senha do usuário do banco, atualizando o arquivo de ambiente em seguida.
2. **Remover a chave literal do projeto:** o `application.properties` local ainda contém uma chave. Preferir `groq.api.key=${GROQ_API_KEY:}`. Se ela entrou no histórico Git, remover da versão atual não elimina a exposição anterior; revogar a credencial continua necessário. Essa alteração não foi feita nesta documentação.
3. **Restringir SSH:** alterar a origem da regra 22 para o IP administrativo ou adotar uma alternativa de acesso apropriada. Não houve confirmação de conclusão dessa etapa.
4. **Configurar backup automático e testar restauração.**
5. **Testar renovação do certificado** com `sudo certbot renew --dry-run`.
6. **Validar todas as funcionalidades:** cadastro, login, perfil, contatos, ocorrências, recuperação de senha, salas, IA, check-in, localização e links compartilhados.
7. **Revisar recuperação de senha antes de uso público:** não foi auditada nem validada nesta publicação. Verificar identidade, autorização, expiração e proteção contra alteração indevida.
8. **Revisar autenticação e limites do chat de IA:** o controlador `/chat` analisado não apresenta uma checagem explícita de sessão em seu método. Verificar a proteção efetiva, limites de requisição e consumo da Groq antes da competição.
9. **Evitar conteúdo pessoal nos logs:** revisar os registros de perguntas e respostas do controlador de IA e definir retenção.
10. **Criar alertas de custo e revisar saldo/validade dos créditos.**
11. **Fazer teste de carga** e revisar o tamanho da VM conforme evidência, sem prometer capacidade com base somente na RAM.
12. **Acompanhar avisos da série D v3** e opções de migração de tamanho no Azure.

## 23. Referências

- Portal Azure: https://portal.azure.com/
- Criação de VM Linux: https://learn.microsoft.com/en-us/azure/virtual-machines/linux/quick-create-portal
- Conexão SSH com VM Linux: https://learn.microsoft.com/en-us/azure/virtual-machines/linux-vm-connect
- Cotas de VM por família: https://learn.microsoft.com/en-us/azure/virtual-machines/quotas
- Certbot com Nginx: https://eff-certbot.readthedocs.io/en/stable/using.html#nginx
- Chaves da Groq: https://console.groq.com/keys
- Calculadora de preços: https://azure.microsoft.com/en-us/pricing/calculator/

Os preços, limites e disponibilidade registrados refletem as telas da sessão. Para novas implantações, conferir novamente a assinatura, região, cota e preço exibidos no portal.
