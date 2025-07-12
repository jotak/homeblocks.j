package net.homeblocks.server;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import net.homeblocks.oauth.Provider;
import net.homeblocks.services.ProfileService;
import net.homeblocks.services.UserService;

import java.nio.file.Paths;

public class Server {
    public final static String FS_ROOT = "..";

    public static void start(Vertx vertx, UserService userService, ProfileService profileService, Promise<Void> startFuture) {
        var oAuthProviders = Provider.loadProviders(FS_ROOT, vertx);
        var router = new Routes(vertx, userService, profileService, oAuthProviders).getRouter();

        var opts = loadOptions(vertx);
        if (opts.tlsCertPath() != null && opts.tlsKeyPath() != null) {
            vertx.createHttpServer(new HttpServerOptions()
                    .setSsl(true).setKeyCertOptions(new PemKeyCertOptions()
                            .setKeyPath(opts.tlsKeyPath())
                            .setCertPath(opts.tlsCertPath())
                    )).requestHandler(router).listen(opts.tlsPort(), http -> {
                if (http.succeeded()) {
                    startFuture.complete();
                    System.out.println("HTTPS server started on port " + opts.tlsPort());
                } else {
                    startFuture.fail(http.cause());
                }
            });

            // Keep listening on 80, and redirect
            vertx.createHttpServer().requestHandler(it -> {
                it.response()
                        .setStatusCode(301)
                        .putHeader("Location", it.absoluteURI().replace("http", "https"))
                        .end();
            }).listen(opts.clearPort(), http -> {
                if (http.succeeded()) {
                    System.out.println("HTTP server started, redirecting to HTTPS");
                } else {
                    System.out.println(http.cause());
                }
            });
        } else {
            // No TLS
            // Keep listening on 80, and redirect
            vertx.createHttpServer().requestHandler(router).listen(opts.clearPort(), http -> {
                if (http.succeeded()) {
                    startFuture.complete();
                    System.out.println("NO TLS! HTTP server started on port " + opts.clearPort());
                } else {
                    startFuture.fail(http.cause());
                }
            });
        }
    }

    private static ServerOptions loadOptions(Vertx vertx) {
        var fs = vertx.fileSystem();
        var json = fs.readFileBlocking(Paths.get(FS_ROOT, "server.json").toFile().getAbsolutePath()).toJsonObject();
        var clearPort = json.getInteger("clearPort", 80);
        var tlsPort = json.getInteger("tlsPort", 443);
        var tlsCertPath = json.getString("tlsCertPath");
        var tlsKeyPath = json.getString("tlsKeyPath");
        return new ServerOptions(clearPort, tlsPort, tlsCertPath, tlsKeyPath);
    }
}
