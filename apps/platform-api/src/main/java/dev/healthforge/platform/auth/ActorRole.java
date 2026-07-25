package dev.healthforge.platform.auth;

import java.util.Locale;

public enum ActorRole {
    REVIEWER,
    ADMINISTRATOR;

    public static ActorRole parse(String value) {
        return ActorRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
