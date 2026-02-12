/*
 * Copyright (c) 2025 - Zip Bomb Evaluator Utility
 * 
 * Utility for detecting and analyzing Apache POI zip bomb vulnerabilities
 * across multiple file formats (XLSX, DOCX, PPTX, XLS, DOC, PDF, ZIP, etc.)
 * 
 * Supports recursive inspection of nested archives (zip within zip, xlsx within zip, etc.)
 */

package org.wso2.carbon.apimgt.impl.indexing.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * ZipBombEvaluator - Detects and analyzes Apache POI zip bomb vulnerabilities
 * 
 * This utility safely evaluates files for zip bomb conditions and provides
 * detailed diagnostic information about problematic files.
 * 
 * Key features:
 * - Supports multiple file formats (XLSX, DOCX, PPTX, XLS, DOC, ZIP, PDF, etc.)
 * - RECURSIVE inspection of nested archives (zip containing xlsx, zip within zip, etc.)
 * - Detects compression ratio anomalies at every nesting level
 * - Provides detailed error diagnostics with nested path reporting
 * - Safe evaluation with resource cleanup and bounded extraction
 * - Configurable recursion depth limit (default: 10)
 * 
 * Usage:
 *   ZipBombEvaluator evaluator = new ZipBombEvaluator();
 *   ZipBombEvaluationResult result = evaluator.evaluateFile("path/to/file.zip");
 *   System.out.println(result);
 */
public class ZipBombEvaluator {
    
    private static final Log log = LogFactory.getLog(ZipBombEvaluator.class);
    
    // Configuration constants
    private static final long MAX_EXPANSION_RATIO = 100L;  // 100:1 max compression ratio
    private static final long MAX_ENTRY_SIZE = 1_073_741_824L;  // 1GB per entry
    private static final long MAX_TOTAL_SIZE = 10_737_418_240L;  // 10GB total
    private static final int MAX_RECURSION_DEPTH = 10;  // Max nesting levels
    
    // Max bytes to extract from a single entry for recursive inspection (100MB safety cap)
    private static final long MAX_EXTRACT_SIZE = 104_857_600L;
    
    // Extensions that should be recursively inspected when found inside archives
    private static final Set<String> RECURSABLE_EXTENSIONS = Set.of(
        "zip", "jar", "war", "ear",         // Archives
        "xlsx", "docx", "pptx",             // Modern Office (zip-based)
        "xls", "doc", "ppt",               // Legacy Office
        "pdf",                              // PDF
        "ods", "odt", "odp"                // OpenDocument (zip-based)
    );
    
    /**
     * Result object containing evaluation details
     */
    public static class ZipBombEvaluationResult {
        private final String fileName;
        private final boolean isZipBomb;
        private final String status;
        private final long fileSize;
        private final String extension;
        private final String details;
        private final Exception thrownException;
        
        public ZipBombEvaluationResult(String fileName, boolean isZipBomb, String status, 
                                       long fileSize, String extension, String details, 
                                       Exception thrownException) {
            this.fileName = fileName;
            this.isZipBomb = isZipBomb;
            this.status = status;
            this.fileSize = fileSize;
            this.extension = extension;
            this.details = details;
            this.thrownException = thrownException;
        }
        
        @Override
        public String toString() {
            if (isZipBomb) {
                return String.format(
                    "=== ZIP BOMB DETECTED ===%n" +
                    "Filename: %s%n" +
                    "Extension: %s%n" +
                    "File Size: %,d bytes%n" +
                    "Status: %s%n" +
                    "Details: %s%n" +
                    "Exception: %s%n" +
                    "Exception Type: %s",
                    fileName, extension, fileSize, status, details,
                    thrownException != null ? thrownException.getMessage() : "N/A",
                    thrownException != null ? thrownException.getClass().getSimpleName() : "N/A"
                );
            } else {
                return String.format(
                    "Filename: %s | Extension: %s | Size: %,d bytes | Status: All good!",
                    fileName, extension, fileSize
                );
            }
        }
        
