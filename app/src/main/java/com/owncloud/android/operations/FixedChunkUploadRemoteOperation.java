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
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
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
    
    // Constants for repeated string literals
    public static final String BYTES_SUFFIX = " bytes";
    public static final String SIZE_SEPARATOR = ", size: ";
    public static final String DAV_UPLOADS_PATH = "/remote.php/dav/uploads/";
    
    // Chunk filename formatting constants
    private static final int CHUNK_NAME_PADDING = 5;
    
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
                     " with size " + mFileSize + BYTES_SUFFIX);
            Log_OC.d(TAG, "FixedChunkUploadRemoteOperation: FIXED_CHUNK_SIZE = " + FIXED_CHUNK_SIZE + BYTES_SUFFIX);
            
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
        Log_OC.d(TAG, "uploadFileDirectly: File size = " + localFile.length() + BYTES_SUFFIX);
        
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
                     SIZE_SEPARATOR + mFileSize + BYTES_SUFFIX + ", using " + FIXED_CHUNK_SIZE + " byte chunks");
            
            mTotalChunks = (mFileSize + FIXED_CHUNK_SIZE - 1) / FIXED_CHUNK_SIZE; // Ceiling division
            mCurrentChunk = 0;
            
            Log_OC.d(TAG, "uploadFileInChunks: File will be uploaded in " + mTotalChunks + " chunks using session: " + mUploadSessionId);
            
            
            // Step 1: Check if we can resume an existing session (for background upload continuation)
            int lastUploadedChunk = checkExistingSession(client);
            
            if (lastUploadedChunk > 0) {
                // Validate that we can actually resume from this point
                if (lastUploadedChunk >= mTotalChunks) {
                    Log_OC.w(TAG, "uploadFileInChunks: All chunks already uploaded (" + lastUploadedChunk + "/" + mTotalChunks + "), attempting to assemble");
                    // All chunks are uploaded, try to assemble
                    RemoteOperationResult assemblyResult = assembleChunks(client);
                    if (assemblyResult.isSuccess()) {
                        updateProgress(mFileSize, localFile.getName());
                        return assemblyResult;
                    } else {
                        Log_OC.w(TAG, "uploadFileInChunks: Assembly failed, will re-upload last chunk");
                        lastUploadedChunk = Math.max(0, lastUploadedChunk - 1); // Retry last chunk
                    }
                }
                
                // Resume from existing session
                Log_OC.d(TAG, "uploadFileInChunks: *** RESUMING UPLOAD *** from chunk " + (lastUploadedChunk + 1) + "/" + mTotalChunks);
                long bytesAlreadyUploaded = Math.min(lastUploadedChunk * FIXED_CHUNK_SIZE, mFileSize);
                updateProgress(bytesAlreadyUploaded, localFile.getName());
                
                // Show toast to indicate resume
                if (mContext != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(mContext, "Resuming upload from " + Math.round(100.0 * bytesAlreadyUploaded / mFileSize) + "%", 
                                     Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                // Create new upload session directory
                Log_OC.d(TAG, "uploadFileInChunks: *** STARTING NEW UPLOAD *** - no existing session found");
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
            long totalBytesUploaded = (lastUploadedChunk > 0) ? Math.min(lastUploadedChunk * FIXED_CHUNK_SIZE, mFileSize) : 0;
            int startChunkIndex = (lastUploadedChunk > 0) ? lastUploadedChunk : 0;
            
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
                             " (bytes " + chunkStart + "-" + (chunkEnd - 1) + SIZE_SEPARATOR + chunkSize + ")");
                    
                    
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
                sessionUrl = client.getBaseUri() + DAV_UPLOADS_PATH + encodedUsername + "/" + encodedSessionId;
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
            Log_OC.d(TAG, "uploadChunkToSession: Uploading chunk " + chunkNumber + " with " + chunkData.length + BYTES_SUFFIX);
            
            // Construct chunk URL - chunks are named as numbers (1, 2, 3...)
            String chunkUrl;
            try {
                String encodedUsername = URLEncoder.encode(client.getCredentials().getUsername(), StandardCharsets.UTF_8.toString());
                String encodedSessionId = URLEncoder.encode(mUploadSessionId, StandardCharsets.UTF_8.toString());
                String chunkFileName = String.format("%0" + CHUNK_NAME_PADDING + "d", chunkNumber);
                chunkUrl = client.getBaseUri() + DAV_UPLOADS_PATH + encodedUsername + "/" + encodedSessionId + "/" + chunkFileName;
            } catch (UnsupportedEncodingException e) {
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
                assemblySourceUrl = client.getBaseUri() + DAV_UPLOADS_PATH + encodedUsername + "/" + encodedSessionId + "/.file";
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
            String sessionUrl = client.getBaseUri() + DAV_UPLOADS_PATH + encodedUsername + "/" + encodedSessionId;
            
            Log_OC.d(TAG, "checkExistingSession: Checking for existing session: " + sessionUrl);
            
            // First, check if the session directory exists using PROPFIND
            if (!sessionDirectoryExists(client, sessionUrl)) {
                Log_OC.d(TAG, "checkExistingSession: Session directory does not exist, starting fresh upload");
                return 0;
            }
            
            Log_OC.d(TAG, "checkExistingSession: Session directory exists, checking for uploaded chunks");
            
            // Check for consecutive chunk files starting from 00001
            int chunkCount = findLastConsecutiveChunk(client, sessionUrl);
            
            Log_OC.d(TAG, "checkExistingSession: Found " + chunkCount + " existing chunks out of " + mTotalChunks + 
                     ", will resume from chunk " + (chunkCount + 1));
            return chunkCount;
            
        } catch (IOException e) {
            Log_OC.e(TAG, "checkExistingSession: IO error checking existing session", e);
            return 0; // Start fresh on any error
        } catch (RuntimeException e) {
            Log_OC.e(TAG, "checkExistingSession: Runtime error checking existing session", e);
            return 0; // Start fresh on any error
        }
    }
    
    /**
     * Check if the upload session directory exists using PROPFIND
     */
    private boolean sessionDirectoryExists(OwnCloudClient client, String sessionUrl) {
        return checkResourceExists(client, sessionUrl, "session directory");
    }
    
    /**
     * Check if a specific chunk exists using PROPFIND
     */
    private boolean chunkExists(OwnCloudClient client, String chunkUrl) {
        return checkResourceExists(client, chunkUrl, "chunk");
    }
    
    /**
     * Find the last consecutive chunk that exists on the server
     * @param client The OwnCloud client
     * @param sessionUrl The base session URL
     * @return The number of the last consecutive chunk found (0 if none)
     */
    private int findLastConsecutiveChunk(OwnCloudClient client, String sessionUrl) {
        int chunkCount = 0;
        for (int i = 1; i <= mTotalChunks; i++) {
            String chunkFileName = String.format("%0" + CHUNK_NAME_PADDING + "d", i);
            String chunkUrl = sessionUrl + "/" + chunkFileName;
            
            if (chunkExists(client, chunkUrl)) {
                chunkCount = i; // Found this chunk
                Log_OC.d(TAG, "findLastConsecutiveChunk: Found existing chunk " + i);
            } else {
                // Missing chunk - this is where we should resume from
                Log_OC.d(TAG, "findLastConsecutiveChunk: Chunk " + i + " missing, can resume from chunk " + (chunkCount + 1));
                break;
            }
        }
        return chunkCount;
    }
    
    /**
     * Generic method to check if a WebDAV resource exists using PROPFIND
     * @param client The OwnCloud client
     * @param resourceUrl The URL of the resource to check
     * @param resourceType Description of resource type for logging
     * @return true if resource exists, false otherwise
     */
    private boolean checkResourceExists(OwnCloudClient client, String resourceUrl, String resourceType) {
        PropFindMethod propFindMethod = null;
        try {
            propFindMethod = new PropFindMethod(resourceUrl, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0);
            int statusCode = client.executeMethod(propFindMethod);
            
            boolean exists = (statusCode == HttpStatus.SC_MULTI_STATUS || 
                            statusCode == HttpStatus.SC_OK);
            
            Log_OC.d(TAG, "checkResourceExists: " + resourceType + " " + 
                     (exists ? "exists" : "does not exist") + " (status: " + statusCode + ")");
            return exists;
            
        } catch (IOException e) {
            Log_OC.w(TAG, "checkResourceExists: IO error checking " + resourceType + ": " + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            Log_OC.w(TAG, "checkResourceExists: Runtime error checking " + resourceType + ": " + e.getMessage());
            return false;
        } finally {
            if (propFindMethod != null) {
                propFindMethod.releaseConnection();
            }
        }
    }

    /**
     * Generate a deterministic session ID based on file characteristics.
     * This ensures the same session ID is used if the upload is restarted,
     * enabling proper background upload continuation even if the app is closed.
     */
    private String generateDeterministicSessionId() {
        try {
            // Use file path, size AND modification time to create a deterministic ID
            // This ensures the same file version always gets the same session ID for resumption
            // but different if the file is modified
            File file = new File(mLocalPath);
            
            // Get canonical path to handle different path representations
            String canonicalPath = file.getCanonicalPath();
            long fileSize = file.length();
            long lastModified = file.lastModified();
            
            // Create deterministic base string using path, size and modification time
            String baseString = canonicalPath + "_" + fileSize + "_" + lastModified;
            
            // Create a hash to make it shorter and more session-like
            int hash = baseString.hashCode();
            String sessionId = "upload_" + Math.abs(hash);
            
            Log_OC.d(TAG, "generateDeterministicSessionId: Generated session ID: " + sessionId + 
                     " for file: " + canonicalPath + " (size: " + fileSize + ", modified: " + lastModified + ")");
            Log_OC.d(TAG, "generateDeterministicSessionId: Base string: " + baseString);
            return sessionId;
        } catch (IOException e) {
            Log_OC.e(TAG, "generateDeterministicSessionId: IO error generating deterministic session ID, falling back to random", e);
            return UUID.randomUUID().toString();
        } catch (SecurityException e) {
            Log_OC.e(TAG, "generateDeterministicSessionId: Security error accessing file, falling back to random", e);
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
            
            Log_OC.d(TAG, "updateProgress: " + bytesUploaded + "/" + mFileSize + BYTES_SUFFIX + " (" + currentPercent + "%)");
            
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