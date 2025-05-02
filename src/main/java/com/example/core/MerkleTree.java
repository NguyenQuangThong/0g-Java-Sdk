package com.example.core;

import org.bouncycastle.crypto.digests.SHA256Digest;

import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    private final List<String> leaves;
    private final List<List<String>> layers;

    public MerkleTree(byte[][] chunks) {
        this.leaves = new ArrayList<>();
        this.layers = new ArrayList<>();
        computeLeaves(chunks);
        buildTree();
    }

    private void computeLeaves(byte[][] chunks) {
        SHA256Digest digest = new SHA256Digest();
        for (byte[] chunk : chunks) {
            digest.update(chunk, 0, chunk.length);
            byte[] hash = new byte[digest.getDigestSize()];
            digest.doFinal(hash, 0);
            leaves.add(bytesToHex(hash));
        }
        layers.add(leaves);
    }

    private void buildTree() {
        List<String> currentLayer = leaves;
        while (currentLayer.size() > 1) {
            List<String> nextLayer = new ArrayList<>();
            for (int i = 0; i < currentLayer.size(); i += 2) {
                String left = currentLayer.get(i);
                String right = i + 1 < currentLayer.size() ? currentLayer.get(i + 1) : left;
                String parent = hashPair(left, right);
                nextLayer.add(parent);
            }
            layers.add(nextLayer);
            currentLayer = nextLayer;
        }
    }

    public String getRootHash() {
        return layers.get(layers.size() - 1).get(0);
    }

    private String hashPair(String left, String right) {
        SHA256Digest digest = new SHA256Digest();
        byte[] combined = (left + right).getBytes();
        digest.update(combined, 0, combined.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
