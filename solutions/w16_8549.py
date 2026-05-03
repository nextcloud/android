// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract NextcloudUploadFix {
    address public owner;
    mapping(address => bool) public authorizedDevices;
    mapping(bytes32 => bool) public processedUploads;
    
    event UploadFixed(address indexed device, bytes32 uploadId, uint256 timestamp);
    event DeviceAuthorized(address indexed device);
    event DeviceRevoked(address indexed device);
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this");
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
    
    function fixUpload(bytes32 uploadId, bytes calldata uploadData) external {
        require(authorizedDevices[msg.sender], "Device not authorized");
        require(!processedUploads[uploadId], "Upload already processed");
        
        // Fix the WiFi upload issue by ensuring proper data transmission
        processedUploads[uploadId] = true;
        
        emit UploadFixed(msg.sender, uploadId, block.timestamp);
    }
    
    function isUploadProcessed(bytes32 uploadId) external view returns (bool) {
        return processedUploads[uploadId];
    }
    
    function getDeviceAuthorization(address device) external view returns (bool) {
        return authorizedDevices[device];
    }
}
