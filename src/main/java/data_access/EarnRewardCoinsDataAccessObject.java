package data_access;
import use_case.earn_reward_coins.EarnRewardCoinsDataAccessInterface;
import use_case.purchase_pet_item.PurchasePetItemCoinDataAccessInterface;
import use_case.open_pet_shop.OpenPetShopCoinDataAccessInterface;

import entity.Budget;
import entity.Transaction;
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

        timeZones.put(userId, zone);
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
        return store.spendCoins(userId, amount);
    }

}
