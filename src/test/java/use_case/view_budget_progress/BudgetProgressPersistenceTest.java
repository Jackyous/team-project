package use_case.view_budget_progress;

import data_access.FileBudgetDataAccessObject;
import data_access.FileTransactionDataAccessObject;
import data_access.InMemoryUserDataAccessObject;
import entity.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import use_case.record_transaction.RecordTransactionInputData;
import use_case.record_transaction.RecordTransactionInteractor;
import use_case.record_transaction.RecordTransactionOutputBoundary;
import use_case.set_budget.SetBudgetInputData;
import use_case.set_budget.SetBudgetInteractor;
import use_case.set_budget.SetBudgetOutputBoundary;
import use_case.set_budget.SetBudgetOutputData;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static use_case.view_budget_progress.ViewBudgetProgressInteractorTest.assertMoney;

class BudgetProgressPersistenceTest {
    @TempDir Path directory;
    private final LocalDate today = LocalDate.of(2026, 11, 28);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-28T01:00:00Z"),
            ZoneId.of("America/Toronto"));
    private final InMemoryUserDataAccessObject users = new InMemoryUserDataAccessObject();

    private FileBudgetDataAccessObject open() {
        return new FileBudgetDataAccessObject(directory.resolve("budgets.json").toString());
    }

    private void setBudget(FileBudgetDataAccessObject store, String user, String limit, String balance) {
        AtomicBoolean saved = new AtomicBoolean();
        new SetBudgetInteractor(store, new SetBudgetOutputBoundary() {
            @Override public void prepareSuccessView(SetBudgetOutputData data) { saved.set(true); }
            @Override public void prepareFailView(String error) { fail(error); }
        }, clock).execute(new SetBudgetInputData(user, limit, balance));
        assertTrue(saved.get());
    }

    private void record(FileBudgetDataAccessObject store, String user, String amount, Transaction.Type type) {
        users.setCurrentUsername(user);
        AtomicBoolean saved = new AtomicBoolean();
        new RecordTransactionInteractor(new FileTransactionDataAccessObject(users, store),
                new RecordTransactionOutputBoundary() {
                    @Override public void prepareSuccessView() { saved.set(true); }
                    @Override public void prepareFailView(String error) { fail(error); }
                }, clock).execute(new RecordTransactionInputData(amount, type));
        assertTrue(saved.get());
    }

    private ViewBudgetProgressOutputData progress(FileBudgetDataAccessObject store, String user, Clock when) {
        users.setCurrentUsername(user);
        ViewBudgetProgressInteractorTest.Result result = new ViewBudgetProgressInteractorTest.Result();
        new ViewBudgetProgressInteractor(new FileTransactionDataAccessObject(users, store),
                store, result, when).execute();
        assertNull(result.error);
        assertNotNull(result.data);
        return result.data;
    }

    @Test void setupRecordAndProgressSharePersistedStateAcrossRestart() {
        FileBudgetDataAccessObject store = open();
        setBudget(store, "alice", "100", "500");
        record(store, "alice", "25.25", Transaction.Type.EXPENSE);
        record(store, "alice", "1000", Transaction.Type.INCOME);
        ViewBudgetProgressOutputData before = progress(store, "alice", clock);
        assertMoney("25.25", before.getSpent());
        assertMoney("74.75", before.getRemaining());
        assertEquals(today, store.getTransactions("alice").get(0).getDate());
        assertNull(store.getBudget("alice", today.minusDays(1)));
        FileBudgetDataAccessObject restarted = open();
        ViewBudgetProgressOutputData after = progress(restarted, "alice", clock);
        assertEquals(before.getDate(), after.getDate());
        assertEquals(before.getPercentage(), after.getPercentage());
        assertMoney("25.25", after.getSpent());
        assertMoney("1474.75", restarted.getWallet("alice").getBalance());
    }

    @Test void editedBudgetStartsAtUtcMidnightAndLatestEditWins() {
        FileBudgetDataAccessObject store = open();
        setBudget(store, "alice", "100", "500");
        record(store, "alice", "125.25", Transaction.Type.EXPENSE);
        setBudget(store, "alice", "200", "");
        setBudget(store, "alice", "150", "");
        ViewBudgetProgressOutputData pending = progress(open(), "alice", clock);
        assertMoney("100", pending.getLimit());
        assertMoney("150", pending.getTomorrowLimit());
        assertMoney("-25.25", pending.getRemaining());
        Clock beforeMidnight = Clock.fixed(Instant.parse("2026-11-28T23:59:59Z"), ZoneOffset.UTC);
        assertMoney("100", progress(store, "alice", beforeMidnight).getLimit());
        Clock midnight = Clock.fixed(Instant.parse("2026-11-29T00:00:00Z"), ZoneId.of("America/Toronto"));
        ViewBudgetProgressOutputData nextDay = progress(store, "alice", midnight);
        assertEquals(today.plusDays(1), nextDay.getDate());
        assertMoney("150", nextDay.getLimit());
        assertMoney("0", nextDay.getSpent());
        assertMoney("150", nextDay.getRemaining());
        assertMoney("374.75", store.getWallet("alice").getBalance());
    }

    @Test void regularAndGoogleAccountsKeepSeparateProgressAfterRestart() {
        FileBudgetDataAccessObject store = open();
        setBudget(store, "alice", "100", "500");
        setBudget(store, "google:test-subject", "20", "50");
        record(store, "alice", "25.25", Transaction.Type.EXPENSE);
        record(store, "google:test-subject", "5", Transaction.Type.EXPENSE);
        FileBudgetDataAccessObject restarted = open();
        assertMoney("15", progress(restarted, "google:test-subject", clock).getRemaining());
        assertMoney("74.75", progress(restarted, "alice", clock).getRemaining());
    }

    @Test void viewingProgressDoesNotChangeFinancialFileOrRewardClaims() throws Exception {
        FileBudgetDataAccessObject store = open();
        setBudget(store, "alice", "100", "500");
        record(store, "alice", "10", Transaction.Type.EXPENSE);
        store.claimReward("alice", today.minusDays(1), 10);
        byte[] before = Files.readAllBytes(directory.resolve("budgets.json"));
        progress(store, "alice", clock);
        progress(store, "alice", clock);
        assertArrayEquals(before, Files.readAllBytes(directory.resolve("budgets.json")));
        assertEquals(10, store.getCoinBalance("alice"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"budget", "transaction"})
    void malformedSavedValuesShowAnErrorInsteadOfFalseProgress(String brokenPart) throws Exception {
        String limit = brokenPart.equals("budget") ? "not-money" : "100";
        String amount = brokenPart.equals("transaction") ? "not-money" : "10";
        Files.writeString(directory.resolve("budgets.json"),
                "{\"alice\":{\"budgetHistory\":{\"2026-11-28\":\"" + limit
                        + "\"},\"transactions\":[{\"amount\":\"" + amount
                        + "\",\"type\":\"EXPENSE\",\"date\":\"2026-11-28\"}]}}");
        FileBudgetDataAccessObject store = open();
        users.setCurrentUsername("alice");
        ViewBudgetProgressInteractorTest.Result result = new ViewBudgetProgressInteractorTest.Result();
        new ViewBudgetProgressInteractor(new FileTransactionDataAccessObject(users, store),
                store, result, clock).execute();
        assertNull(result.data);
        assertEquals("Could not load budget progress. Please try again.", result.error);
    }
}
