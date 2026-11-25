package use_case.earn_reward_coins;

import data_access.EarnRewardCoinsDataAccessObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

import data_access.InMemoryUserDataAccessObject;
import data_access.InMemoryTransactionDataAccessObject;
import entity.Transaction;

import data_access.FileBudgetDataAccessObject;
import entity.Budget;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

class EarnRewardCoinsInteractorTest {

    private static final String USER_ID = "user1";
    private static final int REWARD = 10;

    // In Toronto, this instant is November 16 at 7:30 PM.
    // Therefore, yesterday is November 15, despite UTC being November 17.
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-17T00:30:00Z"),
            ZoneOffset.UTC);

    private final LocalDate yesterday = LocalDate.of(2026, 11, 15);

    private EarnRewardCoinsDataAccessObject dao;
    private TestPresenter presenter;
    private EarnRewardCoinsInteractor interactor;
    private InMemoryTransactionDataAccessObject transactionDao;

    @TempDir
    Path tempDirectory;

    private FileBudgetDataAccessObject budgetDao;


    @BeforeEach
    void setUp() {
        InMemoryUserDataAccessObject userDao =
                new InMemoryUserDataAccessObject();

        transactionDao =
                new InMemoryTransactionDataAccessObject(userDao);

        budgetDao = new FileBudgetDataAccessObject(
                tempDirectory.resolve("budgets.json").toString());

        dao = new EarnRewardCoinsDataAccessObject(transactionDao, budgetDao);
        dao.addUser(USER_ID, ZoneId.of("America/Toronto"));

        presenter = new TestPresenter();

        interactor = new EarnRewardCoinsInteractor(
                dao, presenter, REWARD, clock);
    }

    @Test
    void zeroSpendingEarnsReward() {
        setBudget("20.00");
        // No spending entry: DAO should return zero.

        claim();

        assertNotNull(presenter.result);
        assertNull(presenter.error);
        assertEquals(REWARD, presenter.result.getCoinsEarned());
        assertEquals(REWARD, presenter.result.getNewBalance());
        assertEquals(REWARD, dao.getCoinBalance(USER_ID));
        assertTrue(dao.hasClaimedReward(USER_ID, yesterday));
    }

    @Test
    void spendingBelowBudgetEarnsReward() {
        setBudget("20.00");
        addExpense("19.00");

        claim();

        assertNotNull(presenter.result);
        assertNull(presenter.error);
        assertEquals(REWARD, dao.getCoinBalance(USER_ID));
    }

    @Test
    void spendingEqualToBudgetEarnsReward() {
        setBudget("20.00");
        addExpense("20.00");

        claim();

        assertNotNull(presenter.result);
        assertNull(presenter.error);
        assertEquals(REWARD, presenter.result.getCoinsEarned());
        assertEquals(REWARD, dao.getCoinBalance(USER_ID));
        assertTrue(dao.hasClaimedReward(USER_ID, yesterday));
    }

    @Test
    void spendingAboveBudgetIsRejected() {
        setBudget("20.00");
        addExpense("21.00");

        claim();

        assertRejectedWithoutReward();
    }

    @Test
    void missingBudgetIsRejected() {
        claim();

        assertRejectedWithoutReward();
    }

    @Test
    void zeroBudgetCannotBeCreated() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Budget(BigDecimal.ZERO));
    }

    @Test
    void claimingTwiceDoesNotAddMoreCoins() {
        setBudget("20.00");
        claim();

        assertNotNull(presenter.result);

        claim();

        assertNull(presenter.result);
        assertNotNull(presenter.error);
        assertEquals(REWARD, dao.getCoinBalance(USER_ID));
        assertTrue(dao.hasClaimedReward(USER_ID, yesterday));
    }

    @Test
    void earlierClaimDoesNotPreventYesterdaysReward() {
        setBudget("20.00");

        // Seed a reward earned on an earlier day.
        dao.claimReward(USER_ID, yesterday.minusDays(1), REWARD);

        claim();

        assertNotNull(presenter.result);
        assertNull(presenter.error);
        assertEquals(REWARD * 2, dao.getCoinBalance(USER_ID));
        assertTrue(dao.hasClaimedReward(USER_ID, yesterday));
    }

    @Test
    void yesterdayUsesUsersTimeZoneInsteadOfUtc() {
        setBudget("20.00");

        // UTC yesterday is November 16; Toronto yesterday is November 15.
        LocalDate utcYesterday = LocalDate.now(clock).minusDays(1);
        budgetDao.saveBudgetEffectiveOn(
                USER_ID,
                new Budget(new BigDecimal("1.00")),
                utcYesterday);

        addExpense("10.00");
        claim();

        assertNotNull(presenter.result);
        assertTrue(dao.hasClaimedReward(USER_ID, yesterday));
        assertFalse(dao.hasClaimedReward(USER_ID, utcYesterday));
    }

    private void setBudget(String amount) {
        budgetDao.saveBudgetEffectiveOn(
                USER_ID,
                new Budget(new BigDecimal(amount)),
                yesterday);
    }

    private void addExpense(String amount) {
        transactionDao.save(
                USER_ID,
                new Transaction(
                        new BigDecimal(amount),
                        Transaction.Type.EXPENSE,
                        yesterday));
    }

    private void claim() {
        interactor.execute(new EarnRewardCoinsInputData(USER_ID));
    }

    private void assertRejectedWithoutReward() {
        assertNull(presenter.result);
        assertNotNull(presenter.error);
        assertEquals(0, dao.getCoinBalance(USER_ID));
        assertFalse(dao.hasClaimedReward(USER_ID, yesterday));
    }

    private static class TestPresenter
            implements EarnRewardCoinsOutputBoundary {

        private EarnRewardCoinsOutputData result;
        private String error;

        @Override
        public void prepareSuccessView(EarnRewardCoinsOutputData outputData) {
            result = outputData;
            error = null;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            result = null;
            error = errorMessage;
        }
    }
    @Test
    void higherBudgetTodayDoesNotMakeYesterdayEligible() {
        setBudget("20.00");
        addExpense("25.00");

        budgetDao.saveBudgetEffectiveOn(
                USER_ID,
                new Budget(new BigDecimal("100.00")),
                yesterday.plusDays(1));

        claim();

        assertRejectedWithoutReward();
    }

}