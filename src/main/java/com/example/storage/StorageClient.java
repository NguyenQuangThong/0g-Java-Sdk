package com.example.storage;

import com.example.core.MerkleTree;
import com.example.core.ZgFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StorageClient {
    private final IndexerClient indexerClient;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private static final MediaType MEDIA_TYPE_BINARY = MediaType.parse("application/octet-stream");

    public StorageClient(IndexerClient indexerClient) {
        this.indexerClient = indexerClient;
        this.httpClient = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String uploadFile(Path filePath) throws IOException {
        // Chia file thành các chunk
        ZgFile zgFile = ZgFile.fromFilePath(filePath);
        byte[][] chunks = zgFile.getChunks();
        MerkleTree merkleTree = new MerkleTree(chunks);
        String rootHash = merkleTree.getRootHash();

        // Lấy danh sách node lưu trữ từ indexer
        List<String> nodes = indexerClient.getStorageNodes();
        if (nodes.isEmpty()) {
            throw new IOException("No storage nodes available");
        }

        // Gửi từng chunk đến node đầu tiên (có thể cải tiến để phân phối nhiều node)
        String nodeUrl = nodes.get(0);
        for (int i = 0; i < chunks.length; i++) {
            uploadChunk(nodeUrl, rootHash, i, chunks[i]);
        }

        return rootHash;
    }

    private void uploadChunk(String nodeUrl, String rootHash, int chunkIndex, byte[] chunk) throws IOException {
        String endpoint = String.format("%s/upload/%s/%d", nodeUrl, rootHash, chunkIndex);
        RequestBody body = RequestBody.create(chunk, MEDIA_TYPE_BINARY);
        Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to upload chunk " + chunkIndex + ": " + response.message());
            }
        }
    }

    public void downloadFile(String rootHash, Path outputPath) throws IOException {
        // Lấy danh sách node lưu trữ
        List<String> nodes = indexerClient.getStorageNodes();
        if (nodes.isEmpty()) {
            throw new IOException("No storage nodes available");
        }

        // Giả sử node đầu tiên có tất cả các chunk (có thể cải tiến để lấy từ nhiều node)
        String nodeUrl = nodes.get(0);
        List<byte[]> chunks = downloadChunks(nodeUrl, rootHash);

        // Gộp các chunk thành file
        byte[] fileContent = mergeChunks(chunks);
        Files.write(outputPath, fileContent);

        // Kiểm tra tính toàn vẹn bằng Merkle Tree
        ZgFile zgFile = ZgFile.fromFilePath(outputPath);
        MerkleTree merkleTree = zgFile.createMerkleTree();
        if (!merkleTree.getRootHash().equals(rootHash)) {
            throw new IOException("File integrity check failed");
        }
    }

    private List<byte[]> downloadChunks(String nodeUrl, String rootHash) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        int chunkIndex = 0;

        while (true) {
            String endpoint = String.format("%s/download/%s/%d", nodeUrl, rootHash, chunkIndex);
            Request request = new Request.Builder()
                    .url(endpoint)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    break; // Không còn chunk
                }
                byte[] chunk = response.body().bytes();
                chunks.add(chunk);
                chunkIndex++;
            }
        }

        if (chunks.isEmpty()) {
            throw new IOException("No chunks found for root hash: " + rootHash);
        }
        return chunks;
    }

    private byte[] mergeChunks(List<byte[]> chunks) {
        int totalLength = chunks.stream().mapToInt(chunk -> chunk.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;

        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }
}
