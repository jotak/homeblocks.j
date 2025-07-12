package net.homeblocks.model;

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Profiles {
    public static JsonObject notFound404() {
        var page = new Page(List.of(
            Block.emptyMain(),
            new Block("note", -1, -1, "", "<h3>404,<br/> Blocks not found!</h3><br/>Oops, looks like you entered a wrong URL", List.of()),
            new Block("links", 1, 1, "Try here", "", List.of(
                    new Link("homeblocks.net", "https://www.homeblocks.net/#/u", "Start page")
            ))
        ));
        return new JsonObject().put("title", "404").put("page", page.toJson());
    }

    public static JsonObject login(List<Pair<String, String>> authProvider) {
        var page = new Page(List.of(
            new Block("links", 1, 1, "Login", "", authProvider.stream().map(
                    e -> new Link(e.left(), e.right(), e.left())
            ).collect(Collectors.toList())),
            new Block("note", -1, -1, "", "<h3>Welcome to Homeblocks.net</h3><br/>Build your homepage, block after block!", List.of())
        ));
        return new JsonObject().put("title", "login").put("page", page.toJson());
    }

    public static JsonObject singleBlockLogin(List<Pair<String, String>> authProvider) {
        var page = new Page(List.of(
            new Block("links", 1, 1, "Login", "", authProvider.stream().map(
                    e -> new Link(e.left(), e.right(), e.left())
            ).collect(Collectors.toList()))
        ));
        return new JsonObject().put("title", "login").put("page", page.toJson());
    }

    public static JsonObject user(String refUser, List<String> profiles, String logged) {
        var page = new Page(List.of(
            Block.emptyMain(),
            new Block("links", 1, 0, "Profiles", "", profiles.stream().map(
                    p -> new Link(p, "#/u/" + refUser + "/" + p, "")
            ).collect(Collectors.toList()))
        ));
        return new JsonObject()
                .put("title", refUser + "'s place")
                .put("page", page.toJson())
                .put("refUser", refUser)
                .put("logged", logged);
    }

    public static JsonObject page(String refUser, String profile, Page page, String logged) {
        return new JsonObject()
                .put("title", refUser + "'s " + profile)
                .put("page", page.toJson())
                .put("refUser", refUser)
                .put("profile", profile)
                .put("logged", logged);
    }
}
