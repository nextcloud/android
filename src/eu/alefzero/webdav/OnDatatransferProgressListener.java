package eu.alefzero.webdav;

public interface OnDatatransferProgressListener {
    public void onTransferProgress(long progressRate);
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName);
}
