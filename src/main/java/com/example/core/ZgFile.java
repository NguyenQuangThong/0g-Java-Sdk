package com.example.core;

import com.example.storage.StorageClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ZgFile {
    private final byte[] fileContent;
    private final int chunkSize = 4 * 1024 * 1024; // 4MB
    private final Path filePath;

    private ZgFile(Path filePath, byte[] fileContent) {
        this.filePath = filePath;
        this.fileContent = fileContent;
    }

    public static ZgFile fromFilePath(Path filePath) throws IOException {
        byte[] content = Files.readAllBytes(filePath);
        return new ZgFile(filePath, content);
    }

    public MerkleTree createMerkleTree() throws IOException {
        byte[][] chunks = getChunks();
        return new MerkleTree(chunks);
    }

    public String upload(StorageClient storageClient) throws IOException {
        // Tận dụng StorageClient để tải lên
        return storageClient.uploadFile(filePath);
    }

    public byte[][] getChunks() {
        int chunkCount = (int) Math.ceil((double) fileContent.length / chunkSize);
        List<byte[]> chunks = new ArrayList<>();

        for (int i = 0; i < chunkCount; i++) {
            int start = i * chunkSize;
            int length = Math.min(chunkSize, fileContent.length - start);
            byte[] chunk = new byte[length];
            System.arraycopy(fileContent, start, chunk, 0, length);
            chunks.add(chunk);
        }
        return chunks.toArray(new byte[0][]);
    }
}
