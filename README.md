# Barbershop Booking

Sistema web de agendamento desenvolvido como primeiro projeto freelance para uma barbearia local. A aplicação substitui parte do atendimento manual por um fluxo online: o cliente consulta os serviços, escolhe uma data e um horário realmente disponível e envia a reserva. Pelo painel, o barbeiro organiza os serviços, o expediente e a agenda.

O projeto reúne uma interface responsiva, uma API REST em Java e persistência em PostgreSQL. As regras de disponibilidade são aplicadas no backend e reforçadas pelo banco para evitar reservas duplicadas, inclusive quando duas solicitações chegam ao mesmo tempo.

## Demonstração

### Página de agendamento

<img width="1467" height="736" alt="Página pública do sistema de agendamento" src="https://github.com/user-attachments/assets/2193f150-5494-4e2e-a52c-be612a041323" />

### Experiência em dispositivos móveis

<img width="556" height="908" alt="Fluxo de agendamento em dispositivo móvel" src="https://github.com/user-attachments/assets/6d9b6693-27a3-416c-ae06-5eabb593a472" />

## O que o sistema faz

### Para clientes

- Exibe os serviços ativos, descrições, valores e fotos.
- Calcula e apresenta horários livres conforme o expediente configurado, a data, os agendamentos ativos e os bloqueios da agenda.
- Conduz a reserva em etapas: serviço, data, horário e dados do cliente.
- Valida as informações e confirma o agendamento sem exigir login.
- Permite consultar a confirmação e oferece atalhos para compartilhar pelo WhatsApp e salvar no calendário.

### Para o barbeiro

- Oferece painel protegido para acompanhar a agenda diária e semanal.
- Permite cadastrar, editar, ativar ou desativar serviços e enviar suas fotos.
- Permite configurar o expediente semanal e criar ou remover bloqueios pontuais.
- Permite cancelar agendamentos com um motivo; o horário volta a ficar disponível.
- Notifica sobre novos agendamentos e cancelamentos. Com SMTP configurado, as notificações são enviadas por e-mail; sem SMTP, são registradas nos logs da aplicação.

## Regras e segurança

- Os horários são calculados no servidor em intervalos de 30 minutos e respeitam o fuso `America/Bahia`.
- O backend valida novamente a disponibilidade no momento da gravação.
- Uma restrição no PostgreSQL impede duas reservas ativas no mesmo horário, protegendo também contra concorrência entre requisições simultâneas.
- As rotas administrativas exigem autenticação JWT e o papel de barbeiro.
- Senhas são armazenadas com BCrypt; limites por IP reduzem tentativas repetidas de login e de agendamento.
- As entradas usam validação no servidor e erros de negócio são respondidos pela API.
- O Swagger documenta os endpoints e permite explorar os contratos da API.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Bean Validation |
| Segurança | Spring Security, JWT (JJWT), BCrypt, Bucket4j |
| Persistência | PostgreSQL, Hibernate, Flyway |
| Frontend | HTML, CSS e JavaScript |
| Notificações | Spring Mail; logs como alternativa sem SMTP |
| Documentação | Springdoc OpenAPI / Swagger UI |
| Execução | Maven Wrapper, Docker, Docker Compose |
| Testes | JUnit, Spring Boot Test, H2 e Testcontainers |

## Arquitetura

O backend segue uma separação por responsabilidades:

```text
src/main/java/com/barbershop/
├── config/       configuração de segurança, OpenAPI e arquivos enviados
├── controller/   endpoints públicos, autenticação e administração
├── domain/       entidades e estados do domínio
├── dto/          contratos de entrada e saída da API
├── exception/    erros de negócio e tratamento global
├── notification/ eventos e envio assíncrono de notificações
├── repository/   acesso a dados com Spring Data JPA
├── security/     JWT, filtro de autenticação e rate limiting
└── service/      agendamentos, disponibilidade, catálogo e administração
```

A interface estática fica em `src/main/resources/static` e é servida pela própria aplicação Spring Boot. O Flyway controla a evolução do schema; o Hibernate valida a estrutura do banco ao iniciar.

## Executar localmente

### Pré-requisitos

- Docker Desktop com Docker Compose
- Git

O Compose sobe a aplicação e o PostgreSQL. Para executar os testes com o wrapper Maven fora do Docker, também é necessário um JDK compatível com Java 21.

### 1. Baixar e configurar

```bash
git clone https://github.com/MatheusSousaa/barbershop-booking.git
cd barbershop-booking
```

No Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

No macOS ou Linux:

```bash
cp .env.example .env
```

Edite o `.env` e troque os valores locais de exemplo. Configure, no mínimo, uma senha para o banco, um `JWT_SECRET` aleatório com pelo menos 32 caracteres e as credenciais iniciais do barbeiro. Não use valores de desenvolvimento em produção e não envie o `.env` ao Git.

### 2. Iniciar

```bash
docker compose up --build -d
```

Quando os containers estiverem prontos, acesse:

- Site público: <http://localhost:8080>
- Painel do barbeiro: <http://localhost:8080/admin.html>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health check: <http://localhost:8080/actuator/health>

Comandos úteis:

```bash
docker compose logs -f app
docker compose down
```

Os dados do PostgreSQL e as fotos enviadas são guardados em volumes do Docker. Para também remover os volumes e os dados locais, o comando é `docker compose down -v`.

## Configuração

As variáveis aceitas estão listadas em [`.env.example`](.env.example). Entre as principais:

| Variável | Uso |
| --- | --- |
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Banco PostgreSQL usado pelo Docker Compose |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão da aplicação ao PostgreSQL |
| `APP_PORT` / `PORT` | Porta HTTP da aplicação |
| `JWT_SECRET` | Chave de assinatura dos tokens; use segredo forte e exclusivo |
| `JWT_EXPIRATION_MS` | Validade do token em milissegundos |
| `BARBER_INITIAL_EMAIL`, `BARBER_INITIAL_PASSWORD` | Conta inicial do painel |
| `BARBERSHOP_UPLOAD_DIR` | Diretório persistente das fotos de serviços |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_RECIPIENT` | Configuração opcional de e-mail |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas quando houver frontend separado |

O arquivo de exemplo contém valores fictícios para desenvolvimento. Substitua-os antes de publicar a aplicação. Para redefinir a senha inicial de uma conta já criada, veja o procedimento descrito em [`RAILWAY-DEPLOY.md`](RAILWAY-DEPLOY.md).

## API

Principais rotas:

| Método | Rota | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/public/services` | Público | Lista serviços ativos |
| `GET` | `/api/public/slots?date=AAAA-MM-DD` | Público | Consulta horários disponíveis |
| `POST` | `/api/public/appointments` | Público | Cria um agendamento |
| `/api/auth/**` | Varia | Público | Login e autenticação do barbeiro |
| `/api/admin/services` | Varia | JWT / barbeiro | Gerencia serviços |
| `/api/admin/services/{id}/photo` | `POST` | JWT / barbeiro | Envia foto do serviço |
| `/api/admin/schedules` | `GET`, `PUT` | JWT / barbeiro | Consulta e configura expediente |
| `/api/admin/blocked-slots` | Varia | JWT / barbeiro | Gerencia bloqueios pontuais |
| `/api/admin/appointments/daily?date=AAAA-MM-DD` | `GET` | JWT / barbeiro | Consulta agenda diária |
| `/api/admin/appointments/weekly?fromDate=AAAA-MM-DD` | `GET` | JWT / barbeiro | Consulta agenda semanal |
| `/api/admin/appointments/{id}/cancel` | `POST` | JWT / barbeiro | Cancela um agendamento |

O contrato completo está disponível no Swagger UI em `/swagger-ui.html` quando a aplicação está em execução.

## Testes

Na raiz do projeto:

```bash
./mvnw test
```

No Windows PowerShell:

```powershell
.\mvnw.cmd test
```

A suíte cobre cálculo de horários, concorrência de reservas, segurança/administração e notificações.

## Deploy

O repositório inclui `Dockerfile` e uma configuração detalhada para Railway em [`RAILWAY-DEPLOY.md`](RAILWAY-DEPLOY.md). O deploy requer configurar as variáveis de ambiente, conectar um PostgreSQL e montar um volume persistente para as fotos. SMTP é opcional. O projeto não pressupõe que essas configurações externas já estejam feitas.

## Contexto do projeto

Este foi meu primeiro trabalho freelance: construí uma solução para facilitar o agendamento de uma barbearia local e dar ao barbeiro mais controle sobre os horários, serviços e reservas. Além das telas, o trabalho envolveu modelagem do domínio, regras de disponibilidade, persistência, autenticação, implantação em contêineres e documentação para execução.

## Licença

Distribuído sob a licença MIT. Consulte [`LICENSE`](LICENSE).
