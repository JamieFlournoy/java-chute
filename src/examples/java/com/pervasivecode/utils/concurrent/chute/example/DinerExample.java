package com.pervasivecode.utils.concurrent.chute.example;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.pervasivecode.utils.concurrent.chute.BufferingChute;
import com.pervasivecode.utils.concurrent.chute.Chutes;
import com.pervasivecode.utils.concurrent.chute.ListenableChute;
import com.pervasivecode.utils.time.api.CurrentNanosSource;

/**
 * This example program shows how a single thread, representing a very busy waiter, can handle a
 * large number of production queues, as long as the single thread doesn't have to block waiting for
 * input, because it uses ListenableChute.
 */
public class DinerExample implements ExampleApplication {
  private static final int NUM_CUSTOMERS = 100;
  private static final Joiner AND_JOINER = Joiner.on(" and ");

  public enum DinerItemType {
    MAIN_COURSE, SIDE, BEVERAGE, DESSERT
  }

  private final boolean useVerboseOutput;

  public DinerExample(boolean useVerboseOutput) {
    this.useVerboseOutput = useVerboseOutput;
  }

  @Override
  public void runExample(PrintWriter output) throws Exception {
    DinerMenu menu = DinerMenu.load();

    CurrentNanosSource nanoSource = () -> System.nanoTime();
    long startTimeNanos = nanoSource.currentTimeNanoPrecision();

    ImmutableList.Builder<DinerCustomer> customersBuilder = ImmutableList.builder();
    for (int i = 0; i < NUM_CUSTOMERS; i++) {
      customersBuilder.add(new DinerCustomer(i, menu));
    }
    List<DinerCustomer> customers = customersBuilder.build();

    // This is the clip that holds any number of orders for the chef to read.
    BufferingChute<OrderTicket> kitchenOrderHolder =
        new BufferingChute<>(NUM_CUSTOMERS * DinerCustomer.maxItemsPerCustomer(), nanoSource);

    // In this diner, every kind of item has its own separate chute in the window to the kitchen.
    // Coffee goes in the coffee chute; hamburgers go in the hamburger chute. The cook will ring a
    // bell (represented by the listener being invoked) after putting an item on a shelf. The wait
    // staff just deliver cooked items to the customers as soon as the items are ready.
    Map<DinerItem, ListenableChute<CookedOrder>> cookedItemChutes = new HashMap<>();
    List<DinerItem> menuItemList = menu.allItemsAsList();
    for (int i = 0; i < menuItemList.size(); i++) {
      ListenableChute<CookedOrder> kitchenCounterWindowWithBell =
          Chutes.asListenableChute(new BufferingChute<>(3, nanoSource));
      cookedItemChutes.put(menuItemList.get(i), kitchenCounterWindowWithBell);
    }

    // The cook takes order tickets and puts items on the grill, then puts cooked items in the
    // cookedItemChutes.
    ExecutorService cook = Executors.newSingleThreadExecutor();
    Runnable cookAllOrders = () -> {
      // The grill has unlimited capacity, and everything just sits on the grill until it's done.
      DelayQueue<CookingItem> grill = new DelayQueue<CookingItem>();

      try {
        for (OrderTicket order : Chutes.asIterable(kitchenOrderHolder)) {
          grill.put(CookingItem.of(order, nanoSource));
        }
        while (!grill.isEmpty()) {
          CookedOrder cookedOrder = grill.take().asCookedItem();
          cookedItemChutes.get(cookedOrder.item()).put(cookedOrder);
        }
        for (ListenableChute<CookedOrder> cookedItemChute : cookedItemChutes.values()) {
          cookedItemChute.close();
        }
      } catch (@SuppressWarnings("unused") InterruptedException e) {
        // Just stop cooking.
        return;
      }
    };
    Future<?> cookingResult = cook.submit(cookAllOrders);

    // The waiter first sets up listeners to handle all of the cookedItemChutes, then takes orders
    // from the customers and submits them in the form of OrderTickets to the kitchenOrderHolder.
    // Then, the cookedItemChutes invoke the listeners, causing the waiter thread to deliver the
    // food. The waiter doesn't have to poll the cookedItemChutes for data; the chutes notify him
    // that the food is ready to be picked up.
    ExecutorService waiter = Executors.newSingleThreadExecutor();

    Runnable takeAllOrders = () -> {
      try {
        // Listen to each cookedItemChute for cooked items that should be delivered.
        for (ListenableChute<CookedOrder> cookedItemChute : cookedItemChutes.values()) {
          Runnable deliverCookedItem = () -> {
            Optional<CookedOrder> optionalCookedOrder = cookedItemChute.tryTakeNow();
            if (!optionalCookedOrder.isPresent()) {
              // This should never happen since the waiter is the only consumer of cookedItemChute
              // elements.
              return;
            }
            CookedOrder cookedOrder = optionalCookedOrder.get();
            customers.get(cookedOrder.customerNumber()).deliverFood(cookedOrder.item());
          };
          cookedItemChute.addListener(deliverCookedItem, waiter);
        }

        // Take all of the orders from customers, and put them in the kitchenOrderHolder. This will
        // result in the cook cooking the items and putting them in the chute, which is what will
        // trigger the listeners we just set up above.
        for (int customerNum = 0; customerNum < NUM_CUSTOMERS; customerNum++) {
          for (DinerItem orderItem : customers.get(customerNum).takeOrder()) {
            kitchenOrderHolder.put(OrderTicket.of(customerNum, orderItem));
          }
        }
        kitchenOrderHolder.close();
      } catch (@SuppressWarnings("unused") InterruptedException e) {
        // Just stop taking orders.
        return;
      }
    };
    Future<?> servingResult = waiter.submit(takeAllOrders);

    // Close down the restaurant.
    try {
      servingResult.get(2, SECONDS); // Wait for all of the orders to be taken.
      cookingResult.get(2, SECONDS); // Wait for all of the items to be cooked.
      // Wait for all of the items to be delivered to customers:
      waiter.shutdown();
      waiter.awaitTermination(2, SECONDS);
    } catch (TimeoutException te) {
      output.println(te.getMessage());
      te.printStackTrace(output);
    }

    long endTimeNanos = nanoSource.currentTimeNanoPrecision();

    // Verify that everybody got what they ordered.
    int numUndeliveredItems = 0;
    for (int i = 0; i < NUM_CUSTOMERS; i++) {
      DinerCustomer c = customers.get(i);
      if (useVerboseOutput) {
        output.println(String.format("Customer %d ordered %s", i, AND_JOINER.join(c.takeOrder())));
      }
      for (DinerItem item : c.awaitedItems()) {
        numUndeliveredItems++;
        output.println(String.format("Customer %d didn't get item %s", i, item));
      }
    }

    long elapsedMillis = MILLISECONDS.convert(endTimeNanos - startTimeNanos, NANOSECONDS);
    if (elapsedMillis > 250) {
      output.println(String.format("Total time: %dms", elapsedMillis));
    } else {
      if (numUndeliveredItems == 0) {
        output.println("All customers got what they ordered in less than 250ms.");
      }
    }
    output.flush();
  }
}
