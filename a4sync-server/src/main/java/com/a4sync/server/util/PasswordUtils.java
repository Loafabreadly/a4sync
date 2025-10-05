package com.a4sync.server.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class for password hashing and verification using BCrypt.
 * 
 * Usage for administrators:
 * 1. Set a plain text password in application.properties: a4sync.repository-password=mySecretPassword
 * 2. OR generate a hash using this utility and set: a4sync.repository-password-hash=$2a$10$...
 * 3. Enable authentication: a4sync.authentication-enabled=true
 */
public class PasswordUtils {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * Generate a BCrypt hash for storing the repository password securely
     * @param plainPassword The plain text password to hash
     * @return BCrypt hash suitable for a4sync.repository-password-hash property
     */
    public static String generatePasswordHash(String plainPassword) {
        return encoder.encode(plainPassword);
    }
    
    /**
     * Verify a plain text password against a BCrypt hash
     * @param plainPassword The plain text password to verify
     * @param hash The BCrypt hash to verify against
     * @return true if the password matches the hash
     */
    public static boolean verifyPassword(String plainPassword, String hash) {
        return encoder.matches(plainPassword, hash);
    }
    
    /**
     * Command line utility for generating password hashes
     * Usage: java -cp a4sync-server.jar com.a4sync.server.util.PasswordUtils <password>
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -cp a4sync-server.jar com.a4sync.server.util.PasswordUtils <password>");
            System.out.println("Generates a BCrypt hash for the repository password.");
            System.exit(1);
        }
        
        String password = args[0];
        String hash = generatePasswordHash(password);
        
        System.out.println("BCrypt hash generated for password: " + password);
        System.out.println();
        System.out.println("Add this to your application.properties:");
        System.out.println("a4sync.authentication-enabled=true");
        System.out.println("a4sync.repository-password-hash=" + hash);
        System.out.println();
        System.out.println("Or alternatively, use the plain text password (less secure):");
        System.out.println("a4sync.authentication-enabled=true");
        System.out.println("a4sync.repository-password=" + password);
    }
}
