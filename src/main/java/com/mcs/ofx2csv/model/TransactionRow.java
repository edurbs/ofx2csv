package com.mcs.ofx2csv.model;

import java.time.LocalDate;

/**
 * Represents a single row in the output Excel file.
 *
 * @param date       Transaction date (DD/MM/YYYY in output)
 * @param historico  Combined NAME + MEMO from OFX
 * @param debito     Negative amount (money out), or 0
 * @param credito    Positive amount (money in), or 0
 * @param soma       debito + credito
 */
public record TransactionRow(LocalDate date, String historico, double debito, double credito, double soma) {

    /**
     * Factory method that splits the OFX transaction amount into debito/credito.
     * Negative amounts go to debito, positive to credito. The other column is zero.
     */
    public static TransactionRow fromOfx(LocalDate date, String name, String memo, double amount) {
        String historico = buildHistorico(name, memo);
        double debito = amount < 0 ? amount : 0;
        double credito = amount > 0 ? amount : 0;
        double soma = debito + credito;
        return new TransactionRow(date, historico, debito, credito, soma);
    }

    private static String buildHistorico(String name, String memo) {
        if (name == null || name.isBlank()) {
            return memo != null ? memo : "";
        }
        if (memo == null || memo.isBlank()) {
            return name;
        }
        return name + " - " + memo;
    }
}
