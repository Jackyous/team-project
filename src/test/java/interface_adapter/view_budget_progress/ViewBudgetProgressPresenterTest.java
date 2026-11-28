package interface_adapter.view_budget_progress;

import org.junit.jupiter.api.Test;
import use_case.view_budget_progress.ViewBudgetProgressOutputData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ViewBudgetProgressPresenterTest {
    private final ViewBudgetProgressViewModel model = new ViewBudgetProgressViewModel();
    private final ViewBudgetProgressPresenter presenter = new ViewBudgetProgressPresenter(model);
    private final LocalDate day = LocalDate.of(2026, 11, 28);

    private void present(String spent, String remaining, String percentage, String tomorrow) {
        presenter.prepareSuccessView(new ViewBudgetProgressOutputData(day, new BigDecimal("100"),
                new BigDecimal(spent), new BigDecimal(remaining), new BigDecimal(percentage),
                new BigDecimal(tomorrow)));
    }

    @Test void formatsMoneyAndShowsScheduledChangeWithoutChangingTodaysBudget() {
        AtomicInteger notifications = new AtomicInteger();
        model.addListener(notifications::incrementAndGet);
        present("25.25", "74.75", "25.3", "150");
        String message = model.getState().getMessage();
        assertTrue(message.contains("Date (UTC): 2026-11-28"));
        assertTrue(message.contains("Daily budget: 100.00"));
        assertTrue(message.contains("Spent today: 25.25"));
        assertTrue(message.contains("Remaining: 74.75"));
        assertTrue(message.contains("Budget from 2026-11-29 (UTC): 150.00"));
        assertTrue(message.contains("00:00 UTC"));
        assertEquals(1, notifications.get());
    }

    @Test void equalLimitWithDifferentScaleDoesNotAppearAsChange() {
        present("0", "100", "0.0", "100.00");
        assertFalse(model.getState().getMessage().contains("Budget from"));
        assertEquals(0, model.getState().getPercentage());
    }

    @Test void exactlyAtLimitShowsZeroRemaining() {
        present("100", "0", "100.0", "100");
        assertTrue(model.getState().getMessage().contains("Remaining: 0.00"));
        assertFalse(model.getState().getMessage().contains("Over budget"));
        assertEquals(100, model.getState().getPercentage());
    }

    @Test void overspendingKeepsTruePercentageButCapsTheBar() {
        present("125.25", "-25.25", "125.3", "100");
        assertTrue(model.getState().getMessage().contains("Over budget: 25.25"));
        assertTrue(model.getState().getMessage().contains("Budget used: 125.3%"));
        assertEquals(100, model.getState().getPercentage());
        assertFalse(model.getState().isError());
    }

    @Test void failureClearsPreviousAccountsDetailsAndSuccessClearsError() {
        present("125.25", "-25.25", "125.3", "200");
        presenter.prepareFailView("Set a daily budget first.");
        assertEquals("Set a daily budget first.", model.getState().getMessage());
        assertEquals(0, model.getState().getPercentage());
        assertTrue(model.getState().isError());
        present("0", "100", "0.0", "100");
        assertFalse(model.getState().isError());
    }
}
