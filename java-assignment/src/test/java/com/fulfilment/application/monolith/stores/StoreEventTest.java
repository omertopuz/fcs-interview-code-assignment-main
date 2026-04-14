package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class StoreEventTest {

  @Test
  public void testCreateStoreEventWithCreatedType() {
    // given
    Store store = new Store();
    store.name = "Test Store";
    store.quantityProductsInStock = 50;

    // when
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // then
    assertNotNull(event);
    assertEquals(store, event.getStore());
    assertEquals(StoreEvent.Type.CREATED, event.getType());
  }

  @Test
  public void testCreateStoreEventWithUpdatedType() {
    // given
    Store store = new Store();
    store.name = "Updated Store";
    store.quantityProductsInStock = 100;

    // when
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    // then
    assertNotNull(event);
    assertEquals(store, event.getStore());
    assertEquals(StoreEvent.Type.UPDATED, event.getType());
  }

  @Test
  public void testStoreEventPreservesStoreReference() {
    // given
    Store store = new Store();
    store.name = "Reference Test";

    // when
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // then
    assertEquals("Reference Test", event.getStore().name);
  }
}
