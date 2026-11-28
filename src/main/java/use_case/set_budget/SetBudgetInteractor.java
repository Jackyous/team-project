package use_case.set_budget;

import java.math.BigDecimal;

import entity.Budget;
import entity.Wallet;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * The Set Budget Interactor.
 */
public class SetBudgetInteractor implements SetBudgetInputBoundary {
    private final SetBudgetDataAccessInterface dataAccess;
    private final SetBudgetOutputBoundary presenter;
    private final Clock clock;

    public SetBudgetInteractor(
            SetBudgetDataAccessInterface dataAccess,
            SetBudgetOutputBoundary presenter) {

        this(dataAccess, presenter, Clock.systemUTC());
    }

    public SetBudgetInteractor(
            SetBudgetDataAccessInterface dataAccess,
            SetBudgetOutputBoundary presenter,
            Clock clock) {

        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.clock = clock.withZone(ZoneOffset.UTC);
    }

    @Override
    public void execute(SetBudgetInputData inputData) {
        String username = inputData.getUsername();
        String limitText = inputData.getDailyLimitText();
        String balanceText = inputData.getBalanceText();

        if (isBlank(username) || isBlank(limitText)) {
            presenter.prepareFailView(
                    "Username and daily budget are required.");
            return;
        }

        Wallet existingWallet = dataAccess.getWallet(username);
        boolean firstBalanceSetup = existingWallet == null;

        if (firstBalanceSetup && isBlank(balanceText)) {
            presenter.prepareFailView(
                    "Please enter your starting balance.");
            return;
        }

        BigDecimal dailyLimit;

        try {
            dailyLimit = new BigDecimal(limitText.trim());
        } catch (NumberFormatException exception) {
            presenter.prepareFailView(
                    "Daily limit must be a valid number.");
            return;
        }

        Budget budget;

        try {
            budget = new Budget(dailyLimit);
        } catch (IllegalArgumentException exception) {
            presenter.prepareFailView(exception.getMessage());
            return;
        }

        Wallet wallet = existingWallet;

        if (firstBalanceSetup) {
            BigDecimal startingBalance;

            try {
                startingBalance = new BigDecimal(balanceText.trim());
            } catch (NumberFormatException exception) {
                presenter.prepareFailView(
                        "Starting balance must be a valid number.");
                return;
            }

            wallet = new Wallet(startingBalance);
        }

        LocalDate today = LocalDate.now(clock);

        boolean firstBudgetSetup = !dataAccess.hasBudget(username);

        dataAccess.migrateLegacyBudget(username, today);

        LocalDate effectiveDate =
                firstBudgetSetup ? today : today.plusDays(1);

        dataAccess.saveBudgetEffectiveOn(
                username, budget, effectiveDate);

        // Budget edits must never reset an existing wallet.
        if (firstBalanceSetup) {
            dataAccess.saveWallet(username, wallet);
        }

        presenter.prepareSuccessView(new SetBudgetOutputData(
                username,
                dailyLimit.toPlainString(),
                wallet.getBalance().toPlainString()));
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}