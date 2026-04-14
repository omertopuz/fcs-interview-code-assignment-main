package com.fulfilment.application.monolith.stores;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StoreEventObserverTest {

  private LegacyStoreManagerGateway legacyStoreManagerGateway;
  private StoreEventObserver storeEventObserver;

  @BeforeEach
  public void setUp() {
    legacyStoreManagerGateway = mock(LegacyStoreManagerGateway.class);
    storeEventObserver = new StoreEventObserver();
    storeEventObserver.legacyStoreManagerGateway = legacyStoreManagerGateway;
  }

  @Test
  public void testOnStoreCreatedEvent_ShouldCallCreateOnLegacySystem() {
    // given
    Store store = new Store();
    store.name = "Test Store";
    store.quantityProductsInStock = 100;

    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // when
    storeEventObserver.onStoreEvent(event);

    // then
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(store);
  }

  @Test
  public void testOnStoreUpdatedEvent_ShouldCallUpdateOnLegacySystem() {
    // given
    Store store = new Store();
    store.name = "Test Store";
    store.quantityProductsInStock = 200;

    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    // when
    storeEventObserver.onStoreEvent(event);

    // then
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(store);
  }

  @Test
  public void testOnStoreCreatedEvent_ShouldNotCallUpdate() {
    // given
    Store store = new Store();
    store.name = "Test Store";

    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // when
    storeEventObserver.onStoreEvent(event);

    // then - verify update was NOT called
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(store);
  }

  @Test
  public void testOnStoreUpdatedEvent_ShouldNotCallCreate() {
    // given
    Store store = new Store();
    store.name = "Test Store";

    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    // when
    storeEventObserver.onStoreEvent(event);

    // then - verify create was NOT called
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(store);
  }
}
