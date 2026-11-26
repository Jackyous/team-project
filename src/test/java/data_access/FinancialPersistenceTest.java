package data_access;

import entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class FinancialPersistenceTest {
    @TempDir Path directory;
    private final LocalDate day = LocalDate.of(2026, 11, 24);
    private FileBudgetDataAccessObject open() {
        return new FileBudgetDataAccessObject(directory.resolve("budgets.json").toString());
    }
    @Test void walletAndTransactionsSurviveRestart() throws Exception {
        FileBudgetDataAccessObject first = open();
        first.saveWallet("Paul", new Wallet(new BigDecimal("200")));
        first.recordTransaction("Paul", new Transaction(new BigDecimal("15"), Transaction.Type.EXPENSE, day));
        first.recordTransaction("Paul", new Transaction(new BigDecimal("30"), Transaction.Type.INCOME, day));
        FileBudgetDataAccessObject restarted = open();
        assertEquals(0, new BigDecimal("215").compareTo(restarted.getWallet("Paul").getBalance()));
        assertEquals(2, restarted.getTransactions("Paul").size());
        assertEquals(day, restarted.getTransactions("Paul").get(0).getDate());
        assertNull(restarted.getWallet("Other"));
    }
    @Test void rewardCannotBeClaimedAgainAfterRestart() {
        FileBudgetDataAccessObject first = open();
        assertEquals(10, first.claimReward("Paul", day, 10));
        FileBudgetDataAccessObject restarted = open();
        assertTrue(restarted.hasClaimedReward("Paul", day));
        assertThrows(IllegalStateException.class, () -> restarted.claimReward("Paul", day, 10));
        assertEquals(10, restarted.getCoinBalance("Paul"));
        assertEquals(20, restarted.claimReward("Paul", day.plusDays(1), 10));
        assertEquals(0, restarted.getCoinBalance("Other"));
    }
    @Test void failedTransactionLeavesWalletAndRecordsUnchanged() throws Exception {
        FileBudgetDataAccessObject store = open();
        store.saveWallet("Paul", new Wallet(new BigDecimal("200")));
        Path file = directory.resolve("budgets.json");
        Path backup = directory.resolve("backup.json");
        Files.move(file, backup);
        Files.createDirectory(file);
        Files.writeString(file.resolve("blocker"), "simulate unavailable destination");
        assertThrows(IOException.class, () -> store.recordTransaction("Paul",
                new Transaction(new BigDecimal("15"), Transaction.Type.EXPENSE, day)));
        assertEquals(0, new BigDecimal("200").compareTo(store.getWallet("Paul").getBalance()));
        assertTrue(store.getTransactions("Paul").isEmpty());
        FileBudgetDataAccessObject oldFile = new FileBudgetDataAccessObject(backup.toString());
        assertEquals(0, new BigDecimal("200").compareTo(oldFile.getWallet("Paul").getBalance()));
    }
    @Test void missingWalletRejectsTransactionAndZeroWalletCannotBeReset() throws Exception {
        FileBudgetDataAccessObject store = open();
        assertThrows(IOException.class, () -> store.recordTransaction("Paul",
                new Transaction(BigDecimal.ONE, Transaction.Type.EXPENSE, day)));
        store.saveWallet("Paul", new Wallet(BigDecimal.ZERO));
        assertThrows(IllegalStateException.class,
                () -> store.saveWallet("Paul", new Wallet(new BigDecimal("999"))));
        assertEquals(0, open().getWallet("Paul").getBalance().signum());
    }
    @Test void legacyBalancesAndBudgetHistorySurviveNewWrites() throws Exception {
        Files.writeString(directory.resolve("budgets.json"),
                "{\"Paul\":{\"dailyLimit\":\"20\",\"balance\":\"200\"}}");
        FileBudgetDataAccessObject store = open();
        store.migrateLegacyBudget("Paul", day);
        store.saveBudgetEffectiveOn("Paul", new Budget(new BigDecimal("30")), day.plusDays(1));
        store.recordTransaction("Paul", new Transaction(BigDecimal.ONE, Transaction.Type.EXPENSE, day));
        store.claimReward("Paul", day, 10);
        FileBudgetDataAccessObject restarted = open();
        assertNull(restarted.getBudget("Paul", day.minusDays(1)));
        assertEquals(0, new BigDecimal("20").compareTo(restarted.getBudget("Paul", day).getLimit()));
        assertEquals(0, new BigDecimal("30").compareTo(restarted.getBudget("Paul", day.plusDays(5)).getLimit()));
        assertEquals(10, restarted.getCoinBalance("Paul"));
        assertEquals(1, restarted.getTransactions("Paul").size());
    }
    @Test void failedRewardWriteDoesNotAwardCoinsOrMarkClaimed() throws Exception {
        FileBudgetDataAccessObject store = open();
        store.saveWallet("Paul", new Wallet(BigDecimal.ZERO));
        Path file = directory.resolve("budgets.json");
        Files.move(file, directory.resolve("backup.json"));
        Files.createDirectory(file);
        Files.writeString(file.resolve("blocker"), "simulate unavailable destination");
        assertThrows(java.io.UncheckedIOException.class,
                () -> store.claimReward("Paul", day, 10));
        assertEquals(0, store.getCoinBalance("Paul"));
        assertFalse(store.hasClaimedReward("Paul", day));
    }

    @Test void concurrentClaimsOnSharedStoreAwardOnlyOnce() throws Exception {
        FileBudgetDataAccessObject store = open();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> claim = () -> {
                try { store.claimReward("Paul", day, 10); return true; }
                catch (IllegalStateException duplicate) { return false; }
            };
            java.util.concurrent.Future<Boolean> first = pool.submit(claim);
            java.util.concurrent.Future<Boolean> second = pool.submit(claim);
            assertNotEquals(first.get(), second.get());
            assertEquals(10, open().getCoinBalance("Paul"));
        } finally { pool.shutdownNow(); }
    }
}