        // Getters
        public String getFileName() { return fileName; }
        public boolean isZipBomb() { return isZipBomb; }
        public String getStatus() { return status; }
        public long getFileSize() { return fileSize; }
        public String getExtension() { return extension; }
        public String getDetails() { return details; }
        public Exception getThrownException() { return thrownException; }
    }
    
    // ==================== Public API ====================
    
    /**
     * Evaluates a file for zip bomb conditions (with recursive nested archive inspection)
     */
    public ZipBombEvaluationResult evaluateFile(String filePath) {
        return evaluateFile(Paths.get(filePath));
    }
    
    /**
     * Evaluates a file for zip bomb conditions (with recursive nested archive inspection)
     */
    public ZipBombEvaluationResult evaluateFile(Path path) {
        return evaluateFileInternal(path, 0, null);
    }
    
    // ==================== Internal Evaluation Engine ====================
    
    /**
     * Internal evaluation method that tracks recursion depth and ancestor path.
     * 
     * @param path      Path to the file to evaluate
     * @param depth     Current recursion depth (0 = top-level file)
     * @param ancestor  Display path of parent archives (e.g. "outer.zip -> inner.zip")
     */
    private ZipBombEvaluationResult evaluateFileInternal(Path path, int depth, String ancestor) {
        String fileName = path.getFileName().toString();
        String displayName = ancestor != null ? ancestor + " -> " + fileName : fileName;
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        
        if (depth > MAX_RECURSION_DEPTH) {
            log.warn("Max recursion depth reached at: " + displayName);
            return new ZipBombEvaluationResult(displayName, true, "MAX_DEPTH_EXCEEDED", 0, extension,
                "Archive nesting exceeds maximum depth of " + MAX_RECURSION_DEPTH 
                + " levels - possible recursive zip bomb", null);
        }
        
        try {
            long fileSize = Files.size(path);
            
            if (depth == 0) {
                log.info(String.format("Evaluating file: %s (size: %,d bytes)", fileName, fileSize));
            } else {
                log.info(String.format("  [depth=%d] Evaluating nested: %s (size: %,d bytes)", 
                    depth, displayName, fileSize));
            }
            
            return evaluateByExtension(path, displayName, extension, fileSize, depth);
            
        } catch (IOException e) {
            log.error("Error accessing file: " + displayName, e);
            return new ZipBombEvaluationResult(displayName, true, "IO_ERROR", 0, extension,
                "Failed to access file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Routes file evaluation based on extension
     */
    private ZipBombEvaluationResult evaluateByExtension(Path path, String displayName, 
                                                        String extension, long fileSize,
                                                        int depth) {
        return switch (extension) {
            case "xlsx" -> evaluateXLSX(path, displayName, fileSize);
            case "docx" -> evaluateDOCX(path, displayName, fileSize);
            case "pptx" -> evaluatePPTX(path, displayName, fileSize);
            case "xls"  -> evaluateXLS(path, displayName, fileSize);
            case "doc"  -> evaluateDOC(path, displayName, fileSize);
            case "ppt"  -> evaluatePPT(path, displayName, fileSize);
            case "pdf"  -> evaluatePDF(path, displayName, fileSize);
            case "zip", "jar", "war", "ear" -> evaluateZIP(path, displayName, fileSize, depth);
            default -> evaluateGenericZip(path, displayName, extension, fileSize, depth);
        };
    }
    
    // ==================== Format-Specific Evaluators ====================
    
    private ZipBombEvaluationResult evaluateXLSX(Path path, String displayName, long fileSize) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            
            ZipBombEvaluationResult zipCheck = checkZipStructure(path, displayName, fileSize);
            if (zipCheck.isZipBomb) {
                return zipCheck;
            }
            
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
                log.info("XLSX validation successful: " + displayName);
                return new ZipBombEvaluationResult(displayName, false, "VALID_XLSX", fileSize, "xlsx", 
                    "", null);
            } catch (RecordFormatException e) {
                return handleZipBombException(displayName, "xlsx", fileSize, e, 
                    "RecordFormatException during XLSX parsing");
            } catch (ZipException e) {
                return handleZipBombException(displayName, "xlsx", fileSize, e, 
                    "ZipException - possible corrupted or malicious XLSX");
            }
        } catch (Exception e) {
            return handleZipBombException(displayName, "xlsx", fileSize, e, 
                "Unexpected exception during XLSX evaluation");
        }
    }
    
