# Deploy da Rede Rosa

O frontend na Vercel chama a API Spring Boot no Render por HTTPS. O backend acessa o MySQL no Aiven por JDBC com TLS obrigatório.

Use dados fictícios na demonstração. Antes de publicar para usuárias reais, corrija o fluxo de recuperação de senha, que atualmente permite trocar a senha sem validar um token de recuperação.

## 1. Aiven

O banco `rederosa` já existe no Aiven. Configure `AIVEN_DB_USER` e `AIVEN_DB_PASSWORD` com as credenciais do serviço. No IntelliJ, adicione as duas variáveis em **Run → Edit Configurations → Environment variables** na configuração do backend e execute a aplicação. O Hibernate usa `ddl-auto=update` para criar as tabelas ausentes e atualizar o esquema na inicialização.

[Conexão Java com MySQL no Aiven](https://aiven.io/docs/products/mysql/howto/connect-with-java)

## 2. Render

1. Envie os arquivos de deploy para o repositório GitHub do backend. No painel do Render, vá em **New → Blueprint** e selecione esse repositório e a branch que contém o `render.yaml`. O Blueprint usa o plano gratuito e o Dockerfile da raiz.
2. Preencha `AIVEN_DB_USER` e `AIVEN_DB_PASSWORD` no painel. A URL JDBC já aponta para o banco `rederosa`. Para trocar o endpoint ou o banco, configure `AIVEN_DB_URL` com a URL JDBC completa.
3. Revogue a chave Groq que foi versionada. Para habilitar o chat, gere uma nova e configure `GROQ_API_KEY` em **Environment**. Para fixar a URL usada nos links de acompanhamento, configure `REDE_ROSA_PUBLIC_BASE_URL` com a URL HTTPS do backend sem barra final.
4. Após o deploy, abra `https://<servico>.onrender.com/usuarias/teste`. Verifique o HTTP 200 e confirme nos logs a inicialização do datasource. Use dados fictícios para validar o fluxo de cadastro e login.

[Documentação dos Blueprints do Render](https://render.com/docs/blueprint-spec)

## 3. Vercel

1. Importe o repositório do frontend no painel da Vercel. Se o frontend estiver em uma subpasta, selecione essa pasta em **Root Directory**.
2. Configure no frontend a URL HTTPS do Render como base da API. Use o nome de variável que o código consome. Se for Vite, por exemplo, defina `VITE_API_URL` em **Environment Variables** e faça o cliente HTTP ler `import.meta.env.VITE_API_URL`.
3. Faça o deploy do frontend após salvar a variável. Abra a URL publicada e confirme no navegador que as chamadas vão para o Render. As credenciais do Aiven e a chave Groq ficam no Render.

[Importação de repositórios na Vercel](https://vercel.com/docs/git) · [Variáveis de ambiente da Vercel](https://vercel.com/docs/environment-variables)
