package data_access;
import use_case.earn_reward_coins.EarnRewardCoinsDataAccessInterface;
import use_case.purchase_pet_item.PurchasePetItemCoinDataAccessInterface;
import use_case.open_pet_shop.OpenPetShopCoinDataAccessInterface;

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

public class EarnRewardCoinsDataAccessObject
        implements EarnRewardCoinsDataAccessInterface,
        PurchasePetItemCoinDataAccessInterface, OpenPetShopCoinDataAccessInterface {
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

        timeZones.put(userId, timeZone);
        balances.put(userId, 0);
        budgets.put(userId, new HashMap<>());
        spending.put(userId, new HashMap<>());
        claimedDates.put(userId, new HashSet<>());
    }

    public synchronized void ensureUserExists(
            String userId, ZoneId timeZone) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required.");
        }

        if (timeZone == null) {
            throw new IllegalArgumentException("Time zone is required.");
        }

        if (!timeZones.containsKey(userId)) {
            addUser(userId, timeZone);
        }
    }

    public synchronized void setBudget(
            String userId, LocalDate date, BigDecimal amount) {
        requireUser(userId);

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Budget cannot be negative.");
        }

        budgets.get(userId).put(date, amount);
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

    @Override
    public synchronized int spendCoins(String userId, int amount) {
        requireUser(userId);

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Coins spent must be positive.");
        }

        int currentBalance = balances.get(userId);

        if (currentBalance < amount) {
            throw new IllegalStateException(
                    "You don't have enough coins.");
        }

        int newBalance = currentBalance - amount;
        balances.put(userId, newBalance);

        return newBalance;
    }
}
