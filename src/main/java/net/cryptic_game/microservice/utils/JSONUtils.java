package net.cryptic_game.microservice.utils;

import org.json.simple.JSONObject;

public class JSONUtils {

    public static boolean checkData(String[] keys, Class<?>[] types, JSONObject obj) {
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            Class<?> type = types[i];

            if (!obj.containsKey(key) || obj.get(key).getClass() != type) {
                return false;
            }
        }

        return true;
    }

}
