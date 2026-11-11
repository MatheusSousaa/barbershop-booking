# Deploy em produção no Railway

## Stack deste repositório

O frontend publicado atualmente é HTML, CSS e JavaScript estáticos servidos pelo Spring Boot. Não há projeto React/Vite separado neste repositório; a interface foi aprimorada sem trocar essa arquitetura. A API e a página usam a mesma origem em produção, então CORS é necessário apenas para origens adicionais, como o Vite local.

## Configuração aplicada

- O Spring usa `PORT` do Railway e aceita `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` do ambiente.
- Novas fotos são gravadas no diretório configurado e expostas em `/uploads/{nome_aleatorio.ext}`. URLs antigas em `/uploads/services/{nome_aleatorio.ext}` continuam servidas.
- Configure um Volume no serviço da aplicação com mount path `/app/uploads`. O entrypoint inicializa a permissão do diretório como root e então inicia a JVM com o usuário sem privilégios `app`.
- O PostgreSQL deve ser um serviço Railway separado. O app usa a rede privada com as variáveis referenciadas abaixo.

## `application.properties`

O arquivo completo fica em `src/main/resources/application.properties`. Os principais valores de produção são:

```properties
server.port=${PORT:8080}
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/barbershop}
spring.datasource.username=${DB_USERNAME:barbershop}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
barbershop.upload.directory=${BARBERSHOP_UPLOAD_DIR:${RAILWAY_VOLUME_MOUNT_PATH:./uploads}}
barbershop.jwt.secret=${JWT_SECRET:}
barbershop.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}
barbershop.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:8080,http://localhost:5173,http://127.0.0.1:5500}
```

## Railway Raw Editor

Crie/adicione um serviço PostgreSQL e, nas variáveis do serviço da aplicação, cole o bloco abaixo. Troque `Postgres` pelo nome exato do serviço PostgreSQL no projeto e substitua os valores marcados. Railway documenta as variáveis `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD` e `PGDATABASE` para conexão entre serviços. As referências `${{serviço.VARIÁVEL}}` são resolvidas pelo Railway.

```dotenv
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}

JWT_SECRET=SUBSTITUA_POR_UM_SEGREDO_ALEATORIO_FORTE_COM_32_CARACTERES_OU_MAIS
JWT_EXPIRATION_MS=86400000

BARBERSHOP_UPLOAD_DIR=/app/uploads
BARBER_INITIAL_USERNAME=barbeiro
BARBER_INITIAL_EMAIL=EMAIL_REAL_DO_BARBEIRO
BARBER_INITIAL_PASSWORD=DEFINA_UMA_SENHA_FORTE_E_UNICA
BARBER_INITIAL_NAME=Barber Braga

# Informe a origem pública exata do serviço, incluindo https://.
# O frontend e a API são servidos juntos; inclua localhost apenas se necessário.
CORS_ALLOWED_ORIGINS=https://SEU-DOMINIO.up.railway.app,http://localhost:5173,http://localhost:8080
FORWARD_HEADERS_STRATEGY=framework

# O container já escuta na variável PORT fornecida pelo Railway.
# SMTP é opcional; sem ele, notificações por e-mail ficam no log da aplicação.
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_RECIPIENT=
```

Use o painel de variáveis do Railway para guardar segredos. Não use o bloco acima sem substituir o segredo JWT, a senha inicial e o domínio de exemplo. No serviço da aplicação, crie um Volume com mount path `/app/uploads`; o valor deve corresponder a `BARBERSHOP_UPLOAD_DIR`.

## Checklist pós-deploy

1. Crie o serviço PostgreSQL Railway e confirme que o nome de referência `Postgres` (ou o nome substituído no bloco) corresponde ao serviço real.
2. Publique o serviço da aplicação usando o Dockerfile deste repositório.
3. Configure as variáveis de ambiente da aplicação e substitua todas as credenciais e o domínio de exemplo.
4. Anexe um Volume ao serviço da aplicação em `/app/uploads`.
5. Gere um domínio público HTTPS para a aplicação e use essa origem em `CORS_ALLOWED_ORIGINS` quando um frontend externo precisar chamar a API.
6. Aguarde a aplicação ficar saudável e confira `https://SEU-DOMINIO/actuator/health`.
7. Abra a página pública e confirme que os serviços e horários carregam, que o formulário valida os dados e que um agendamento de teste recebe confirmação.
8. Entre em `/admin.html`, valide as credenciais iniciais e confira se o agendamento aparece no painel.
9. Envie uma foto de serviço no painel e abra a URL retornada em uma aba anônima. Deve responder em `/uploads/{nome_aleatorio.ext}`.
10. Faça novo deploy e confirme que a mesma foto continua acessível pelo Volume persistente.
11. Configure backups do PostgreSQL e do Volume conforme a política de retenção do negócio; valide também a restauração antes de depender deles.

> A configuração do código está preparada. O deploy real ainda depende de configurar esses valores e o Volume no projeto Railway da conta.
