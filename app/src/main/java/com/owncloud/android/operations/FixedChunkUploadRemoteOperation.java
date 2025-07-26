/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.network.ProgressiveDataTransfer;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;

import java.io.InputStream;
import java.util.UUID;

// Import for WebDAV operations - Nextcloud chunk assembly
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fixed chunk size upload operation that uses 1 MB chunks for large files (>2MB).
 * This ensures multipart upload behavior and better reliability for large files.
 * Small files continue to use normal upload for optimal performance.
 */

/**
 * Custom MOVE method for WebDAV operations (since it's not available in commons-httpclient)
 */
class MoveMethod extends EntityEnclosingMethod {
    public MoveMethod(String uri) {
        super(uri);
    }
    
    @Override
    public String getName() {
        return "MOVE";
    }
}
public class FixedChunkUploadRemoteOperation extends RemoteOperation implements ProgressiveDataTransfer {
    
    private static final String TAG = FixedChunkUploadRemoteOperation.class.getSimpleName();
    
    // Fixed 1 MB chunk size (1024 * 1024 bytes)
    public static final long FIXED_CHUNK_SIZE = 1024 * 1024;
    
    private final String mLocalPath;
    private final String mRemotePath;
    private final String mMimeType;
    private final String mEtagInConflict;
    private final long mLastModificationTimestamp;
    private final Long mCreationTimestamp;
    private final String mToken;
    private final boolean mDisableRetries;
    private final Context mContext;
    
    private final Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<>();
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    
    private long mFileSize;
    private long mTransferredBytes;
    private long mTotalChunks;
    private long mCurrentChunk;
    private String mUploadSessionId;
    private long mLastProgressUpdate = 0;
    
    /**
     * Constructor for fixed chunk upload operation
     */
    public FixedChunkUploadRemoteOperation(String localPath,
                                         String remotePath,
                                         String mimeType,
                                         String etagInConflict,
                                         long lastModificationTimestamp,
                                         Long creationTimestamp,
                                         String token,
                                         boolean disableRetries,
                                         Context context) {
        mLocalPath = localPath;
        mRemotePath = remotePath;
        mMimeType = mimeType;
        mEtagInConflict = etagInConflict;
        mLastModificationTimestamp = lastModificationTimestamp;
        mCreationTimestamp = creationTimestamp;
        mToken = token;
        mDisableRetries = disableRetries;
        mContext = context;
        mFileSize = 0;
        mTransferredBytes = 0;
        mTotalChunks = 0;
        mCurrentChunk = 0;
        // Generate deterministic session ID based on file path and modification time
        // This ensures the same session ID is used if upload is restarted (background execution)
        mUploadSessionId = generateDeterministicSessionId();
    }
    
    /**
     * Constructor without token
     */
    public FixedChunkUploadRemoteOperation(String localPath,
                                         String remotePath,
                                         String mimeType,
                                         String etagInConflict,
                                         long lastModificationTimestamp,
                                         Long creationTimestamp,
                                         boolean disableRetries,
                                         Context context) {
        this(localPath, remotePath, mimeType, etagInConflict, lastModificationTimestamp, creationTimestamp, null, disableRetries, context);
    }
    
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation.run() - ENTRY");
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Local path = " + mLocalPath);
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Remote path = " + mRemotePath);
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: MIME type = " + mMimeType);
        
        mCancellationRequested.set(false);
        
