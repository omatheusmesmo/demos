package dev.omatheusmesmo.demos.invoicing.rules;

import static org.junit.Assert.assertEquals;

import dev.omatheusmesmo.demos.invoicing.domain.CustomerTier;
import java.math.BigDecimal;
import org.junit.BeforeClass;
import org.junit.Test;

public class DiscountRuleEngineTest {

    private static DiscountRuleEngine engine;

    @BeforeClass
    public static void loadRules() {
        engine = new DiscountRuleEngine();
        engine.init();
    }

    private static void assertDiscount(String expected, CustomerTier tier, String subtotal) {
        assertEquals(new BigDecimal(expected), engine.discountFor(tier, new BigDecimal(subtotal)));
    }

    @Test
    public void goldCustomersGetTenPercent() {
        assertDiscount("123.45", CustomerTier.GOLD, "1234.50");
    }

    @Test
    public void silverCustomersGetFivePercentAboveFiveHundred() {
        assertDiscount("30.00", CustomerTier.SILVER, "600.00");
    }

    @Test
    public void silverCustomersGetNothingAtExactlyFiveHundred() {
        assertDiscount("0.00", CustomerTier.SILVER, "500.00");
    }

    @Test
    public void largeOrdersGetAFlatDiscountAboveOneThousand() {
        assertDiscount("25.00", CustomerTier.STANDARD, "1000.01");
    }

    @Test
    public void standardCustomersGetNothingAtExactlyOneThousand() {
        assertDiscount("0.00", CustomerTier.STANDARD, "1000.00");
    }

    /**
     * The rules compute in doubles: 516.30 * 0.05 is 25.814999999999998, which rounds to 25.81, not to the 25.82
     * that exact decimal arithmetic gives. Invoices already issued carry these amounts, so the behaviour is pinned.
     */
    @Test
    public void halfCentResultsRoundTheWayTheIssuedInvoicesDid() {
        assertDiscount("25.81", CustomerTier.SILVER, "516.30");
        assertDiscount("1.71", CustomerTier.GOLD, "17.15");
    }
}
