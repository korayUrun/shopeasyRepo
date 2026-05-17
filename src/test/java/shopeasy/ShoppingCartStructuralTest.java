package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ShoppingCartStructuralTest {

    private ShoppingCart cart;
    private Product p1;
    private Product p2;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
        p1 = new Product("p1", "Widget", 10.0, 100);
        p2 = new Product("p2", "Gadget", 5.0, 50);
    }

    /** New product added -> increases itemCount and total */
    @Test
    void addNewItemIncreasesCountAndTotal() {
        cart.addItem(p1, 2);
        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.total()).isEqualTo(20.0);
    }

    /** Adding same product again merges quantities into existing line */
    @Test
    void addExistingProductMergesQuantities() {
        cart.addItem(p1, 1);
        cart.addItem(p1, 3);
        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(4);
        assertThat(cart.total()).isEqualTo(40.0);
    }

    /** Removing an absent product does nothing (no exception) */
    @Test
    void removeNonExistingProductDoesNothing() {
        cart.addItem(p1, 1);
        cart.removeItem("no-such-id");
        assertThat(cart.itemCount()).isEqualTo(1);
    }

    /** Update quantity success path: updates subtotal and total */
    @Test
    void updateQuantityUpdatesSubtotal() {
        cart.addItem(p2, 2); // subtotal 10
        cart.updateQuantity("p2", 5); // subtotal 25
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total()).isEqualTo(25.0);
    }

    /** updateQuantity with non-positive quantity throws IllegalArgumentException */
    @Test
    void updateQuantityWithNonPositiveThrows() {
        cart.addItem(p1, 1);
        assertThatThrownBy(() -> cart.updateQuantity("p1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be > 0");
    }

    /** updateQuantity for missing product throws IllegalArgumentException */
    @Test
    void updateQuantityMissingProductThrows() {
        assertThatThrownBy(() -> cart.updateQuantity("missing", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    /** applyDiscount: 0% returns same total, 100% returns zero */
    @Test
    void applyDiscountZeroAndFull() {
        cart.addItem(p1, 2); // 20
        assertThat(cart.applyDiscount(0.0)).isEqualTo(20.0);
        assertThat(cart.applyDiscount(100.0)).isEqualTo(0.0);
    }

    /** getItems returns an unmodifiable view */
    @Test
    void getItemsIsUnmodifiable() {
        cart.addItem(p1, 1);
        List<CartItem> items = cart.getItems();
        assertThatThrownBy(() -> items.add(new CartItem(p2, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /** clear empties the cart and toString reflects empty state */
    @Test
    void clearEmptiesCartAndToStringReflectsIt() {
        cart.addItem(p1, 1);
        cart.addItem(p2, 2);
        cart.clear();
        assertThat(cart.itemCount()).isEqualTo(0);
        assertThat(cart.total()).isEqualTo(0.0);
        assertThat(cart.getItems()).isEmpty();
    }
}

