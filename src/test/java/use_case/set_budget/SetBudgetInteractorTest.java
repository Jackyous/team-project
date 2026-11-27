package use_case.set_budget;

import java.math.BigDecimal;

import data_access.InMemorySetBudgetDataAccessObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import entity.Budget;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

class SetBudgetInteractorTest {

    @Test
    void successTest() {
        InMemorySetBudgetDataAccessObject dataAccess = new InMemorySetBudgetDataAccessObject();
        SetBudgetInputData inputData = new SetBudgetInputData("Paul", "50", "200");

        SetBudgetOutputBoundary successPresenter = new SetBudgetOutputBoundary() {
            @Override
            public void prepareSuccessView(SetBudgetOutputData outputData) {
                assertEquals("Paul", outputData.getUsername());
                assertEquals("50", outputData.getDailyLimitText());
                assertEquals("200", outputData.getBalanceText());
            }

            @Override
            public void prepareFailView(String errorMessage) {
                fail("Use case failure is unexpected.");
            }
        };

        SetBudgetInputBoundary interactor = new SetBudgetInteractor(dataAccess, successPresenter);
        interactor.execute(inputData);

        assertEquals(0, new BigDecimal("50").compareTo(dataAccess.getBudget(
                "Paul", java.time.LocalDate.now()).getLimit()));
        assertEquals(0, new BigDecimal("200").compareTo(dataAccess.getWallet("Paul").getBalance()));
    }

    @Test
    void changingBudgetDoesNotResetExistingBalance() {
        InMemorySetBudgetDataAccessObject dataAccess =
                new InMemorySetBudgetDataAccessObject();

        SetBudgetOutputBoundary presenter =
                new SetBudgetOutputBoundary() {
                    @Override
                    public void prepareSuccessView(
                            SetBudgetOutputData outputData) {
                        // State is checked below.
                    }

                    @Override
                    public void prepareFailView(String errorMessage) {
                        fail(errorMessage);
                    }
                };

        SetBudgetInteractor interactor =
                new SetBudgetInteractor(dataAccess, presenter);

        interactor.execute(
                new SetBudgetInputData("Paul", "50", "200"));

        // Attempting to supply a new starting balance must not reset it.
        interactor.execute(
                new SetBudgetInputData("Paul", "75", "999"));

        assertEquals(
                0,
                new BigDecimal("200").compareTo(
                        dataAccess.getWallet("Paul").getBalance()));
    }

    private SetBudgetOutputBoundary successfulPresenter() {
        return new SetBudgetOutputBoundary() {
            @Override
            public void prepareSuccessView(SetBudgetOutputData outputData) {
                // Each test checks the saved data.
            }

            @Override
            public void prepareFailView(String errorMessage) {
                fail(errorMessage);
            }
        };
    }

    @Test
    void budgetChangesApplyTomorrowAndPreserveTodaysBudget() {
        InMemorySetBudgetDataAccessObject dataAccess =
                new InMemorySetBudgetDataAccessObject();

        Clock clock = Clock.fixed(
                Instant.parse("2026-11-25T17:00:00Z"),
                ZoneId.of("America/Toronto"));

        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);

        SetBudgetInteractor interactor = new SetBudgetInteractor(
                dataAccess, successfulPresenter(), clock);

        // Initial setup applies today.
        interactor.execute(
                new SetBudgetInputData("Paul", "20", "200"));

        assertEquals(0, new BigDecimal("20").compareTo(
                dataAccess.getBudget("Paul", today).getLimit()));

        // Changes apply tomorrow. Balance is already initialized.
        interactor.execute(
                new SetBudgetInputData("Paul", "30", ""));

        assertEquals(0, new BigDecimal("20").compareTo(
                dataAccess.getBudget("Paul", today).getLimit()));

        assertEquals(0, new BigDecimal("30").compareTo(
                dataAccess.getBudget("Paul", tomorrow).getLimit()));

        // Another edit replaces tomorrow's pending limit.
        interactor.execute(
                new SetBudgetInputData("Paul", "25", ""));

        assertEquals(0, new BigDecimal("25").compareTo(
                dataAccess.getBudget("Paul", tomorrow).getLimit()));

        // The limit carries forward without a new entry every day.
        assertEquals(0, new BigDecimal("25").compareTo(
                dataAccess.getBudget("Paul", today.plusDays(10)).getLimit()));

        // Budget changes preserve the money balance.
        assertEquals(0, new BigDecimal("200").compareTo(
                dataAccess.getWallet("Paul").getBalance()));
    }

    @Test
    void legacyBudgetMigrationDoesNotInventPastHistory() {
        InMemorySetBudgetDataAccessObject dataAccess =
                new InMemorySetBudgetDataAccessObject();

        LocalDate today = LocalDate.of(2026, 11, 25);

        // Simulate an old budget with no effective date.
        dataAccess.saveBudget("Paul", new Budget(new BigDecimal("20")));

        dataAccess.migrateLegacyBudget("Paul", today);

        assertNull(dataAccess.getBudget("Paul", today.minusDays(1)));

        assertEquals(0, new BigDecimal("20").compareTo(
                dataAccess.getBudget("Paul", today).getLimit()));

        dataAccess.saveBudgetEffectiveOn(
                "Paul", new Budget(new BigDecimal("30")), today.plusDays(1));

        // Repeating migration must not overwrite the scheduled change.
        dataAccess.migrateLegacyBudget("Paul", today.plusDays(1));

        assertEquals(0, new BigDecimal("30").compareTo(
                dataAccess.getBudget("Paul", today.plusDays(1)).getLimit()));
    }
}