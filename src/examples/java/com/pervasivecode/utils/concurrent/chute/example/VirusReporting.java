package com.pervasivecode.utils.concurrent.chute.example;

import java.io.PrintWriter;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;

public class VirusReporting {
  public static void printInfectedFilenameAndVirusNames(PrintWriter output,
      String section, Multimap<String, Integer> virusesFound) {
    ImmutableList<String> virusNames = ImmutableSortedSet.copyOf(virusesFound.keySet()).asList();

    output.print(Archive.getSectionFilename(section));
    output.print(" contains the ");
    output.print(asQuotedCommaSeparatedList(virusNames));
    output.println(virusNames.size() == 1 ? " virus!" : " viruses!");
  }

  public static void printVirusOccurencesByLine(PrintWriter output,
      Multimap<String, Integer> virusesFound, List<String> lines) {
    for (String virusName : virusesFound.keySet()) {
      for (int lineNum : virusesFound.get(virusName)) {
        output.println(String.format("  line %d: %s", lineNum, lines.get(lineNum)));
      }
    }
  }

  private static String asQuotedCommaSeparatedList(List<String> virusNames) {
    StringBuilder sb = new StringBuilder();
    int lastIndex = virusNames.size() - 1;
    for (int i = 0; i <= lastIndex; i++) {
      sb.append('"');
      sb.append(virusNames.get(i));
      sb.append('"');

      if (i != lastIndex) {
        if (lastIndex == 1) {
          sb.append(" ");
        } else {
          sb.append(", ");
        }
        if (i == lastIndex - 1) {
          sb.append("and ");
        }
      }
    }
    return sb.toString();
  }
}
