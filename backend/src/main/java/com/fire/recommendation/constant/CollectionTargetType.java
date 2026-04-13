package com.fire.recommendation.constant;

/**
 * user_collection.target_type，与 content_id 联合定位实体。
 */
public final class CollectionTargetType {

    public static final int KNOWLEDGE = 1;
    public static final int FORUM_POST = 2;
    public static final int NEWS = 3;
    public static final int EQUIPMENT = 4;

    private CollectionTargetType() {
    }

    /** 用户端 query：knowledge | forum | news | equipment */
    public static int fromModuleParam(String module) {
        if (module == null || module.isBlank()) {
            return KNOWLEDGE;
        }
        return switch (module.trim().toLowerCase()) {
            case "forum" -> FORUM_POST;
            case "news" -> NEWS;
            case "equipment" -> EQUIPMENT;
            default -> KNOWLEDGE;
        };
    }
}
