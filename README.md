# OFX2CSV

Aplicativo desktop em JavaFX 25 para converter extratos bancários brasileiros no formato OFX (.ofx) em planilhas Excel (.xlsx).

## Funcionalidades

- Leitura de arquivos OFX de bancos brasileiros (compatível com Banco do Brasil e outros)
- Conversão para planilha Excel com colunas: Data Balancete, Histórico, Crédito, Débito, Soma
- Formatação monetária (R$) nas colunas numéricas
- Filtro automático de transações com valor zero (marcadores de saldo)
- Interface gráfica com seleção de arquivos e diretório de saída
- Suporte a conversão de múltiplos arquivos ao mesmo tempo

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

## Formato de saída

| Coluna | Descrição | Formato |
|--------|-----------|---------|
| Data Balancete | Data da transação | DD/MM/AAAA |
| Histórico | Descrição combinada (nome + memo) | Texto |
| Crédito | Valor recebido | Decimal (R$) |
| Débito | Valor gasto | Decimal (R$) |
| Soma | Crédito + Débito | Decimal (R$) |

O arquivo de saída é salvo como `{nome_do_arquivo_original}.xlsx` no diretório selecionado.

## Tecnologias

- [JavaFX 25](https://openjfx.io/) — Interface gráfica
- [OFX4J](https://github.com/webcohesion/ofx4j) — Parser de arquivos OFX
- [Apache POI](https://poi.apache.org/) — Geração de planilhas Excel
- [JUnit 5](https://junit.org/junit5/) + [TestFX](https://github.com/TestFX/TestFX) — Testes unitários e de interface

## Licença

Este projeto é de uso privado.
