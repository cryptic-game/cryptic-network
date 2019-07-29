package net.cryptic_game.microservice.network.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.cryptic_game.microservice.utils.JSONUtils.error;
import static net.cryptic_game.microservice.utils.JSONUtils.simple;

import net.cryptic_game.microservice.utils.JSONUtils;
import org.json.simple.JSONObject;

import net.cryptic_game.microservice.endpoint.MicroserviceEndpoint;
import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.communication.Device;
import net.cryptic_game.microservice.network.model.Invitation;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;

public class NetworkEndpoint {

	@UserEndpoint(path = { "member" }, keys = { "device" }, types = { String.class })
	public static JSONObject getAll(JSONObject data, UUID user) {
		UUID device = UUID.fromString((String) data.get("device"));

		List<Network> networks = Member.getNetworks(device);

		List<JSONObject> jsonNetworks = new ArrayList<>();
		for (Network network : networks) {
			jsonNetworks.add(network.serialize());
		}

		return simple("networks", jsonNetworks);
	}

	@UserEndpoint(path = { "get" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject getByUUID(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		return Network.get(uuid).serialize();
	}

	@UserEndpoint(path = { "public" }, keys = {}, types = {})
	public static JSONObject getAllPublicNetworks(JSONObject data, UUID user) {
		List<Network> networks = Network.getPublicNetworks();

		List<JSONObject> jsonNetworks = new ArrayList<>();

		for (Network network : networks) {
			jsonNetworks.add(network.serialize());
		}

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

			if (count >= 3) { // maximum 3 networks per device
				return error("maximum_networks_reached");
			}
			Network network = Network.create(device, name, hidden);

			return network.serialize();
		} else {
			return error("no_permissions");
		}
	}

	@UserEndpoint(path = { "delete" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject delete(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		Network network = Network.get(uuid);
		
		if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
			return error("network_not_found");
		}

		network.delete();

		return simple("result", true);
	}

	@UserEndpoint(path = { "invite" }, keys = { "uuid", "device" }, types = { String.class, String.class })
	public static JSONObject invite(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
			return JSONUtils.error("network_not_found");
		}

		Invitation invitation = Invitation.invite(device, network.getUUID());

		return invitation.serialize();
	}

	@UserEndpoint(path = { "request" }, keys = { "uuid", "device" }, types = { String.class, String.class })
	public static JSONObject request(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		if (network == null) {
			return JSONUtils.error("network_not_found");
		}

		if (Device.checkPermissions(device, user)) {
			Invitation invitation = Invitation.request(device, network.getUUID());

			return invitation.serialize();
		} else {
			return JSONUtils.error("no_permissions");
		}
	}

	@UserEndpoint(path = { "accept" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject accept(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		Invitation invitation = Invitation.getInvitation(uuid);

		if (invitation == null) {
			return error("invitation_not_found");
		}

		if (!invitation.isRequest()) {
			if (!Device.checkPermissions(invitation.getDevice(), user)) {
				return error("no_permissions");
			}
		} else {
			if (!Device.checkPermissions(Network.get(invitation.getNetwork()).getOwner(), user)) {
				return error("no_permissions");
			}
		}

		invitation.accept();

		return simple("result", true);
	}

	@UserEndpoint(path = { "deny" }, keys = { "uuid" }, types = { String.class })
	public static JSONObject deny(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));

		Invitation invitation = Invitation.getInvitation(uuid);

		if (invitation == null) {
			return error("invitation_not_found");
		}

		if (invitation.isRequest()) {
			if (!Device.checkPermissions(Network.get(invitation.getNetwork()).getOwner(), user)) {
				return error("no_permissions");
			}
		} else {
			if (!Device.checkPermissions(invitation.getDevice(), user)) {
				return error("no_permissions");
			}
		}
		invitation.deny();

		return simple("result", true);
	}

	@UserEndpoint(path = { "invitations" }, keys = { "device" }, types = { String.class })
	public static JSONObject invitations(JSONObject data, UUID user) {
		UUID device = UUID.fromString((String) data.get("device"));

		if(!Device.checkPermissions(device, user)) {
			return JSONUtils.error("no_permissions");
		}

		List<JSONObject> invitations = new ArrayList<>();

		for(Invitation invitation : Invitation.getInvitationsOfDevice(device, false)) {
			invitations.add(invitation.serialize());
		}

		return JSONUtils.simple("invitations", invitations);
	}

	@UserEndpoint(path = { "requests" }, keys = { "uuid", "device" }, types = { String.class, String.class })
	public static JSONObject requests(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		if(network == null || !Device.checkPermissions(device, user) || !network.getOwner().equals(device)) {
			return JSONUtils.error("no_permissions");
		}

		List<JSONObject> invitations = new ArrayList<>();

		for(Invitation invitation : Invitation.getInvitationsOfNetwork(uuid, true)) {
			invitations.add(invitation.serialize());
		}

		return JSONUtils.simple("requests", invitations);
	}

	@UserEndpoint(path = { "kick" }, keys = { "uuid", "device" }, types = { String.class, String.class })
	public static JSONObject kick(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		if (network == null || !Device.checkPermissions(network.getOwner(), user) || !network.getOwner().equals(network)) {
			return error("no_permissions");
		}

		for (Member member : Member.getMembers(network.getUUID())) {
			if (member.getDevice().equals(device)) {
				member.delete();

				return simple("result", true);
			}
		}

		return simple("result", false);
	}

	@UserEndpoint(path = {"leave"}, keys = {"uuid", "device"}, types = {String.class, String.class})
	public static JSONObject leave(JSONObject data, UUID user) {
		UUID uuid = UUID.fromString((String) data.get("uuid"));
		UUID device = UUID.fromString((String) data.get("device"));

		Network network = Network.get(uuid);

		if(network == null || !Device.checkPermissions(device, user)) {
			return JSONUtils.error("no_permissions");
		}

		for(Member member : Member.getMembers(network.getUUID())) {
			if(member.getDevice().equals(device)) {
				member.delete();

				return simple("result", true);
			}
		}

		return simple("result", false);
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
