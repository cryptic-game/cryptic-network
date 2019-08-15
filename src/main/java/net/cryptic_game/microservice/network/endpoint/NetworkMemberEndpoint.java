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

import static net.cryptic_game.microservice.utils.JSONUtils.simple;

public class NetworkMemberEndpoint {

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

    @UserEndpoint(path = { "request" }, keys = { "uuid", "device" }, types = { String.class, String.class })
    public static JSONObject request(JSONObject data, UUID user) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));
        UUID device = UUID.fromString((String) data.get("device"));

        Network network = Network.get(uuid);

        if (network == null) {
            return JSONUtils.error("network_not_found");
        }

        if (Device.checkPermissions(device, user)) {

            if(network.getOwner().equals(device)) {
                return error("already_member_of_network");
            }

            List<Member> members = Member.getMembers(uuid);
            for(Member member : members) {
                if (member.getDevice().equals(device)) {
                    return error("already_member_of_network");
                }
            }

            Invitation invitation = Invitation.request(device, network.getUUID());

            return invitation.serialize();
        } else {
            return JSONUtils.error("no_permissions");
        }
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

}
