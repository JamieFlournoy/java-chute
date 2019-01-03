package com.pervasivecode.utils.concurrent.chute.example;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This is a single-threaded implementation of the ParallelVirusScannerExample code, to help clarify
 * what the parallel code does.
 * 
 * @see ParallelVirusScannerExample
 */
public class SingleThreadedVirusScannerExample implements ExampleApplication {
  private final boolean useVerboseOutput;

  public SingleThreadedVirusScannerExample(boolean useVerboseOutput) {
    this.useVerboseOutput = useVerboseOutput;
  }

  @Override
  public void runExample(PrintWriter output) throws IOException {
    if (useVerboseOutput) {
      output.println("Scanning for viruses...");
    }
    // Read all of the archive data into a String.
    String archiveContents = Archive.getArchiveAsString();
    // Split the String into chapters.
    Iterable<String> sections = Archive.getSections(archiveContents);
    // Search each chapter for "virus" occurrences and print scan reports to the output PrintWriter.
    for (String section : sections) {
      VirusScanner.scanOneFile(section, output, useVerboseOutput);
    }
  }

  public static void main(String[] args) throws Exception {
    new SingleThreadedVirusScannerExample(true)
        .runExample(new PrintWriter(System.out, true, UTF_8));
  }
}
