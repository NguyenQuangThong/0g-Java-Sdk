package com.example;

import com.example.blockchain.StorageContract;
import com.example.core.ZgFile;
import com.example.storage.IndexerClient;
import com.example.storage.StorageClient;
import com.example.utils.Config;

import java.math.BigInteger;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        // Cấu hình
        Config config = new Config(
                "https://rpc.0g.ai", // RPC URL
                "https://indexer.0g.ai", // Indexer URL
                "your-private-key" // Private key
        );

        // Khởi tạo các client
        IndexerClient indexerClient = new IndexerClient(config.indexerUrl());
        StorageClient storageClient = new StorageClient(indexerClient);
        StorageContract storageContract = new StorageContract(
                config.rpcUrl(),
                config.privateKey(),
                "0xYourContractAddress" // Địa chỉ hợp đồng
        );

        // Tải lên file
        Path filePath = Path.of("test.txt");
        ZgFile zgFile = ZgFile.fromFilePath(filePath);
        String rootHash = zgFile.upload(storageClient);
        System.out.println("File uploaded with root hash: " + rootHash);

        // Lưu root hash lên blockchain
        storageContract.storeFileHash(rootHash);
        System.out.println("Root hash stored on blockchain");

        // Thanh toán phí lưu trữ (giả sử 0.01 ETH)
        BigInteger amount = BigInteger.valueOf(10_000_000_000_000_000L); // 0.01 ETH
        storageContract.payStorageFee(rootHash, amount);
        System.out.println("Storage fee paid");

        // Tải xuống file
        Path outputPath = Path.of("downloaded_test.txt");
        storageClient.downloadFile(rootHash, outputPath);
        System.out.println("File downloaded to: " + outputPath);
    }
}
