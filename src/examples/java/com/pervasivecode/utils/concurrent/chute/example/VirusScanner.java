package com.pervasivecode.utils.concurrent.chute.example;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class VirusScanner {
  private static ImmutableMap<String, String> VIRUS_DEFINITIONS =
      ImmutableMap.<String, String>builder() //
          .put("ahoy", "ahoy") //
          .put("Ahoy", "ahoy") //
          .put("gangway", "gangway") //
          .put("tallow", "tallow") //
          .put("Yonder", "yonder") //
          .put("yonder", "yonder") //
          .build();

  public static void scanOneFile(String contents, PrintWriter output, boolean useVerboseOutput) {
    Set<Map.Entry<String, String>> signaturesAndNames = VIRUS_DEFINITIONS.entrySet();
    Multimap<String, Integer> virusesFound = MultimapBuilder //
        .treeKeys() // <- keySet will return keys in natural-sort order
        .arrayListValues() // <- values will be returned in the order in which they were added.
        .build();

    // The "virus signatures" don't contain any line breaks, so we can make this pretty simple.
    List<String> lines = ImmutableList.copyOf(Splitter.on("\r\n").split(contents));
    for (Map.Entry<String, String> signatureAndName : signaturesAndNames) {
      for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
        if (lines.get(lineNum).contains(signatureAndName.getKey())) {
          virusesFound.put(signatureAndName.getValue(), lineNum);
        }
      }
    }

    if (virusesFound.isEmpty()) {
      return;
    }

    VirusReporting.printInfectedFilenameAndVirusNames(output, contents, virusesFound);
    if (useVerboseOutput) {
      VirusReporting.printVirusOccurencesByLine(output, virusesFound, lines);
    }
  }
}
