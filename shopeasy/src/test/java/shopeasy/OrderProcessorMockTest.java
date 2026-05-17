package shopeasy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Mockito-based unit tests for {@link OrderProcessor} isolating external services.
 */
@ExtendWith(MockitoExtension.class)
class OrderProcessorMockTest {

    @Mock
    InventoryService inventoryService;

    @Mock
    PaymentGateway paymentGateway;

    @InjectMocks
    OrderProcessor processor;

    @Test
    void happyPath_inventoryAvailableAndPaymentSucceeds_returnsOrder() {
        Product p = new Product("P1", "Prod", 10.0, 100);
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(p, 2);

        when(inventoryService.isAvailable(p, 2)).thenReturn(true);
        when(paymentGateway.charge("CUST-1", cart.total())).thenReturn(true);

        Order order = processor.process("CUST-1", cart);

        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("CUST-1");
        assertThat(order.getTotal()).isEqualTo(cart.total());

        verify(inventoryService).isAvailable(p, 2);
        verify(paymentGateway).charge("CUST-1", cart.total());
    }

    @Test
    void inventoryFailure_noPaymentAttempted_returnsNull() {
        Product p = new Product("P2", "Prod2", 5.0, 0);
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(p, 1);

        when(inventoryService.isAvailable(p, 1)).thenReturn(false);

        Order order = processor.process("CUST-2", cart);

        assertThat(order).isNull();
        verify(inventoryService).isAvailable(p, 1);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    void paymentFailure_inventoryOk_chargeFails_returnsNull() {
        Product p = new Product("P3", "Prod3", 7.5, 10);
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(p, 3);

        when(inventoryService.isAvailable(p, 3)).thenReturn(true);
        when(paymentGateway.charge("CUST-3", cart.total())).thenReturn(false);

        Order order = processor.process("CUST-3", cart);

        assertThat(order).isNull();
        verify(inventoryService).isAvailable(p, 3);
        verify(paymentGateway).charge("CUST-3", cart.total());
    }

    @Test
    void partialQuantity_someItemsUnavailable_expectAbortAndNoCharge() {
        Product a = new Product("PA", "A", 2.0, 5);
        Product b = new Product("PB", "B", 3.0, 1);
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(a, 2);
        cart.addItem(b, 4); // requesting more than available

        when(inventoryService.isAvailable(a, 2)).thenReturn(true);
        when(inventoryService.isAvailable(b, 4)).thenReturn(false);

        Order order = processor.process("CUST-4", cart);

        assertThat(order).isNull();
        verify(inventoryService).isAvailable(a, 2);
        verify(inventoryService).isAvailable(b, 4);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }
    }
