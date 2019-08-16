package net.cryptic_game.microservice.network.endpoint;

import com.sun.org.apache.xpath.internal.operations.Bool;
import net.cryptic_game.microservice.endpoint.MicroServiceEndpoint;
import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.network.communication.Device;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;
import net.cryptic_game.microservice.utils.JSON;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.cryptic_game.microservice.utils.JSONBuilder.*;

public class NetworkEndpoint {

    @UserEndpoint(path = {"name"}, keys = {"name"}, types = {String.class})
    public static JSONObject getByName(JSON data, UUID user) {
        String name = data.get("name");

        Network network = Network.getNetworkByName(name);
        if (network == null) {
            return error("network_not_found");
        }

        return network.serialize();
    }

    @UserEndpoint(path = {"get"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject getByUUID(JSON data, UUID user) {
        UUID uuid = data.getUUID("uuid");

        Network network = Network.get(uuid);
        if (network == null) {
            return error("network_not_found");
        }

        return network.serialize();
    }

    @UserEndpoint(path = {"public"}, keys = {}, types = {})
    public static JSONObject getAllPublicNetworks(JSON data, UUID user) {
        List<Network> networks = Network.getPublicNetworks();

        List<JSONObject> jsonNetworks = new ArrayList<>();

        for (Network network : networks) {
            jsonNetworks.add(network.serialize());
        }

        return simple("networks", jsonNetworks);
    }

    @UserEndpoint(path = {"create"}, keys = {"device", "name", "hidden"}, types = {String.class, String.class,
            Boolean.class})
    public static JSONObject create(JSON data, UUID user) {
        UUID device = data.getUUID("device");
        String name = data.get("name");
        Boolean hidden = data.get("hidden", Boolean.class);

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

    @MicroServiceEndpoint(path = {"check"}, keys = {"source", "destination"}, types = {String.class,
            String.class})
    public static JSONObject check(JSON data, UUID user) {
        UUID source = data.getUUID("source");
        UUID destination = data.getUUID("destination");

        for (Network network : Member.getNetworks(source)) {
            for (Member member : Member.getMembers(network.getUUID())) {
                if (member.getDevice().equals(destination)) {
                    return simple("connected", true);
                }
            }
        }

        return simple("connected", false);
    }


    @UserEndpoint(path = {"members"}, keys = {"uuid"}, types = {String.class})
    public static JSONObject members(JSON data, UUID user) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));

        Network network = Network.get(uuid);
        if (network == null || !Device.checkPermissions(network.getOwner(), user)) {
            return error("network_not_found");
        }

        List<Member> members = Member.getMembers(uuid);
        List<JSONObject> jsonMembers = new ArrayList<>();

        for (Member member : members) {
            jsonMembers.add(member.serialize());
        }

        return simple("members", jsonMembers);
    }

}
