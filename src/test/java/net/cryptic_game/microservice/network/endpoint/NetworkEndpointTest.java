package net.cryptic_game.microservice.network.endpoint;

import net.cryptic_game.microservice.network.communication.Device;
import net.cryptic_game.microservice.network.model.Invitation;
import net.cryptic_game.microservice.network.model.Member;
import net.cryptic_game.microservice.network.model.Network;
import net.cryptic_game.microservice.utils.JSON;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Network.class, Device.class, Member.class, Invitation.class})
public class NetworkEndpointTest {

    private Random rand;
    private List<Network> networks;
    private List<Member> members;
    private List<Invitation> invitations;

    @Mock
    private Network network;

    @Mock
    private Member member;

    @Mock
    private Invitation invitation;

    @Mock
    private Member networkMember;

    @Before
    public void setUp() {
        rand = new Random();
        networks = new ArrayList<>();
        members = new ArrayList<>();
        invitations = new ArrayList<>();

        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(Network.class);
        PowerMockito.mockStatic(Device.class);
        PowerMockito.mockStatic(Member.class);
        PowerMockito.mockStatic(Invitation.class);
    }

    @Test
    public void getByNameTest() {
        networks = new ArrayList<>();

        PowerMockito.when(Network.getNetworkByName(Mockito.anyString())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            for(Network all : networks) {
                if(all.getName().equals(invocationOnMock.getArgument(0))) {
                    return all;
                }
            }
            return null;
        });

