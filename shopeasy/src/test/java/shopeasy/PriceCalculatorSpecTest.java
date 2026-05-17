package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 1 – Specification-Based Testing (Chapter 2)
 *
 * <p>Target class: {@link PriceCalculator}
 *
 * <p>Your goal is to test {@code PriceCalculator.calculate(basePrice, discountRate, taxRate)}
 * using the domain testing technique from Chapter 2:
 * <ol>
 *   <li>Identify equivalence partitions for each input dimension.</li>
 *   <li>Identify boundary values between partitions (on-point / off-point).</li>
 *   <li>Write at least 10 meaningful test cases that cover both partitions and boundaries.</li>
 *   <li>Use {@code @ParameterizedTest} with {@code @CsvSource} for tests that share structure.</li>
 *   <li>Add a comment above each test method explaining which partition or boundary it covers.</li>
 * </ol>
 *
 * <h3>Input dimensions to consider</h3>
 * <ul>
 *   <li><b>basePrice</b>  – zero, positive, very large</li>
 *   <li><b>discountRate</b> – 0 (no discount), (0,100) typical, 100 (full discount)</li>
 *   <li><b>taxRate</b>    – 0 (no tax), (0,100) typical, 100 (100% tax)</li>
 * </ul>
 */
class PriceCalculatorSpecTest {

    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PriceCalculator();
    }
    // -----------------------------------------------------------------------
    // Specification-based tests for PriceCalculator
    // Each test has a short comment describing the partition or boundary covered.

    /** Partition: zero base price — result must always be 0 regardless of rates */
    @Test
    void zeroPriceAlwaysReturnsZero() {
        assertThat(calculator.calculate(0.0, 20.0, 10.0)).isEqualTo(0.0);
    }

    /** Boundary: discountRate at lower bound (0%) — no discount applied */
    @Test
    void discountRateZeroMeansNoDiscount() {
        assertThat(calculator.calculate(100.0, 0.0, 0.0)).isEqualTo(100.0);
    }

    /** Boundary: discountRate at upper bound (100%) — full discount wipes price to 0 (even with tax) */
    @Test
    void discountRateHundredMeansFullDiscount() {
        assertThat(calculator.calculate(100.0, 100.0, 10.0)).isEqualTo(0.0);
    }

    /** Boundary: taxRate at lower bound (0%) — no tax applied */
    @Test
    void taxRateZeroMeansNoTax() {
        assertThat(calculator.calculate(100.0, 10.0, 0.0)).isEqualTo(90.0);
    }

    /** Boundary: taxRate at upper bound (100%) — doubles the discounted price */
    @Test
    void taxRateHundredDoublesPriceAfterDiscount() {
        // base=100, discount=50% => 50; tax=100% => 100
        assertThat(calculator.calculate(100.0, 50.0, 100.0)).isEqualTo(100.0);
    }

    /** Partition: very large base price — ensure calculation scales (no overflow here) */
    @Test
    void largeBasePriceHandledCorrectly() {
        double base = 1_000_000_000.0; // 1 billion
        double expected = base * (1.0 - 10.0 / 100.0) * (1.0 + 5.0 / 100.0); // 0.9 * 1.05 = 0.945
        assertThat(calculator.calculate(base, 10.0, 5.0)).isCloseTo(expected, within(0.01));
    }

    /** Typical values — parameterized cases covering several partitions and boundaries */
    @ParameterizedTest(name = "base={0}, disc={1}%, tax={2}% => {3}")
    @CsvSource({
        // typical: discount then tax
        "100.0, 10.0, 20.0, 108.0",
        // no discount, some tax
        "200.0, 0.0, 10.0, 220.0",
        // half discount, no tax
        "50.0, 50.0, 0.0, 25.0",
        // discount only
        "100.0, 10.0, 0.0, 90.0"
    })
    void typicalValues(double base, double disc, double tax, double expected) {
        assertThat(calculator.calculate(base, disc, tax)).isCloseTo(expected, within(0.001));
    }

    /** Edge/invalid: negative discount (outside pre-condition) — treated by formula (increases price) */
    @Test
    void negativeDiscountIncreasesPriceAccordingToFormula() {
        // base=100, discount=-10% => 110
        assertThat(calculator.calculate(100.0, -10.0, 0.0)).isCloseTo(110.0, within(0.001));
    }

    /** Edge/invalid: discount > 100% (outside pre-condition) — formula produces negative intermediate price */
    @Test
    void discountOverHundredProducesNegativeIntermediatePrice() {
        // base=100, discount=150% => discounted = -50
        assertThat(calculator.calculate(100.0, 150.0, 0.0)).isCloseTo(-50.0, within(0.001));
    }

    // -----------------------------------------------------------------------

}
