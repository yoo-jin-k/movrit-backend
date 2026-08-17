package com.movrit.movrit.order;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateTest {

    @Test
    void paidPreparingShippedDeliveredFlowIsSequential() {
        Order order = Order.builder().status(OrderStatus.PENDING_PAYMENT).build();

        order.markPaid();
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertNotNull(order.getPaidAt());

        order.markPreparing();
        order.markShipped();
        order.markDelivered();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void shippedOrderCannotBeCancelled() {
        Order order = Order.builder().status(OrderStatus.PREPARING).build();
        order.markShipped();
        assertThrows(IllegalStateException.class, order::cancel);
    }
}
