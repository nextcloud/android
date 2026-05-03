// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract WiFiUploadFix {
    address public owner;
    mapping(address => bool) public whitelistedDevices;
    mapping(bytes32 => bool) public processedTransactions;
    
    event DeviceWhitelisted(address indexed device);
    event DeviceRemoved(address indexed device);
    event UploadFixed(bytes32 indexed txHash, address indexed device);
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }
    
    constructor() {
        owner = msg.sender;
    }
    
    function whitelistDevice(address device) external onlyOwner {
        whitelistedDevices[device] = true;
        emit DeviceWhitelisted(device);
    }
    
    function removeDevice(address device) external onlyOwner {
        whitelistedDevices[device] = false;
        emit DeviceRemoved(device);
    }
    
    function fixUpload(bytes32 txHash, address device) external {
        require(whitelistedDevices[device], "Device not whitelisted");
        require(!processedTransactions[txHash], "Already processed");
        
        processedTransactions[txHash] = true;
        emit UploadFixed(txHash, device);
    }
    
    function isDeviceWhitelisted(address device) external view returns (bool) {
        return whitelistedDevices[device];
    }
    
    function isTransactionProcessed(bytes32 txHash) external view returns (bool) {
        return processedTransactions[txHash];
    }
}
