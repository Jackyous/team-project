package data_access;

import entity.Budget;
import entity.Transaction;
import use_case.earn_reward_coins.EarnRewardCoinsDataAccessInterface;
import use_case.view_budget_progress.ViewBudgetProgressTransactionDataAccessInterface;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EarnRewardCoinsDataAccessObject implements EarnRewardCoinsDataAccessInterface {
    private final Map<String, ZoneId> timeZones = new HashMap<>();
    private final ViewBudgetProgressTransactionDataAccessInterface transactions;
    private final FileBudgetDataAccessObject store;

    public EarnRewardCoinsDataAccessObject(
            ViewBudgetProgressTransactionDataAccessInterface transactions,
            FileBudgetDataAccessObject store) {
        this.transactions = transactions;
        this.store = store;
    }

    public synchronized void addUser(String userId, ZoneId zone) {
        if (timeZones.containsKey(userId)) {
            throw new IllegalArgumentException("User already exists.");
        }
        timeZones.put(userId, zone);
        // Never reset persisted coin balances or claims here.
    }
    public synchronized void ensureUser(String userId, ZoneId zone) {
        if (!timeZones.containsKey(userId)) { addUser(userId, zone); }
    }
    @Override public synchronized ZoneId getUserTimeZone(String userId) {
        if (!timeZones.containsKey(userId)) { throw new IllegalArgumentException("Unknown user."); }
        return timeZones.get(userId);
    }
    @Override public Optional<BigDecimal> getBudget(String userId, LocalDate date) {
        return Optional.ofNullable(store.getBudget(userId, date)).map(Budget::getLimit);
    }
    @Override public BigDecimal getTotalSpending(String userId, LocalDate date) {
        return transactions.getTransactions(userId).stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE && t.getDate().equals(date))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    @Override public boolean hasClaimedReward(String userId, LocalDate date) {
        return store.hasClaimedReward(userId, date);
    }
    @Override public int getCoinBalance(String userId) { return store.getCoinBalance(userId); }
    @Override public int claimReward(String userId, LocalDate date, int coins) {
        return store.claimReward(userId, date, coins);
    }
}
