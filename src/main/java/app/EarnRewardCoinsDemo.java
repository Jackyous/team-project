package app;

import data_access.EarnRewardCoinsDataAccessObject;
import interface_adapter.earn_reward_coins.*;
import use_case.earn_reward_coins.EarnRewardCoinsInteractor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import use_case.earn_reward_coins.EarnRewardCoinsStatusInteractor;
import view.EarnRewardCoinsView;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import data_access.InMemoryUserDataAccessObject;
import data_access.InMemoryTransactionDataAccessObject;

import data_access.FileBudgetDataAccessObject;
import entity.Budget;

public class EarnRewardCoinsDemo {
    public static void main(String[] args) {
        Clock clock = Clock.systemUTC();
        ZoneId timeZone = ZoneId.of("America/Toronto");

        LocalDate yesterday =
                LocalDate.now(clock.withZone(timeZone)).minusDays(1);

        InMemoryUserDataAccessObject userDao =
                new InMemoryUserDataAccessObject();

        InMemoryTransactionDataAccessObject transactionDao =
                new InMemoryTransactionDataAccessObject(userDao);

        FileBudgetDataAccessObject budgetDao =
                new FileBudgetDataAccessObject("reward-demo-budgets.json");

        EarnRewardCoinsDataAccessObject dao =
                new EarnRewardCoinsDataAccessObject(transactionDao, budgetDao);

        dao.addUser("user1", timeZone);
        // Seed historical data for this standalone demo only.
        budgetDao.saveBudgetEffectiveOn(
                "user1",
                new Budget(new BigDecimal("20.00")),
                yesterday);

        // No spending entry: yesterday's spending defaults to zero.

        EarnRewardCoinsViewModel viewModel =
                new EarnRewardCoinsViewModel();

        EarnRewardCoinsPresenter presenter =
                new EarnRewardCoinsPresenter(viewModel);

        // Example reward amount; change this to your chosen amount.
        EarnRewardCoinsInteractor interactor =
                new EarnRewardCoinsInteractor(dao, presenter, 10, clock);

        EarnRewardCoinsStatusInteractor statusInteractor =
                new EarnRewardCoinsStatusInteractor(dao, presenter, clock);

        EarnRewardCoinsController controller =
                new EarnRewardCoinsController(interactor, statusInteractor);

        viewModel.addPropertyChangedListener(event -> {
            EarnRewardCoinsState state = viewModel.getState();

            if (!state.getError().isEmpty()) {
                System.out.println(state.getError());
            } else {
                System.out.println(state.getMessage());
                System.out.println("Balance: " + state.getCoinBalance());
            }
        });

        SwingUtilities.invokeLater(() -> {
            EarnRewardCoinsView view = new EarnRewardCoinsView(
                    "user1", controller, viewModel);

            JFrame frame = new JFrame("Earn Reward Coins");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(view);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
