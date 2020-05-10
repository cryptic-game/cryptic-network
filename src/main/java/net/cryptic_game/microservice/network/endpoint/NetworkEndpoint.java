package net.cryptic_game.microservice.network.endpoint;

import net.cryptic_game.microservice.endpoint.MicroserviceEndpoint;
import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.communication.Device;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.cryptic_game.microservice.utils.JSONUtils.error;
import static net.cryptic_game.microservice.utils.JSONUtils.simple;

public class NetworkEndpoint {

	@UserEndpoint(path = { "name" }, keys = { "name" }, types = { String.class })
	public static JSONObject getByName(JSONObject data, UUID user) {
	    String name = (String) data.get("name");

	    Network network =  Network.getNetworkByName(name);
	    if(network == null) {
	    	error("network_not_found");
		}

		return network.serialize();
	}

	@UserEndpoint(path = { "get" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject getByUUID(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		Network network =  Network.get(uuid);
		if(network == null) {
			error("network_not_found");
		}
		
		assert network != null;
		return network.serialize();
	}

	@UserEndpoint(path = { "public" }, keys = {}, types = {})
	public static JSONObject getAllPublicNetworks(JSONObject data, UUID user) {
		List<Network> networks = Network.getPublicNetworks();

		List<JSONObject> jsonNetworks = new ArrayList<>();

		for (Network network : networks) {
			jsonNetworks.add(network.serialize());
		}

		assert network != null;
		return simple("networks", jsonNetworks);
	}

	@UserEndpoint(path = { "create" }, keys = { "device", "name", "hidden" }, types = { String.class, String.class,
			Boolean.class })
	public static JSONObject create(JSONObject data, UUID user) {
		UUID device = UUID.fromString((String) data.get("device"));
		String name = (String) data.get("name");
		boolean hidden = (boolean) data.get("hidden");

		if (Device.checkPermissions(device, user)) {

			int count = Network.getCountOfNetworksByDevice(device);

			if (count >= 2) { // maximum 2 networks ownership per device
				return error("maximum_networks_reached");
			}

			if (!Network.checkName(name)) {
				return error("invalid_name");
			}

			if (Network.getNetworkByName(name) != null) {
				return error("name_already_in_use");
			}

			Network network = Network.create(device, name, hidden);

			return network.serialize();
		} else {
			return error("no_permissions");
		}
	}

	@MicroserviceEndpoint(path = { "check" }, keys = { "source", "destination" }, types = { String.class,
			String.class })
	public static JSONObject check(JSONObject data, UUID user) {
		UUID source = UUID.fromString((String) data.get("source"));
		UUID destination = UUID.fromString((String) data.get("destination"));

		for (Network network : Member.getNetworks(source)) {
			for (Member member : Member.getMembers(network.getUUID())) {
				if (member.getDevice().equals(destination)) {
					return simple("connected", true);
				}
			}
		}

		return simple("connected", false);
	}

}
