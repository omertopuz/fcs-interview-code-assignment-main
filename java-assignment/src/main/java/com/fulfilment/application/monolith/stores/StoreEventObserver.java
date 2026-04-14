package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
public class StoreEventObserver {

  @Inject
  LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreEvent(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
    switch (event.getType()) {
      case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(event.getStore());
      case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(event.getStore());
    }
  }
}
