package net.homeblocks;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import net.homeblocks.server.Server;
import net.homeblocks.services.ProfileService;
import net.homeblocks.services.UserService;

public class MainVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        var userService = new UserService(vertx, Server.FS_ROOT);
        var profileService = new ProfileService(vertx, userService);
        Server.start(vertx, userService, profileService, startPromise);
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle()).onFailure(Throwable::printStackTrace);
    }
}
