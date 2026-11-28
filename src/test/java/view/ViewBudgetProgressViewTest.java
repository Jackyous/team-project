package view;

import data_access.InMemoryBudgetDataAccessObject;
import data_access.InMemoryTransactionDataAccessObject;
import data_access.InMemoryUserDataAccessObject;
import entity.Transaction;
import interface_adapter.view_budget_progress.ViewBudgetProgressController;
import interface_adapter.view_budget_progress.ViewBudgetProgressPresenter;
import interface_adapter.view_budget_progress.ViewBudgetProgressViewModel;
import org.junit.jupiter.api.Test;
import use_case.view_budget_progress.ViewBudgetProgressInteractor;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class ViewBudgetProgressViewTest {
    @Test void refreshBackAndAccountChangesUseCurrentState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InMemoryUserDataAccessObject users = new InMemoryUserDataAccessObject();
            InMemoryTransactionDataAccessObject transactions = new InMemoryTransactionDataAccessObject(users);
            InMemoryBudgetDataAccessObject budgets = new InMemoryBudgetDataAccessObject();
            ViewBudgetProgressViewModel model = new ViewBudgetProgressViewModel();
            ViewBudgetProgressView view = new ViewBudgetProgressView(model);
            assertFalse(button(view, "Refresh").isEnabled());
            assertFalse(button(view, "Back").isEnabled());
            Clock clock = Clock.fixed(Instant.parse("2026-11-28T01:00:00Z"), ZoneOffset.UTC);
            view.setController(new ViewBudgetProgressController(
                    new ViewBudgetProgressInteractor(transactions, budgets,
                            new ViewBudgetProgressPresenter(model), clock)));
            users.setCurrentUsername("alice");
            budgets.saveDailyLimit("alice", new BigDecimal("100"));
            button(view, "Refresh").doClick();
            assertTrue(component(view, JTextArea.class).getText().contains("Spent today: 0.00"));
            transactions.save("alice", new Transaction(new BigDecimal("25.25"),
                    Transaction.Type.EXPENSE, LocalDate.now(clock)));
            button(view, "Refresh").doClick();
            assertTrue(component(view, JTextArea.class).getText().contains("Remaining: 74.75"));
            assertEquals(25, component(view, JProgressBar.class).getValue());
            users.setCurrentUsername("bob");
            button(view, "Refresh").doClick();
            assertEquals("Set a daily budget first.", component(view, JTextArea.class).getText());
            assertFalse(component(view, JProgressBar.class).isVisible());
            budgets.saveDailyLimit("bob", new BigDecimal("10"));
            button(view, "Refresh").doClick();
            assertTrue(component(view, JProgressBar.class).isVisible());
            assertTrue(component(view, JTextArea.class).getText().contains("Remaining: 10.00"));
            users.setCurrentUsername(null);
            button(view, "Refresh").doClick();
            assertFalse(component(view, JProgressBar.class).isVisible());
            boolean[] back = {false};
            view.setBackAction(() -> back[0] = true);
            button(view, "Back").doClick();
            assertTrue(back[0]);
        });
    }

    private static JButton button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText())) return (JButton) child;
            if (child instanceof Container) {
                JButton found = button((Container) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Component> T component(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) {
                T found = component((Container) child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
