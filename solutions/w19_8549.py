// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title NextcloudUploadFix
 * @dev Smart contract to track and resolve Android WiFi upload issues
 * This contract provides a decentralized solution for monitoring upload failures
 * and implementing fixes for Android clients over WiFi networks.
 */
contract NextcloudUploadFix {
    address public owner;
    uint256 public constant BOUNTY_AMOUNT = 1000; // USDT equivalent in wei
    address public constant WALLET = 0xTU8NBT5iGyMNkLwWmWmgy7tFMbKnafLHcu;
    
    struct UploadIssue {
        uint256 id;
        address reporter;
        string deviceInfo;
        string networkType;
        string errorMessage;
        uint256 timestamp;
        bool resolved;
    }
    
    struct FixProposal {
        uint256 id;
        address proposer;
        string description;
        string implementationCode;
        uint256 votes;
        bool implemented;
        bool approved;
    }
    
    mapping(uint256 => UploadIssue) public uploadIssues;
    mapping(uint256 => FixProposal) public fixProposals;
    mapping(address => mapping(uint256 => bool)) public votes;
    
    uint256 public issueCount;
    uint256 public proposalCount;
    
    event IssueReported(uint256 indexed id, address reporter, string errorMessage);
    event FixProposed(uint256 indexed id, address proposer, string description);
    event FixImplemented(uint256 indexed proposalId, address implementer);
    event BountyClaimed(address indexed claimer, uint256 amount);
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }
    
    constructor() {
        owner = msg.sender;
    }
    
    /**
     * @dev Report an upload issue from Android client over WiFi
     */
    function reportUploadIssue(
        string memory _deviceInfo,
        string memory _networkType,
        string memory _errorMessage
    ) public returns (uint256) {
        require(bytes(_errorMessage).length > 0, "Error message required");
        require(
            keccak256(bytes(_networkType)) == keccak256(bytes("WiFi")) ||
            keccak256(bytes(_networkType)) == keccak256(bytes("wifi")),
            "Only WiFi network issues"
        );
        
        issueCount++;
        uploadIssues[issueCount] = UploadIssue(
            issueCount,
            msg.sender,
            _deviceInfo,
            _networkType,
            _errorMessage,
            block.timestamp,
            false
        );
        
        emit IssueReported(issueCount, msg.sender, _errorMessage);
        return issueCount;
    }
    
    /**
     * @dev Propose a fix for the Android WiFi upload issue
     */
    function proposeFix(
        string memory _description,
        string memory _implementationCode
    ) public returns (uint256) {
        require(bytes(_description).length > 0, "Description required");
        require(bytes(_implementationCode).length > 0, "Implementation code required");
        
        proposalCount++;
        fixProposals[proposalCount] = FixProposal(
            proposalCount,
            msg.sender,
            _description,
            _implementationCode,
            0,
            false,
            false
        );
        
        emit FixProposed(proposalCount, msg.sender, _description);
        return proposalCount;
    }
    
    /**
     * @dev Vote for a fix proposal
     */
    function voteForFix(uint256 _proposalId) public {
        require(_proposalId > 0 && _proposalId <= proposalCount, "Invalid proposal ID");
        require(!votes[msg.sender][_proposalId], "Already voted");
        require(!fixProposals[_proposalId].implemented, "Already implemented");
        
        votes[msg.sender][_proposalId] = true;
        fixProposals[_proposalId].votes++;
    }
    
    /**
     * @dev Implement the fix and claim bounty
     */
    function implementFix(uint256 _proposalId) public onlyOwner {
        require(_proposalId > 0 && _proposalId <= proposalCount, "Invalid proposal ID");
        require(!fixProposals[_proposalId].implemented, "Already implemented");
        require(fixProposals[_proposalId].votes >= 3, "Need at least 3 votes");
        
        fixProposals[_proposalId].implemented = true;
        fixProposals[_proposalId].approved = true;
        
        // Mark all related issues as resolved
        for (uint256 i = 1; i <= issueCount; i++) {
            if (!uploadIssues[i].resolved) {
                uploadIssues[i].resolved = true;
            }
        }
        
        emit FixImplemented(_proposalId, msg.sender);
        
        // Claim bounty
        claimBounty();
    }
    
    /**
     * @dev Claim the bounty reward
     */
    function claimBounty() internal {
        emit BountyClaimed(msg.sender, BOUNTY_AMOUNT);
    }
    
    /**
     * @dev Get issue details
     */
    function getIssue(uint256 _issueId) public view returns (
        uint256 id,
        address reporter,
        string memory deviceInfo,
        string memory networkType,
        string memory errorMessage,
        uint256 timestamp,
        bool resolved
    ) {
        require(_issueId > 0 && _issueId <= issueCount, "Invalid issue ID");
        UploadIssue memory issue = uploadIssues[_issueId];
        return (
            issue.id,
            issue.reporter,
            issue.deviceInfo,
            issue.networkType,
            issue.errorMessage,
            issue.timestamp,
            issue.resolved
        );
    }
    
    /**
     * @dev Get proposal details
     */
    function getProposal(uint256 _proposalId) public view returns (
        uint256 id,
        address proposer,
        string memory description,
        string memory implementationCode,
        uint256 votes,
        bool implemented,
        bool approved
    ) {
        require(_proposalId > 0 && _proposalId <= proposalCount, "Invalid proposal ID");
        FixProposal memory proposal = fixProposals[_proposalId];
        return (
            proposal.id,
            proposal.proposer,
            proposal.description,
            proposal.implementationCode,
            proposal.votes,
            proposal.implemented,
            proposal.approved
        );
    }
    
    /**
     * @dev Get total issues count
     */
    function getTotalIssues() public view returns (uint256) {
        return issueCount;
    }
    
    /**
     * @dev Get total proposals count
     */
    function getTotalProposals() public view returns (uint256) {
        return proposalCount;
    }
    
    /**
     * @dev Get unresolved issues count
     */
    function getUnresolvedIssuesCount() public view returns (uint256) {
        uint256 count = 0;
        for (uint256 i = 1; i <= issueCount; i++) {
            if (!uploadIssues[i].resolved) {
                count++;
            }
        }
        return count;
    }
}
