# Barber Braga

Sistema de agendamento para barbearias, com painel administrativo.

## Stack

Java 21, Spring Boot, PostgreSQL, HTML, CSS, JavaScript e Docker Compose.

## Executar

Copie `.env.example` para `.env`, ajuste os valores locais e execute:

```sh
docker compose up --build -d
```

## Testes

```sh
./mvnw test
```

No Windows:

```powershell
.\mvnw.cmd test
```
