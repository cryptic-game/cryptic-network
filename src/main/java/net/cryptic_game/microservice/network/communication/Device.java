package net.cryptic_game.microservice.network.communication;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.microservice.utils.JSON;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.json.simple.JSONObject;

import java.util.UUID;

import static net.cryptic_game.microservice.utils.JSONBuilder.simple;

public class Device {

    private static boolean isOwner(UUID device, UUID user) {
        JSON result = new JSON(MicroService.getInstance().contactMicroService("device", new String[]{"owner"},
                simple("device_uuid", device.toString())));

        UUID owner = result.getUUID("owner");

        return owner != null && owner.equals(user);
    }

    private static boolean isPartOwner(UUID device, UUID user) {
        JSON result = new JSON(MicroService.getInstance().contactMicroService("service", new String[]{"check_part_owner"},
                JSONBuilder.anJSON().add("device_uuid", device.toString()).add("user_uuid", user.toString()).build()));

        Boolean ok = result.get("ok", Boolean.class);

        return ok != null && ok;
    }

    public static boolean checkPermissions(UUID device, UUID user) {
        return isOwner(device, user) || isPartOwner(device, user);
    }

    /**
     * Checks if a Device is online.
     *
     * @param device the {@link UUID} of the Device.
     * @return <code>true</code>: The Device is Online.<br><code>false</code>: The Device is Offline or can't be found.
     */
    public static boolean isOnline(final UUID device) {
        final JSONObject result = MicroService.getInstance().contactMicroService("device", new String[]{"ping"},
                simple("device_uuid", device.toString()));

        if (result.containsKey("online")) {
            return (boolean) result.get("online");
        }

        return false;
    }
}
