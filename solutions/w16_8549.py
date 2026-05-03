// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract NextcloudUploadFix {
    address public owner;
    mapping(address => bool) public authorizedDevices;
    mapping(string => bool) public uploadedFiles;
    uint256 public constant MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    uint256 public constant MIN_BANDWIDTH = 1000000; // 1 Mbps
    
    event FileUploaded(address indexed uploader, string fileHash, uint256 timestamp);
    event DeviceAuthorized(address indexed device);
    event DeviceRevoked(address indexed device);
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }
    
    modifier onlyAuthorized() {
        require(authorizedDevices[msg.sender], "Device not authorized");
        _;
    }
    
    constructor() {
        owner = msg.sender;
    }
    
    function authorizeDevice(address device) external onlyOwner {
        authorizedDevices[device] = true;
        emit DeviceAuthorized(device);
    }
    
    function revokeDevice(address device) external onlyOwner {
        authorizedDevices[device] = false;
        emit DeviceRevoked(device);
    }
    
    function uploadFile(string memory fileHash, uint256 fileSize, uint256 bandwidth) external onlyAuthorized {
        require(bytes(fileHash).length > 0, "Invalid file hash");
        require(fileSize > 0 && fileSize <= MAX_FILE_SIZE, "Invalid file size");
        require(bandwidth >= MIN_BANDWIDTH, "Insufficient bandwidth for WiFi upload");
        require(!uploadedFiles[fileHash], "File already uploaded");
        
        uploadedFiles[fileHash] = true;
        emit FileUploaded(msg.sender, fileHash, block.timestamp);
    }
    
    function checkUploadStatus(string memory fileHash) external view returns (bool) {
        return uploadedFiles[fileHash];
    }
    
    function isDeviceAuthorized(address device) external view returns (bool) {
        return authorizedDevices[device];
    }
    
    function getOwner() external view returns (address) {
        return owner;
    }
}
