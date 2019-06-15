package net.cryptic_game.microservice.network.communication;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;

import net.cryptic_game.microservice.MicroService;

public class Device {

	private static boolean isOwner(UUID device, UUID user) {
		Map<String, Object> jsonMap = new HashMap<String, Object>();

		jsonMap.put("device_uuid", device.toString());

		JSONObject result = MicroService.getInstance().contactMicroservice("device", new String[] { "owner" },
				new JSONObject(jsonMap));

		return result.containsKey("owner") && UUID.fromString((String) result.get("owner")).equals(user);
	}

	private static boolean isPartOwner(UUID device, UUID user) {
		Map<String, Object> jsonMap = new HashMap<String, Object>();

		jsonMap.put("device_uuid", device.toString());
		jsonMap.put("user_uuid", user.toString());

		JSONObject result = MicroService.getInstance().contactMicroservice("service", new String[] { "check_part_owner" },
				new JSONObject(jsonMap));

		return result.containsKey("ok") && (boolean) result.get("ok");
	}
	
	public static boolean checkPermissions(UUID device, UUID user) {
		return isOwner(device, user) || isPartOwner(device, user);
	}

}
