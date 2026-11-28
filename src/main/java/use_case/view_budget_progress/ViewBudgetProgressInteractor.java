package use_case.view_budget_progress;
import entity.Transaction;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class ViewBudgetProgressInteractor implements ViewBudgetProgressInputBoundary {
    private final ViewBudgetProgressTransactionDataAccessInterface transactions;
    private final ViewBudgetProgressBudgetDataAccessInterface budgets;
    private final ViewBudgetProgressOutputBoundary presenter;
    private final Clock clock;

    public ViewBudgetProgressInteractor(ViewBudgetProgressTransactionDataAccessInterface transactions,
            ViewBudgetProgressBudgetDataAccessInterface budgets,
            ViewBudgetProgressOutputBoundary presenter) {
        this(transactions, budgets, presenter, Clock.systemUTC());
    }

    public ViewBudgetProgressInteractor(ViewBudgetProgressTransactionDataAccessInterface transactions,
            ViewBudgetProgressBudgetDataAccessInterface budgets,
            ViewBudgetProgressOutputBoundary presenter, Clock clock) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.presenter = presenter;
        this.clock = clock.withZone(ZoneOffset.UTC);
    }

    @Override
    public void execute() {
        String username = transactions.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Please sign in to view budget progress.");
            return;
        }

        LocalDate today = LocalDate.now(clock);
        final BigDecimal limit;
        final BigDecimal tomorrowLimit;
        final List<Transaction> records;
        try {
            limit = budgets.getDailyLimit(username, today);
            tomorrowLimit = budgets.getDailyLimit(username, today.plusDays(1));
            records = transactions.getTransactions(username);
        } catch (IllegalStateException | UncheckedIOException exception) {
            presenter.prepareFailView("Could not load budget progress. Please try again.");
            return;
        }

        if (limit == null) {
            presenter.prepareFailView("Set a daily budget first.");
            return;
        }
        if (limit.signum() <= 0 || (tomorrowLimit != null && tomorrowLimit.signum() <= 0)) {
            presenter.prepareFailView("The daily budget must be greater than zero.");
            return;
        }

        BigDecimal spent = BigDecimal.ZERO;
        for (Transaction transaction : records) {
            if (transaction.getType() == Transaction.Type.EXPENSE
                    && transaction.getDate().equals(today)) {
                spent = spent.add(transaction.getAmount());
            }
        }
        BigDecimal remaining = limit.subtract(spent);
        BigDecimal percentage = spent.multiply(new BigDecimal("100"))
                .divide(limit, 1, RoundingMode.HALF_UP);
        presenter.prepareSuccessView(new ViewBudgetProgressOutputData(
                today, limit, spent, remaining, percentage, tomorrowLimit));
    }
}
