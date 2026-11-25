package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import entity.Budget;
import entity.Wallet;
import org.json.JSONObject;
import use_case.set_budget.SetBudgetDataAccessInterface;
import use_case.view_budget_progress.ViewBudgetProgressBudgetDataAccessInterface;

import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * DAO for budgets and wallets, stored in a local JSON file.
 * The file looks like: {"Paul": {"dailyLimit": "50", "balance": "200"}}
 */
public class FileBudgetDataAccessObject implements SetBudgetDataAccessInterface, ViewBudgetProgressBudgetDataAccessInterface {

    private final Path file;
    private final Map<String, Budget> budgets = new HashMap<>();
    private final Map<String, Wallet> wallets = new HashMap<>();
    private final Map<String, NavigableMap<LocalDate, Budget>> budgetHistory =
            new HashMap<>();

    public FileBudgetDataAccessObject(String jsonPath) {
        this.file = Path.of(jsonPath).toAbsolutePath();
        try {
            if (Files.exists(file) && Files.size(file) > 0) {
                load();
            }
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Could not read budgets file.", exception);
        }
    }

    private void load() throws IOException {
        final JSONObject root = new JSONObject(Files.readString(file, StandardCharsets.UTF_8));
        for (String username : root.keySet()) {
            final JSONObject entry = root.getJSONObject(username);
            if (entry.has("budgetHistory")) {
                JSONObject savedHistory = entry.getJSONObject("budgetHistory");
                NavigableMap<LocalDate, Budget> history = new TreeMap<>();

                for (String dateText : savedHistory.keySet()) {
                    history.put(
                            LocalDate.parse(dateText),
                            new Budget(new BigDecimal(
                                    savedHistory.getString(dateText))));
                }

                budgetHistory.put(username, history);
            }
            if (entry.has("dailyLimit")) {
                budgets.put(username, new Budget(new BigDecimal(entry.getString("dailyLimit"))));
            }
            if (entry.has("balance")) {
                wallets.put(username, new Wallet(new BigDecimal(entry.getString("balance"))));
            }
        }
    }

    private void write() throws IOException {
        final JSONObject root = new JSONObject();
        for (Map.Entry<String, Budget> budget : budgets.entrySet()) {
            entryFor(root, budget.getKey())
                    .put("dailyLimit", budget.getValue().getLimit().toPlainString());
        }
        for (Map.Entry<String, Wallet> wallet : wallets.entrySet()) {
            entryFor(root, wallet.getKey())
                    .put("balance", wallet.getValue().getBalance().toPlainString());
        }
        for (Map.Entry<String, NavigableMap<LocalDate, Budget>> userEntry
                : budgetHistory.entrySet()) {

            JSONObject savedHistory = new JSONObject();

            for (Map.Entry<LocalDate, Budget> budgetEntry
                    : userEntry.getValue().entrySet()) {

                savedHistory.put(
                        budgetEntry.getKey().toString(),
                        budgetEntry.getValue().getLimit().toPlainString());
            }

            entryFor(root, userEntry.getKey())
                    .put("budgetHistory", savedHistory);
        }
        Files.writeString(file, root.toString(2), StandardCharsets.UTF_8);
    }

    private static JSONObject entryFor(JSONObject root, String username) {
        if (!root.has(username)) {
            root.put(username, new JSONObject());
        }
        return root.getJSONObject(username);
    }

    @Override
    public void saveBudget(String username, Budget budget) {
        budgets.put(username, budget);
        save();
    }

    @Override
    public void saveWallet(String username, Wallet wallet) {
        wallets.put(username, wallet);
        save();
    }

    @Override
    public BigDecimal getDailyLimit(String username) {
        final Budget budget = budgets.get(username);
        return budget == null ? null : budget.getLimit();
    }

    private void save() {
        try {
            write();
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Could not save budgets file.", exception);
        }
    }

    public Budget getBudget(String username) {
        return budgets.get(username);
    }

    public Budget getBudget(String username, LocalDate date) {
        NavigableMap<LocalDate, Budget> history =
                budgetHistory.get(username);

        if (history == null) {
            return null;
        }

        Map.Entry<LocalDate, Budget> entry = history.floorEntry(date);

        return entry == null ? null : entry.getValue();
    }

    @Override
    public Wallet getWallet(String username) {
        return wallets.get(username);
    }

    @Override
    public void saveBudgetEffectiveOn(
            String username, Budget budget, LocalDate effectiveDate) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("A username is required.");
        }
        if (budget == null || effectiveDate == null) {
            throw new IllegalArgumentException(
                    "Budget and effective date are required.");
        }

        budgetHistory.computeIfAbsent(
                        username, key -> new TreeMap<>())
                .put(effectiveDate, budget);

        save();
    }

    @Override
    public boolean hasBudget(String username) {
        NavigableMap<LocalDate, Budget> history =
                budgetHistory.get(username);

        return budgets.containsKey(username)
                || (history != null && !history.isEmpty());
    }

    public void migrateLegacyBudget(String username, LocalDate today) {
        NavigableMap<LocalDate, Budget> history =
                budgetHistory.get(username);

        if ((history == null || history.isEmpty())
                && budgets.containsKey(username)) {

            saveBudgetEffectiveOn(
                    username, budgets.get(username), today);
        }
    }

    @Override
    public BigDecimal getDailyLimit(String username, LocalDate date) {
        Budget budget = getBudget(username, date);
        return budget == null ? null : budget.getLimit();
    }
}