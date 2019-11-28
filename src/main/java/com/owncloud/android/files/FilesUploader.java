package com.owncloud.android.files;

import android.accounts.Account;
import android.content.Intent;
import android.text.TextUtils;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

public class FilesUploader {

    private FileActivity fileActivity;

    private Set<String> foldersToAutoCreate = new HashSet<>();
    private List<UploadTarget> remoteTargets = new ArrayList<>();
    private List<File> localFiles = new ArrayList<>();
    private int resultCode;

    public static class UploadTarget {
        private String path;
        private boolean directory;
        public UploadTarget(String path, boolean directory) {
            this.path = path;
            this.directory = directory;
        }

        public boolean isDirectory() {
            return directory;
        }

        public String getPath() {
            return path;
        }
    }

    public FilesUploader(FileActivity fileActivity) {
        this.fileActivity = fileActivity;
    }

    /**
     * Requests an upload from local filesystem.
     * @param localFiles            Local files to upload
     * @param resultCode            result code
     * @param currentDir            the directory user currently is in
     * @param user                  current user
     */
    public void uploadFromFileSystem(List<File> localFiles,
                                     int resultCode,
                                     String replicationStartDir,
                                     OCFile currentDir,
                                     User user) {
        List<UploadTarget> remoteTargets = getRemoteTargets(localFiles, replicationStartDir, currentDir);
        Set<String> requiredFolders = getRequiredFolders(remoteTargets);
        for (String folder : requiredFolders) {
            createFolders(folder, user);
        }
        setFilesToUpload(localFiles, remoteTargets, resultCode);
        if (!this.isCreatingFolders()) {
            // Immediately upload, no folders needed
            completeFileUploadOperation(user);
        }
    }

    /**
     * Call this from activity when "create folder" operation finishes
     * @param operation operation
     * @param result    result
     * @param user      current user
     * @return          true if this operation was consumed, false otherwise
     */
    public boolean onCreateFolderOperationFinish(CreateFolderOperation operation,
                                                 RemoteOperationResult result,
                                                 User user) {
        String createdPath = operation.getRemotePath();
        if (createdPath.endsWith("/")) {
            createdPath = createdPath.substring(0, createdPath.length() - 1);
        }
        if ((result.isSuccess() || result.getCode() == RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS)
            && foldersToAutoCreate.contains(createdPath) && !localFiles.isEmpty() && !remoteTargets.isEmpty()) {
            foldersToAutoCreate.remove(createdPath);
            if (foldersToAutoCreate.isEmpty()) {
                completeFileUploadOperation(user);
            }
            return true;
        }
        return false;
    }

    private List<UploadTarget> getRemoteTargets(@NonNull List<File> localFiles,
                                                String replicationStartDir,
                                                OCFile currentDir) {

        List<UploadTarget> remoteTargets = new ArrayList<>();
        String remotePathBase = currentDir.getRemotePath();

        for (File localFile : localFiles) {

            String localPath = localFile.getAbsolutePath();
            String remoteDirectory = localFile.getName();

            if (replicationStartDir != null && localPath.startsWith(replicationStartDir)) {
                remoteDirectory = localPath.replace(replicationStartDir, "");
            }

            String remotePath = remotePathBase + remoteDirectory;
            UploadTarget remoteTarget = new UploadTarget(remotePath, localFile.isDirectory());
            remoteTargets.add(remoteTarget);
        }
        return remoteTargets;
    }

    public Set<String> getRequiredFolders(@NonNull List<UploadTarget> remoteTargets) {
        Set<String> foldersToAutoCreate = new HashSet<>();

        for (UploadTarget remoteTarget : remoteTargets) {

            String remotePath = remoteTarget.getPath();
            String folder = remotePath.replace(remotePath.substring(remotePath.lastIndexOf('/')), "");
            if (remoteTarget.isDirectory()) {
                folder = remotePath;
            }

            if (TextUtils.isEmpty(folder)) {
                // Uploading to root so no folder creation needed
                continue;
            }

            foldersToAutoCreate.add(folder);
        }

        return foldersToAutoCreate;
    }

    private void completeFileUploadOperation(User user) {
        if (localFiles == null) {
            return;
        }
        int behaviour;
        switch (resultCode) {
            case UploadFilesActivity.RESULT_OK_AND_MOVE:
                behaviour = FileUploader.LOCAL_BEHAVIOUR_MOVE;
                break;

            case UploadFilesActivity.RESULT_OK_AND_DELETE:
                behaviour = FileUploader.LOCAL_BEHAVIOUR_DELETE;
                break;

            case UploadFilesActivity.RESULT_OK_AND_DO_NOTHING:
                behaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;
                break;

            default:
                behaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;
                break;
        }

        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        Account account = user.toPlatformAccount();

        String[] localPaths = getLocalFilePaths(localFiles).toArray(new String[]{});
        String[] remotePaths = getRemoteTargetPaths(remoteTargets).toArray(new String[]{});

        requester.uploadNewFile(
            fileActivity,
            account,
            localPaths,
            remotePaths,
            null,           // MIME type will be detected from file name
            behaviour,
            false,          // do not create parent folder if not existent
            UploadFileOperation.CREATED_BY_USER,
            false,
            false
        );
        localFiles.clear();
        remoteTargets.clear();
    }

    private void createFolders(String folder, User user) {
        if (foldersToAutoCreate.contains(folder)) {
            // Folder already being created, don't create again
            return;
        }
        this.foldersToAutoCreate.add(folder);
        Intent intent = new Intent(fileActivity, OperationsService.class);
        intent.setAction(OperationsService.ACTION_CREATE_FOLDER);
        intent.putExtra(OperationsService.EXTRA_ACCOUNT, user.toPlatformAccount());
        intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, folder);
        intent.putExtra(OperationsService.EXTRA_CREATE_FULL_PATH, true);
        fileActivity.getOperationsServiceBinder().queueNewOperation(intent);
    }

    private boolean isCreatingFolders() {
        return !this.foldersToAutoCreate.isEmpty();
    }

    private void setFilesToUpload(List<File> localFiles, List<UploadTarget> remoteTargets, int resultCode) {
        this.localFiles = localFiles;
        this.remoteTargets = remoteTargets;
        this.resultCode = resultCode;
    }

    private List<String> getRemoteTargetPaths(Collection<UploadTarget> remoteTargets) {
        List<String> filePaths = new ArrayList<>();
        for (UploadTarget remoteTarget : remoteTargets) {
            if (!remoteTarget.isDirectory()) {
                filePaths.add(remoteTarget.getPath());
            }
        }
        return filePaths;
    }

    private List<String> getLocalFilePaths(Collection<File> localFiles) {
        List<String> filePaths = new ArrayList<>();
        for (File localFile : localFiles) {
            if (!localFile.isDirectory()) {
                filePaths.add(localFile.getAbsolutePath());
            }
        }
        return filePaths;
    }
}
