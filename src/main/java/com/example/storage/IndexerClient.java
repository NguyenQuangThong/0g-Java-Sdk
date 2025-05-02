package com.example.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class IndexerClient {
    private final String url;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public IndexerClient(String url) {
        this.url = url;
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    public List<String> getStorageNodes() throws IOException {
        Request request = new Request.Builder()
                .url(url + "/nodes")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch nodes: " + response.message());
            }
            String json = response.body().string();
            String[] nodes = mapper.readValue(json, String[].class);
            return Arrays.asList(nodes);
        }
    }
}
