package net.homeblocks.model;

import io.vertx.core.json.JsonObject;

public class Link {
    private final String title;
    private final String url;
    private final String description;

    public Link(String title, String url, String description) {
        this.title = title;
        this.url = url;
        this.description = description;
    }

    public JsonObject toJson() {
        return new JsonObject().put("title", this.title).put("url", this.url).put("description", this.description);
    }

    public static Link fromJson(JsonObject json) {
        return new Link(json.getString("title"), json.getString("url"), json.getString("description"));
    }
}
