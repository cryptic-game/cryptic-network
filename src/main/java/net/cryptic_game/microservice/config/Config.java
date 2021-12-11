package net.cryptic_game.microservice.config;

import java.util.Map;

public class Config {

    private static final Map<String, String> env = System.getenv();
    private static final Map<String, String> defaults = DefaultConfig.defaults();

    public static String get(String key) {
        if (env.containsKey(key)) {
            return env.get(key);
        } else if (defaults.containsKey(key)) {
            return defaults.get(key);
        }
        return null;
    }

    public static int getInteger(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static String get(DefaultConfig d) {
        return get(d.toString());
    }

    public static int getInteger(DefaultConfig d) {
        return getInteger(d.toString());
    }

    public static boolean getBoolean(DefaultConfig d) {
        return getBoolean(d.toString());
    }

}
