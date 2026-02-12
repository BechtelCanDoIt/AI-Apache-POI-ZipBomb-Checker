/*
 * DocumentIndexerSecurityFilter - Standalone version
 * 
 * This is the standalone-compilable version. For WSO2 integration, see
 * reference/DocumentIndexerSecurityFilter_WSO2.java which uses WSO2 Registry APIs.
 */

package org.wso2.carbon.apimgt.impl.indexing.indexer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator;
import org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator.ZipBombEvaluationResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * DocumentIndexerSecurityFilter
 * 
 * Integrates ZipBombEvaluator into a document indexing pipeline to prevent
 * zip bomb attacks and provide detailed security diagnostics.
 * 
 * For WSO2 API Manager integration (using AsyncIndexer.File2Index and Registry),
 * see the reference version in reference/DocumentIndexerSecurityFilter_WSO2.java
 */
public class DocumentIndexerSecurityFilter {
    
    private static final Log log = LogFactory.getLog(DocumentIndexerSecurityFilter.class);
    private final ZipBombEvaluator evaluator = new ZipBombEvaluator();
    
    /**
     * Validates a file path before processing
     * 
     * @param filePath Path to the file to validate
     * @return true if the document is safe to process
     * @throws SecurityException if a zip bomb is detected
     */
    public boolean validateDocument(Path filePath) throws SecurityException {
        ZipBombEvaluationResult result = evaluator.evaluateFile(filePath);
        
        if (result.isZipBomb()) {
            String errorMsg = String.format(
                "Security violation detected in document '%s': %s - %s",
                filePath, result.getStatus(), result.getDetails()
            );
            log.error(errorMsg);
            throw new SecurityException(errorMsg);
        }
        
        log.debug(String.format("Document safety check passed for: %s", filePath));
        return true;
    }
    
    /**
     * Validates a file path (string) before processing
     * 
     * @param filePath String path to the file to validate
     * @return true if the document is safe to process
     * @throws SecurityException if a zip bomb is detected
     */
    public boolean validateDocument(String filePath) throws SecurityException {
        return validateDocument(Path.of(filePath));
    }
    
    /**
     * Validates document byte array by writing to temp file and evaluating
     * 
     * @param documentBytes Raw document bytes
     * @param fileName Original filename (for extension detection and logging)
     * @return true if the document is safe to process
     * @throws SecurityException if a zip bomb is detected
     */
    public boolean validateDocumentBytes(byte[] documentBytes, String fileName) 
            throws SecurityException {
        
        File tempFile = null;
        try {
            // Preserve the original extension for proper format detection
            String suffix = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                suffix = fileName.substring(dotIndex);
            }
            
            tempFile = File.createTempFile("doc_scan_", suffix);
            
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(documentBytes);
            }
            
            ZipBombEvaluationResult result = evaluator.evaluateFile(tempFile.getAbsolutePath());
            
            if (result.isZipBomb()) {
                String errorMsg = String.format(
                    "Security violation in '%s': %s - %s",
                    fileName, result.getStatus(), result.getDetails()
                );
                log.error(errorMsg);
                throw new SecurityException(errorMsg);
            }
            
            log.debug(String.format("Document safety check passed for: %s", fileName));
            return true;
            
        } catch (IOException e) {
            log.error("Error validating document bytes: " + fileName, e);
            throw new SecurityException("Failed to validate document: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
