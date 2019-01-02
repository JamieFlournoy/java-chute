package com.pervasivecode.utils.concurrent.chute.example;

import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import com.google.common.base.Splitter;
import com.google.common.io.ByteSource;

public class Archive {

  private static final String CR_LF = "\r\n";
  private static final String SECTION_SEPARATOR = CR_LF + CR_LF + CR_LF + "CHAPTER ";

  private static final Pattern SECTION_SEPARATOR_PATTERN =
      Pattern.compile("\r\n\r\n\r\n(?=CHAPTER )");

  private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile("^CHAPTER [^\r\n]*");

  public static String sectionSeparator() {
    return SECTION_SEPARATOR;
  }

  private static final String ARCHIVE_RESOURCE_PATH =
      "com/pervasivecode/utils/concurrent/chute/example/moby_dick.txt.gz";

  public static String getArchiveAsString() throws IOException {
    return new String(getArchiveBytesUncompressed(), UTF_8);
  }

  private static byte[] getArchiveBytesUncompressed() throws IOException {
    ByteSource archiveByteSource = asByteSource(getResource(ARCHIVE_RESOURCE_PATH));
    try (InputStream bufferedInputStream = archiveByteSource.openBufferedStream();
        GZIPInputStream uncompressed = new GZIPInputStream(bufferedInputStream)) {
      return uncompressed.readAllBytes();
    }
  }

  public static String getSectionFilename(String section) {
    Matcher sectionHeaderMatcher = SECTION_HEADER_PATTERN.matcher(section);
    final String sectionHeader;
    if (sectionHeaderMatcher.find()) {
      sectionHeader = sectionHeaderMatcher.group().toLowerCase(Locale.US)
          .replaceAll("[^a-z0-9]+", " ").trim().replace(' ', '_');
    } else {
      sectionHeader = "chapter_0";
    }
    return sectionHeader.concat(".txt");
  }

  static Iterable<String> getSections(String archiveContents) {
    Iterable<String> sections = Splitter.on(SECTION_SEPARATOR_PATTERN).split(archiveContents);
    return sections;
  }
}
