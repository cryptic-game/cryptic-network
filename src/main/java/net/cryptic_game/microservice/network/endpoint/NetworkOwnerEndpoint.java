package net.cryptic_game.microservice.network.endpoint;

import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.Error;
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

public class NetworkOwnerEndpoint {

    @UserEndpoint(path = {"owner"}, keys = {"device"}, types = {String.class})
    public static JSONObject getAll(JSON data, UUID user) {
        UUID device = data.getUUID("device");

        if (!Device.isOnline(device)) {
            return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
        }

        List<Network> networks = Network.getNetworks(device);

        List<JSONObject> jsonNetworks = new ArrayList<>();
        for (Network network : networks) {
            jsonNetworks.add(network.serialize());
        }

        return simple("networks", jsonNetworks);
    }

    @UserEndpoint(path = {"invite"}, keys = {"uuid", "device"}, types = {String.class, String.class})
    public static JSONObject invite(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");
        UUID device = data.getUUID("device");

        if (!Device.isOnline(device)) {
            return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
        }

        Network network = Network.get(uuid);

        if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
            return error("network_not_found");
        }

        if (network.getOwner().equals(device)) {
            return error("already_member_of_network");
        }

        List<Member> members = Member.getMembers(uuid);
        for (Member member : members) {
            if (member.getDevice().equals(device)) {
                return error("already_member_of_network");
            }
        }

        List<Invitation> invitations = Invitation.getInvitationsOfDevice(device, false);
        for (Invitation inv : invitations) {
            if (inv.getNetwork().equals(uuid)) {
                return error("invitation_already_exists");
            }
        }

        Invitation invitation = Invitation.invite(device, network.getUUID());

        return invitation.serialize();
    }

    @UserEndpoint(path = {"accept"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject accept(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");

        Invitation invitation = Invitation.getInvitation(uuid);

        if (invitation == null) {
            return error("invitation_not_found");
        }

        if (!invitation.isRequest()) {
            final UUID device = invitation.getDevice();

            if (!Device.checkPermissions(device, user)) {
                return error("no_permissions");
            }

            if (!Device.isOnline(device)) {
                return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
            }
        } else {
            final UUID device = Network.get(invitation.getNetwork()).getOwner();

            if (!Device.checkPermissions(device, user)) {
                return error("no_permissions");
            }

            if (!Device.isOnline(device)) {
                return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
            }
        }

        invitation.accept();

        return simple("result", true);
    }

    @UserEndpoint(path = {"deny"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject deny(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");

        Invitation invitation = Invitation.getInvitation(uuid);

        if (invitation == null) {
            return error("invitation_not_found");
        }

        if (invitation.isRequest()) {
            final UUID device = Network.get(invitation.getNetwork()).getOwner();

            if (!Device.checkPermissions(device, user)) {
                return error("no_permissions");
            }

            if (!Device.isOnline(device)) {
                return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
            }
        } else {
            final UUID device = invitation.getDevice();

            if (!Device.checkPermissions(device, user)) {
                return error("no_permissions");
            }

            if (!Device.isOnline(device)) {
                return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
            }
        }

        invitation.deny();

        return simple("result", true);
    }

    @UserEndpoint(path = {"requests"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject requests(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");

        Network network = Network.get(uuid);

        if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
            return error("no_permissions");
        }

        if (!Device.isOnline(network.getOwner())) {
            return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
        }

        List<JSONObject> invitations = new ArrayList<>();

        for (Invitation invitation : Invitation.getInvitationsOfNetwork(uuid, true)) {
            invitations.add(invitation.serialize());
        }

        return simple("requests", invitations);
    }

    @UserEndpoint(path = {"invitations", "network"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject invitationsNetwork(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");

        Network network = Network.get(uuid);

        if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
            return error("no_permissions");
        }

        if (!Device.isOnline(network.getOwner())) {
            return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
        }

        List<JSONObject> invitations = new ArrayList<>();

        for (Invitation invitation : Invitation.getInvitationsOfNetwork(uuid, false)) {
            invitations.add(invitation.serialize());
        }

        return simple("invitations", invitations);
    }

    @UserEndpoint(path = {"kick"}, keys = {"uuid", "device"}, types = {String.class, String.class})
    public static JSONObject kick(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");
        UUID device = data.getUUID("device");

        Network network = Network.get(uuid);

        if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
            return error("no_permissions");
        }

        if (!Device.isOnline(network.getOwner())) {
            return error("device_not_online");
        }

        if (network.getOwner().equals(device)) {
            return error("cannot_kick_owner");
        }

        for (Member member : Member.getMembers(network.getUUID())) {
            if (member.getDevice().equals(device)) {
                member.delete();

                return simple("result", true);
            }
        }

        return simple("result", false);
    }

    @UserEndpoint(path = {"delete"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject delete(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");

        Network network = Network.get(uuid);

        if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
            return error("network_not_found");
        }

        if (!Device.isOnline(network.getOwner())) {
            return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
        }

        network.delete();

        List<Member> members = Member.getMembers(uuid);

        for (Member member : members) {
            member.delete();
        }

        List<Invitation> invitations = Invitation.getInvitationsOfNetwork(uuid, true);
        invitations.addAll(Invitation.getInvitationsOfNetwork(uuid, false));

        for (Invitation invitation : invitations) {
            invitation.delete();
        }

        return simple("result", true);
    }

    @UserEndpoint(path = {"revoke"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject revoke(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");

        Invitation invitation = Invitation.getInvitation(uuid);

        if (invitation == null) {
            return error("invitation_not_found");
        }

        if (!invitation.isRequest()) {
            final UUID device = Network.get(invitation.getNetwork()).getOwner();

            if (!Device.checkPermissions(device, user)) {
                return error("no_permissions");
            }

            if (!Device.isOnline(device)) {
                return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
            }
        } else {
            final UUID device = invitation.getDevice();

            if (!Device.checkPermissions(device, user)) {
                return error("no_permissions");
            }

            if (!Device.isOnline(device)) {
                return error(Error.ERROR_DEVICE_NOT_ONLINE.toString());
            }
        }

        invitation.revoke();

        return simple("result", true);
    }
}
