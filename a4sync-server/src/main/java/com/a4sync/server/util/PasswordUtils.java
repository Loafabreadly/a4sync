package com.a4sync.server.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class PasswordUtils {
    
    /**
     * Generate a BCrypt hash for storing the repository password securely
     * @param plainPassword The plain text password to hash
     * @return BCrypt hash suitable for a4sync.repository-password property
     */
    public static String generateServerHash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
    
    /**
     * Generate a client-side hash for sending in the X-Repository-Auth header
     * @param plainPassword The plain text password to hash
     * @return SHA-256 hash of the password
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public static String generateClientHash(String plainPassword) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(plainPassword.getBytes());
        return HexFormat.of().formatHex(hash);
    }
    
    /**
     * Command line utility for generating password hashes
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -cp a4sync-server.jar com.a4sync.server.util.PasswordUtils <password>");
            System.exit(1);
        }
        
        String password = args[0];
        try {
            String serverHash = generateServerHash(password);
            String clientHash = generateClientHash(password);
            
            System.out.println("For application.properties:");
            System.out.println("a4sync.repository-password=" + serverHash);
            System.out.println("\nFor X-Repository-Auth header:");
            System.out.println(clientHash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to generate hash: " + e.getMessage());
            System.exit(1);
        }
    }
}
