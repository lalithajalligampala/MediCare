
package com.medicare.storage;

import com.medicare.model.UserProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UserProfileStorage {

    private static final Path FILE_PATH =
            Path.of("data", "profile.txt");

    public static void saveProfile(UserProfile profile)
            throws IOException {

        Files.createDirectories(FILE_PATH.getParent());

        String data =
                profile.getName() + "\n" +
                profile.getAge() + "\n" +
                (profile.getContact() == null ? "" : profile.getContact());

        Files.writeString(FILE_PATH, data);
    }

    public static UserProfile loadProfile()
            throws IOException {

        if (!Files.exists(FILE_PATH)) {
            return null;
        }

        List<String> lines = Files.readAllLines(FILE_PATH);

        if (lines.size() < 2) {
            return null;
        }

        try {

            String name = lines.get(0);
            int age = Integer.parseInt(lines.get(1));

            String contact = lines.size() >= 3
                    ? lines.get(2)
                    : "";

            return new UserProfile(name, age, contact);

        } catch (NumberFormatException e) {
            return null;
        }
    }
}
