package com.movrit.movrit.inventory;

import com.movrit.movrit.inventory.entity.Inventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    @Test
    void reserveCommitAndReleaseKeepQuantitiesConsistent() {
        Inventory inventory = Inventory.builder()
                .onHandQuantity(100L)
                .reservedQuantity(0L)
                .safetyStockQuantity(10L)
                .build();

        assertEquals(90L, inventory.availableQuantity());
        inventory.reserve(20L);
        assertEquals(70L, inventory.availableQuantity());
        assertEquals(20L, inventory.getReservedQuantity());

        inventory.release(5L);
        assertEquals(75L, inventory.availableQuantity());
        assertEquals(15L, inventory.getReservedQuantity());

        inventory.commit(10L);
        assertEquals(90L, inventory.getOnHandQuantity());
        assertEquals(5L, inventory.getReservedQuantity());
        assertEquals(75L, inventory.availableQuantity());
    }

    @Test
    void reserveRejectsQuantityBeyondAvailableStock() {
        Inventory inventory = Inventory.builder()
                .onHandQuantity(10L)
                .reservedQuantity(0L)
                .safetyStockQuantity(2L)
                .build();

        assertThrows(IllegalStateException.class, () -> inventory.reserve(9L));
    }
}
