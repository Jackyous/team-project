package data_access;

import entity.Transaction;
import use_case.login.LoginUserDataAccessInterface;
import use_case.record_transaction.RecordTransactionDataAccessInterface;
import use_case.view_budget_progress.ViewBudgetProgressTransactionDataAccessInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stores transactions per account for the current application run.
 */
public class InMemoryTransactionDataAccessObject
        implements RecordTransactionDataAccessInterface,
            ViewBudgetProgressTransactionDataAccessInterface {

    private final LoginUserDataAccessInterface userDataAccess;
    private final Map<String, List<Transaction>> transactions = new HashMap<>();

    public InMemoryTransactionDataAccessObject(
            LoginUserDataAccessInterface userDataAccess) {
        this.userDataAccess = userDataAccess;
    }

    @Override
    public String getCurrentUsername() {
        return userDataAccess.getCurrentUsername();
    }

    @Override
    public void save(String accountId, Transaction transaction) {
        transactions.computeIfAbsent(accountId, key -> new ArrayList<>())
                .add(transaction);
    }

    /**
     * Returns a copy of the account's transactions.
     */
    public List<Transaction> getTransactions(String accountId) {
        return new ArrayList<>(
                transactions.getOrDefault(accountId, Collections.emptyList()));
    }

    public BigDecimal getTotalExpenses(
            String userId, LocalDate date) {

        return getTransactions(userId).stream()
                .filter(transaction ->
                        transaction.getType() == Transaction.Type.EXPENSE)
                .filter(transaction ->
                        transaction.getDate().equals(date))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getNetBalanceChange(String userId) {
        BigDecimal total = BigDecimal.ZERO;

        for (Transaction transaction : getTransactions(userId)) {
            if (transaction.getType() == Transaction.Type.INCOME) {
                total = total.add(transaction.getAmount());
            } else {
                total = total.subtract(transaction.getAmount());
            }
        }

        return total;
    }
}