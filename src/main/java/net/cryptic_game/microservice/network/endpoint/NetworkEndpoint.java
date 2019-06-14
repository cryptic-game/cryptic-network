package net.cryptic_game.microservice.network.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;

public class NetworkEndpoint {

	@UserEndpoint(path = { "get", "member" }, keys = { "device" }, types = { UUID.class })
	public static JSONObject getAll(JSONObject data, UUID user) {
		UUID device = (UUID) data.get("device");

		List<Network> networks = Member.getNetworks(device);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();
		List<JSONObject> jsonNetworks = new ArrayList<JSONObject>();

		for (Network network : networks) {
			jsonNetworks.add(network.serialize());
		}

		jsonMap.put("networks", (JSONArray) jsonNetworks);

		return new JSONObject(jsonMap);
	}

	@UserEndpoint(path = { "get", "uuid" }, keys = { "uuid" }, types = { UUID.class })
	public static JSONObject getByUUID(JSONObject data, UUID user) {
		UUID uuid = (UUID) data.get("uuid");

		return Network.get(uuid).serialize();
	}

	@UserEndpoint(path = { "get", "public" }, keys = {}, types = {})
	public static JSONObject getAllPublicNetworks(JSONObject data, UUID user) {
		List<Network> networks = Network.getPublicNetworks();

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();
		List<JSONObject> jsonNetworks = new ArrayList<JSONObject>();

		for (Network network : networks) {
			jsonNetworks.add(network.serialize());
		}

		jsonMap.put("networks", (JSONArray) jsonNetworks);

		return new JSONObject(jsonMap);
	}

	@UserEndpoint(path = { "create" }, keys = { "name", "hidden" }, types = { String.class, Boolean.class })
	public static JSONObject create(JSONObject data, UUID user) {
		String name = (String) data.get("name");
		boolean hidden = (boolean) data.get("hidden");

		int count = Network.getCountOfNetworksByUser(user);

		if (count >= 3) { // maximum 3 networks per user -> can change to device
			Map<String, Object> jsonMap = new HashMap<String, Object>();

			jsonMap.put("error", "maximum_networks_reached");

			return new JSONObject(jsonMap);
		}
		Network network = Network.create(user, name, hidden);

		return network.serialize();
	}

	@UserEndpoint(path = { "delete" }, keys = { "uuid" }, types = { UUID.class })
	public static JSONObject delete(JSONObject data, UUID user) {
		UUID uuid = (UUID) data.get("uuid");

		Network network = Network.get(uuid);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();

		if (network == null || !network.getOwner().equals(user)) {
			jsonMap.put("error", "network_not_found");
			jsonMap.put("result", false);

			return new JSONObject(jsonMap);
		}

		jsonMap.put("result", true);

		return new JSONObject(jsonMap);
	}

}
