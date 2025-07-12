package net.homeblocks.server;

import io.vertx.ext.auth.User;
import net.homeblocks.model.UserInfo;

public record HttpUser(User oAuthUser, UserInfo userInfo, String stateToken) {}
