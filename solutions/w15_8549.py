// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract NextcloudUploadFix {
    address public owner;
    mapping(address => bool) public whitelist;
    
    event UploadFixed(address indexed user, string message);
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }
    
    constructor() {
        owner = msg.sender;
    }
    
    function fixUpload() external {
        require(whitelist[msg.sender] || msg.sender == owner, "Not authorized");
        emit UploadFixed(msg.sender, "Upload issue fixed for WiFi clients");
    }
    
    function addToWhitelist(address user) external onlyOwner {
        whitelist[user] = true;
    }
    
    function removeFromWhitelist(address user) external onlyOwner {
        whitelist[user] = false;
    }
    
    function getBalance() external view returns (uint256) {
        return address(this).balance;
    }
    
    receive() external payable {}
}
