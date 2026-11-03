package com.opspilot.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

@Service
public class DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentTextExtractor.class);

    private static final Pattern STREAM_PATTERN = Pattern.compile("stream\\r?\\n(.*?)\\r?\\nendstream", Pattern.DOTALL);
    private static final Pattern TEXT_BLOCK_PATTERN = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|'|\")|\\[(.*?)\\]\\s*TJ", Pattern.DOTALL);
    private static final Pattern PAGE_COUNT_PATTERN = Pattern.compile("/Type\\s*/Page\\b");

    public ExtractedDocumentText extract(Path filePath) {
        long startTime = System.currentTimeMillis();
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            String rawContent = new String(fileBytes, StandardCharsets.ISO_8859_1);

            int pageCount = 0;
            Matcher pageMatcher = PAGE_COUNT_PATTERN.matcher(rawContent);
            while (pageMatcher.find()) {
                pageCount++;
            }
            if (pageCount == 0) {
                pageCount = 1;
            }

            StringBuilder extractedText = new StringBuilder();

            // 1. Look for text in uncompressed or decompressed streams
            Matcher streamMatcher = STREAM_PATTERN.matcher(rawContent);
            while (streamMatcher.find()) {
                String streamData = streamMatcher.group(1);
                byte[] streamBytes = streamData.getBytes(StandardCharsets.ISO_8859_1);

                // Try FlateDecode decompress
                String textCandidate = decompressStream(streamBytes);
                if (textCandidate == null) {
                    textCandidate = streamData;
                }

                extractTextFromStream(textCandidate, extractedText);
            }

            // 2. If nothing extracted via streams, search directly in document
            if (extractedText.length() < 10) {
                extractTextFromStream(rawContent, extractedText);
            }

            long durationMs = System.currentTimeMillis() - startTime;
            String text = extractedText.toString().trim();
            boolean success = !text.isBlank();

            log.info("Document extraction completed: bytes={}, pages={}, textLength={}, durationMs={}",
                    fileBytes.length, pageCount, text.length(), durationMs);

            return new ExtractedDocumentText(text, pageCount, success, durationMs, null);

        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("Failed to extract document text: {}", ex.getMessage(), ex);
            return new ExtractedDocumentText("", 0, false, durationMs, ex.getMessage());
        }
    }

    private void extractTextFromStream(String content, StringBuilder sb) {
        Matcher matcher = TEXT_BLOCK_PATTERN.matcher(content);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                sb.append(cleanPdfString(matcher.group(1))).append(" ");
            } else if (matcher.group(2) != null) {
                String arrayContent = matcher.group(2);
                Matcher innerMatcher = Pattern.compile("\\((.*?)\\)").matcher(arrayContent);
                while (innerMatcher.find()) {
                    sb.append(cleanPdfString(innerMatcher.group(1)));
                }
                sb.append(" ");
            }
        }
    }

    private String cleanPdfString(String str) {
        return str.replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\(", "(")
                  .replace("\\)", ")")
                  .replace("\\\\", "\\");
    }

    private String decompressStream(byte[] compressedBytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
             InflaterInputStream inflater = new InflaterInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inflater.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            return null; // Not a flate stream or corrupted
        }
    }

    public static class ExtractedDocumentText {
        private final String text;
        private final int pageCount;
        private final boolean success;
        private final long durationMs;
        private final String error;

        public ExtractedDocumentText(String text, int pageCount, boolean success, long durationMs, String error) {
            this.text = text;
            this.pageCount = pageCount;
            this.success = success;
            this.durationMs = durationMs;
            this.error = error;
        }

        public String getText() {
            return text;
        }

        public int getPageCount() {
            return pageCount;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getError() {
            return error;
        }
    }
}
