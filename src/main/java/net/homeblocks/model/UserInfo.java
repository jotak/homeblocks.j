package net.homeblocks.model;

import io.vertx.core.json.JsonObject;

public class UserInfo {
    public final String prov;
    public final String provUId;
    public final int intIdx;
    public final String name;

    public UserInfo(String prov, String provUId, int intIdx, String name) {
        this.prov = prov;
        this.provUId = provUId;
        this.intIdx = intIdx;
        this.name = name;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("prov", this.prov)
                .put("provUId", this.provUId)
                .put("intIdx", this.intIdx)
                .put("name", this.name);
    }

    public static UserInfo fromJson(JsonObject json) {
      return new UserInfo(json.getString("prov"), json.getString("provUId"), json.getInteger("intIdx"), json.getString("name"));
    }
}
