package dev.omatheusmesmo.demos.invoicing.rules;

import dev.omatheusmesmo.demos.invoicing.domain.CustomerTier;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

@ApplicationScoped
public class DiscountRuleEngine {

    private static final String RULES = "/rules/discounts.js";

    private ScriptEngine engine;

    @PostConstruct
    public void init() {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try (Reader rules = new InputStreamReader(getClass().getResourceAsStream(RULES), "UTF-8")) {
            engine.eval(rules);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load discount rules from " + RULES, e);
        }
    }

    public synchronized BigDecimal discountFor(CustomerTier tier, BigDecimal subtotal) {
        try {
            Object result = ((Invocable) engine).invokeFunction("discount", tier.name(), subtotal.doubleValue());
            return BigDecimal.valueOf(((Number) result).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new IllegalStateException("Discount rules failed for " + tier + " " + subtotal, e);
        }
    }
}
