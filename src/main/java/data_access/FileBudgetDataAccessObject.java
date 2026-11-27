package data_access;

import entity.Budget;
import entity.Transaction;
import entity.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;
import use_case.set_budget.SetBudgetDataAccessInterface;
import use_case.view_budget_progress.ViewBudgetProgressBudgetDataAccessInterface;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One shared instance per application owns all financial state in this file. */
public class FileBudgetDataAccessObject implements SetBudgetDataAccessInterface,
        ViewBudgetProgressBudgetDataAccessInterface {
    private final Path file;
    private JSONObject data;

    public FileBudgetDataAccessObject(String jsonPath) {
        file = Path.of(jsonPath).toAbsolutePath();
        try {
            data = Files.exists(file)
                    ? new JSONObject(Files.readString(file, StandardCharsets.UTF_8))
                    : new JSONObject();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read financial data.", exception);
        }
        // Invalid JSON fails visibly; it must never be replaced with an empty account.
    }

    private JSONObject copy() { return new JSONObject(data.toString()); }

    private static JSONObject account(JSONObject root, String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("A username is required.");
        }
        if (!root.has(username)) { root.put(username, new JSONObject()); }
        return root.getJSONObject(username);
    }

    private JSONObject account(String username) {
        JSONObject entry = data.optJSONObject(username);
        return entry == null ? new JSONObject() : entry;
    }

    /** Publish memory only after a complete replacement file has reached disk. */
    private void commit(JSONObject next) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "financial-", ".tmp");
        boolean moved = false;
        try {
            byte[] bytes = next.toString(2).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) { channel.write(buffer); }
                channel.force(true);
            }
            // Fail without publishing if this filesystem cannot replace atomically.
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            moved = true;
            data = next;
        } finally {
            if (!moved) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private void save(JSONObject next) {
        try { commit(next); }
        catch (IOException exception) {
            throw new UncheckedIOException("Could not save financial data.", exception);
        }
    }

    @Override
    public synchronized void saveBudget(String username, Budget budget) {
        // Compatibility for legacy callers. New setup uses dated history.
        JSONObject next = copy();
        account(next, username).put("dailyLimit", budget.getLimit().toPlainString());
        save(next);
    }

    @Override
    public synchronized void saveWallet(String username, Wallet wallet) {
        if (getWallet(username) != null) {
            throw new IllegalStateException("Starting balance has already been set.");
        }
        JSONObject next = copy();
        account(next, username).put("balance", wallet.getBalance().toPlainString());
        save(next);
    }

    @Override
    public synchronized Wallet getWallet(String username) {
        JSONObject entry = account(username);
        return entry.has("balance")
                ? new Wallet(new BigDecimal(entry.getString("balance"))) : null;
    }

    @Override
    public synchronized boolean hasBudget(String username) {
        JSONObject entry = account(username);
        JSONObject history = entry.optJSONObject("budgetHistory");
        return entry.has("dailyLimit") || (history != null && history.length() > 0);
    }

    @Override
    public synchronized void saveBudgetEffectiveOn(String username, Budget budget, LocalDate date) {
        Objects.requireNonNull(budget);
        Objects.requireNonNull(date);
        JSONObject next = copy();
        JSONObject entry = account(next, username);
        if (!entry.has("budgetHistory")) { entry.put("budgetHistory", new JSONObject()); }
        entry.getJSONObject("budgetHistory").put(date.toString(), budget.getLimit().toPlainString());
        save(next);
    }

    @Override
    public synchronized void migrateLegacyBudget(String username, LocalDate today) {
        JSONObject entry = account(username);
        JSONObject history = entry.optJSONObject("budgetHistory");
        if (entry.has("dailyLimit") && (history == null || history.length() == 0)) {
            saveBudgetEffectiveOn(username,
                    new Budget(new BigDecimal(entry.getString("dailyLimit"))), today);
        }
    }

    public synchronized Budget getBudget(String username, LocalDate date) {
        JSONObject history = account(username).optJSONObject("budgetHistory");
        if (history == null) { return null; }
        LocalDate selected = null;
        for (String key : history.keySet()) {
            LocalDate effective = LocalDate.parse(key);
            if (!effective.isAfter(date) && (selected == null || effective.isAfter(selected))) {
                selected = effective;
            }
        }
        return selected == null ? null
                : new Budget(new BigDecimal(history.getString(selected.toString())));
    }

    public synchronized Budget getBudget(String username) {
        return getBudget(username, LocalDate.now());
    }

    @Override
    public synchronized BigDecimal getDailyLimit(String username) {
        return getDailyLimit(username, LocalDate.now());
    }

    @Override
    public synchronized BigDecimal getDailyLimit(String username, LocalDate date) {
        Budget budget = getBudget(username, date);
        return budget == null ? null : budget.getLimit();
    }

    /** Wallet and transaction are committed together, never separately. */
    public synchronized void recordTransaction(String username, Transaction transaction) throws IOException {
        Objects.requireNonNull(transaction);
        Wallet wallet = getWallet(username);
        if (wallet == null) {
            throw new IOException("Set your starting balance before recording transactions.");
        }
        BigDecimal balance = transaction.getType() == Transaction.Type.INCOME
                ? wallet.getBalance().add(transaction.getAmount())
                : wallet.getBalance().subtract(transaction.getAmount());
        JSONObject next = copy();
        JSONObject entry = account(next, username);
        entry.put("balance", balance.toPlainString());
        if (!entry.has("transactions")) { entry.put("transactions", new JSONArray()); }
        entry.getJSONArray("transactions").put(new JSONObject()
                .put("amount", transaction.getAmount().toPlainString())
                .put("type", transaction.getType().name())
                .put("date", transaction.getDate().toString()));
        commit(next);
    }

    public synchronized List<Transaction> getTransactions(String username) {
        JSONArray records = account(username).optJSONArray("transactions");
        List<Transaction> result = new ArrayList<>();
        if (records != null) {
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                result.add(new Transaction(new BigDecimal(record.getString("amount")),
                        Transaction.Type.valueOf(record.getString("type")),
                        LocalDate.parse(record.getString("date"))));
            }
        }
        return result;
    }

    public synchronized int getCoinBalance(String username) {
        JSONObject entry = account(username);
        return entry.has("coinBalance") ? entry.getInt("coinBalance") : 0;
    }

    public synchronized boolean hasClaimedReward(String username, LocalDate date) {
        JSONObject claims = account(username).optJSONObject("rewardClaims");
        return claims != null && claims.has(date.toString());
    }

    public synchronized int claimReward(String username, LocalDate date, int coins) {
        if (coins <= 0) { throw new IllegalArgumentException("Coins must be positive."); }
        if (hasClaimedReward(username, date)) {
            throw new IllegalStateException("Reward already claimed.");
        }
        int balance = Math.addExact(getCoinBalance(username), coins);
        JSONObject next = copy();
        JSONObject entry = account(next, username);
        entry.put("coinBalance", balance);
        if (!entry.has("rewardClaims")) { entry.put("rewardClaims", new JSONObject()); }
        entry.getJSONObject("rewardClaims").put(date.toString(), coins);
        save(next);
        return balance;
    }

    public synchronized int spendCoins(String userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Coins spent must be positive.");
        }

        int currentBalance = getCoinBalance(userId);

        if (currentBalance < amount) {
            throw new IllegalStateException(
                    "You don't have enough coins.");
        }

        int newBalance = currentBalance - amount;

        JSONObject next = copy();
        account(next, userId).put("coinBalance", newBalance);

        save(next);
        return newBalance;
    }
}