        // Early cancellation check
        if (mCancellationRequested.get()) {
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Upload cancelled at start");
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.CANCELLED);
        }
        
        try {
            File localFile = new File(mLocalPath);
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Checking if local file exists: " + localFile.getAbsolutePath());
            
            if (!localFile.exists()) {
                Log_OC.e(TAG, "FixedChunkUploadRemoteOperation: Local file does not exist!");
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND);
            }
            
            mFileSize = localFile.length();
            
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Processing file " + localFile.getName() + 
                     " with size " + mFileSize + " bytes");
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: FIXED_CHUNK_SIZE = " + FIXED_CHUNK_SIZE + " bytes");
            
            // This operation is only used for large files (>2MB) to provide multipart upload
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Using chunked upload for large file");
            
            // Show toast to indicate chunked upload is being used
            
            RemoteOperationResult result = uploadFileInChunks(client, localFile);
            
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation.run() - EXIT with result: " + 
                     (result.isSuccess() ? "SUCCESS" : "FAILURE - " + result.getLogMessage()));
            return result;
            
        } catch (Exception e) {
            Log_OC.e(TAG, "FixedChunkUploadRemoteOperation.run() - Exception occurred", e);
            return new RemoteOperationResult(e);
        }
    }
    
    /**
     * Upload file directly for small files
     */
    private RemoteOperationResult uploadFileDirectly(OwnCloudClient client, File localFile) {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation.uploadFileDirectly() - ENTRY");
        Log_OC.d(TAG, "uploadFileDirectly: File size = " + localFile.length() + " bytes");
        
        try {
            // Use the existing UploadFileRemoteOperation for small files
            Log_OC.d(TAG, "uploadFileDirectly: Creating UploadFileRemoteOperation");
            com.owncloud.android.lib.resources.files.UploadFileRemoteOperation operation = 
                new com.owncloud.android.lib.resources.files.UploadFileRemoteOperation(
                    mLocalPath,
                    mRemotePath,
                    mMimeType,
                    mEtagInConflict,
                    mLastModificationTimestamp,
                    mCreationTimestamp,
                    mToken,
                    mDisableRetries
                );
            
            // Forward progress listeners
            Log_OC.d(TAG, "uploadFileDirectly: Adding " + mDataTransferListeners.size() + " progress listeners");
            for (OnDatatransferProgressListener listener : mDataTransferListeners) {
                operation.addDataTransferProgressListener(listener);
            }
            
            Log_OC.d(TAG, "uploadFileDirectly: Executing direct upload operation");
            
            
            RemoteOperationResult result = operation.execute(client);
            
            Log_OC.d(TAG, "uploadFileDirectly: Direct upload " + 
                     (result.isSuccess() ? "completed successfully" : "failed: " + result.getLogMessage()));
            return result;
            
        } catch (Exception e) {
            Log_OC.e(TAG, "uploadFileDirectly: Exception occurred", e);
            return new RemoteOperationResult(e);
        }
    }
    
    /**
     * Upload file using Nextcloud v2 chunked upload protocol
     */
    private RemoteOperationResult uploadFileInChunks(OwnCloudClient client, File localFile) {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation.uploadFileInChunks() - ENTRY");
        
        try {
            Log_OC.d(TAG, "uploadFileInChunks: Starting Nextcloud v2 chunked upload for file: " + localFile.getName() + 
                     ", size: " + mFileSize + " bytes, using " + FIXED_CHUNK_SIZE + " byte chunks");
            
            mTotalChunks = (mFileSize + FIXED_CHUNK_SIZE - 1) / FIXED_CHUNK_SIZE; // Ceiling division
            mCurrentChunk = 0;
            
            Log_OC.d(TAG, "uploadFileInChunks: File will be uploaded in " + mTotalChunks + " chunks using session: " + mUploadSessionId);
            
            
            // Step 1: Check if we can resume an existing session (for background upload continuation)
            int startChunk = checkExistingSession(client);
            
            if (startChunk > 0) {
                // Resume from existing session
                Log_OC.d(TAG, "uploadFileInChunks: Resuming upload from chunk " + (startChunk + 1) + "/" + mTotalChunks);
                mCurrentChunk = startChunk + 1; // Next chunk to upload
                long bytesAlreadyUploaded = startChunk * FIXED_CHUNK_SIZE;
                updateProgress(bytesAlreadyUploaded, localFile.getName());
            } else {
                // Create new upload session directory
                RemoteOperationResult sessionResult = createUploadSession(client);
                if (!sessionResult.isSuccess()) {
                    Log_OC.e(TAG, "uploadFileInChunks: Failed to create upload session");
                    return sessionResult;
                }
                
                Log_OC.d(TAG, "uploadFileInChunks: Upload session created successfully");
                
                // Report initial progress (0%)
                updateProgress(0, localFile.getName());
            }
            
            // Step 2: Upload each chunk to the session directory
            long totalBytesUploaded = (startChunk > 0) ? startChunk * FIXED_CHUNK_SIZE : 0;
            int startChunkIndex = (startChunk > 0) ? startChunk : 0;
            
            try (RandomAccessFile fileAccess = new RandomAccessFile(localFile, "r")) {
                for (int chunkIndex = startChunkIndex; chunkIndex < mTotalChunks; chunkIndex++) {
                    if (mCancellationRequested.get()) {
                        Log_OC.d(TAG, "uploadFileInChunks: Upload cancelled at chunk " + (chunkIndex + 1));
                        return new RemoteOperationResult(RemoteOperationResult.ResultCode.CANCELLED);
                    }
                    
                    mCurrentChunk = chunkIndex + 1;
                    long chunkStart = chunkIndex * FIXED_CHUNK_SIZE;
                    long chunkEnd = Math.min(chunkStart + FIXED_CHUNK_SIZE, mFileSize);
                    long chunkSize = chunkEnd - chunkStart;
                    
                    Log_OC.d(TAG, "uploadFileInChunks: Uploading chunk " + mCurrentChunk + "/" + mTotalChunks + 
                             " (bytes " + chunkStart + "-" + (chunkEnd - 1) + ", size: " + chunkSize + ")");
                    
                    
                    // Read chunk data
                    byte[] chunkData = new byte[(int) chunkSize];
                    fileAccess.seek(chunkStart);
                    int bytesRead = fileAccess.read(chunkData);
                    
                    if (bytesRead != chunkSize) {
                        Log_OC.e(TAG, "uploadFileInChunks: Failed to read expected chunk size. Expected: " + 
                                 chunkSize + ", Read: " + bytesRead);
                        return new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND);
                    }
                    
                    // Upload this specific chunk to the session directory
                    RemoteOperationResult chunkResult = uploadChunkToSession(client, chunkData, (int) mCurrentChunk, localFile.getName());
                    
                    if (!chunkResult.isSuccess()) {
                        Log_OC.e(TAG, "uploadFileInChunks: Chunk " + mCurrentChunk + " failed: " + chunkResult.getLogMessage());
                        return chunkResult;
                    }
                    
                    totalBytesUploaded += chunkSize;
                    
                    // Report progress ONLY after chunk completion, respecting throttling
                    updateProgress(totalBytesUploaded, localFile.getName());
                    
                    Log_OC.d(TAG, "uploadFileInChunks: Chunk " + mCurrentChunk + " uploaded successfully");
                }
            }
            
            Log_OC.d(TAG, "uploadFileInChunks: ALL CHUNKS uploaded successfully! Total bytes: " + totalBytesUploaded);
            
            // Report 95% progress before assembly
            updateProgress((long) (mFileSize * 0.95), localFile.getName());
            
            // Step 3: Assemble chunks into final file
            RemoteOperationResult assemblyResult = assembleChunks(client);
            if (!assemblyResult.isSuccess()) {
                Log_OC.e(TAG, "uploadFileInChunks: Failed to assemble chunks: " + assemblyResult.getLogMessage());
                return assemblyResult;
            }
            
            Log_OC.d(TAG, "uploadFileInChunks: Chunks assembled successfully!");
            
            // Report final 100% completion
            updateProgress(mFileSize, localFile.getName());
            
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
            
        } catch (Exception e) {
            Log_OC.e(TAG, "uploadFileInChunks: Exception occurred", e);
            return new RemoteOperationResult(e);
        }
    }
    
    /**
     * Create upload session directory for Nextcloud v2 chunked upload
     */
    private RemoteOperationResult createUploadSession(OwnCloudClient client) {
        // Check for cancellation before creating session
        if (mCancellationRequested.get()) {
            Log_OC.d(TAG, "createUploadSession: Upload cancelled before creating session");
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.CANCELLED);
        }
        
        try {
            Log_OC.d(TAG, "createUploadSession: Creating session directory: " + mUploadSessionId);
            
            // Construct session directory URL
            String sessionUrl;
            try {
                String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
                String encodedSessionId = URLEncoder.encode(mUploadSessionId, StandardCharsets.UTF_8.toString());
                sessionUrl = client.getBaseUri() + "/remote.php/dav/uploads/" + encodedUsername + "/" + encodedSessionId;
            } catch (Exception e) {
                Log_OC.e(TAG, "createUploadSession: Error encoding URL components", e);
                return new RemoteOperationResult(e);
            }
            
            Log_OC.d(TAG, "createUploadSession: Session URL: " + sessionUrl);
            
            // Create MKCOL request to create directory
            MkColMethod mkColMethod = new MkColMethod(sessionUrl);
            
            // Add required headers for Nextcloud v2 chunked upload
            String destinationUrl;
            try {
                String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
                String encodedRemotePath = URLEncoder.encode(mRemotePath, StandardCharsets.UTF_8.toString());
                destinationUrl = client.getBaseUri() + "/remote.php/dav/files/" + encodedUsername + encodedRemotePath;
            } catch (Exception e) {
                Log_OC.e(TAG, "createUploadSession: Error encoding destination URL", e);
                return new RemoteOperationResult(e);
            }
            
            mkColMethod.addRequestHeader("Destination", destinationUrl);
            mkColMethod.addRequestHeader("OC-Total-Length", String.valueOf(mFileSize));
            
            if (mToken != null && !mToken.isEmpty()) {
                mkColMethod.addRequestHeader("Authorization", "Bearer " + mToken);
            }
            
            // Execute the request
            int status = client.executeMethod(mkColMethod);
            
            Log_OC.d(TAG, "createUploadSession: HTTP response status: " + status);
            
            if (status == HttpStatus.SC_CREATED || status == HttpStatus.SC_OK) {
                Log_OC.d(TAG, "createUploadSession: Session directory created successfully");
                return new RemoteOperationResult(true, mkColMethod);
            } else {
                Log_OC.e(TAG, "createUploadSession: Failed to create session directory, status: " + status);
                return new RemoteOperationResult(false, mkColMethod);
            }
            
        } catch (Exception e) {
            Log_OC.e(TAG, "createUploadSession: Exception creating session", e);
            return new RemoteOperationResult(e);
        }
    }
    
    /**
     * Upload a chunk to the session directory (Nextcloud v2 protocol)
     */
    private RemoteOperationResult uploadChunkToSession(OwnCloudClient client, byte[] chunkData, int chunkNumber, String fileName) {
        // Check for cancellation before starting chunk upload
        if (mCancellationRequested.get()) {
            Log_OC.d(TAG, "uploadChunkToSession: Upload cancelled before chunk " + chunkNumber);
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.CANCELLED);
        }
        
        try {
            Log_OC.d(TAG, "uploadChunkToSession: Uploading chunk " + chunkNumber + " with " + chunkData.length + " bytes");
            
            // Construct chunk URL - chunks are named as numbers (1, 2, 3...)
            String chunkUrl;
            try {
                String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
                String encodedSessionId = URLEncoder.encode(mUploadSessionId, StandardCharsets.UTF_8.toString());
                String chunkFileName = String.format("%05d", chunkNumber); // 5-digit padded number
                chunkUrl = client.getBaseUri() + "/remote.php/dav/uploads/" + encodedUsername + "/" + encodedSessionId + "/" + chunkFileName;
            } catch (Exception e) {
                Log_OC.e(TAG, "uploadChunkToSession: Error encoding URL components", e);
                return new RemoteOperationResult(e);
            }
            
            Log_OC.d(TAG, "uploadChunkToSession: Chunk URL: " + chunkUrl);
            
            // Create PUT request for this chunk
            PutMethod putMethod = new PutMethod(chunkUrl);
            
            // Set headers required for Nextcloud v2 chunked upload
            putMethod.addRequestHeader("Content-Type", "application/octet-stream");
            putMethod.addRequestHeader("Content-Length", String.valueOf(chunkData.length));
            
            // Add destination header (required for v2)
            String destinationUrl;
            try {
                String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
                String encodedRemotePath = URLEncoder.encode(mRemotePath, StandardCharsets.UTF_8.toString());
                destinationUrl = client.getBaseUri() + "/remote.php/dav/files/" + encodedUsername + encodedRemotePath;
            } catch (Exception e) {
                Log_OC.e(TAG, "uploadChunkToSession: Error encoding destination URL", e);
                return new RemoteOperationResult(e);
            }
            
            putMethod.addRequestHeader("Destination", destinationUrl);
            putMethod.addRequestHeader("OC-Total-Length", String.valueOf(mFileSize));
            
            if (mToken != null && !mToken.isEmpty()) {
                putMethod.addRequestHeader("Authorization", "Bearer " + mToken);
            }
            
            // Create request entity with chunk data
            putMethod.setRequestEntity(new ByteArrayRequestEntity(chunkData));
            
            // Final cancellation check before executing HTTP request
            if (mCancellationRequested.get()) {
                Log_OC.d(TAG, "uploadChunkToSession: Upload cancelled before HTTP request for chunk " + chunkNumber);
                putMethod.releaseConnection();
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.CANCELLED);
            }
            
            // Execute the request
            int status = client.executeMethod(putMethod);
            
            // Check for cancellation immediately after HTTP request
            if (mCancellationRequested.get()) {
                Log_OC.d(TAG, "uploadChunkToSession: Upload cancelled after HTTP request for chunk " + chunkNumber);
                putMethod.releaseConnection();
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.CANCELLED);
            }
            
            Log_OC.d(TAG, "uploadChunkToSession: HTTP response status: " + status);
            
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT) {
                Log_OC.d(TAG, "uploadChunkToSession: Chunk " + chunkNumber + " uploaded successfully");
                return new RemoteOperationResult(true, putMethod);
            } else {
                Log_OC.e(TAG, "uploadChunkToSession: Chunk upload failed with status: " + status);
                return new RemoteOperationResult(false, putMethod);
            }
            
        } catch (Exception e) {
            Log_OC.e(TAG, "uploadChunkToSession: Exception uploading chunk " + chunkNumber, e);
            return new RemoteOperationResult(e);
        }
    }
    
    /**
     * Assemble uploaded chunks into final file using MOVE request
     */
    private RemoteOperationResult assembleChunks(OwnCloudClient client) {
        // Check for cancellation before assembling chunks
        if (mCancellationRequested.get()) {
            Log_OC.d(TAG, "assembleChunks: Upload cancelled before assembling chunks");
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.CANCELLED);
        }
        
        try {
            Log_OC.d(TAG, "assembleChunks: Assembling chunks for session: " + mUploadSessionId);
            
            // Construct assembly URL (MOVE from .file in session directory to final destination)
            String assemblySourceUrl;
            try {
                String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
                String encodedSessionId = URLEncoder.encode(mUploadSessionId, StandardCharsets.UTF_8.toString());
                assemblySourceUrl = client.getBaseUri() + "/remote.php/dav/uploads/" + encodedUsername + "/" + encodedSessionId + "/.file";
            } catch (Exception e) {
                Log_OC.e(TAG, "assembleChunks: Error encoding source URL", e);
                return new RemoteOperationResult(e);
            }
            
            Log_OC.d(TAG, "assembleChunks: Assembly source URL: " + assemblySourceUrl);
            
            // Create MOVE request
            MoveMethod moveMethod = new MoveMethod(assemblySourceUrl);
            
            // Set destination header (final file location)
            String destinationUrl;
            try {
                String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
                String encodedRemotePath = URLEncoder.encode(mRemotePath, StandardCharsets.UTF_8.toString());
                destinationUrl = client.getBaseUri() + "/remote.php/dav/files/" + encodedUsername + encodedRemotePath;
            } catch (Exception e) {
                Log_OC.e(TAG, "assembleChunks: Error encoding destination URL", e);
                return new RemoteOperationResult(e);
            }
            
            moveMethod.addRequestHeader("Destination", destinationUrl);
            moveMethod.addRequestHeader("OC-Total-Length", String.valueOf(mFileSize));
            
            // Set modification time if available
            if (mLastModificationTimestamp > 0) {
                moveMethod.addRequestHeader("X-OC-Mtime", String.valueOf(mLastModificationTimestamp));
            }
            
            if (mToken != null && !mToken.isEmpty()) {
                moveMethod.addRequestHeader("Authorization", "Bearer " + mToken);
            }
            
            Log_OC.d(TAG, "assembleChunks: Destination URL: " + destinationUrl);
            
            // Execute the MOVE request
            int status = client.executeMethod(moveMethod);
            
            Log_OC.d(TAG, "assembleChunks: HTTP response status: " + status);
            
            if (status == HttpStatus.SC_CREATED || status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT) {
                Log_OC.d(TAG, "assembleChunks: Chunks assembled successfully into final file");
                return new RemoteOperationResult(true, moveMethod);
            } else {
                Log_OC.e(TAG, "assembleChunks: Assembly failed with status: " + status);
                return new RemoteOperationResult(false, moveMethod);
            }
            
        } catch (Exception e) {
            Log_OC.e(TAG, "assembleChunks: Exception during assembly", e);
            return new RemoteOperationResult(e);
        }
    }
    
    /**
     * Custom request entity for byte array data
     */
    private static class ByteArrayRequestEntity implements RequestEntity {
        private final byte[] data;
        
        public ByteArrayRequestEntity(byte[] data) {
            this.data = data;
        }
        
        @Override
        public boolean isRepeatable() {
            return true;
        }
        
        @Override
        public void writeRequest(java.io.OutputStream out) throws IOException {
            out.write(data);
        }
        
        @Override
        public long getContentLength() {
            return data.length;
        }
        
        @Override
        public String getContentType() {
            return "application/octet-stream";
        }
    }
    
    @Override
    public void addDataTransferProgressListener(OnDatatransferProgressListener listener) {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Adding progress listener");
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Total progress listeners: " + mDataTransferListeners.size());
        }
    }
    
    @Override
    public void addDataTransferProgressListeners(Collection<OnDatatransferProgressListener> listeners) {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Adding " + listeners.size() + " progress listeners");
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.addAll(listeners);
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Total progress listeners: " + mDataTransferListeners.size());
        }
    }
    
    @Override
    public void removeDataTransferProgressListener(OnDatatransferProgressListener listener) {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Removing progress listener");
        synchronized (mDataTransferListeners) {
            boolean removed = mDataTransferListeners.remove(listener);
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Listener removed: " + removed + ", Total remaining: " + mDataTransferListeners.size());
        }
    }
    
    public void cancel() {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Cancel requested (no reason)");
        mCancellationRequested.set(true);
        
        // Interrupt the current thread to make HTTP operations more responsive
        Thread.currentThread().interrupt();
    }
    
    public void cancel(RemoteOperationResult.ResultCode cancellationReason) {
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Cancel requested with reason: " + cancellationReason);
        mCancellationRequested.set(true);
        
        // Interrupt the current thread to make HTTP operations more responsive  
        Thread.currentThread().interrupt();
    }
    
    public boolean isCancelled() {
        boolean cancelled = mCancellationRequested.get();
        Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: Checking cancellation status: " + cancelled);
        return cancelled;
    }
    
    /**
     * Check if an existing upload session can be resumed.
     * This enables background uploads to continue even if the app was closed.
     * 
     * @param client OwnCloud client
     * @return The number of chunks already uploaded (0 if no resumable session found)
     */
    private int checkExistingSession(OwnCloudClient client) {
        try {
            String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
            String encodedSessionId = URLEncoder.encode(mUploadSessionId, StandardCharsets.UTF_8.toString());
            String sessionUrl = client.getBaseUri() + "/remote.php/dav/uploads/" + encodedUsername + "/" + encodedSessionId;
            
            Log_OC.d(TAG, "checkExistingSession: Checking for existing session: " + sessionUrl);
            
            // Use PROPFIND to check if session directory exists and list chunks
            // For simplicity, we'll check for consecutive chunk files starting from 00001
            int chunkCount = 0;
            for (int i = 1; i <= mTotalChunks; i++) {
                String chunkFileName = String.format("%05d", i);
                String chunkUrl = sessionUrl + "/" + chunkFileName;
                
                // Use a simple HEAD request to check if chunk exists
                PutMethod headMethod = new PutMethod(chunkUrl) {
                    @Override
                    public String getName() { return "HEAD"; }
                };
                
                try {
                    int statusCode = client.executeMethod(headMethod);
                    if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NO_CONTENT) {
                        chunkCount = i; // Found this chunk
                        Log_OC.d(TAG, "checkExistingSession: Found existing chunk " + i);
                    } else {
                        // Missing chunk - stop here
                        Log_OC.d(TAG, "checkExistingSession: Chunk " + i + " missing (status: " + statusCode + "), can resume from chunk " + chunkCount);
                        break;
                    }
                } catch (Exception e) {
                    Log_OC.d(TAG, "checkExistingSession: Error checking chunk " + i + ", stopping resume check", e);
                    break;
                } finally {
                    headMethod.releaseConnection();
                }
            }
            
            Log_OC.d(TAG, "checkExistingSession: Found " + chunkCount + " existing chunks out of " + mTotalChunks);
            return chunkCount;
            
        } catch (Exception e) {
            Log_OC.e(TAG, "checkExistingSession: Error checking existing session", e);
            return 0; // Start fresh
        }
    }

    /**
     * Generate a deterministic session ID based on file characteristics.
     * This ensures the same session ID is used if the upload is restarted,
     * enabling proper background upload continuation even if the app is closed.
     */
    private String generateDeterministicSessionId() {
        try {
            // Use file path and size ONLY to create a truly deterministic ID
            // This ensures the same file always gets the same session ID for resumption
            File file = new File(mLocalPath);
            
            // Get canonical path to handle different path representations
            String canonicalPath = file.getCanonicalPath();
            long fileSize = file.length();
            
            // Create deterministic base string using path and size only
            String baseString = canonicalPath + "_" + fileSize;
            
            // Create a hash to make it shorter and more session-like
            int hash = baseString.hashCode();
            String sessionId = "upload_" + Math.abs(hash);
            
            Log_OC.d(TAG, "generateDeterministicSessionId: Generated session ID: " + sessionId + 
                     " for file: " + canonicalPath + " (size: " + fileSize + ")");
            Log_OC.d(TAG, "generateDeterministicSessionId: Base string: " + baseString);
            return sessionId;
        } catch (Exception e) {
            Log_OC.e(TAG, "generateDeterministicSessionId: Error generating deterministic session ID, falling back to random", e);
            return UUID.randomUUID().toString();
        }
    }


    
    /**
     * Update progress respecting FileUploadWorker throttling (750ms minimum interval)
     */
    private void updateProgress(long bytesUploaded, String fileName) {
        long currentTime = System.currentTimeMillis();
        
        // Respect 750ms minimum update interval (same as FileUploadWorker)
        if (currentTime - mLastProgressUpdate >= 750) {
            // Calculate percentage - same logic as FileUploadWorker's getPercent()
            int currentPercent = (int) ((100.0 * bytesUploaded) / mFileSize);
            
            Log_OC.d(TAG, "updateProgress: " + bytesUploaded + "/" + mFileSize + " bytes (" + currentPercent + "%)");
            
            // Report progress to all listeners (including FileUploadWorker)
            for (OnDatatransferProgressListener listener : mDataTransferListeners) {
                listener.onTransferProgress(
                    bytesUploaded, // progressRate (bytes uploaded this update)
                    bytesUploaded, // totalTransferredSoFar
                    mFileSize,     // totalToTransfer
                    fileName
                );
            }
            
            mLastProgressUpdate = currentTime;
        }
    }
    
} 