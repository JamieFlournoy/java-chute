package com.pervasivecode.utils.concurrent.chute.example;

import static java.util.concurrent.TimeUnit.SECONDS;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.google.auto.value.AutoValue;
import com.pervasivecode.utils.concurrent.chute.BufferingChute;
import com.pervasivecode.utils.concurrent.chute.ChuteEntrance;
import com.pervasivecode.utils.concurrent.chute.ChuteExit;
import com.pervasivecode.utils.concurrent.chute.SynchronousMultiplexer;
import com.pervasivecode.utils.time.api.CurrentNanosSource;

/**
 * This example program is a virus scanner. It reads all of the individual files from an archive,
 * and scans each one for viruses using a predefined set of virus signatures.
 * <p>
 * When it finds a virus, it reports which file was being scanned and which virus or viruses were
 * found.
 * <p>
 * Optionally, it will also print the context of the match, showing the byte position where the
 * match occurred and the bytes before and after the match.
 * <p>
 * Well, actually, this is a fake virus scanner. The "archive" is the The Project Gutenberg EBook of
 * Moby Dick; the "files" are individual chapters, and the "virus signatures" are the words "ahoy",
 * "Ahoy", "gangway", "tallow", "Yonder", and "yonder". The context shown in the verbose output
 * includes the whole line that includes the "signature". See examples.feature for what the real
 * output looks like.
 * <p>
 * The "archive" is split into files in a separate thread that searches the archive content for
 * "file headers" (which are actually just chapter headings).
 * <p>
 * The "files" are also searched in parallel.
 *
 * @see SingleThreadedVirusScannerExample for a simpler implementation that might help you
 *      understand what this code is doing.
 */
public class ParallelVirusScannerExample implements ExampleApplication {

  private final boolean useVerboseOutput;

  public ParallelVirusScannerExample(boolean useVerboseOutput) {
    this.useVerboseOutput = useVerboseOutput;
  }

  @Override
  public void runExample(PrintWriter output) throws Exception {
    if (useVerboseOutput) {
      output.println("Scanning for viruses...");
    }

    int numCores = Runtime.getRuntime().availableProcessors();
    int numSectionProcessors = numCores;
    int numWorkers = numSectionProcessors + 2; // 1 for sectionSplitter, 1 for resultCombiner
    ExecutorService executor = Executors.newFixedThreadPool(numWorkers);

    CurrentNanosSource nanosSource = () -> System.nanoTime();
    int sectionChuteSize = 10; // chosen arbitrarily
    BufferingChute<SectionWithIndex> sectionChute =
        new BufferingChute<>(sectionChuteSize, nanosSource);

    String archiveContents = Archive.getArchiveAsString();

    // Extracting individual sections from the archive is done lazily, so iterating over "sections"
    // in a separate thread allows other threads to work on virus-scanning each section at the same
    // time as additional sections are being extracted from archiveContents.
    Iterable<String> sections = Archive.getSections(archiveContents);
    Callable<Void> sectionSplitter = () -> {
      int sectionIndex = 0;
      for (String section : sections) {
        sectionChute.put(SectionWithIndex.of(sectionIndex, section));
        sectionIndex++;
      }
      sectionChute.close();
      return null;
    };
    Future<Void> futureSectionSplitResult = executor.submit(sectionSplitter);

    // Sections will be virus-scanned in parallel, and results of scanning each section will be put
    // into a chute as soon as they are ready, which is nondeterministic. The resultCombiner worker
    // will grab these unordered results as fast as it can, put them back into order, and print the
    // sorted, consecutive scan reports as soon as they become available.

    int sectionResultChuteSize = 10; // chosen arbitrarily
    BufferingChute<SectionResult> sectionResultChute =
        new BufferingChute<>(sectionResultChuteSize, nanosSource);

    // Give each SectionProcessor its own ChuteEntrance that it can close when it's done, closing
    // the real sectionResultChute when the last of these ChuteEntrances is closed.
    SynchronousMultiplexer<SectionResult> sectionResultMultiplexer =
        new SynchronousMultiplexer<>(numSectionProcessors, sectionResultChute);
    List<ChuteEntrance<SectionResult>> resultChutes = sectionResultMultiplexer.inputChutes();

    List<Future<?>> futureSectionProcessorResults = new ArrayList<>();
    for (int i = 0; i < numSectionProcessors; i++) {
      ChuteEntrance<SectionResult> resultChute = resultChutes.get(i);
      SectionProcessor task = new SectionProcessor(sectionChute, resultChute, useVerboseOutput);
      futureSectionProcessorResults.add(executor.submit(task));
    }

    Callable<Void> resultCombiner = () -> {
      int nextSectionIndexToPrint = 0;
      PriorityQueue<SectionResult> unprintedResults = new PriorityQueue<>();
      while (!sectionResultChute.isClosedAndEmpty()) {
        // Grab the next SectionResult.
        Optional<SectionResult> maybeSectionResult = sectionResultChute.take();
        if (!maybeSectionResult.isPresent()) {
          break;
        }
        unprintedResults.add(maybeSectionResult.get());

        // Print as many consecutive results starting with nextSectionIndexToPrint as we have queued
        // up in unprintedResults right now.
        while (!unprintedResults.isEmpty()
            && (unprintedResults.peek().sectionIndex() == nextSectionIndexToPrint)) {
          SectionResult resultToPrint = unprintedResults.remove();
          output.print(resultToPrint.scanReport());
          nextSectionIndexToPrint++;
        }
      }
      return null;
    };
    Future<Void> futureResultCombinerResult = executor.submit(resultCombiner);

    futureSectionSplitResult.get(10, SECONDS);
    for (Future<?> futureSectionProcessorResult : futureSectionProcessorResults) {
      futureSectionProcessorResult.get(10, SECONDS);
    }
    futureResultCombinerResult.get(10, SECONDS);
  }

