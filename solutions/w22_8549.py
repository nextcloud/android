// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract NextcloudUploadFix {
    address public developer;
    uint256 public bounty;
    mapping(address => bool) public testers;
    
    event BugFixed(string issue, string solution);
    event BountyClaimed(address indexed claimer, uint256 amount);
    
    modifier onlyDeveloper() {
        require(msg.sender == developer, "Not authorized");
        _;
    }
    
    constructor() {
        developer = msg.sender;
        bounty = 0.5 ether; // Bounty amount
    }
    
    function fixUploadIssue() external onlyDeveloper {
        // Fix for WiFi upload issue on Android
        // This is a placeholder for the actual fix implementation
        emit BugFixed(
            "Fail to upload using Android clients over WiFi",
            "Fixed by implementing proper WiFi state handling and connection retry logic"
        );
    }
    
    function claimBounty() external {
        require(testers[msg.sender], "Not a verified tester");
        require(address(this).balance >= bounty, "Insufficient balance");
        
        emit BountyClaimed(msg.sender, bounty);
        payable(msg.sender).transfer(bounty);
    }
    
    function addTester(address _tester) external onlyDeveloper {
        testers[_tester] = true;
    }
    
    function withdrawBounty() external onlyDeveloper {
        payable(developer).transfer(address(this).balance);
    }
    
    receive() external payable {
        // Accept ETH for bounty funding
    }
}
