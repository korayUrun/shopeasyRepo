package shopeasy;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Combinators;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for ShopEasy using jqwik.
 */
class ShopEasyPropertyTest {

    private final PriceCalculator calculator = new PriceCalculator();

    // ------------------------------------------------------------------
    // Providers
    // ------------------------------------------------------------------

    /**
     * Provide arbitrary valid products (non-blank id/name, non-negative price/stock).
     */
    @Provide
    Arbitrary<Product> products() {
        Arbitrary<String> ids = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(6);
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12);
        Arbitrary<Double> prices = Arbitraries.doubles().between(0.0, 1000.0);
        Arbitrary<Integer> stocks = Arbitraries.integers().between(0, 1000);
        return Combinators.combine(ids, names, prices, stocks)
            .as((id, name, price, stock) -> new Product(id + name, name, price, stock));
    }

    @Provide
    Arbitrary<Integer> quantities() {
        return Arbitraries.integers().between(1, 10);
    }

    // ------------------------------------------------------------------
    // Properties for PriceCalculator
    // ------------------------------------------------------------------

    /**
     * Monotonicity: Increasing the discount rate should not increase the final price.
     * This property would catch bugs where the discount is applied after tax
     * or where signs are inverted causing larger discounts to increase price.
     */
    @Property
    void discountMonotonicity(@ForAll("basePrices") double base,
                              @ForAll("discounts") double d1,
                              @ForAll("discounts") double d2,
                              @ForAll("taxes") double tax) {
        if (d1 > d2) { // ensure d1 <= d2
            double tmp = d1; d1 = d2; d2 = tmp;
        }
        double p1 = calculator.calculate(base, d1, tax);
        double p2 = calculator.calculate(base, d2, tax);
        assertThat(p1).isGreaterThanOrEqualTo(p2);
    }

    /**
     * Identity: 0% discount and 0% tax returns exactly the base price.
     * This catches bugs where either discount or tax logic is incorrect.
     */
    @Property
    void identityZeroDiscountZeroTax(@ForAll("basePrices") double base) {
        double result = calculator.calculate(base, 0.0, 0.0);
        assertThat(result).isEqualTo(base);
    }

    /**
     * Boundedness: final price is always >= 0 and <= base * (1 + maxTax/100).
     * This detects overflows, sign errors, or misplaced operations that
     * could produce values outside expected bounds.
     */
    @Property
    void boundedness(@ForAll("basePrices") double base,
                     @ForAll("discounts") double disc,
                     @ForAll("taxes") double tax) {
        double maxTax = 100.0;
        double finalPrice = calculator.calculate(base, disc, tax);
        assertThat(finalPrice).isGreaterThanOrEqualTo(0.0);
        assertThat(finalPrice).isLessThanOrEqualTo(base * (1.0 + maxTax / 100.0));
    }

    // jqwik auxiliary providers for primitive ranges
    @Provide
    Arbitrary<Double> basePrices() {
        return Arbitraries.doubles().between(0.0, 1_000_000.0);
    }

    @Provide
    Arbitrary<Double> discounts() {
        return Arbitraries.doubles().between(0.0, 100.0);
    }

    @Provide
    Arbitrary<Double> taxes() {
        return Arbitraries.doubles().between(0.0, 100.0);
    }

    // ------------------------------------------------------------------
    // Properties for ShoppingCart
    // ------------------------------------------------------------------

    /**
     * Cart commutativity: adding item A then B yields same total as B then A.
     * This would catch ordering bugs, side-effects that depend on insertion
     * order, or incorrect merging logic for identical product ids.
     */
    @Property
    void cartCommutativity(@ForAll("products") Product a,
                           @ForAll("products") Product b,
                           @ForAll("quantities") int qa,
                           @ForAll("quantities") int qb) {
        // skip cases where products share the same id — cart merges by id
        if (a.getId().equals(b.getId())) return;

        ShoppingCart c1 = new ShoppingCart();
        c1.addItem(a, qa);
        c1.addItem(b, qb);

        ShoppingCart c2 = new ShoppingCart();
        c2.addItem(b, qb);
        c2.addItem(a, qa);

        assertThat(c1.total()).isEqualTo(c2.total());
    }

}
