// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/security/ReentrancyGuard.sol";

contract WiFiUploadBounty is Ownable, ReentrancyGuard {
    IERC20 public usdtToken;
    address public developerWallet;
    uint256 public constant BOUNTY_AMOUNT = 1000 * 10**6; // 1000 USDT (6 decimals)
    
    mapping(address => bool) public hasClaimed;
    mapping(address => uint256) public contributions;
    uint256 public totalContributions;
    
    event BountyClaimed(address indexed developer, uint256 amount);
    event ContributionReceived(address indexed contributor, uint256 amount);
    
    constructor(address _usdtToken, address _developerWallet) {
        require(_usdtToken != address(0), "Invalid USDT address");
        require(_developerWallet != address(0), "Invalid developer wallet");
        usdtToken = IERC20(_usdtToken);
        developerWallet = _developerWallet;
    }
    
    function contribute(uint256 amount) external {
        require(amount > 0, "Amount must be greater than 0");
        require(usdtToken.transferFrom(msg.sender, address(this), amount), "Transfer failed");
        
        contributions[msg.sender] += amount;
        totalContributions += amount;
        
        emit ContributionReceived(msg.sender, amount);
    }
    
    function claimBounty() external nonReentrant {
        require(msg.sender == developerWallet, "Only developer can claim");
        require(!hasClaimed[msg.sender], "Already claimed");
        require(totalContributions >= BOUNTY_AMOUNT, "Bounty not fully funded");
        
        hasClaimed[msg.sender] = true;
        
        // Transfer bounty to developer
        require(usdtToken.transfer(msg.sender, BOUNTY_AMOUNT), "Bounty transfer failed");
        
        // Return excess contributions to contributors proportionally
        if (totalContributions > BOUNTY_AMOUNT) {
            uint256 excess = totalContributions - BOUNTY_AMOUNT;
            uint256 remainingExcess = excess;
            
            for (uint256 i = 0; i < contributors.length; i++) {
                address contributor = contributors[i];
                uint256 contributorShare = (contributions[contributor] * excess) / totalContributions;
                if (contributorShare > 0) {
                    require(usdtToken.transfer(contributor, contributorShare), "Refund failed");
                    remainingExcess -= contributorShare;
                }
            }
            
            // Send any dust to owner
            if (remainingExcess > 0) {
                require(usdtToken.transfer(owner(), remainingExcess), "Dust transfer failed");
            }
        }
        
        emit BountyClaimed(msg.sender, BOUNTY_AMOUNT);
    }
    
    address[] private contributors;
    
    function getContributors() external view returns (address[] memory) {
        return contributors;
    }
    
    function addContributor(address contributor) internal {
        if (contributions[contributor] == 0) {
            contributors.push(contributor);
        }
    }
    
    // Override transferFrom to track contributors
    function contributeWithTracking(uint256 amount) external {
        contribute(amount);
        addContributor(msg.sender);
    }
    
    // Emergency withdraw for owner
    function emergencyWithdraw(address token, uint256 amount) external onlyOwner {
        require(token != address(usdtToken) || amount <= totalContributions - BOUNTY_AMOUNT, "Cannot withdraw bounty funds");
        IERC20(token).transfer(owner(), amount);
    }
}
