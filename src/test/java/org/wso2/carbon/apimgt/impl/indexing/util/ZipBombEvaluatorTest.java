/*
 * Test and usage examples for ZipBombEvaluator
 * 
 * PACKAGE: org.wso2.carbon.apimgt.impl.indexing.util
 * LOCATION: src/test/java/org/wso2/carbon/apimgt/impl/indexing/util/ZipBombEvaluatorTest.java
 */

package org.wso2.carbon.apimgt.impl.indexing.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ZipBombEvaluatorTest
 * 
 * JUnit tests and usage examples for the ZipBombEvaluator utility.
 */
public class ZipBombEvaluatorTest {
    
    // ==================== JUnit Tests ====================
    
    @Test
    public void testEvaluateNonExistentFile() {
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        ZipBombEvaluator.ZipBombEvaluationResult result = evaluator.evaluateFile("/nonexistent/file.xlsx");
        // Should report an IO error, flagged as potential threat (safe default)
        assertTrue("Non-existent file should be flagged", result.isZipBomb());
        assertEquals("IO_ERROR", result.getStatus());
    }

    @Test
    public void testEvaluatePlainTextFile() throws IOException {
        // Create a temporary text file - should pass validation
        Path tempFile = Files.createTempFile("test_", ".txt");
        Files.writeString(tempFile, "Hello, this is a test file.");
        
        try {
            ZipBombEvaluator evaluator = new ZipBombEvaluator();
            ZipBombEvaluator.ZipBombEvaluationResult result = evaluator.evaluateFile(tempFile);
            assertFalse("Plain text file should not be flagged as zip bomb", result.isZipBomb());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    public void testResultToStringNoException() {
        // Verify the fixed toString() doesn't throw MissingFormatArgumentException
        ZipBombEvaluator.ZipBombEvaluationResult result = 
            new ZipBombEvaluator.ZipBombEvaluationResult(
                "test.xlsx", false, "VALID_XLSX", 1024, "xlsx", "", null);
        
        String output = result.toString();
        assertNotNull("toString() should not return null", output);
        assertTrue("Output should contain filename", output.contains("test.xlsx"));
        assertTrue("Output should contain extension", output.contains("xlsx"));
    }
    
    @Test
    public void testResultToStringWithZipBomb() {
        ZipBombEvaluator.ZipBombEvaluationResult result = 
            new ZipBombEvaluator.ZipBombEvaluationResult(
                "evil.zip", true, "EXCESSIVE_COMPRESSION_RATIO", 42, "zip", 
                "Ratio 10000:1", new java.util.zip.ZipException("Bomb detected"));
        
        String output = result.toString();
        assertNotNull(output);
        assertTrue(output.contains("ZIP BOMB DETECTED"));
        assertTrue(output.contains("evil.zip"));
    }
    
    @Test
    public void testGetters() {
        Exception ex = new RuntimeException("test");
        ZipBombEvaluator.ZipBombEvaluationResult result = 
            new ZipBombEvaluator.ZipBombEvaluationResult(
                "file.docx", true, "POSSIBLE_ZIP_BOMB", 999, "docx", "details here", ex);
        
        assertEquals("file.docx", result.getFileName());
        assertTrue(result.isZipBomb());
        assertEquals("POSSIBLE_ZIP_BOMB", result.getStatus());
        assertEquals(999, result.getFileSize());
        assertEquals("docx", result.getExtension());
        assertEquals("details here", result.getDetails());
        assertSame(ex, result.getThrownException());
    }

    // ==================== Example / Demo Methods ====================
    
    /**
     * Example 1: Evaluate a single file
     */
    public static void exampleEvaluateSingleFile(String filePath) {
        System.out.println("\n=== EXAMPLE 1: SINGLE FILE EVALUATION ===\n");
        
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        ZipBombEvaluator.ZipBombEvaluationResult result = evaluator.evaluateFile(filePath);
        
        System.out.println(result);
        System.out.println();
        
        if (result.isZipBomb()) {
            System.err.printf("Warning: Zip bomb detected in file '%s'%n", result.getFileName());
            System.err.printf("Status: %s%n", result.getStatus());
            System.err.printf("Details: %s%n", result.getDetails());
        } else {
            System.out.printf("File '%s' passed security validation%n", result.getFileName());
        }
    }
    
    /**
     * Example 2: Batch evaluation of multiple files
     */
    public static void exampleBatchEvaluation(String... filePaths) {
        System.out.println("\n=== EXAMPLE 2: BATCH FILE EVALUATION ===\n");
        
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        int totalFiles = filePaths.length;
        int flaggedFiles = 0;
        
        for (String filePath : filePaths) {
            ZipBombEvaluator.ZipBombEvaluationResult result = evaluator.evaluateFile(filePath);
            
            if (result.isZipBomb()) {
                flaggedFiles++;
                System.out.printf("[FLAGGED] %s: %s%n", result.getFileName(), result.getStatus());
            } else {
                System.out.printf("[OK] %s%n", result.getFileName());
            }
        }
        
        System.out.printf("%nSummary: %d/%d files flagged for potential security issues%n", 
            flaggedFiles, totalFiles);
    }
    
    /**
     * Example 3: Detailed analysis of a file
     */
    public static void exampleDetailedAnalysis(String filePath) {
        System.out.println("\n=== EXAMPLE 3: DETAILED FILE ANALYSIS ===\n");
        
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        ZipBombEvaluator.ZipBombEvaluationResult result = evaluator.evaluateFile(filePath);
        
        System.out.printf("Filename:      %s%n", result.getFileName());
        System.out.printf("Extension:     %s%n", result.getExtension());
        System.out.printf("File Size:     %,d bytes%n", result.getFileSize());
        System.out.printf("Status:        %s%n", result.getStatus());
        System.out.printf("Is Zip Bomb:   %s%n", result.isZipBomb());
        System.out.printf("Details:       %s%n", result.getDetails());
        
        if (result.getThrownException() != null) {
            System.out.printf("Exception Type: %s%n", 
                result.getThrownException().getClass().getSimpleName());
            System.out.printf("Exception Msg:  %s%n", 
                result.getThrownException().getMessage());
        }
    }
    
    /**
     * Example 4: Integration with document processing workflow
     */
    public static void exampleDocumentProcessing(String documentPath) throws IOException {
        System.out.println("\n=== EXAMPLE 4: DOCUMENT PROCESSING WITH SECURITY CHECK ===\n");
        
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        
        ZipBombEvaluator.ZipBombEvaluationResult validation = 
            evaluator.evaluateFile(documentPath);
        
        if (validation.isZipBomb()) {
            System.err.printf("SECURITY ALERT: Document '%s' failed security validation%n", 
                validation.getFileName());
            System.err.printf("Reason: %s%n", validation.getDetails());
            System.err.println("Document will not be processed");
            return;
        }
        
        System.out.printf("Document '%s' passed security validation%n", 
            validation.getFileName());
        System.out.println("Proceeding with document processing...");
        
        String extension = validation.getExtension();
        System.out.printf("Processing %s document (size: %,d bytes)%n", 
            extension.toUpperCase(), validation.getFileSize());
        System.out.println("Document processed successfully");
    }
    
    /**
     * Example 5: Programmatic utility method for filtering
     */
    public static boolean isDocumentSafe(String filePath) {
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        ZipBombEvaluator.ZipBombEvaluationResult result = evaluator.evaluateFile(filePath);
        return !result.isZipBomb();
    }
    
    /**
     * Example 6: Generate security report for a directory
     */
    public static void exampleSecurityReport(String directoryPath) throws IOException {
        System.out.println("\n=== EXAMPLE 6: SECURITY REPORT FOR DIRECTORY ===\n");
        
        Path dir = Paths.get(directoryPath);
        
        System.out.printf("Scanning: %s%n", directoryPath);
        System.out.println("---");
        
        if (!Files.isDirectory(dir)) {
            System.err.println("Error: Not a directory: " + directoryPath);
            return;
        }
        
        ZipBombEvaluator evaluator = new ZipBombEvaluator();
        
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                  .sorted()
                  .forEach(path -> {
                      ZipBombEvaluator.ZipBombEvaluationResult result = 
                          evaluator.evaluateFile(path.toString());
                      
                      String status = result.isZipBomb() ? "FAILED" : "PASSED";
                      System.out.printf("%s | %-30s | %-30s | %,10d bytes%n",
                          status,
                          result.getFileName(),
                          result.getStatus(),
                          result.getFileSize()
                      );
                      
                      if (result.isZipBomb()) {
                          System.out.printf("   Details: %s%n", result.getDetails());
                      }
                  });
        }
    }
    
    /**
     * Example 7: Format comparison info
     */
    public static void exampleFormatComparison() {
        System.out.println("\n=== SUPPORTED FORMATS ===\n");
        
        String[][] formats = {
            {"XLSX", "Microsoft Excel 2007+", "Modern format, zip-based"},
            {"DOCX", "Microsoft Word 2007+", "Modern format, zip-based"},
            {"PPTX", "Microsoft PowerPoint 2007+", "Modern format, zip-based"},
            {"XLS", "Microsoft Excel 2003", "Legacy format, OLE-based"},
            {"DOC", "Microsoft Word 2003", "Legacy format, OLE-based"},
            {"PPT", "Microsoft PowerPoint 2003", "Legacy format, OLE-based"},
            {"PDF", "Adobe Portable Document", "Binary format"},
            {"ZIP", "Compressed Archive", "Standard zip format"},
            {"JAR", "Java Archive", "Zip-based archive"},
            {"WAR", "Web Archive", "Zip-based archive"},
        };
        
        for (String[] format : formats) {
            System.out.printf("%-6s | %-30s | %s%n", format[0], format[1], format[2]);
        }
    }
    
    // ==================== Interactive Main ====================
    
    public static void main(String[] args) {
        System.out.println("\nZipBombEvaluator Test Suite - Usage Examples\n");
        
        if (args.length == 0) {
            printUsageExamples();
            return;
        }
        
        String command = args[0];
        
        try {
            switch (command) {
                case "single" -> {
                    if (args.length > 1) exampleEvaluateSingleFile(args[1]);
                    else System.err.println("Usage: single <file_path>");
                }
                case "batch" -> {
                    if (args.length > 1) {
                        String[] files = new String[args.length - 1];
                        System.arraycopy(args, 1, files, 0, files.length);
                        exampleBatchEvaluation(files);
                    } else System.err.println("Usage: batch <file1> [file2]...");
                }
                case "detailed" -> {
                    if (args.length > 1) exampleDetailedAnalysis(args[1]);
                    else System.err.println("Usage: detailed <file_path>");
                }
                case "process" -> {
                    if (args.length > 1) exampleDocumentProcessing(args[1]);
                    else System.err.println("Usage: process <file_path>");
                }
                case "report" -> {
                    if (args.length > 1) exampleSecurityReport(args[1]);
                    else System.err.println("Usage: report <directory_path>");
                }
                case "formats" -> exampleFormatComparison();
                case "help", "-h", "--help" -> printUsageExamples();
                default -> exampleEvaluateSingleFile(command);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printUsageExamples() {
        System.out.println("COMMANDS:");
        System.out.println("  single <file>              Evaluate a single file");
        System.out.println("  batch <file1> [file2]...   Evaluate multiple files");
        System.out.println("  detailed <file>            Show detailed analysis");
        System.out.println("  process <file>             Test document processing flow");
        System.out.println("  report <directory>         Scan entire directory");
        System.out.println("  formats                    Show supported formats");
        System.out.println();
    }
}
