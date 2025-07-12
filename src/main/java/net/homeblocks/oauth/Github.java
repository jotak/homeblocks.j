package net.homeblocks.oauth;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

public class Github extends Provider {
    public Github(String name, String displayName, String redirectURI, OAuth2Auth oAuth2) {
        super(name, displayName, redirectURI, oAuth2);
    }

    public String authorizeURL(String state) {
        return super.oAuth2.authorizeURL(new JsonObject().put("state", state));
    }

    public Future<String> getUID(User user) {
        return oAuth2.userInfo(user).map(info -> info.getInteger("id").toString());
    };
}
