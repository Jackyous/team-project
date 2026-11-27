package data_access;


import java.util.HashMap;
import java.util.Map;

import entity.Budget;
import entity.Wallet;
import use_case.set_budget.SetBudgetDataAccessInterface;

import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * In-memory implementation of the DAO for budgets and wallets. This implementation does
 * NOT persist data between runs of the program.
 */
public class InMemorySetBudgetDataAccessObject implements SetBudgetDataAccessInterface {

    private final Map<String, Budget> budgets = new HashMap<>();
    private final Map<String, Wallet> wallets = new HashMap<>();
    private final Map<String, NavigableMap<LocalDate, Budget>> budgetHistory =
            new HashMap<>();

    @Override
    public void saveBudget(String username, Budget budget) {
        budgets.put(username, budget);
    }

    @Override
    public void saveWallet(String username, Wallet wallet) {
        wallets.put(username, wallet);
    }

    public Budget getBudget(String username) {
        return budgets.get(username);
    }

    @Override
    public Wallet getWallet(String username) {
        return wallets.get(username);
    }

    @Override
    public boolean hasBudget(String username) {
        NavigableMap<LocalDate, Budget> history =
                budgetHistory.get(username);

        return budgets.containsKey(username)
                || (history != null && !history.isEmpty());
    }

    @Override
    public void saveBudgetEffectiveOn(
            String username, Budget budget, LocalDate effectiveDate) {

        budgetHistory.computeIfAbsent(
                        username, key -> new TreeMap<>())
                .put(effectiveDate, budget);
    }

    @Override
    public void migrateLegacyBudget(String username, LocalDate today) {
        NavigableMap<LocalDate, Budget> history =
                budgetHistory.get(username);

        if ((history == null || history.isEmpty())
                && budgets.containsKey(username)) {

            saveBudgetEffectiveOn(
                    username, budgets.get(username), today);
        }
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
}