package net.homeblocks.model;

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Page {
    private final List<Block> blocks;

    public Page(List<Block> blocks) {
        this.blocks = blocks;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("blocks", this.blocks.stream().map(Block::toJson).collect(Collectors.toList()));
    }

    public static Page fromJson(JsonObject json) {
        List<Block> blocks = List.of();
        var jsonArray = json.getJsonArray("blocks");
        if (jsonArray != null) {
            blocks = jsonArray.stream().map(b -> {
                if (b instanceof JsonObject) {
                    return Block.fromJson((JsonObject) b);
                }
                throw new RuntimeException("Expected JsonObject in List");
            }).collect(Collectors.toList());
        }
        return new Page(blocks);
    }

    public static Page empty() {
        return new Page(List.of(Block.emptyMain()));
    }
}
