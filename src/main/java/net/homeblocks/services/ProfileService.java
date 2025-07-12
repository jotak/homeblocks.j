package net.homeblocks.services;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import net.homeblocks.model.Page;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileService {
    private final FileSystem fs;
    private final UserService userService;

    public ProfileService(Vertx vertx, UserService userService) {
        this.fs = vertx.fileSystem();
        this.userService = userService;
    }

    private File userPath(int userID) {
        return Paths.get(this.userService.userDir.getPath(), String.valueOf(userID)).toFile();
    }

    private File profilePath(int userID, String profile) {
        return Paths.get(this.userService.userDir.getPath(), String.valueOf(userID), profile + ".json").toFile();
    }

    public List<String> list(int userID) {
        var path = userPath(userID);
        var files = path.listFiles();
        if (files != null) {
            return Arrays.stream(files).filter(File::isFile).map(f -> {
                String fileName = f.getName();
                int dotIndex = fileName.lastIndexOf('.');
                return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            }).collect(Collectors.toList());
        }
        return List.of();
    }

    public Page load(int userID, String profile) {
        var path = profilePath(userID, profile);
        if (path.isFile()) {
            var pageJson = fs.readFileBlocking(path.getAbsolutePath()).toJsonObject();
            return Page.fromJson(pageJson);
        }
        throw new RuntimeException("Can't load profile: file not found");
    }

    public Page createEmpty(int userID, String profile) {
        userPath(userID).mkdirs();
        var path = profilePath(userID, profile);
        if (path.exists()) {
            throw new RuntimeException("Trying to create '" + path + "', but it already exists");
        }
        var page = Page.empty();
        fs.writeFileBlocking(path.getAbsolutePath(), page.toJson().toBuffer());
        return page;
    }

    public void update(int userID, String profile, Page page) {
        userPath(userID).mkdirs();
        var path = profilePath(userID, profile);
        if (!path.exists()) {
            throw new RuntimeException("Could not retrieve the profile to update");
        }
        fs.writeFileBlocking(path.getAbsolutePath(), page.toJson().toBuffer());
    }
}
