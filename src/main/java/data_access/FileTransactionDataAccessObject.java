package data_access;

import entity.Transaction;
import use_case.login.LoginUserDataAccessInterface;
import use_case.record_transaction.RecordTransactionDataAccessInterface;
import use_case.view_budget_progress.ViewBudgetProgressTransactionDataAccessInterface;
import java.io.IOException;
import java.util.List;

/** Adapter sharing the same financial store as budgets and rewards. */
public class FileTransactionDataAccessObject implements RecordTransactionDataAccessInterface,
        ViewBudgetProgressTransactionDataAccessInterface {
    private final LoginUserDataAccessInterface users;
    private final FileBudgetDataAccessObject store;

    public FileTransactionDataAccessObject(LoginUserDataAccessInterface users,
            FileBudgetDataAccessObject store) {
        this.users = users;
        this.store = store;
    }

    @Override public String getCurrentUsername() { return users.getCurrentUsername(); }
    @Override public void save(String userId, Transaction transaction) throws IOException {
        store.recordTransaction(userId, transaction);
    }
    @Override public List<Transaction> getTransactions(String userId) {
        return store.getTransactions(userId);
    }
}
