package com.pervasivecode.utils.concurrent.chute.example;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.pervasivecode.utils.concurrent.chute.example.DinerExample.DinerItemType;

public class DinerCustomer {
  private final int customerNumber;
  private final ImmutableSet<DinerItem> originalOrder;
  private final Set<DinerItem> awaitedItems;

  public DinerCustomer(int customerNumber, DinerMenu menu) {
    this.customerNumber = customerNumber;
    this.originalOrder = generateOrder(customerNumber, menu);
    this.awaitedItems = new HashSet<>();
    awaitedItems.addAll(originalOrder);
  }

  public Set<DinerItem> takeOrder() {
    return originalOrder;
  }

  public void deliverFood(DinerItem item) {
    boolean removed = awaitedItems.remove(item);
    if (!removed) {
      throw new IllegalArgumentException(
          String.format("Customer %d did not order item %s", customerNumber, item.name()));
    }
  }

  public Set<DinerItem> awaitedItems() {
    return ImmutableSet.copyOf(this.awaitedItems);
  }

  private static ImmutableSet<DinerItem> generateOrder(int customerNumber, DinerMenu menu) {
    ImmutableSet.Builder<DinerItem> orderBuilder = ImmutableSet.builder();

    for (DinerItemType type : menu.itemTypes()) {
      List<DinerItem> itemCollection = menu.getItemsOfType(type);
      // Not every customer wants each type of item, so skip items sometimes.
      if ((customerNumber + itemCollection.size()) % 3 == 0) {
        continue;
      }
      ImmutableList<DinerItem> items = ImmutableList.copyOf(itemCollection);
      int itemIndex = (customerNumber + type.name().length()) % items.size();
      orderBuilder.add(items.get(itemIndex));
    }
    return orderBuilder.build();
  }

  public static int maxItemsPerCustomer() {
    return DinerItemType.values().length;
  }
}
