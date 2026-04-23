# OFX2CSV

Aplicativo desktop em JavaFX 25 para converter extratos bancarios brasileiros no formato OFX (.ofx) em planilhas Excel (.xlsx).

## Funcionalidades

- Leitura de arquivos OFX de bancos brasileiros (compativel com Banco do Brasil e outros)
- Conversao para planilha Excel com colunas: Data Balancete, Historico, Credito, Debito, Soma
- Formatacao monetaria (R$) nas colunas numericas
- Filtro automatico de transacoes com valor zero (marcadores de saldo)
- Interface grafica com selecao de arquivos e diretorio de saida
- Suporte a conversao de multiplos arquivos ao mesmo tempo

## Requisitos

- Java 21 ou superior

## Como usar

### Compilar

```bash
./gradlew build
```

### Executar

```bash
./gradlew run
```

### Executar testes

```bash
./gradlew test
```

## Formato de saida

| Coluna | Descricao | Formato |
|--------|-----------|---------|
| Data Balancete | Data da transacao | DD/MM/AAAA |
| Historico | Descricao combinada (nome + memo) | Texto |
| Credito | Valor recebido | Decimal (R$) |
| Debito | Valor gasto | Decimal (R$) |
| Soma | Credito + Debito | Decimal (R$) |

O arquivo de saida e salvo como `{nome_do_arquivo_original}.xlsx` no diretorio selecionado.

## Tecnologias

- [JavaFX 25](https://openjfx.io/) — Interface grafica
- [OFX4J](https://github.com/webcohesion/ofx4j) — Parser de arquivos OFX
- [Apache POI](https://poi.apache.org/) — Geracao de planilhas Excel
- [JUnit 5](https://junit.org/junit5/) + [TestFX](https://github.com/TestFX/TestFX) — Testes unitarios e de interface

## Licenca

Este projeto e de uso privado.
