package net.homeblocks.server;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import net.homeblocks.model.Page;
import net.homeblocks.model.Pair;
import net.homeblocks.model.Profiles;
import net.homeblocks.model.UserInfo;
import net.homeblocks.oauth.Provider;
import net.homeblocks.services.ProfileService;
import net.homeblocks.services.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Routes {
    private final UserService userService;
    private final ProfileService profileService;
    private final List<Provider> oauthProviders;
    private final Map<String, JsonObject> tmpStates = new HashMap<>();
    private final Router router;

    public Routes(Vertx vertx, UserService userService, ProfileService profileService, List<Provider> oauthProviders) {
        this.userService = userService;
        this.profileService = profileService;
        this.oauthProviders = oauthProviders;
        this.router = Router.router(vertx);

        var store = LocalSessionStore.create(vertx);
        router.route().handler(SessionHandler.create(store));

        // Login endpoints
        router.get("/api/login").handler(this::getLoginPage);
        router.post("/api/login").handler(this::postLoginPage);
        router.get("/api/logout").handler(this::logout);
        router.get("/api/logged").handler(this::getLogged);

        // Oauth endpoints
        oauthProviders.forEach(prov -> router.get("/oauthclbk-" + prov.name).handler(it -> this.providerCallback(it, prov)));

        // API endpoints
        router.get("/api/user/:user").handler(this::getUser);
        router.get("/api/user/:user/profile/:name").handler(this::getProfile);
        router.put("/api/user/:user/profile/:name").handler(this::createProfile);
        router.post("/api/user/:user/profile/:name").handler(this::updateProfile);
        router.put("/api/alias/:alias").handler(this::setAlias);

        // Serve static
        router.route("/*").handler(StaticHandler.create("public"));
    }

    public Router getRouter() {
        return this.router;
    }

    private static void error(RoutingContext ctx, int errorCode, String msg) {
        ctx.response().setStatusCode(errorCode);
        ctx.response().end(msg);
    }

    private static HttpUser getLoggedUser(RoutingContext ctx) {
        var s = ctx.session();
        if (s == null) {
            return null;
        }
        return s.get("user");
    }

    private static String getLoggedUserName(RoutingContext ctx) {
        var logged = getLoggedUser(ctx);
        if (logged != null) {
            return logged.userInfo().name();
        }
        return null;
    }

    private static boolean isValidLoggedUser(RoutingContext ctx, UserInfo user) {
      var logged = getLoggedUser(ctx);
      if (logged != null) {
        if (user.intIdx() == logged.userInfo().intIdx()) {
          return !logged.oAuthUser().expired();
        }
      }
      return false;
    }

    private static void updateLoggedUser(RoutingContext ctx, UserInfo newUser) {
        var logged = getLoggedUser(ctx);
        if (logged != null) {
            if (newUser.intIdx() == logged.userInfo().intIdx()) {
                var session = ctx.session();
                if (session != null) {
                    session.put("user", new HttpUser(logged.oAuthUser(), newUser, logged.stateToken()));
                }
            }
        }
    }

    private List<Pair<String, String>> buildLoginPageInfo(JsonObject json) {
        var state = UUID.randomUUID().toString();
        tmpStates.put(state, json);
        return oauthProviders.stream().map(it -> {
            var authorizationUrl = it.authorizeURL(state);
            return new Pair<>("Login with " + it.displayName, authorizationUrl);
        }).collect(Collectors.toList());
    }

    private void getLoginPage(RoutingContext ctx) {
        ctx.response().end(Profiles.login(buildLoginPageInfo(new JsonObject())).toString());
    }

    private void postLoginPage(RoutingContext ctx) {
        try {
            ctx.request().bodyHandler(b ->
                    ctx.response().end(Profiles.singleBlockLogin(buildLoginPageInfo(b.toJsonObject())).toString())
            );
        } catch (Throwable e) {
            error(ctx, 500, e.toString());
            e.printStackTrace();
        }
    }

    private void logout(RoutingContext ctx) {
        var user = getLoggedUser(ctx);
        if (user != null) {
            ctx.session().remove("user");
        }
        ctx.response().end();
    }

    private JsonObject popState(String state) {
        var obj = tmpStates.get(state);
        if (obj != null) {
            tmpStates.remove(state);
            return obj;
        }
        return null;
    }

    private void getLogged(RoutingContext ctx) {
        var logged = getLoggedUser(ctx);
        if (logged != null) {
            var json = popState(logged.stateToken());
            if (json != null) {
                json.put("logged", logged.userInfo().name());
                ctx.response().end(json.toString());
            } else {
                ctx.response().end(new JsonObject().put("logged", logged.userInfo().name()).toString());
            }
        } else {
            ctx.response().end();
        }
    }

    private void getUser(RoutingContext ctx) {
        var res = ctx.response();
        var user = ctx.request().getParam("user");
        if (user != null) {
            var userInfo = userService.findByAlias(user);
            if (userInfo != null) {
                var logged = getLoggedUserName(ctx);
                try {
                    var profiles = profileService.list(userInfo.intIdx());
                    res.end(Profiles.user(user, profiles, logged).toString());
                } catch (Throwable t) {
                    res.end(Profiles.notFound404().toString());
                    t.printStackTrace();
                }
            } else {
                res.end(Profiles.notFound404().toString());
            }
        }
    }

    private void getProfile(RoutingContext ctx) {
        var res = ctx.response();
        var user = ctx.request().getParam("user");
        var profile = ctx.request().getParam("name");
        if (user != null && profile != null) {
            var userInfo = userService.findByAlias(user);
            if (userInfo != null) {
                var logged = getLoggedUserName(ctx);
                try {
                    var page = profileService.load(userInfo.intIdx(), profile);
                    res.end(Profiles.page(user, profile, page, logged).toString());
                } catch (Throwable t) {
                    res.end(Profiles.notFound404().toString());
                    t.printStackTrace();
                }
            } else {
                res.end(Profiles.notFound404().toString());
            }
        }
    }

    private void createProfile(RoutingContext ctx) {
        var res = ctx.response();
        var user = ctx.request().getParam("user");
        var profile = ctx.request().getParam("name");
        if (user != null && profile != null) {
            var userInfo = userService.findByAlias(user);
            if (userInfo != null) {
                // Is still logged?
                if (isValidLoggedUser(ctx, userInfo)) {
                    var logged = getLoggedUserName(ctx);
                    try {
                        var page = profileService.createEmpty(userInfo.intIdx(), profile);
                        res.end(Profiles.page(user, profile, page, logged).toString());
                    } catch (Throwable t) {
                        error(ctx, 500, t.getMessage());
                        t.printStackTrace();
                    }
                } else {
                    error(ctx, 403, "You must log in");
                }
            } else {
                res.end(Profiles.notFound404().toString());
            }
        }
    }

    private void updateProfile(RoutingContext ctx) {
        var res = ctx.response();
        var user = ctx.request().getParam("user");
        var profile = ctx.request().getParam("name");
        if (user != null && profile != null) {
            var userInfo = userService.findByAlias(user);
            if (userInfo != null) {
                // Is still logged?
                if (isValidLoggedUser(ctx, userInfo)) {
                    try {
                        ctx.request().bodyHandler(it -> {
                            profileService.update(userInfo.intIdx(), profile, Page.fromJson(it.toJsonObject()));
                            res.end();
                        });
                    } catch (Throwable t) {
                        error(ctx, 500, t.getMessage());
                        t.printStackTrace();
                    }
                } else {
                    error(ctx, 403, "You must log in");
                }
            } else {
                error(ctx, 404, "User not found");
            }
        }
    }

    private void setAlias(RoutingContext ctx) {
        var res = ctx.response();
        var alias = ctx.request().getParam("alias");
        if (alias != null) {
            var logged = getLoggedUser(ctx);
            if (logged != null) {
                var userInfo = logged.userInfo();
                if (userInfo != null) {
                    // Is still logged?
                    if (isValidLoggedUser(ctx, userInfo)) {
                        try {
                            var newUser = userService.saveAlias(userInfo.intIdx(), alias);
                            // Update logged user
                            updateLoggedUser(ctx, newUser);
                            res.end(String.valueOf(newUser.name().equals(alias)));
                        } catch (Throwable t) {
                            error(ctx, 500, t.getMessage());
                            t.printStackTrace();
                        }
                    } else {
                        error(ctx, 403, "You must log in");
                    }
                } else {
                    error(ctx, 403, "You must log in");
                }
            } else {
                res.end(Profiles.notFound404().toString());
            }
        }
    }

    private void providerCallback(RoutingContext ctx, Provider prov) {
        var state = ctx.request().getParam("state");
        var code = ctx.request().getParam("code");
        if (state != null && code != null) {
            if (!tmpStates.containsKey(state)) {
                error(ctx, 403, "Invalid state");
            } else {
                prov.authenticate(code)
                        .onSuccess(user -> {
                            var userInfo = userService.findOrCreate(prov.name, user.id());
                            ctx.session().put("user", new HttpUser(user.oAuthUser(), userInfo, state));
                            ctx.reroute("/reroute.html");
                        })
                        .onFailure(err -> {
                            error(ctx, 403, err.getMessage());
                            err.printStackTrace();
                        });
            }
        } else {
            error(ctx, 403, "Authentication failure");
        }
    }
}
