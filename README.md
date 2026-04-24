# OFX2CSV

Aplicativo desktop em JavaFX 25 para converter extratos bancários brasileiros no formato OFX (.ofx) em planilhas Excel (.xlsx).

## Funcionalidades

- Leitura de arquivos OFX de bancos brasileiros (compatível com Banco do Brasil e outros)
- Conversão para planilha Excel com colunas: Data Balancete, Histórico, Crédito, Débito, Soma
- Formatação monetária (R$) nas colunas numéricas
- Filtro automático de transações com valor zero (marcadores de saldo)
- Interface gráfica com seleção de arquivos e diretório de saída
- Suporte a conversão de múltiplos arquivos ao mesmo tempo
- Arrastar e soltar arquivos OFX diretamente na interface
- Associação automática com arquivos `.ofx` no Windows (clique duplo abre o app)

## Instalação (Windows)

Baixe o instalador `.msi` na página de [Releases](https://github.com/edurbs/ofx2csv/releases). O instalador cria atalhos no Menu Iniciar e na Área de Trabalho, e associa arquivos `.ofx` ao aplicativo.

Não é necessário instalar Java separadamente — o instalador inclui o runtime.

## Desenvolvimento

### Requisitos

- Java 25 (Liberica JDK com JavaFX ou Temurin + JavaFX Maven)

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

### Gerar instalador

```bash
./gradlew jpackageInstaller
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
- [jpackage](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) + [GitHub Actions](https://github.com/features/actions) — Instalador MSI para Windows
- [OFX4J](https://github.com/webcohesion/ofx4j) — Parser de arquivos OFX
- [Apache POI](https://poi.apache.org/) — Geração de planilhas Excel
- [JUnit 5](https://junit.org/junit5/) + [TestFX](https://github.com/TestFX/TestFX) — Testes unitários e de interface

## Licença

Este projeto está licenciado sob a [GNU General Public License v3.0](LICENSE).
