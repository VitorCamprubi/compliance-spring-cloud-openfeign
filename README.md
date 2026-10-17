# Compliance — Integração com serviços externos usando Spring Cloud OpenFeign

Projeto desenvolvido durante o **Bootcamp CI&T – Java com IA (DIO)**, no módulo de Spring Cloud OpenFeign.

O cenário é um onboarding de empresas com checagem de **KYC** (Know Your Customer, lista de sanções) e **AML** (Anti-Money Laundering, risco de lavagem de dinheiro e pessoas politicamente expostas). Quando uma empresa é cadastrada, a aplicação consulta os dois serviços externos, aplica a política de compliance e grava a avaliação de risco.

## Fluxo

1. `POST /companies` cadastra a empresa (Spring Data REST).
2. O `CompanyEventHandler` (`@HandleAfterCreate`) dispara o `AnalyzeCompanyRiskUseCase`.
3. Os clientes Feign consultam o serviço de sanções (KYC) e o de AML.
4. As respostas externas são convertidas para objetos de domínio (`ComplianceScreening`).
5. A `CompliancePolicy` decide o status (`APPROVED`, `MANUAL_REVIEW`, `REJECTED`) e o nível de risco (`LOW` → `CRITICAL`).
6. A empresa é salva com a avaliação de risco.

Regras da política:

- Sanção com confiança acima de 0.8 → **REJECTED** (risco `CRITICAL`).
- Pessoa politicamente exposta (PEP) → **MANUAL_REVIEW**.
- Score de AML acima de 70 → **MANUAL_REVIEW**.
- Caso contrário → **APPROVED**.

## Tecnologias

- Java 25 + Spring Boot 4
- Spring Cloud OpenFeign
- Spring Cloud Circuit Breaker (Resilience4j)
- Spring Data KeyValue (repositório em memória) + Spring Data REST
- Spring Boot Actuator
- Mockoon (simulação das APIs externas)
- Lombok

## Estrutura

```text
src/main/java/dio/compliance
├── domain/          -> Company, CompliancePolicy, ComplianceScreening, RiskAssessment...
├── application/     -> AnalyzeCompanyRiskUseCase
└── infrastructure/
    ├── rest/        -> clientes Feign (SanctionClient, AntiMoneyLaunderingClient) e DTOs externos
    └── persistence/ -> entidade KeyValue, repositórios e event handler
```

## O que aprendi

- **Clientes HTTP declarativos**: com `@FeignClient` basta uma interface com `@GetMapping` e o Spring gera a implementação. URL, headers de autenticação e nível de log ficam no `application.properties`, por cliente.
- **Tradução na fronteira**: os DTOs (`SanctionResult`, `AmlResult`) têm o formato da API externa e são convertidos com `toDomain()`. O domínio não depende do contrato de ninguém.
- **Política de domínio isolada**: a decisão de risco está toda na `CompliancePolicy`, fácil de ler e de testar.
- **Resiliência com fallback**: com o circuit breaker habilitado, se o serviço de sanções falhar o `SanctionClient.Fallback` devolve uma lista vazia e o fluxo continua de forma previsível.
- **Spring Data KeyValue**: um repositório em memória com a mesma API do Spring Data, ótimo para focar na integração sem precisar de banco.

## Simulando as APIs com Mockoon

As APIs externas são simuladas localmente com o [Mockoon](https://mockoon.com/).

1. Instale o Mockoon Desktop (ou a CLI).
2. Importe os ambientes de `src/main/resources`:
   - `mockoon_kyc.json` — API de sanções na porta **3001** (exige o header `X-API-KEY`)
   - `mockoon_aml.json` — API de AML na porta **3002** (exige `Authorization: Bearer ...`)
3. Inicie os dois ambientes.

O mock de KYC tem vários cenários (empresa sem riscos, empresa sancionada, alta latência, erro 500, 404). Dá para trocar a resposta ativa no Mockoon e ver como a aplicação reage, inclusive o fallback.

## Como rodar

```bash
./gradlew bootRun
```

Cadastrando uma empresa e consultando a avaliação:

```bash
curl -i -X POST http://localhost:8080/companies \
  -H "Content-Type: application/json" \
  -d '{"name":"ACME Ltda","registrationNumber":"12345678000199"}'

curl http://localhost:8080/companies
```
