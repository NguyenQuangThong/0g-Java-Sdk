package com.example.blockchain;

import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class StorageContract {
    private final Web3j web3j;
    private final TransactionManager transactionManager;
    private final String contractAddress;
    private final StaticGasProvider gasProvider;

    public StorageContract(String rpcUrl, String privateKey, String contractAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.transactionManager = new RawTransactionManager(
                web3j,
                Credentials.create(privateKey),
                0 // Chain ID, thay bằng chain ID của 0G
        );
        this.contractAddress = contractAddress;
        this.gasProvider = new StaticGasProvider(
                BigInteger.valueOf(20_000_000_000L), // Gas price (20 Gwei)
                BigInteger.valueOf(300_000) // Gas limit
        );
    }

    public TransactionReceipt storeFileHash(String rootHash) throws Exception {
        Function function = new Function(
                "storeFileHash",
                List.of(new org.web3j.abi.datatypes.Utf8String(rootHash)),
                Collections.emptyList()
        );
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        String transactionHash = transactionManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        ).getTransactionHash();

        return web3j.ethGetTransactionReceipt(transactionHash)
                .send()
                .getTransactionReceipt()
                .orElseThrow(() -> new Exception("Transaction receipt not found"));
    }

    public TransactionReceipt payStorageFee(String fileHash, BigInteger amount) throws Exception {
        Function function = new Function(
                "payStorageFee",
                List.of(
                        new org.web3j.abi.datatypes.Utf8String(fileHash),
                        new Uint256(amount)
                ),
                Collections.emptyList()
        );
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        String transactionHash = transactionManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                contractAddress,
                encodedFunction,
                amount // Gửi ETH/token cùng giao dịch
        ).getTransactionHash();

        return web3j.ethGetTransactionReceipt(transactionHash)
                .send()
                .getTransactionReceipt()
                .orElseThrow(() -> new Exception("Transaction receipt not found"));
    }
}
