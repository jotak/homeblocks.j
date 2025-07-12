package net.homeblocks.services;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.homeblocks.model.UserInfo;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class UserService {
    private final FileSystem fs;
    private final File fsRoot;
    final File userDir;
    private final File indexFile;
    private final Map<Integer, UserInfo> usersIndex = new HashMap<>();
    private final Map<String, UserInfo> aliasUsersIndex = new HashMap<>();
    private final Map<String, UserInfo> providerUsersIndex = new HashMap<>();
    private final AtomicInteger maxIdx = new AtomicInteger(0);

    public UserService(Vertx vertx, String strRoot) {
        this.fs = vertx.fileSystem();
        this.fsRoot = new File(strRoot);
        this.userDir = Paths.get(strRoot, "users").toFile();
        this.indexFile = Paths.get(strRoot, "users", "_index.json").toFile();
        init();
    }

    private void init() {
        userDir.mkdirs();

        // Read users index
        if (!indexFile.exists()) {
            fs.writeFileBlocking(indexFile.getAbsolutePath(), Buffer.buffer("[]"));
        } else {
            var users = fs.readFileBlocking(indexFile.getAbsolutePath()).toJsonArray();
            users.forEach(it -> {
                if (it instanceof JsonObject) {
                    var userInfo = UserInfo.fromJson((JsonObject) it);
                    var provKey = providerKey(userInfo);
                    if (!usersIndex.containsKey(userInfo.intIdx)
                            && !aliasUsersIndex.containsKey(userInfo.name)
                            && !providerUsersIndex.containsKey(provKey)) {
                        usersIndex.put(userInfo.intIdx, userInfo);
                        aliasUsersIndex.put(userInfo.name, userInfo);
                        providerUsersIndex.put(provKey, userInfo);
                        if (userInfo.intIdx > maxIdx.get()) {
                            maxIdx.set(userInfo.intIdx);
                        }
                    } else {
                        System.out.println("Cannot load index for user " + userInfo.intIdx + " (" + userInfo.name
                                + "), index or alias already used");
                    }
                } else {
                    System.out.println("Users index corrupted, object expected but got: " + it.toString());
                }
            });
        }
    }

    private static String providerKey(UserInfo userInfo) {
        return userInfo.prov + "-" + userInfo.provUId;
    }

    private boolean isAliasAvailable(String userAlias) {
        if (userAlias.startsWith("@user")) {
            // Reserved for internal alias generation
            return false;
        }
        return !aliasUsersIndex.containsKey(userAlias);
    }

    private void writeUsersIndex() {
        var arr = new JsonArray(usersIndex.values().stream().map(UserInfo::toJson).collect(Collectors.toList()));
        fs.writeFileBlocking(indexFile.getAbsolutePath(), arr.toBuffer());
    }

    private UserInfo updateUsersIndex(UserInfo userInfo) {
        var old = usersIndex.get(userInfo.intIdx);
        if (old != null) {
            aliasUsersIndex.remove(old.name);
            providerUsersIndex.remove(providerKey(old));
        }
        usersIndex.put(userInfo.intIdx, userInfo);
        aliasUsersIndex.put(userInfo.name, userInfo);
        providerUsersIndex.put(providerKey(userInfo), userInfo);
        writeUsersIndex();
        return userInfo;
    }

    public UserInfo findOrCreate(String provider, String provUID) {
        var userInfo = providerUsersIndex.get(provider + "-" + provUID);
        if (userInfo != null) {
            return userInfo;
        }
        var id = maxIdx.addAndGet(1);
        var newUserInfo = new UserInfo(provider, provUID, id, "@user" + id);
        return updateUsersIndex(newUserInfo);
    }

    public UserInfo saveAlias(int id, String userAlias) {
        var oldUser = usersIndex.get(id);
        if (oldUser != null) {
            if (isAliasAvailable(userAlias)) {
                var newUser = new UserInfo(oldUser.prov, oldUser.provUId, id, userAlias);
                return updateUsersIndex(newUser);
            }
            return oldUser;
        }
        throw new RuntimeException("Could not find existing user");
    }

    private UserInfo findById(int id) {
        return usersIndex.get(id);
    }

    public UserInfo findByAlias(String name) {
        return aliasUsersIndex.get(name);
    }
}
