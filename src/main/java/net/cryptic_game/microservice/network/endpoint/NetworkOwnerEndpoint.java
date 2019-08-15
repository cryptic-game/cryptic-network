package net.cryptic_game.microservice.network.endpoint;

import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.communication.Device;
import net.cryptic_game.microservice.network.model.Invitation;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;
import net.cryptic_game.microservice.utils.JSONUtils;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.cryptic_game.microservice.utils.JSONUtils.error;
import static net.cryptic_game.microservice.utils.JSONUtils.simple;

public class NetworkOwnerEndpoint {

    @UserEndpoint(path = { "owner" }, keys = { "device" }, types = { String.class })
    public static JSONObject getAll(JSONObject data, UUID user) {
        UUID device = UUID.fromString((String) data.get("device"));

        List<Network> networks = Network.getNetworks(device);

        List<JSONObject> jsonNetworks = new ArrayList<>();
        for (Network network : networks) {
            jsonNetworks.add(network.serialize());
        }

        return simple("networks", jsonNetworks);
    }

    @UserEndpoint(path = { "invite" }, keys = { "uuid", "device" }, types = { String.class, String.class })
    public static JSONObject invite(JSONObject data, UUID user) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));
        UUID device = UUID.fromString((String) data.get("device"));

        Network network = Network.get(uuid);

        if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
            return JSONUtils.error("network_not_found");
        }

        if(network.getOwner().equals(device)) {
            return error("already_member_of_network");
        }

        List<Member> members = Member.getMembers(uuid);
        for(Member member : members) {
            if(member.getDevice().equals(device)) {
                return error("already_member_of_network");
            }
        }

        Invitation invitation = Invitation.invite(device, network.getUUID());

        return invitation.serialize();
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

    @UserEndpoint(path = { "requests" }, keys = { "uuid" }, types = { String.class })
    public static JSONObject requests(JSONObject data, UUID user) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));

        Network network = Network.get(uuid);

        if(network == null || !Device.checkPermissions(network.getOwner(), user)) {
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

        if (network == null || !Device.checkPermissions(network.getOwner(), user) || !Device.checkPermissions(network.getOwner(), user)){
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
}