        String name = "network_" + rand.nextInt(100);
        Network network = new Network(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), name);
        networks.add(network);

        assertEquals(network.serialize(), NetworkEndpoint.getByName(new JSON(JSONBuilder.anJSON().add("name", name).build()), UUID.randomUUID()));

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(),
                NetworkEndpoint.getByName(new JSON(JSONBuilder.anJSON().add("name", "name").build()), UUID.randomUUID()));
    }

    @Test
    public void getByUUIDTest() {
        networks = new ArrayList<>();

        PowerMockito.when(Network.get(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            for(Network all : networks) {
                if(all.getUUID().equals(invocationOnMock.getArgument(0))) {
                    return all;
                }
            }
            return null;
        });

        UUID uuid = UUID.randomUUID();
        Network network = new Network(uuid, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100));
        networks.add(network);

        assertEquals(network.serialize(), NetworkEndpoint.getByUUID(new JSON(JSONBuilder.anJSON().add("uuid", uuid.toString()).build()), UUID.randomUUID()));

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(),
                NetworkEndpoint.getByUUID(new JSON(JSONBuilder.anJSON().add("uuid", UUID.randomUUID().toString()).build()), UUID.randomUUID()));
    }

    @Test
    public void getPublicNetworksTest() {
        networks = new ArrayList<>();

        PowerMockito.when(Network.getPublicNetworks()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Network> publicNetworks = new ArrayList<>();
            for(Network all : networks) {
                if(!all.isHidden()) {
                    publicNetworks.add(all);
                }
            }
            return publicNetworks;
        });

        Network network = new Network(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), false, "network_" + rand.nextInt(100));
        networks.add(network);

        List<JSONObject> networkList = new ArrayList<>();
        networkList.add(network.serialize());
        assertEquals(JSONBuilder.anJSON().add("networks", networkList).build(),
                NetworkEndpoint.getAllPublicNetworks(new JSON(new JSONObject()), UUID.randomUUID()));

        networks = new ArrayList<>();
        networkList = new ArrayList<>();
        assertEquals(JSONBuilder.anJSON().add("networks", networkList).build(),
                NetworkEndpoint.getAllPublicNetworks(new JSON(new JSONObject()), UUID.randomUUID()));
    }

    @Test
    public void createTest() {
        networks = new ArrayList<>();

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);
        PowerMockito.when(Network.checkName(Mockito.anyString())).thenCallRealMethod();
        PowerMockito.when(Network.create(Mockito.any(), Mockito.anyString(), Mockito.anyBoolean())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            Network network = new Network(UUID.randomUUID(), invocationOnMock.getArgument(0), UUID.randomUUID(), invocationOnMock.getArgument(2), invocationOnMock.getArgument(1));
            networks.add(network);
            return network;
        });

        UUID device = UUID.randomUUID();
        String name = "network_" + rand.nextInt(100);
        boolean hidden = rand.nextBoolean();
        JSON json = new JSON(JSONBuilder.anJSON().add("device", device.toString()).add("name", name).add("hidden", hidden).build());

        JSONObject jsonObject = NetworkEndpoint.create(json, UUID.randomUUID());
        assertEquals(networks.get(0).serialize(), jsonObject);

        networks = new ArrayList<>();
        List<Network> networkList = new ArrayList<>();
        networkList.add(new Network());
        networkList.add(new Network());
        networkList.add(new Network());
        PowerMockito.when(Network.getNetworks(Mockito.any())).thenReturn(networkList);

        jsonObject = NetworkEndpoint.create(json, UUID.randomUUID());
        assertEquals(JSONBuilder.anJSON().add("error", "maximum_networks_reached").build(), jsonObject);

        json = new JSON(JSONBuilder.anJSON().add("device", device.toString()).add("name", "net").add("hidden", hidden).build());
        networks = new ArrayList<>();

        PowerMockito.when(Network.getNetworks(Mockito.any())).thenReturn(new ArrayList<>());

        jsonObject = NetworkEndpoint.create(json, UUID.randomUUID());
        assertEquals(JSONBuilder.anJSON().add("error", "invalid_name").build(), jsonObject);

        json = new JSON(JSONBuilder.anJSON().add("device", device.toString()).add("name", name).add("hidden", hidden).build());
        networks = new ArrayList<>();

        PowerMockito.when(Network.getNetworkByName(Mockito.any())).thenReturn(new Network());

        jsonObject = NetworkEndpoint.create(json, UUID.randomUUID());
        assertEquals(JSONBuilder.anJSON().add("error", "name_already_in_use").build(), jsonObject);

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);
        jsonObject = NetworkEndpoint.create(json, UUID.randomUUID());
        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), jsonObject);
    }

    @Test
    public void checkTest() {
        networks = new ArrayList<>();
        members = new ArrayList<>();

        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        UUID network = UUID.randomUUID();

        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));

        members.add(new Member(UUID.randomUUID(), source, UUID.randomUUID(), network));
        members.add(new Member(UUID.randomUUID(), destination, UUID.randomUUID(), network));

        PowerMockito.when(Network.get(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            for (Network all : networks) {
                if (all.getUUID().equals(invocationOnMock.getArgument(0))) {
                    return all;
                }
            }

            return null;
        });

        PowerMockito.when(Member.getNetworks(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Network> returnNetworks = new ArrayList<>();
            for (Member all : members) {
                if (all.getDevice().equals(invocationOnMock.getArgument(0))) {
                    returnNetworks.add(Network.get(all.getNetwork()));
                }
            }

            return returnNetworks;
        });

        PowerMockito.when(Member.getMembers(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Member> returnMembers = new ArrayList<>();
            for (Member all : members) {
                if (all.getNetwork().equals(invocationOnMock.getArgument(0))) {
                    returnMembers.add(all);
                }
            }

            return returnMembers;
        });

        JSON data = new JSON(JSONBuilder.anJSON().add("source", source.toString()).add("destination", destination.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("connected", true).build(), NetworkEndpoint.check(data, "ms"));

        members.remove(1);
        assertEquals(JSONBuilder.anJSON().add("connected", false).build(), NetworkEndpoint.check(data, "ms"));
    }

    @Test
    public void membersTest() {
        networks = new ArrayList<>();
        members = new ArrayList<>();

        UUID network = UUID.randomUUID();

        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));

        members.add(new Member(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), network));
        members.add(new Member(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), network));

        PowerMockito.when(Network.get(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            for (Network all : networks) {
                if (all.getUUID().equals(invocationOnMock.getArgument(0))) {
                    return all;
                }
            }

            return null;
        });

        PowerMockito.when(Member.getMembers(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Member> returnMembers = new ArrayList<>();
            for (Member all : members) {
                if (all.getNetwork().equals(invocationOnMock.getArgument(0))) {
                    returnMembers.add(all);
                }
            }

            return returnMembers;
        });

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).build());

        JSONObject json = NetworkEndpoint.members(data, UUID.randomUUID());

        assertTrue(json.containsKey("members"));
        List<JSONObject> memberList = (List<JSONObject>) json.get("members");

        assertEquals(2, memberList.size());

        members.clear();

        json = NetworkEndpoint.members(data, UUID.randomUUID());

        assertTrue(json.containsKey("members"));
        memberList = (List<JSONObject>) json.get("members");

        assertEquals(0, memberList.size());

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);
        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(), NetworkEndpoint.members(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);
        networks.clear();

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(), NetworkEndpoint.members(data, UUID.randomUUID()));
    }

    @Test
    public void deleteUserTest() throws Exception {
        networks = new ArrayList<>();
        members = new ArrayList<>();
        invitations = new ArrayList<>();

        List<Member> networkMembers = new ArrayList<>();

        UUID user = UUID.randomUUID();

        networks.add(network);
        members.add(member);
        invitations.add(invitation);
        networkMembers.add(networkMember);

        PowerMockito.when(Network.getNetworksOfUser(user)).thenReturn(new ArrayList<>(networks));
        PowerMockito.when(Member.getMembershipsOfUser(user)).thenReturn(new ArrayList<>(members));
        PowerMockito.when(Invitation.getInvitationsOfUser(user)).thenReturn(new ArrayList<>(invitations));
        PowerMockito.when(Member.getMembers(Mockito.any())).thenReturn(new ArrayList<>(networkMembers));


        PowerMockito.when(network, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> {
            networks.remove(network);
            return null;
        });

        PowerMockito.when(member, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> {
            members.remove(member);
            return null;
        });

        PowerMockito.when(invitation, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> {
            invitations.remove(invitation);
            return null;
        });

        PowerMockito.when(networkMember, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> {
            networkMembers.remove(networkMember);
            return null;
        });

        JSON data = new JSON(JSONBuilder.anJSON().add("user_uuid", user.toString()).build());
        NetworkEndpoint.deleteUser(data, "ms");

        assertEquals(0, networks.size());
        assertEquals(0, members.size());
        assertEquals(0, invitations.size());
        assertEquals(0, networkMembers.size());
    }
}
