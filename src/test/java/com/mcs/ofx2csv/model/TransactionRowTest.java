package com.mcs.ofx2csv.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransactionRowTest {

    @Test
    void negativeAmountGoesToDebito() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "Payee", "Memo text", -150.00);

        assertEquals(150.00, row.debito(), 0.001);
        assertEquals(0.0, row.credito(), 0.001);
        assertEquals(150.00, row.soma(), 0.001);
    }

    @Test
    void positiveAmountGoesToCredito() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "Payee", "Memo text", 500.50);

        assertEquals(0.0, row.debito(), 0.001);
        assertEquals(500.50, row.credito(), 0.001);
        assertEquals(500.50, row.soma(), 0.001);
    }

    @Test
    void zeroAmountGivesBothZero() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "Payee", "Memo", 0.0);

        assertEquals(0.0, row.debito(), 0.001);
        assertEquals(0.0, row.credito(), 0.001);
        assertEquals(0.0, row.soma(), 0.001);
    }

    @Test
    void historicoCombinesNameAndMemo() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "JOAO SILVA", "PIX transfer", -100.0);

        assertEquals("JOAO SILVA - PIX transfer", row.historico());
    }

    @Test
    void historicoMemoOnlyWhenNameIsEmpty() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "", "PIX - RECEBIDO", 200.0);

        assertEquals("PIX - RECEBIDO", row.historico());
    }

    @Test
    void historicoMemoOnlyWhenNameIsNull() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), null, "PIX - RECEBIDO", 200.0);

        assertEquals("PIX - RECEBIDO", row.historico());
    }
}
