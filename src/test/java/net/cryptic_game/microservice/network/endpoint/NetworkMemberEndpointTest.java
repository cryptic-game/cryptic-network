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

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Network.class, Member.class, Invitation.class, Device.class})
public class NetworkMemberEndpointTest {

    private Random rand;
    private List<Network> networks;
    private List<Member> members;
    private List<Invitation> invitations;

    @Mock
    private Member member;

    @Before
    public void setUp() {
        rand = new Random();
        networks = new ArrayList<>();
        members = new ArrayList<>();
        invitations = new ArrayList<>();

        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(Network.class);
        PowerMockito.mockStatic(Member.class);
        PowerMockito.mockStatic(Invitation.class);
        PowerMockito.mockStatic(Device.class);

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
    }

    @Test
    public void getAllTest() {
        networks = new ArrayList<>();
        members = new ArrayList<>();

        UUID device = UUID.randomUUID();

        networks.add(new Network(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        members.add(new Member(UUID.randomUUID(), device, UUID.randomUUID(), networks.get(0).getUUID()));

        JSON data = new JSON(JSONBuilder.anJSON().add("device", device.toString()).build());

        List<Network> networkList = (List<Network>) NetworkMemberEndpoint.getAll(data, UUID.randomUUID()).get("networks");
        assertEquals(1, networkList.size());
    }

    @Test
    public void requestTest() {
        networks = new ArrayList<>();
        invitations = new ArrayList<>();
        members = new ArrayList<>();

        UUID network = UUID.randomUUID();
        UUID device = UUID.randomUUID();

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).add("device", device.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(), NetworkMemberEndpoint.request(data, UUID.randomUUID()));

        networks.add(new Network(network, device, UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkMemberEndpoint.request(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);
        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkMemberEndpoint.request(data, UUID.randomUUID()));
        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        assertEquals(JSONBuilder.anJSON().add("error", "already_member_of_network").build(), NetworkMemberEndpoint.request(data, UUID.randomUUID()));

        networks.clear();
        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        members.add(new Member(UUID.randomUUID(), device, UUID.randomUUID(), network));
        PowerMockito.when(Member.getMembers(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Member> returnMembers = new ArrayList<>();

            for (Member all : members) {
                if (all.getNetwork().equals(invocationOnMock.getArgument(0))) {
                    returnMembers.add(all);
                }
            }

            return returnMembers;
        });

        assertEquals(JSONBuilder.anJSON().add("error", "already_member_of_network").build(), NetworkMemberEndpoint.request(data, UUID.randomUUID()));

        members.clear();
        invitations.add(new Invitation(UUID.randomUUID(), device, UUID.randomUUID(), network, true));
        PowerMockito.when(Invitation.getInvitationsOfDevice(Mockito.any(), Mockito.anyBoolean())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnInvitations = new ArrayList<>();

            for (Invitation all : invitations) {
                if (all.getDevice().equals(invocationOnMock.getArgument(0))) {
                    returnInvitations.add(all);
                }
            }

            return returnInvitations;
        });

        members.add(new Member(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), network));

        assertEquals(JSONBuilder.anJSON().add("error", "invitation_already_exists").build(), NetworkMemberEndpoint.request(data, UUID.randomUUID()));

        invitations.clear();
        PowerMockito.when(Invitation.request(Mockito.any(), Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            Invitation invitation = new Invitation(UUID.randomUUID(), invocationOnMock.getArgument(0), UUID.randomUUID(), invocationOnMock.getArgument(1), true);
            invitations.add(invitation);
            return invitation;
        });

        invitations.add(new Invitation(UUID.randomUUID(), device, UUID.randomUUID(), UUID.randomUUID(), true));

        JSONObject json = NetworkMemberEndpoint.request(data, UUID.randomUUID());

        assertEquals(2, invitations.size());
        assertEquals(JSONBuilder.anJSON().add("uuid", invitations.get(1).getUUID().toString()).add("network", network.toString()).add("device", device.toString()).add("request", true).build(), json);

    }

    @Test
    public void invitationsTest() {
        invitations = new ArrayList<>();

        UUID device = UUID.randomUUID();
        JSON data = new JSON(JSONBuilder.anJSON().add("device", device.toString()).build());

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkMemberEndpoint.invitations(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);
        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkMemberEndpoint.invitations(data, UUID.randomUUID()));
        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        invitations.add(new Invitation(UUID.randomUUID(), device, UUID.randomUUID(), UUID.randomUUID(), false));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);
        PowerMockito.when(Invitation.getInvitationsOfDevice(Mockito.any(), Mockito.anyBoolean())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnInvitations = new ArrayList<>();

            for (Invitation all : invitations) {
                if (all.getDevice().equals(invocationOnMock.getArgument(0))) {
                    returnInvitations.add(all);
                }
            }

            return returnInvitations;
        });

        List<JSONObject> invitationList = (List<JSONObject>) NetworkMemberEndpoint.invitations(data, UUID.randomUUID()).get("invitations");

        assertEquals(1, invitationList.size());
        assertEquals(JSONBuilder.anJSON().add("uuid", invitations.get(0).getUUID().toString()).add("network", invitations.get(0).getNetwork().toString()).add("device", device.toString()).add("request", false).build(), invitationList.get(0));

    }

    @Test
    public void leaveTest() throws Exception {
        invitations = new ArrayList<>();
        networks = new ArrayList<>();
        members = new ArrayList<>();

        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();
        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).add("device", device.toString()).build());

        networks.add(new Network(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));

        PowerMockito.when(Network.get(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            for (Network all : networks) {
                if (all.getUUID().equals(invocationOnMock.getArgument(0))) {
                    return all;
                }
            }

            return null;
        });

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkMemberEndpoint.leave(data, UUID.randomUUID()));

        networks.clear();
        networks.add(new Network(network, device, UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkMemberEndpoint.leave(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkMemberEndpoint.leave(data, UUID.randomUUID()));
        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        assertEquals(JSONBuilder.anJSON().add("error", "cannot_leave_own_network").build(), NetworkMemberEndpoint.leave(data, UUID.randomUUID()));

        networks.clear();
        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        members.add(member);
        PowerMockito.when(Member.getMembers(Mockito.any())).thenReturn(new ArrayList<>(members));
        PowerMockito.doNothing().when(member, "delete");
        PowerMockito.when(member.getDevice()).thenReturn(device);

        assertEquals(JSONBuilder.anJSON().add("result", true).build(), NetworkMemberEndpoint.leave(data, UUID.randomUUID()));

        PowerMockito.when(member.getDevice()).thenReturn(UUID.randomUUID());

        assertEquals(JSONBuilder.anJSON().add("result", false).build(), NetworkMemberEndpoint.leave(data, UUID.randomUUID()));

        members.clear();
        PowerMockito.when(Member.getMembers(Mockito.any())).thenReturn(new ArrayList<>(members));

        assertEquals(JSONBuilder.anJSON().add("result", false).build(), NetworkMemberEndpoint.leave(data, UUID.randomUUID()));
    }
}
