package com.chainmind.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlockchainService {

    @Value("${mst.rpc.url}")
    private String rpcUrl;

    @Value("${mst.chain.id}")
    private long chainId;

    @Value("${mst.contract.address}")
    private String contractAddress;

    @Value("${mst.wallet.privatekey:}")
    private String privateKey;

    private Web3j web3j;
    private Credentials credentials;

    @PostConstruct
    public void init() {
        web3j = Web3j.build(new HttpService(rpcUrl));
        if (privateKey != null && !privateKey.isEmpty()) {
            credentials = Credentials.create(privateKey);
        }
    }

    public String createEscrow(String workerAddress, BigInteger amountInWei) throws Exception {
        Function function = new Function(
                "createEscrow",
                Arrays.asList(new Address(workerAddress)),
                Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(encodedFunction, amountInWei);
    }

    public String releasePayment(BigInteger escrowId) throws Exception {
        Function function = new Function(
                "releasePayment",
                Arrays.asList(new Uint256(escrowId)),
                Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(encodedFunction, BigInteger.ZERO);
    }

    public String markCompleted(BigInteger escrowId) throws Exception {
        Function function = new Function(
                "markCompleted",
                Arrays.asList(new Uint256(escrowId)),
                Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);
        return sendTransaction(encodedFunction, BigInteger.ZERO);
    }

    public EscrowDetails getEscrow(BigInteger escrowId) throws Exception {
        Function function = new Function(
                "escrows",
                Arrays.asList(new Uint256(escrowId)),
                Arrays.asList(
                        new TypeReference<Address>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Bool>() {},
                        new TypeReference<Bool>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);
        String callerAddress = credentials != null
                ? credentials.getAddress()
                : "0x0000000000000000000000000000000000000000";

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(callerAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        EscrowDetails details = new EscrowDetails();
        details.client = (String) results.get(0).getValue();
        details.worker = (String) results.get(1).getValue();
        details.amount = (BigInteger) results.get(2).getValue();
        details.completed = (Boolean) results.get(3).getValue();
        details.released = (Boolean) results.get(4).getValue();
        return details;
    }

    private String sendTransaction(String encodedFunction, BigInteger valueInWei) throws Exception {
        if (credentials == null) {
            throw new IllegalStateException("Wallet private key set nahi hai (mst.wallet.privatekey)");
        }

        String fromAddress = credentials.getAddress();

        EthGetTransactionCount txCount = web3j.ethGetTransactionCount(
                fromAddress, DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = txCount.getTransactionCount();

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(300000);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, valueInWei, encodedFunction
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction response = web3j.ethSendRawTransaction(hexValue).send();

        if (response.hasError()) {
            throw new RuntimeException("Transaction failed: " + response.getError().getMessage());
        }

        return response.getTransactionHash();
    }

    public String getExplorerLink(String txHash) {
        return "https://mstscan.com/tx/" + txHash;
    }

    public static class EscrowDetails {
        public String client;
        public String worker;
        public BigInteger amount;
        public boolean completed;
        public boolean released;
    }
}
