package use_case.view_budget_progress;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Today's spending and the daily limits effective today and tomorrow, in UTC. */
public class ViewBudgetProgressOutputData {
    private final LocalDate date;
    private final BigDecimal limit;
    private final BigDecimal spent;
    private final BigDecimal remaining;
    private final BigDecimal percentage;
    private final BigDecimal tomorrowLimit;

    public ViewBudgetProgressOutputData(LocalDate date, BigDecimal limit, BigDecimal spent,
            BigDecimal remaining, BigDecimal percentage) {
        this(date, limit, spent, remaining, percentage, limit);
    }

    public ViewBudgetProgressOutputData(LocalDate date, BigDecimal limit, BigDecimal spent,
            BigDecimal remaining, BigDecimal percentage, BigDecimal tomorrowLimit) {
        this.date = date;
        this.limit = limit;
        this.spent = spent;
        this.remaining = remaining;
        this.percentage = percentage;
        this.tomorrowLimit = tomorrowLimit;
    }

    public LocalDate getDate() { return date; }
    public BigDecimal getLimit() { return limit; }
    public BigDecimal getSpent() { return spent; }
    public BigDecimal getRemaining() { return remaining; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getTomorrowLimit() { return tomorrowLimit; }
}
