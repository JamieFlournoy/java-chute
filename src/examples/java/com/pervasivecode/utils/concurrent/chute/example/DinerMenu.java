package com.pervasivecode.utils.concurrent.chute.example;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.io.Resources;
import com.pervasivecode.utils.concurrent.chute.example.DinerExample.DinerItemType;

public class DinerMenu {
  private static final String DINER_ITEMS_RESOURCE_PATH =
      "com/pervasivecode/utils/concurrent/chute/example/diner_items.txt";

  private static final Pattern DINER_ITEM_HEADING = Pattern.compile("^\\[ (.*) \\]$");

  private static ImmutableListMultimap<DinerItemType, DinerItem> loadMenu() throws IOException {
    List<String> lines =
        Resources.readLines(Resources.getResource(DINER_ITEMS_RESOURCE_PATH), UTF_8);

    ImmutableListMultimap.Builder<DinerItemType, DinerItem> itemMapBuilder =
        ImmutableListMultimap.builder();
    DinerItemType currentType = DinerItemType.MAIN_COURSE;
    for (String line : lines) {
      Matcher dinerItemHeadingMatcher = DINER_ITEM_HEADING.matcher(line);
      if (dinerItemHeadingMatcher.find()) {
        String headingText = dinerItemHeadingMatcher.group(1);
        currentType = DinerItemType.valueOf(headingText.toUpperCase().replace(' ', '_'));
        continue;
      }
      if (line.length() > 0) {
        DinerItem item = DinerItem.of(currentType, line.trim());
        itemMapBuilder.put(currentType, item);
      }
    }
    return itemMapBuilder.build();
  }

  public static DinerMenu load() throws IOException {
    return new DinerMenu(loadMenu());
  }

  private final ImmutableListMultimap<DinerItemType, DinerItem> menuData;

  private DinerMenu(ImmutableListMultimap<DinerItemType, DinerItem> menuData) {
    this.menuData = menuData;
  }

  public List<DinerItemType> itemTypes() {
    return this.menuData.keys().asList();
  }

  public List<DinerItem> getItemsOfType(DinerItemType type) {
    return this.menuData.get(type);
  }

  public List<DinerItem> allItemsAsList() {
    return ImmutableList.copyOf(this.menuData.values());
  }
}
