package net.cryptic_game.microservice.network.endpoint;

import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.communication.Device;
import net.cryptic_game.microservice.network.model.Invitation;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;
import net.cryptic_game.microservice.utils.JSON;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.cryptic_game.microservice.utils.JSONBuilder.error;
import static net.cryptic_game.microservice.utils.JSONBuilder.simple;

public class NetworkMemberEndpoint {

    @UserEndpoint(path = {"member"}, keys = {"device"}, types = {String.class})
    public static JSONObject getAll(JSON data, UUID user) {
        UUID device = data.getUUID("device");

        List<Network> networks = Member.getNetworks(device);

        List<JSONObject> jsonNetworks = new ArrayList<>();
        for (Network network : networks) {
            jsonNetworks.add(network.serialize());
        }

        return simple("networks", jsonNetworks);
    }

    @UserEndpoint(path = {"request"}, keys = {"uuid", "device"}, types = {String.class, String.class})
    public static JSONObject request(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");
        UUID device = data.getUUID("device");

        Network network = Network.get(uuid);

        if (network == null) {
            return error("network_not_found");
        }

        if (Device.checkPermissions(device, user)) {

            if (network.getOwner().equals(device)) {
                return error("already_member_of_network");
            }

            List<Member> members = Member.getMembers(uuid);
            for (Member member : members) {
                if (member.getDevice().equals(device)) {
                    return error("already_member_of_network");
                }
            }

            Invitation invitation = Invitation.request(device, network.getUUID());

            return invitation.serialize();
        } else {
            return error("no_permissions");
        }
    }

    @UserEndpoint(path = {"invitations"}, keys = {"device"}, types = {String.class})
    public static JSONObject invitations(JSON data, UUID user) {
        UUID device = data.getUUID("device");

        if (!Device.checkPermissions(device, user)) {
            return error("no_permissions");
        }

        List<JSONObject> invitations = new ArrayList<>();

        for (Invitation invitation : Invitation.getInvitationsOfDevice(device, false)) {
            invitations.add(invitation.serialize());
        }

        return simple("invitations", invitations);
    }

    @UserEndpoint(path = {"leave"}, keys = {"uuid", "device"}, types = {String.class, String.class})
    public static JSONObject leave(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");
        UUID device = data.getUUID("device");

        Network network = Network.get(uuid);

        if (network == null || !Device.checkPermissions(device, user)) {
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

}