  private static class SectionProcessor implements Callable<Void> {
    private final ChuteExit<SectionWithIndex> sectionChute;
    private final ChuteEntrance<SectionResult> sectionResultChute;
    private boolean useVerboseOutput;

    public SectionProcessor(ChuteExit<SectionWithIndex> sectionChute,
        ChuteEntrance<SectionResult> sectionResultChute, boolean useVerboseOutput) {
      this.sectionChute = sectionChute;
      this.sectionResultChute = sectionResultChute;
      this.useVerboseOutput = useVerboseOutput;
    }

    @Override
    public Void call() throws Exception {
      while (!sectionChute.isClosedAndEmpty()) {
        Optional<SectionWithIndex> maybeSection = sectionChute.take();
        if (!maybeSection.isPresent()) {
          break;
        }
        SectionWithIndex sectionWithIndex = maybeSection.get();
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
          VirusScanner.scanOneFile(sectionWithIndex.sectionContent(), pw, useVerboseOutput);
          pw.flush();
          // We don't want to write to System.out from a bunch of threads at the same time, and the
          // threads will be processing sections in a nondeterministic order anyway, so just using a
          // lock to let them take turns isn't good enough. Instead, we'll capture the reports
          // individually and pass them all to the resultCombiner, which will put them back in order
          // and will print them all to the output PrintWriter from a single thread.
          sectionResultChute
              .put(SectionResult.of(sectionWithIndex.sectionIndex(), sw.toString()));
        }
      }
      sectionResultChute.close();
      return null;
    }
  }

  @AutoValue
  public abstract static class SectionWithIndex {
    public static SectionWithIndex of(int sectionIndex, String sectionContent) {
      return new AutoValue_ParallelVirusScannerExample_SectionWithIndex(sectionIndex,
          sectionContent);
    }
    public abstract int sectionIndex();
    public abstract String sectionContent();
  }

  @AutoValue
  public abstract static class SectionResult implements Comparable<SectionResult> {
    public static SectionResult of(int sectionIndex, String scanReport) {
      return new AutoValue_ParallelVirusScannerExample_SectionResult(sectionIndex, scanReport);
    }
    public abstract int sectionIndex();
    public abstract String scanReport();

    // Order by sectionIndex, then by scanReport.
    @Override
    public int compareTo(SectionResult other) {
      Objects.requireNonNull(other);
      int sectionIndexCmp = Integer.compare(this.sectionIndex(), other.sectionIndex());
      if (sectionIndexCmp != 0) {
        return sectionIndexCmp;
      }
      return scanReport().compareTo(other.scanReport());
    }
  }
}
