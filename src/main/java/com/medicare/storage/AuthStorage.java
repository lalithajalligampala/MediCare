
package com.medicare.storage;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

public class AuthStorage {

    private static final Path FILE_PATH =
            Path.of("data", "users.txt");

    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;

    private static byte[] generateSalt() {

        byte[] salt = new byte[16];

        new SecureRandom().nextBytes(salt);

        return salt;
    }

    private static byte[] hashPassword(
            char[] password,
            byte[] salt) throws IOException {

        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            return factory.generateSecret(spec).getEncoded();

        } catch (GeneralSecurityException e) {

            throw new IOException(
                    "Unable to secure password.",
                    e
            );

        } finally {

            spec.clearPassword();
        }
    }

    public static boolean register(
            String username,
            char[] password) throws IOException {

        Files.createDirectories(FILE_PATH.getParent());

        username = username.trim();

        if (username.isEmpty()
                || username.contains("|")
                || username.contains("\n")
                || username.contains("\r")) {

            throw new IOException("Invalid username.");
        }

        if (Files.exists(FILE_PATH)) {

            List<String> users =
                    Files.readAllLines(FILE_PATH);

            for (String line : users) {

                String[] parts = line.split("\\|", -1);

                if (parts.length == 3
                        && parts[0].equalsIgnoreCase(username)) {

                    return false;
                }
            }
        }

        byte[] salt = generateSalt();

        byte[] passwordHash =
                hashPassword(password, salt);

        String saltText =
                Base64.getEncoder().encodeToString(salt);

        String hashText =
                Base64.getEncoder().encodeToString(passwordHash);

        String userData =
                username + "|" + saltText + "|" + hashText
                        + System.lineSeparator();

        Files.writeString(
                FILE_PATH,
                userData,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

        return true;
    }

    public static boolean authenticate(
            String username,
            char[] password) throws IOException {

        if (!Files.exists(FILE_PATH)) {
            return false;
        }

        List<String> users =
                Files.readAllLines(FILE_PATH);

        for (String line : users) {

            String[] parts = line.split("\\|", -1);

            if (parts.length != 3) {
                continue;
            }

            if (!parts[0].equalsIgnoreCase(username.trim())) {
                continue;
            }

            try {

                byte[] salt =
                        Base64.getDecoder().decode(parts[1]);

                byte[] storedHash =
                        Base64.getDecoder().decode(parts[2]);

                byte[] enteredHash =
                        hashPassword(password, salt);

                return MessageDigest.isEqual(
                        storedHash,
                        enteredHash
                );

            } catch (IllegalArgumentException ex) {

                return false;
            }
        }

        return false;
    }
}
