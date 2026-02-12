/*
 * ZipBombEvaluatorMain - Standalone executable entry point
 * 
 * Usage:
 *   java -jar zipbomb-evaluator-1.0.0-standalone.jar file1.xlsx file2.docx archive.zip
 *   java org.wso2.carbon.apimgt.impl.indexing.ZipBombEvaluatorMain document.xlsx
 */

package org.wso2.carbon.apimgt.impl.indexing;

import org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator;
import org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator.ZipBombEvaluationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ZipBombEvaluatorMain
 * 
 * Standalone entry point for the ZipBomb Evaluator utility.
 * Can be executed as:
 * 1. Standalone JAR: java -jar zipbomb-evaluator-1.0.0-standalone.jar [files...]
 * 2. Direct invocation: mvn exec:java -Dexec.mainClass=... -Dexec.args="file1 file2"
 * 3. Class-based: java org.wso2.carbon.apimgt.impl.indexing.ZipBombEvaluatorMain [files...]
 */
public class ZipBombEvaluatorMain {
    
    private static final String VERSION = "1.0.0";
    private static final String TOOL_NAME = "ZipBomb Evaluator";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ZipBombEvaluator evaluator;
    private int totalFiles = 0;
    private int flaggedFiles = 0;
    private int processedFiles = 0;
    private long totalSize = 0;
    
    public ZipBombEvaluatorMain() {
        this.evaluator = new ZipBombEvaluator();
    }
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        ZipBombEvaluatorMain application = new ZipBombEvaluatorMain();
        
        try {
            application.run(args);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
    
    /**
     * Main execution logic
     */
    private void run(String[] args) throws IOException {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        // Handle special flags
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printUsage();
            System.exit(0);
        }
        
        if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))) {
            System.out.println(TOOL_NAME + " v" + VERSION);
            System.exit(0);
        }
        
        // Print header
        printHeader();
        
        // Process files
        List<ZipBombEvaluationResult> results = new ArrayList<>();
        for (String filePath : args) {
            try {
                Path path = Paths.get(filePath);
                
                // Validate file exists
                if (!Files.exists(path)) {
                    System.err.println("✗ File not found: " + filePath);
                    continue;
                }
                
                if (!Files.isRegularFile(path)) {
                    System.err.println("✗ Not a regular file: " + filePath);
                    continue;
                }
                
                // Evaluate the file
                ZipBombEvaluationResult result = evaluator.evaluateFile(path);
                results.add(result);
                totalFiles++;
                totalSize += result.getFileSize();
                
                // Print individual result
                printResult(result);
                
                if (result.isZipBomb()) {
                    flaggedFiles++;
                } else {
                    processedFiles++;
                }
                
            } catch (Exception e) {
                System.err.println("✗ Error processing file '" + filePath + "': " + e.getMessage());
                totalFiles++;
            }
        }
        
        // Print summary
        printSummary();
        
        // Exit with appropriate code
        System.exit(flaggedFiles > 0 ? 1 : 0);
    }
    
    /**
     * Print usage information
     */
    private void printUsage() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║          " + TOOL_NAME + " v" + VERSION);
        System.out.println("║   Apache POI Zip Bomb Detection and Analysis Utility");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -jar zipbomb-evaluator-1.0.0-standalone.jar [OPTIONS] <file> [file2] [file3]...");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  -h, --help       Show this help message");
        System.out.println("  -v, --version    Show version information");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  java -jar zipbomb-evaluator-1.0.0-standalone.jar document.xlsx");
        System.out.println("  java -jar zipbomb-evaluator-1.0.0-standalone.jar *.xlsx *.docx");
        System.out.println("  java -jar zipbomb-evaluator-1.0.0-standalone.jar /path/to/file.pptx");
        System.out.println();
        System.out.println("SUPPORTED FORMATS:");
        System.out.println("  Microsoft Office (2007+): .xlsx, .docx, .pptx");
        System.out.println("  Microsoft Office (2003):  .xls, .doc, .ppt");
        System.out.println("  OpenDocument:              .odt, .ods, .odp");
        System.out.println("  Adobe:                     .pdf");
        System.out.println("  Archives:                  .zip, .jar, .war");
        System.out.println("  Text Formats:              .txt, .xml, .wsdl");
        System.out.println();
        System.out.println("SECURITY THRESHOLDS:");
        System.out.println("  Compression Ratio Limit:   100:1");
        System.out.println("  Max Entry Size:            1 GB");
        System.out.println("  Max Total Size:            10 GB");
        System.out.println();
        System.out.println("EXIT CODES:");
        System.out.println("  0 - All files passed security validation");
        System.out.println("  1 - One or more files flagged as security threats");
        System.out.println();
    }
    
    /**
     * Print header banner
     */
    private void printHeader() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║          " + TOOL_NAME + " v" + VERSION);
        System.out.println("║   Apache POI Zip Bomb Detection and Analysis Utility");
        System.out.println("║   Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Starting file evaluation...");
        System.out.println("---");
    }
    
    /**
     * Print individual file result
     */
    private void printResult(ZipBombEvaluationResult result) {
        if (result.isZipBomb()) {
            System.out.println();
            System.out.println("⚠️  ZIP BOMB DETECTED");
            System.out.println("  Filename:  " + result.getFileName());
            System.out.println("  Extension: " + result.getExtension());
            System.out.println("  Size:      " + formatBytes(result.getFileSize()));
            System.out.println("  Status:    " + result.getStatus());
            System.out.println("  Details:   " + result.getDetails());
            if (result.getThrownException() != null) {
                System.out.println("  Exception: " + result.getThrownException().getClass().getSimpleName());
                System.out.println("  Message:   " + result.getThrownException().getMessage());
            }
            System.out.println();
        } else {
            System.out.println("✓ " + result.getFileName() + 
                " (" + formatBytes(result.getFileSize()) + ") - PASSED");
        }
    }
    
    /**
     * Print summary statistics
     */
    private void printSummary() {
        System.out.println();
        System.out.println("---");
        System.out.println("SUMMARY REPORT");
        System.out.println("---");
        System.out.println("Total Files Scanned:    " + totalFiles);
        System.out.println("Passed Security Check:  " + processedFiles);
        System.out.println("Flagged as Threats:     " + flaggedFiles);
        System.out.println("Total Data Scanned:     " + formatBytes(totalSize));
        System.out.println("Completion Time:        " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println();
        
        if (flaggedFiles > 0) {
            System.out.println("⚠️  SECURITY ALERT: " + flaggedFiles + " file(s) failed validation");
            System.out.println();
        } else {
            System.out.println("✓ All files passed security validation");
            System.out.println();
        }
    }
    
    /**
     * Format bytes to human-readable string
     */
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }
        
        return String.format("%.2f %s", 
            bytes / Math.pow(1024, digitGroups), 
            units[digitGroups]);
    }
}
