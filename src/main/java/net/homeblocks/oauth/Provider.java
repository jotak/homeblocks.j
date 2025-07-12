package net.homeblocks.oauth;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class Provider {
    public final String name;
    public final String displayName;
    public final String redirectURI;
    public final OAuth2Auth oAuth2;

    public Provider(String name, String displayName, String redirectURI, OAuth2Auth oAuth2) {
        this.name = name;
        this.displayName = displayName;
        this.redirectURI = redirectURI;
        this.oAuth2 = oAuth2;
    }

    public abstract String authorizeURL(String state);
    public abstract Future<String> getUID(User user);

    public Future<UserEnriched> authenticate(String code) {
        return oAuth2.authenticate(new Oauth2Credentials().setCode(code).setRedirectUri(redirectURI))
                .flatMap(u -> getUID(u).map(id -> new UserEnriched(u, id)));
    }

    public static Provider createProvider(File file, Vertx vertx, FileSystem fs) {
        var descriptor = fs.readFileBlocking(file.getAbsolutePath()).toJsonObject();
        var type = descriptor.getString("type");
        var name = descriptor.getString("shortName");
        var displayName = descriptor.getString("displayName");
        var redirectURI = descriptor.getString("redirectURI");
        var config = descriptor.getJsonObject("config");
        var oauth2 = OAuth2Auth.create(vertx, new OAuth2Options(config));
        switch (type) {
            case "github":
                return new Github(name, displayName, redirectURI, oauth2);
        }
        System.err.println("Unrecognized OAuth provider: " + type);
        return null;
    }

    public static List<Provider> loadProviders(String fsRoot, Vertx vertx) {
        var fs = vertx.fileSystem();
        var files = Paths.get(fsRoot, "oauth").toFile().listFiles();
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files).filter(File::isFile)
                .map(f -> createProvider(f, vertx, fs))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
