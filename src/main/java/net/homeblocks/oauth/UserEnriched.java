package net.homeblocks.oauth;

import io.vertx.ext.auth.User;

public record UserEnriched(User oAuthUser, String id) {}
