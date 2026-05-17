package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Task 3 – Design by Contract (Chapter 4)
 *
 * <p>This task has two parts:
 *
 * <h3>Part A – Add contracts to production code</h3>
 * Open {@link ShoppingCart} and {@link PriceCalculator} and add {@code assert}
 * statements for the pre-conditions and post-conditions described in their Javadoc.
 * Note: assertions are enabled via {@code -ea} in Maven Surefire (already configured
 * in {@code pom.xml}).
 *
 * <p>Contracts to implement:
 * <ul>
 *   <li><b>ShoppingCart.addItem</b>: pre — {@code product != null}, {@code quantity > 0};
 *       post — {@code itemCount()} increased or product quantity updated.</li>
 *   <li><b>ShoppingCart.applyDiscount</b>: pre — {@code 0 <= discountRate <= 100};
 *       post — result &lt;= {@code total()} when {@code discountRate > 0}.</li>
 *   <li><b>PriceCalculator.calculate</b>: pre — {@code basePrice >= 0},
 *       {@code 0 <= discountRate <= 100}, {@code 0 <= taxRate <= 100};
 *       post — result {@code >= 0}.</li>
 *   <li><b>ShoppingCart invariant</b>: {@code total() >= 0} after any operation.</li>
 * </ul>
 *
 * <h3>Part B – Write contract tests</h3>
 * Write tests below that:
 * <ol>
 *   <li>Verify contracts hold for valid inputs (positive tests).</li>
 *   <li>Verify contracts are violated ({@code AssertionError}) for invalid inputs (negative tests).</li>
 * </ol>
 *
 * <p>Use {@code assertThatThrownBy(...).isInstanceOf(AssertionError.class)} to test violations.
 */
class ContractTest {

    private ShoppingCart cart;
    private PriceCalculator calculator;
    private Product product;
    private InventoryService inventoryService;
    private PaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        cart       = new ShoppingCart();
        calculator = new PriceCalculator();
        product    = new Product("P001", "Widget", 10.0, 50);
        inventoryService = mock(InventoryService.class);
        paymentGateway = mock(PaymentGateway.class);
    }

    /** Valid inputs satisfy the ShoppingCart.addItem contract and preserve the invariant. */
    @Test
    void addItemValidInputShouldNotThrowAndKeepInvariant() {
        assertThatCode(() -> cart.addItem(product, 3)).doesNotThrowAnyException();
        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    /** Invalid product violates the ShoppingCart.addItem pre-condition. */
    @Test
    void addItemNullProductShouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(null, 1))
                .isInstanceOf(AssertionError.class);
    }

    /** Invalid quantity violates the ShoppingCart.addItem pre-condition. */
    @Test
    void addItemNonPositiveQuantityShouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(product, 0))
                .isInstanceOf(AssertionError.class);
    }

    /** Valid discount rate satisfies the ShoppingCart.applyDiscount contract. */
    @Test
    void applyDiscountValidInputShouldNotThrow() {
        cart.addItem(product, 2);
        assertThatCode(() -> cart.applyDiscount(10.0)).doesNotThrowAnyException();
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    /** Invalid discount rate violates the ShoppingCart.applyDiscount pre-condition. */
    @Test
    void applyDiscountInvalidRateShouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.applyDiscount(101.0))
                .isInstanceOf(AssertionError.class);
    }

    /** Valid inputs satisfy the PriceCalculator.calculate contract. */
    @Test
    void calculateValidInputShouldNotThrowAndStayNonNegative() {
        assertThatCode(() -> calculator.calculate(100.0, 10.0, 20.0)).doesNotThrowAnyException();
        assertThat(calculator.calculate(100.0, 10.0, 20.0)).isGreaterThanOrEqualTo(0.0);
    }

    /** Invalid base price violates the PriceCalculator.calculate pre-condition. */
    @Test
    void calculateNegativeBasePriceShouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(-1.0, 10.0, 20.0))
                .isInstanceOf(AssertionError.class);
    }

    /** Invalid tax rate violates the PriceCalculator.calculate pre-condition. */
    @Test
    void calculateOutOfRangeTaxShouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100.0, 10.0, 101.0))
                .isInstanceOf(AssertionError.class);
    }

    /** Constructor pre-conditions are enforced for OrderProcessor dependencies. */
    @Test
    void orderProcessorNullDependenciesShouldViolatePreCondition() {
        assertThatThrownBy(() -> new OrderProcessor(null, paymentGateway))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> new OrderProcessor(inventoryService, null))
                .isInstanceOf(AssertionError.class);
    }

    /** Valid OrderProcessor inputs should complete a successful checkout path. */
    @Test
    void orderProcessorValidInputShouldNotThrow() {
        cart.addItem(product, 2);
        when(inventoryService.isAvailable(product, 2)).thenReturn(true);
        when(paymentGateway.charge("C001", 20.0)).thenReturn(true);

        OrderProcessor processor = new OrderProcessor(inventoryService, paymentGateway);
        assertThatCode(() -> processor.process("C001", cart)).doesNotThrowAnyException();
        assertThat(processor.process("C001", cart)).isNotNull();
    }

    /** Invalid checkout inputs violate the OrderProcessor.process pre-conditions. */
    @Test
    void orderProcessorInvalidCheckoutInputsShouldViolatePreConditions() {
        OrderProcessor processor = new OrderProcessor(inventoryService, paymentGateway);

        assertThatThrownBy(() -> processor.process(null, cart))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> processor.process("   ", cart))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> processor.process("C001", null))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> processor.process("C001", new ShoppingCart()))
                .isInstanceOf(AssertionError.class);
    }

}
