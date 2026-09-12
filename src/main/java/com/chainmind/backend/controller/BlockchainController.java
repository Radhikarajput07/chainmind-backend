package com.chainmind.backend.controller;

import com.chainmind.backend.service.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/blockchain")
@CrossOrigin(origins = "*")
public class BlockchainController {

    @Autowired
    private BlockchainService blockchainService;

    @PostMapping("/create-escrow")
    public Map<String, Object> createEscrow(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String workerAddress = (String) body.get("workerAddress");
            BigInteger amount = new BigInteger(body.get("amountInWei").toString());

            String txHash = blockchainService.createEscrow(workerAddress, amount);

            response.put("status", "success");
            response.put("txHash", txHash);
            response.put("explorerLink", blockchainService.getExplorerLink(txHash));
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/escrow/{id}")
    public Map<String, Object> getEscrow(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            BlockchainService.EscrowDetails details = blockchainService.getEscrow(new BigInteger(id));
            response.put("status", "success");
            response.put("client", details.client);
            response.put("worker", details.worker);
            response.put("amount", details.amount);
            response.put("completed", details.completed);
            response.put("released", details.released);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/release/{id}")
    public Map<String, Object> releasePayment(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            String txHash = blockchainService.releasePayment(new BigInteger(id));
            response.put("status", "success");
            response.put("txHash", txHash);
            response.put("explorerLink", blockchainService.getExplorerLink(txHash));
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}