    private ZipBombEvaluationResult evaluateDOCX(Path path, String displayName, long fileSize) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            
            ZipBombEvaluationResult zipCheck = checkZipStructure(path, displayName, fileSize);
            if (zipCheck.isZipBomb) {
                return zipCheck;
            }
            
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                extractor.getText();
                log.info("DOCX validation successful: " + displayName);
                return new ZipBombEvaluationResult(displayName, false, "VALID_DOCX", fileSize, "docx", 
                    "", null);
            } catch (RecordFormatException e) {
                return handleZipBombException(displayName, "docx", fileSize, e, 
                    "RecordFormatException during DOCX parsing");
            } catch (ZipException e) {
                return handleZipBombException(displayName, "docx", fileSize, e, 
                    "ZipException - possible corrupted or malicious DOCX");
            }
        } catch (Exception e) {
            return handleZipBombException(displayName, "docx", fileSize, e, 
                "Unexpected exception during DOCX evaluation");
        }
    }
    
    private ZipBombEvaluationResult evaluatePPTX(Path path, String displayName, long fileSize) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            
            ZipBombEvaluationResult zipCheck = checkZipStructure(path, displayName, fileSize);
            if (zipCheck.isZipBomb) {
                return zipCheck;
            }
            
            try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(fileBytes));
                 SlideShowExtractor extractor = new SlideShowExtractor(slideShow)) {
                extractor.getText();
                log.info("PPTX validation successful: " + displayName);
                return new ZipBombEvaluationResult(displayName, false, "VALID_PPTX", fileSize, "pptx", 
                    "", null);
            } catch (RecordFormatException e) {
                return handleZipBombException(displayName, "pptx", fileSize, e, 
                    "RecordFormatException during PPTX parsing");
            } catch (ZipException e) {
                return handleZipBombException(displayName, "pptx", fileSize, e, 
                    "ZipException - possible corrupted or malicious PPTX");
            }
        } catch (Exception e) {
            return handleZipBombException(displayName, "pptx", fileSize, e, 
                "Unexpected exception during PPTX evaluation");
        }
    }
    
    private ZipBombEvaluationResult evaluateXLS(Path path, String displayName, long fileSize) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            
            try (POIFSFileSystem fs = new POIFSFileSystem(new ByteArrayInputStream(fileBytes));
                 ExcelExtractor extractor = new ExcelExtractor(fs)) {
                extractor.getText();
                log.info("XLS validation successful: " + displayName);
                return new ZipBombEvaluationResult(displayName, false, "VALID_XLS", fileSize, "xls", 
                    "", null);
            } catch (OfficeXmlFileException e) {
                return new ZipBombEvaluationResult(displayName, true, "FORMAT_MISMATCH", fileSize, "xls",
                    "File appears to be newer Office format (use appropriate extractor)", e);
            } catch (Exception e) {
                return handleZipBombException(displayName, "xls", fileSize, e, 
                    "Exception during XLS evaluation");
            }
        } catch (Exception e) {
            return handleZipBombException(displayName, "xls", fileSize, e, 
                "Unexpected exception during XLS evaluation");
        }
    }
    
    private ZipBombEvaluationResult evaluateDOC(Path path, String displayName, long fileSize) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            
            try (POIFSFileSystem fs = new POIFSFileSystem(new ByteArrayInputStream(fileBytes))) {
                WordExtractor extractor = new WordExtractor(fs);
                extractor.getText();
                extractor.close();
                log.info("DOC validation successful: " + displayName);
                return new ZipBombEvaluationResult(displayName, false, "VALID_DOC", fileSize, "doc", 
                    "", null);
            } catch (Exception e) {
                return handleZipBombException(displayName, "doc", fileSize, e, 
                    "Exception during DOC evaluation");
            }
        } catch (Exception e) {
            return handleZipBombException(displayName, "doc", fileSize, e, 
                "Unexpected exception during DOC evaluation");
        }
    }
    
    private ZipBombEvaluationResult evaluatePPT(Path path, String displayName, long fileSize) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            
            try (HSLFSlideShow slideShow = new HSLFSlideShow(new ByteArrayInputStream(fileBytes));
                 SlideShowExtractor extractor = new SlideShowExtractor(slideShow)) {
                extractor.getText();
                log.info("PPT validation successful: " + displayName);
                return new ZipBombEvaluationResult(displayName, false, "VALID_PPT", fileSize, "ppt", 
                    "", null);
            } catch (Exception e) {
                return handleZipBombException(displayName, "ppt", fileSize, e, 
                    "Exception during PPT evaluation");
            }
        } catch (Exception e) {
            return handleZipBombException(displayName, "ppt", fileSize, e, 
                "Unexpected exception during PPT evaluation");
        }
    }
    
    private ZipBombEvaluationResult evaluatePDF(Path path, String displayName, long fileSize) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            try (PDDocument pdDocument = Loader.loadPDF(fileBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.getText(pdDocument);
                log.info("PDF validation successful: " + displayName);
                return new ZipBombEvaluationResult(displayName, false, "VALID_PDF", fileSize, "pdf", 
                    "", null);
            } catch (Exception e) {
                return handleZipBombException(displayName, "pdf", fileSize, e, 
                    "Exception during PDF evaluation");
            }
        } catch (Exception e) {
            return handleZipBombException(displayName, "pdf", fileSize, e, 
                "Unexpected exception during PDF evaluation");
        }
    }
    
    // ==================== ZIP / Archive Evaluation (with recursion) ====================
    
    /**
     * Evaluates ZIP/JAR/WAR/EAR archives for zip bomb conditions.
     * 
     * Performs TWO passes:
     *   1. Structural check - examines zip metadata for suspicious compression ratios
     *   2. Recursive content check - extracts each entry with a recognizable extension
     *      to a temp file and fully evaluates it, catching nested bombs that hide 
     *      inside innocent-looking outer archives
     */
    private ZipBombEvaluationResult evaluateZIP(Path path, String displayName, long fileSize, int depth) {
        try {
            // Pass 1: Check the outer zip's own structure (compression ratios, entry sizes)
            ZipBombEvaluationResult structureCheck = checkZipStructure(path, displayName, fileSize);
            if (structureCheck.isZipBomb) {
                return structureCheck;
            }
            
            // Pass 2: Recursively inspect every recognizable entry inside the archive
            ZipBombEvaluationResult contentCheck = evaluateZipContentsRecursively(
                path, displayName, fileSize, depth);
            if (contentCheck != null) {
                return contentCheck;  // A nested bomb was found
            }
            
            // Everything passed
            log.info("ZIP evaluation passed (including nested contents): " + displayName);
            return new ZipBombEvaluationResult(displayName, false, "VALID_ZIP", fileSize, "zip", 
                "", null);
            
        } catch (Exception e) {
            return handleZipBombException(displayName, "zip", fileSize, e, 
                "Exception during ZIP evaluation");
        }
    }
    
    /**
     * Extracts each entry from a zip file to a temp file and recursively evaluates it.
     * Only extracts entries whose extension is in RECURSABLE_EXTENSIONS.
     * 
     * @return A ZipBombEvaluationResult if a nested bomb is found, or null if all entries are safe
     */
    private ZipBombEvaluationResult evaluateZipContentsRecursively(
            Path zipPath, String displayName, long outerFileSize, int depth) {
        
        List<File> tempFiles = new ArrayList<>();
        
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            var entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                String entryExtension = FilenameUtils.getExtension(entryName).toLowerCase();
                
                // Only recursively inspect entries with known evaluable extensions
                if (!RECURSABLE_EXTENSIONS.contains(entryExtension)) {
                    continue;
                }
                
                log.info(String.format("  [depth=%d] Inspecting nested entry: %s -> %s", 
                    depth, displayName, entryName));
                
                // Safety: reject entries that declare an absurd size before we even extract
                long declaredSize = entry.getSize();
                if (declaredSize > MAX_EXTRACT_SIZE) {
                    String details = String.format(
                        "Nested entry '%s' in '%s' declares size %,d bytes " + 
                        "(exceeds extraction safety limit of %,d bytes)",
                        entryName, displayName, declaredSize, MAX_EXTRACT_SIZE);
                    return new ZipBombEvaluationResult(
                        displayName + " -> " + entryName, true, 
                        "NESTED_ENTRY_TOO_LARGE", outerFileSize, entryExtension, details, null);
                }
                
                // Extract entry to a temp file (preserving extension for format detection)
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("zbeval_", "." + entryExtension);
                    tempFiles.add(tempFile);
                    
                    try (InputStream zis = zipFile.getInputStream(entry);
                         OutputStream fos = new FileOutputStream(tempFile)) {
                        
                        // Bounded copy - abort if actual bytes exceed safety cap
                        byte[] buffer = new byte[8192];
                        long totalRead = 0;
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            totalRead += bytesRead;
                            if (totalRead > MAX_EXTRACT_SIZE) {
                                String details = String.format(
                                    "Nested entry '%s' in '%s' exceeded extraction limit " + 
                                    "during read (%,d bytes extracted before abort)",
                                    entryName, displayName, totalRead);
                                return new ZipBombEvaluationResult(
                                    displayName + " -> " + entryName, true,
                                    "NESTED_ENTRY_EXTRACTION_OVERFLOW", outerFileSize, 
                                    entryExtension, details, null);
                            }
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Recursively evaluate the extracted file at depth+1
                    ZipBombEvaluationResult nestedResult = evaluateFileInternal(
                        tempFile.toPath(), depth + 1, displayName);
                    
                    if (nestedResult.isZipBomb()) {
                        log.warn("Nested zip bomb found: " + nestedResult.getFileName());
                        return nestedResult;
                    }
                    
                } catch (IOException e) {
                    log.warn("Error extracting entry '" + entryName + "' from " + displayName, e);
                    // Continue checking other entries
                }
            }
            
        } catch (ZipException e) {
            log.debug("ZipException during recursive inspection of " + displayName, e);
        } catch (IOException e) {
            log.warn("Error during recursive inspection of " + displayName, e);
        } finally {
            // Clean up all temp files
            for (File temp : tempFiles) {
                if (temp != null && temp.exists()) {
                    temp.delete();
                }
            }
        }
        
        return null;  // No nested bombs found
    }
    
    /**
     * Generic evaluation for unknown zip-based formats
     */
    private ZipBombEvaluationResult evaluateGenericZip(Path path, String displayName, 
                                                       String extension, long fileSize,
                                                       int depth) {
        try {
            ZipBombEvaluationResult zipCheck = checkZipStructure(path, displayName, fileSize);
            if (zipCheck.isZipBomb) {
                return zipCheck;
            }
            
            // If it IS a valid zip, also check contents recursively
            if ("VALID_ZIP".equals(zipCheck.getStatus())) {
                ZipBombEvaluationResult contentCheck = evaluateZipContentsRecursively(
                    path, displayName, fileSize, depth);
                if (contentCheck != null) {
                    return contentCheck;
                }
            }
            
            log.info("File extension '" + extension + "' not specifically handled: " + displayName);
            return new ZipBombEvaluationResult(displayName, false, "UNKNOWN_FORMAT", fileSize, 
                extension, "", null);
        } catch (Exception e) {
            log.warn("Could not evaluate file format: " + displayName, e);
            return new ZipBombEvaluationResult(displayName, false, "UNKNOWN_FORMAT", fileSize, 
                extension, "", null);
        }
    }
    
    // ==================== Zip Structure Analysis ====================
    
    /**
     * Checks the raw zip structure metadata for anomalies indicating zip bombs.
     * This examines the zip's central directory for suspicious compression ratios,
     * entry sizes, and total sizes. It does NOT inspect file contents.
     */
    private ZipBombEvaluationResult checkZipStructure(Path path, String displayName, long fileSize) {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            long totalUncompressedSize = 0;
            
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                long compressedSize = entry.getCompressedSize();
                long uncompressedSize = entry.getSize();
                
                if (uncompressedSize > MAX_ENTRY_SIZE) {
                    String details = String.format(
                        "Entry '%s': uncompressed size %,d bytes exceeds limit of %,d bytes. " +
                        "Compression ratio: %s (ratio: %d:1)",
                        entry.getName(), uncompressedSize, MAX_ENTRY_SIZE,
                        compressedSize > 0 ? String.format("%.2f%%", (100.0 * compressedSize / uncompressedSize)) : "N/A",
                        compressedSize > 0 ? uncompressedSize / compressedSize : 0
                    );
                    return new ZipBombEvaluationResult(displayName, true, "ENTRY_SIZE_LIMIT_EXCEEDED", 
                        fileSize, "zip", details, null);
                }
                
                if (compressedSize > 0 && uncompressedSize > compressedSize) {
                    long ratio = uncompressedSize / compressedSize;
                    if (ratio > MAX_EXPANSION_RATIO) {
                        String details = String.format(
                            "Entry '%s': suspicious compression ratio of %d:1 (compressed: %,d bytes, " +
                            "uncompressed: %,d bytes)",
                            entry.getName(), ratio, compressedSize, uncompressedSize
                        );
                        return new ZipBombEvaluationResult(displayName, true, "EXCESSIVE_COMPRESSION_RATIO", 
                            fileSize, "zip", details, null);
                    }
                }
                
                totalUncompressedSize += uncompressedSize;
            }
            
            if (totalUncompressedSize > MAX_TOTAL_SIZE) {
                String details = String.format(
                    "Total uncompressed size %,d bytes exceeds maximum allowed %,d bytes",
                    totalUncompressedSize, MAX_TOTAL_SIZE
                );
                return new ZipBombEvaluationResult(displayName, true, "TOTAL_SIZE_LIMIT_EXCEEDED", 
                    fileSize, "zip", details, null);
            }
            
            log.info("ZIP structure validation successful: " + displayName);
            return new ZipBombEvaluationResult(displayName, false, "VALID_ZIP", fileSize, "zip", 
                "", null);
                
        } catch (ZipException e) {
            return handleZipBombException(displayName, "zip", fileSize, e, 
                "ZipException during structure validation - possibly corrupted archive");
        } catch (Exception e) {
            log.debug("File is not a valid zip archive: " + displayName);
            return new ZipBombEvaluationResult(displayName, false, "NOT_ZIP", fileSize, "unknown", 
                "", null);
        }
    }
    
    // ==================== Exception Handling ====================
    
    private ZipBombEvaluationResult handleZipBombException(String displayName, String extension, 
                                                          long fileSize, Exception exception, 
                                                          String context) {
        String exceptionClass = exception.getClass().getSimpleName();
        boolean isZipBomb = isLikelyZipBomb(exception);
        
        String details = String.format(
            "%s - Exception: %s - Message: %s",
            context, exceptionClass, exception.getMessage()
        );
        
        log.warn("Potential zip bomb detected: " + displayName + " - " + details, exception);
        
        return new ZipBombEvaluationResult(displayName, isZipBomb, 
            isZipBomb ? "POSSIBLE_ZIP_BOMB" : "PROCESSING_ERROR", fileSize, extension, 
            details, exception);
    }
    
    private boolean isLikelyZipBomb(Exception exception) {
        String exceptionClass = exception.getClass().getSimpleName();
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        
        return exceptionClass.contains("ZipException") 
            || exceptionClass.contains("RecordFormat")
            || message.contains("zip")
            || message.contains("bomb")
            || message.contains("corrupt")
            || message.contains("uncompressed")
            || message.contains("ratio");
    }
    
    // ==================== Main ====================
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java ZipBombEvaluator <file_path> [file_path2] ...");
            System.err.println("\nExample: java ZipBombEvaluator document.xlsx report.pptx archive.zip");
            System.exit(1);
        }
        
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        
        for (String filePath : args) {
            ZipBombEvaluationResult result = evaluator.evaluateFile(filePath);
            System.out.println(result);
            System.out.println("---");
        }
    }
}
