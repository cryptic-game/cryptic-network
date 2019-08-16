package net.cryptic_game.microservice.network.communication;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.json.simple.JSONObject;

import java.util.UUID;

public class Device {

    private static boolean isOwner(UUID device, UUID user) {
        JSONObject result = MicroService.getInstance().contactMicroService("device", new String[]{"owner"},
                JSONBuilder.simple("device_uuid", device.toString()));

        return result.containsKey("owner") && UUID.fromString((String) result.get("owner")).equals(user);
    }

    private static boolean isPartOwner(UUID device, UUID user) {
        JSONObject result = MicroService.getInstance().contactMicroService("service", new String[]{"check_part_owner"},
                JSONBuilder.anJSON().add("device_uuid", device.toString()).add("user_uuid", user.toString()).build());

        return result.containsKey("ok") && (boolean) result.get("ok");
    }

    public static boolean checkPermissions(UUID device, UUID user) {
        return isOwner(device, user) || isPartOwner(device, user);
    }

}
