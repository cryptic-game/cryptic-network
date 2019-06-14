package net.cryptic_game.microservice.network.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;

import net.cryptic_game.microservice.endpoint.MicroserviceEndpoint;
import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.model.Invitation;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;

public class NetworkEndpoint {

	@UserEndpoint(path = { "member" }, keys = { "device" }, types = { String.class })
	public static JSONObject getAll(JSONObject data, UUID user) {
		UUID device = UUID.fromString((String) data.get("device"));

		List<Network> networks = Member.getNetworks(device);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();
		List<JSONObject> jsonNetworks = new ArrayList<JSONObject>();

		for (Network network : networks) {
			jsonNetworks.add(network.serialize());
		}

		jsonMap.put("networks", jsonNetworks);

		return new JSONObject(jsonMap);
	}

	@UserEndpoint(path = { "get" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject getByUUID(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		return Network.get(uuid).serialize();
	}

	@UserEndpoint(path = { "public" }, keys = {}, types = {})
	public static JSONObject getAllPublicNetworks(JSONObject data, UUID user) {
		List<Network> networks = Network.getPublicNetworks();

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();
		List<JSONObject> jsonNetworks = new ArrayList<JSONObject>();

		for (Network network : networks) {
			jsonNetworks.add(network.serialize());
		}

		jsonMap.put("networks", jsonNetworks);

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

	@UserEndpoint(path = { "delete" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject delete(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		Network network = Network.get(uuid);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();

		if (network == null || !network.getOwner().equals(user)) {
			jsonMap.put("error", "network_not_found");
			jsonMap.put("result", false);

			return new JSONObject(jsonMap);
		}

		network.delete();

		jsonMap.put("result", true);

		return new JSONObject(jsonMap);
	}

	@UserEndpoint(path = { "invite" }, keys = { "uuid", "device" }, types = { String.class, String.class })
	public static JSONObject invite(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();

		if (network == null || !network.getOwner().equals(user)) {
			jsonMap.put("error", "network_not_found");
			jsonMap.put("result", false);

			return new JSONObject(jsonMap);
		}

		Invitation invitation = Invitation.invite(device, network.getUUID());

		return invitation.serialize();
	}

	@UserEndpoint(path = { "request" }, keys = { "uuid", "device" }, types = { String.class, String.class })
	public static JSONObject request(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();

		if (network == null) {
			jsonMap.put("error", "network_not_found");
			jsonMap.put("result", false);

			return new JSONObject(jsonMap);
		}

		// contact cryptic-device to proof that this user is owner of that device

		Invitation invitation = Invitation.request(device, network.getUUID());

		return invitation.serialize();
	}

	@UserEndpoint(path = { "accept" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject accept(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		Invitation invitation = Invitation.getInvitation(uuid);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();

		if (invitation == null) {
			jsonMap.put("error", "invitation_not_found");
			jsonMap.put("result", false);

			return new JSONObject(jsonMap);
		}

		// contact cryptic-device to proof that this user is owner of that device
		if (invitation.isRequest()) {
			// owner
		} else {
			// user
		}

		invitation.accept();

		jsonMap.put("result", true);

		return new JSONObject(jsonMap);
	}

	@UserEndpoint(path = { "deny" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject deny(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		Invitation invitation = Invitation.getInvitation(uuid);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();

		if (invitation == null) {
			jsonMap.put("error", "invitation_not_found");
			jsonMap.put("result", false);

			return new JSONObject(jsonMap);
		}

		// contact cryptic-device to proof that this user is owner of that device
		if (invitation.isRequest()) {
			// owner
		} else {
			// user
		}

		invitation.deny();

		jsonMap.put("result", true);

		return new JSONObject(jsonMap);
	}

	@UserEndpoint(path = { "kick" }, keys = { "uuid", "device" }, types = { String.class, String.class })
	public static JSONObject kick(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		HashMap<String, Object> jsonMap = new HashMap<String, Object>();

		if (network == null || !network.getOwner().equals(user)) {
			jsonMap.put("error", "invitation_not_found");
			jsonMap.put("result", false);

			return new JSONObject(jsonMap);
		}

		// contact cryptic-device to proof that this user is owner of that device
		for (Member member : Member.getMembers(network.getUUID())) {
			if (member.getDevice().equals(device)) {
				member.delete();

				jsonMap.put("result", true);

				return new JSONObject(jsonMap);
			}
		}

		jsonMap.put("result", false);

		return new JSONObject(jsonMap);
	}

	@MicroserviceEndpoint(path = { "check" }, keys = { "source", "destination" }, types = { String.class,
			String.class })
	public static JSONObject check(JSONObject data, UUID user) {
		UUID source = UUID.fromString((String) data.get("source"));
		UUID destination = UUID.fromString((String) data.get("destination"));

		Map<String, Object> jsonMap = new HashMap<String, Object>();

		for (Network network : Member.getNetworks(source)) {
			for (Member member : Member.getMembers(network.getUUID())) {
				if (member.getDevice().equals(destination)) {
					jsonMap.put("connected", true);

					return new JSONObject(jsonMap);
				}
			}
		}

		jsonMap.put("connected", false);

		return new JSONObject(jsonMap);
	}

}
