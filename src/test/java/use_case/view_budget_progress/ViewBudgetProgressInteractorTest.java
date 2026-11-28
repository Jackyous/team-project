package use_case.view_budget_progress;

import data_access.InMemoryTransactionDataAccessObject;
import data_access.InMemoryUserDataAccessObject;
import entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ViewBudgetProgressInteractorTest {
    // Already November 28 in UTC, but still November 27 in Toronto.
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-28T01:00:00Z"),
            ZoneId.of("America/Toronto"));
    private final LocalDate today = LocalDate.of(2026, 11, 28);
    private final InMemoryUserDataAccessObject users = new InMemoryUserDataAccessObject();
    private final InMemoryTransactionDataAccessObject transactions = new InMemoryTransactionDataAccessObject(users);
    private final Map<LocalDate, BigDecimal> limits = new HashMap<>();
    private final Result result = new Result();
    private final ViewBudgetProgressBudgetDataAccessInterface budgets = new ViewBudgetProgressBudgetDataAccessInterface() {
        @Override public BigDecimal getDailyLimit(String username) {
            fail("Progress must request the budget for an explicit date.");
            return null;
        }
        @Override public BigDecimal getDailyLimit(String username, LocalDate date) {
            assertEquals("alice", username);
            return limits.get(date);
        }
    };

    @BeforeEach void signIn() {
        users.setCurrentUsername("alice");
        limits.put(today, new BigDecimal("100.00"));
        limits.put(today.plusDays(1), new BigDecimal("100.00"));
    }

    private void execute() {
        new ViewBudgetProgressInteractor(transactions, budgets, result, clock).execute();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void requiresSignedInUser(String username) {
        users.setCurrentUsername(username);
        execute();
        assertNull(result.data);
        assertEquals("Please sign in to view budget progress.", result.error);
    }

    @Test void requiresBudgetEffectiveTodayEvenIfTomorrowHasOne() {
        limits.remove(today);
        execute();
        assertNull(result.data);
        assertEquals("Set a daily budget first.", result.error);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectsNonPositiveBudget(String limit) {
        limits.put(today, new BigDecimal(limit));
        execute();
        assertNull(result.data);
        assertNotNull(result.error);
    }

    @Test void noTransactionsMeansNoSpending() {
        execute();
        assertNull(result.error);
        assertMoney("0", result.data.getSpent());
        assertMoney("100", result.data.getRemaining());
        assertMoney("0.0", result.data.getPercentage());
    }

    @Test void onlyCurrentUsersExpensesOnUtcDateCount() {
        transactions.save("alice", new Transaction(new BigDecimal("25.25"), Transaction.Type.EXPENSE, today));
        transactions.save("alice", new Transaction(new BigDecimal("1000"), Transaction.Type.INCOME, today));
        transactions.save("alice", new Transaction(new BigDecimal("50"), Transaction.Type.EXPENSE, today.minusDays(1)));
        transactions.save("alice", new Transaction(new BigDecimal("60"), Transaction.Type.EXPENSE, today.plusDays(1)));
        transactions.save("bob", new Transaction(new BigDecimal("10"), Transaction.Type.EXPENSE, today));
        execute();
        assertEquals(today, result.data.getDate());
        assertMoney("25.25", result.data.getSpent());
        assertMoney("74.75", result.data.getRemaining());
        assertEquals(new BigDecimal("25.3"), result.data.getPercentage());
    }

    @Test void keepsTruePercentageAboveOneHundredAndDoesNotModifyRecords() {
        transactions.save("alice", new Transaction(new BigDecimal("125.25"), Transaction.Type.EXPENSE, today));
        execute();
        execute();
        assertMoney("-25.25", result.data.getRemaining());
        assertEquals(new BigDecimal("125.3"), result.data.getPercentage());
        assertEquals(1, transactions.getTransactions("alice").size());
    }

    @Test void tomorrowBudgetDoesNotReplaceTodaysLimit() {
        limits.put(today.plusDays(1), new BigDecimal("200"));
        execute();
        assertMoney("100", result.data.getLimit());
        assertMoney("200", result.data.getTomorrowLimit());
    }

    @Test void readFailureDoesNotBecomeZeroSpending() {
        ViewBudgetProgressTransactionDataAccessInterface broken = new ViewBudgetProgressTransactionDataAccessInterface() {
            @Override public String getCurrentUsername() { return "alice"; }
            @Override public java.util.List<Transaction> getTransactions(String username) {
                throw new UncheckedIOException(new IOException("Unavailable file"));
            }
        };
        new ViewBudgetProgressInteractor(broken, budgets, result, clock).execute();
        assertNull(result.data);
        assertEquals("Could not load budget progress. Please try again.", result.error);
    }

    static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    static class Result implements ViewBudgetProgressOutputBoundary {
        ViewBudgetProgressOutputData data;
        String error;
        @Override public void prepareSuccessView(ViewBudgetProgressOutputData value) {
            data = value;
            error = null;
        }
        @Override public void prepareFailView(String message) {
            error = message;
            data = null;
        }
    }
}
