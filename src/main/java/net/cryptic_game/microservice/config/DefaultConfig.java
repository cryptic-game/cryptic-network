package net.cryptic_game.microservice.config;

import net.cryptic_game.microservice.sql.SqlServerType;

import java.util.HashMap;
import java.util.Map;

public enum DefaultConfig {

    MSSOCKET_HOST("127.0.0.1"),
    MSSOCKET_PORT(1239),

    SQL_SERVER_TYPE(SqlServerType.MARIADB_10_03.toString()),
    SQL_SERVER_LOCATION("//localhost:3306"),
    SQL_SERVER_USERNAME("cryptic"),
    SQL_SERVER_PASSWORD("cryptic"),
    SQL_SERVER_DATABASE("cryptic"),

    PRODUCTIVE(true),
    STORAGE_LOCATION("data/"),
    LOG_LEVEL("WARN");


    private final Object value;

    DefaultConfig(Object value) {
        this.value = value;
    }

    public static Map<String, String> defaults() {
        Map<String, String> defaults = new HashMap<String, String>();

        for (DefaultConfig e : DefaultConfig.values()) {
            defaults.put(e.toString(), e.getValue().toString());
        }

        return defaults;
    }

    public Object getValue() {
        return value;
    }

}
