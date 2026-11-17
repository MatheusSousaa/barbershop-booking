# Sistema de Agendamento para Barbearia

Projeto desenvolvido para uma barbearia do meu bairro. Clientes consultam serviços e horários disponíveis e fazem agendamentos online. O barbeiro gerencia a agenda pelo painel administrativo.

<img width="1467" height="736" alt="image" src="https://github.com/user-attachments/assets/2193f150-5494-4e2e-a52c-be612a041323" />
<img width="556" height="908" alt="image" src="https://github.com/user-attachments/assets/6d9b6693-27a3-416c-ae06-5eabb593a472" />



## Funcionalidades

- Agendamento online com horários disponíveis.
- Regra de negócio e índice no PostgreSQL impedem duas reservas ativas no mesmo horário.
- Painel protegido por JWT para gerenciar agenda, serviços, expediente e bloqueios.
- Cadastro de fotos e cancelamento de agendamentos.
- Botão de WhatsApp; o número está vazio por enquanto.

## Tecnologias

Java 21, Spring Boot, Spring Security, JWT, PostgreSQL, Flyway, HTML, CSS, JavaScript e Docker Compose.

## Como executar

Requisitos: Docker Desktop e Git.

```bash
git clone https://github.com/MatheusSousaa/barbershop-booking.git
cd barbershop-booking
cp .env.example .env
No Windows PowerShell, use Copy-Item .env.example .env.

Edite o .env, troque os valores de exemplo e configure DB_PASSWORD, JWT_SECRET (mínimo de 32 caracteres) e BARBER_INITIAL_*. Não compartilhe nem envie esse arquivo ao Git.

Inicie a aplicação:
docker compose up --build -d
- Site: http://localhost:8080
- Painel: http://localhost:8080/admin.html
- Swagger: http://localhost:8080/swagger-ui.html
Para ver os logs: docker compose logs -f app. Para parar: docker compose down.

Os dados do banco e as fotos são mantidos em volumes do Docker. O Flyway aplica as migrações automaticamente.
Testes
./mvnw test
No Windows: .\mvnw.cmd test.
Deploy
A aplicação pode ser executada com o Dockerfile em plataformas como Railway. Configure as variáveis de ambiente, conecte um PostgreSQL e use um volume persistente para as fotos. Não use senhas de exemplo em produção.
