// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract AIEscrow {

    struct Escrow {
        address payable client;
        address payable worker;
        uint256 amount;
        bool completed;
        bool released;
    }

    uint256 public nextEscrowId;

    mapping(uint256 => Escrow) public escrows;

    event EscrowCreated(
        uint256 indexed escrowId,
        address indexed client,
        address indexed worker,
        uint256 amount
    );

    event PaymentReleased(
        uint256 indexed escrowId,
        address indexed worker,
        uint256 amount
    );

    function createEscrow(address payable worker)
        external
        payable
        returns (uint256)
    {
        require(worker != address(0), "Invalid worker");
        require(msg.value > 0, "Payment required");

        uint256 escrowId = nextEscrowId;

        escrows[escrowId] = Escrow({
            client: payable(msg.sender),
            worker: worker,
            amount: msg.value,
            completed: false,
            released: false
        });

        nextEscrowId++;

        emit EscrowCreated(
            escrowId,
            msg.sender,
            worker,
            msg.value
        );

        return escrowId;
    }

    function markCompleted(uint256 escrowId) external {
        Escrow storage e = escrows[escrowId];

        require(e.amount > 0, "Escrow not found");
        require(msg.sender == e.worker, "Only worker");
        require(!e.released, "Already released");

        e.completed = true;
    }

    function releasePayment(uint256 escrowId) external {
        Escrow storage e = escrows[escrowId];

        require(e.amount > 0, "Escrow not found");
        require(msg.sender == e.client, "Only client");
        require(e.completed, "Work not completed");
        require(!e.released, "Already released");

        e.released = true;

        uint256 payment = e.amount;
        e.amount = 0;

        (bool success, ) = e.worker.call{value: payment}("");
        require(success, "Payment failed");

        emit PaymentReleased(
            escrowId,
            e.worker,
            payment
        );
    }
}