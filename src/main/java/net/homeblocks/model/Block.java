package net.homeblocks.model;

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Block {
    private final String type;
    private final int posx;
    private final int posy;
    private final String title;
    private final String description;
    private final List<Link> links;

    public Block(String type, int posx, int posy, String title, String description, List<Link> links) {
        this.type = type;
        this.posx = posx;
        this.posy = posy;
        this.title = title;
        this.description = description;
        this.links = links;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("type", this.type)
                .put("posx", this.posx)
                .put("posy", this.posy)
                .put("title", this.title)
                .put("description", this.description)
                .put("links", this.links.stream().map(Link::toJson).collect(Collectors.toList()));
    }

    public static Block fromJson(JsonObject json) {
        List<Link> links = List.of();
        var jsonArray = json.getJsonArray("links");
        if (jsonArray != null) {
            links = jsonArray.stream().map(link -> {
                if (link instanceof JsonObject) {
                    return Link.fromJson((JsonObject) link);
                }
                throw new RuntimeException("Expected JsonObject in List");
            }).collect(Collectors.toList());
        }
        return new Block(
                json.getString("type"),
                json.getInteger("posx"),
                json.getInteger("posy"),
                json.getString("title"),
                json.getString("description"),
                links
        );
    }

    public static Block emptyMain() {
        return new Block("main", 0, 0, "", "", List.of());
    }
}
