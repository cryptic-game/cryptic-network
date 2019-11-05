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
@PrepareForTest({Network.class, Invitation.class, Device.class, Member.class})
public class NetworkOwnerEndpointTest {

    private Random rand;
    private List<Network> networks = new ArrayList<>();
    private List<Invitation> invitations = new ArrayList<>();
    private List<Member> members = new ArrayList<>();

    @Mock
    private Invitation invitation;

    @Mock
    private Member member;

    @Mock
    private Network network;

    @Before
    public void setUp() {
        rand = new Random();
        networks = new ArrayList<>();
        invitations = new ArrayList<>();
        members = new ArrayList<>();

        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(Network.class);
        PowerMockito.mockStatic(Invitation.class);
        PowerMockito.mockStatic(Member.class);
        PowerMockito.mockStatic(Device.class);

        PowerMockito.when(Network.getNetworks(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Network> returnNetworks = new ArrayList<>();
            for (Network all : networks) {
                if (all.getOwner().equals(invocationOnMock.getArgument(0))) {
                    returnNetworks.add(all);
                }
            }
            return returnNetworks;
        });

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

        PowerMockito.when(Invitation.getInvitationsOfDevice(Mockito.any(), Mockito.anyBoolean())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnMembers = new ArrayList<>();
            for (Invitation all : invitations) {
                if (all.getDevice().equals(invocationOnMock.getArgument(0))) {
                    returnMembers.add(all);
                }
            }
            return returnMembers;
        });

        PowerMockito.when(Invitation.getInvitationsOfNetwork(Mockito.any(), Mockito.anyBoolean())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnMembers = new ArrayList<>();
            for (Invitation all : invitations) {
                if (all.getNetwork().equals(invocationOnMock.getArgument(0))) {
                    returnMembers.add(all);
                }
            }
            return returnMembers;
        });

        PowerMockito.when(Invitation.invite(Mockito.any(), Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            Invitation invitation = new Invitation(UUID.randomUUID(), invocationOnMock.getArgument(0), UUID.randomUUID(), invocationOnMock.getArgument(1), false);
            invitations.add(invitation);
            return invitation;
        });

        PowerMockito.when(Invitation.getInvitation(Mockito.any())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            for (Invitation all : invitations) {
                if (all.getUUID().equals(invocationOnMock.getArgument(0))) {
                    return all;
                }
            }
            return null;
        });
    }

    @Test
    public void getAllTest() {
        networks = new ArrayList<>();

        UUID owner = UUID.randomUUID();
        networks.add(new Network(UUID.randomUUID(), owner, UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));

        List<JSONObject> json = (List<JSONObject>) NetworkOwnerEndpoint.getAll(new JSON(JSONBuilder.anJSON().add("device", owner.toString()).build()), UUID.randomUUID()).get("networks");

        assertEquals(1, json.size());
        assertEquals(networks.get(0).serialize(), json.get(0));
    }

    @Test
    public void inviteTest() {
        networks = new ArrayList<>();
        invitations = new ArrayList<>();

        UUID network = UUID.randomUUID();
        UUID device = UUID.randomUUID();

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).add("device", device.toString()).build());

        networks.add(new Network(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(), NetworkOwnerEndpoint.invite(data, UUID.randomUUID()));

        networks.clear();
        networks.add(new Network(network, device, UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(), NetworkOwnerEndpoint.invite(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        assertEquals(JSONBuilder.anJSON().add("error", "already_member_of_network").build(), NetworkOwnerEndpoint.invite(data, UUID.randomUUID()));

        networks.clear();
        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        members.add(new Member(UUID.randomUUID(), device, UUID.randomUUID(), network));

        assertEquals(JSONBuilder.anJSON().add("error", "already_member_of_network").build(), NetworkOwnerEndpoint.invite(data, UUID.randomUUID()));

        members.clear();
        members.add(new Member(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), network));
        invitations.add(new Invitation(UUID.randomUUID(), device, UUID.randomUUID(), network, false));

        assertEquals(JSONBuilder.anJSON().add("error", "invitation_already_exists").build(), NetworkOwnerEndpoint.invite(data, UUID.randomUUID()));

        invitations.clear();
        invitations.add(new Invitation(UUID.randomUUID(), device, UUID.randomUUID(), UUID.randomUUID(), false));

        JSONObject json = NetworkOwnerEndpoint.invite(data, UUID.randomUUID());

        assertEquals(invitations.get(1).serialize(), json);
    }

    @Test
    public void acceptTest() throws Exception {
        invitations = new ArrayList<>();
        networks = new ArrayList<>();

        UUID invitation = UUID.randomUUID();

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", invitation.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "invitation_not_found").build(), NetworkOwnerEndpoint.accept(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);
        networks.add(new Network(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        invitations.add(new Invitation(invitation, UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), false));

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.accept(data, UUID.randomUUID()));

        invitations.clear();
        invitations.add(new Invitation(invitation, UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), true));

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.accept(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);

        PowerMockito.when(this.invitation.isRequest()).thenReturn(true);
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.accept(data, UUID.randomUUID()));

        PowerMockito.when(this.invitation.isRequest()).thenReturn(false);
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.accept(data, UUID.randomUUID()));

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        invitations.clear();

        invitations.add(this.invitation);
        PowerMockito.when(this.invitation.getUUID()).thenReturn(invitation);
        PowerMockito.when(this.invitation.isRequest()).thenReturn(true);
        PowerMockito.when(this.invitation, "accept").thenAnswer((InvocationOnMock invocationOnMock) -> null);
        PowerMockito.when(this.invitation.getNetwork()).thenReturn(networks.get(0).getUUID());

        assertEquals(JSONBuilder.anJSON().add("result", true).build(), NetworkOwnerEndpoint.accept(data, UUID.randomUUID()));
    }

    @Test
    public void denyTest() throws Exception {
        invitations = new ArrayList<>();
        networks = new ArrayList<>();

        UUID invitation = UUID.randomUUID();

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", invitation.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "invitation_not_found").build(), NetworkOwnerEndpoint.deny(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);
        networks.add(new Network(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        invitations.add(new Invitation(invitation, UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), false));

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.deny(data, UUID.randomUUID()));

        invitations.clear();
        invitations.add(new Invitation(invitation, UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), true));

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.deny(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        invitations.clear();

        invitations.add(this.invitation);
        PowerMockito.when(this.invitation.getUUID()).thenReturn(invitation);
        PowerMockito.when(this.invitation.isRequest()).thenReturn(true);
        PowerMockito.when(this.invitation, "deny").thenAnswer((InvocationOnMock invocationOnMock) -> null);
        PowerMockito.when(this.invitation.getNetwork()).thenReturn(networks.get(0).getUUID());

        assertEquals(JSONBuilder.anJSON().add("result", true).build(), NetworkOwnerEndpoint.deny(data, UUID.randomUUID()));

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);
        PowerMockito.when(Invitation.getInvitation(Mockito.any())).thenReturn(new Invitation());

        invitations.clear();
        invitations.add(new Invitation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true));
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.deny(data, UUID.randomUUID()));

        invitations.clear();
        invitations.add(new Invitation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), false));
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.deny(data, UUID.randomUUID()));
    }

    @Test
    public void requestsTest() {
        networks = new ArrayList<>();
        invitations = new ArrayList<>();

        UUID network = UUID.randomUUID();

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.requests(data, UUID.randomUUID()));

        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.requests(data, UUID.randomUUID()));

        invitations.add(new Invitation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), network, true));
        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        List<JSONObject> json = (List<JSONObject>) NetworkOwnerEndpoint.requests(data, UUID.randomUUID()).get("requests");

        assertEquals(1, json.size());
        assertEquals(invitations.get(0).serialize(), json.get(0));
    }

    @Test
    public void invitationsTest() {
        networks = new ArrayList<>();
        invitations = new ArrayList<>();

        UUID network = UUID.randomUUID();

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.invitationsNetwork(data, UUID.randomUUID()));

        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.invitationsNetwork(data, UUID.randomUUID()));

        invitations.add(new Invitation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), network, false));
        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        List<JSONObject> json = (List<JSONObject>) NetworkOwnerEndpoint.invitationsNetwork(data, UUID.randomUUID()).get("invitations");

        assertEquals(1, json.size());
        assertEquals(invitations.get(0).serialize(), json.get(0));
    }

    @Test
    public void kickTest() throws Exception {
        networks = new ArrayList<>();
        invitations = new ArrayList<>();
        members = new ArrayList<>();

        UUID network = UUID.randomUUID();
        UUID device = UUID.randomUUID();

        networks.clear();
        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).add("device", device.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.kick(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.kick(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);

        invitations.clear();
        invitations.add(new Invitation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), true));
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.kick(data, UUID.randomUUID()));

        invitations.clear();
        invitations.add(new Invitation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), false));
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.kick(data, UUID.randomUUID()));
        invitations.clear();

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        networks.clear();
        networks.add(new Network(network, device, UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        assertEquals(JSONBuilder.anJSON().add("error", "cannot_kick_owner").build(), NetworkOwnerEndpoint.kick(data, UUID.randomUUID()));

        networks.clear();
        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        members.add(new Member(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), network));

        assertEquals(JSONBuilder.anJSON().add("result", false).build(), NetworkOwnerEndpoint.kick(data, UUID.randomUUID()));

        members.clear();
        members.add(this.member);
        PowerMockito.when(this.member.getNetwork()).thenReturn(network);
        PowerMockito.when(this.member.getDevice()).thenReturn(device);
        PowerMockito.when(this.member, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> null);

        assertEquals(JSONBuilder.anJSON().add("result", true).build(), NetworkOwnerEndpoint.kick(data, UUID.randomUUID()));
    }

    @Test
    public void deleteTest() throws Exception {
        networks = new ArrayList<>();
        invitations = new ArrayList<>();
        members = new ArrayList<>();

        UUID network = UUID.randomUUID();
        UUID invitation = UUID.randomUUID();

        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        invitations.add(new Invitation(invitation, UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), false));

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", network.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(), NetworkOwnerEndpoint.delete(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "network_not_found").build(), NetworkOwnerEndpoint.delete(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.delete(data, UUID.randomUUID()));

        invitations.clear();
        invitations.add(new Invitation(invitation, UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), true));
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.delete(data, UUID.randomUUID()));
        invitations.clear();
        invitations.add(new Invitation(invitation, UUID.randomUUID(), UUID.randomUUID(), networks.get(0).getUUID(), false));

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        networks.clear();
        networks.add(this.network);
        PowerMockito.when(this.network.getUUID()).thenReturn(network);
        PowerMockito.when(this.network, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> null);

        members.add(this.member);
        PowerMockito.when(this.member.getNetwork()).thenReturn(network);
        PowerMockito.when(this.member, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> null);

        invitations.clear();
        invitations.add(this.invitation);
        PowerMockito.when(this.invitation.getNetwork()).thenReturn(network);
        PowerMockito.when(this.invitation, "delete").thenAnswer((InvocationOnMock invocationOnMock) -> null);

//        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        assertEquals(JSONBuilder.anJSON().add("result", true).build(), NetworkOwnerEndpoint.delete(data, UUID.randomUUID()));
    }

    @Test
    public void revokeTest() throws Exception {
        invitations = new ArrayList<>();

        UUID invitation = UUID.randomUUID();
        UUID network = UUID.randomUUID();

        JSON data = new JSON(JSONBuilder.anJSON().add("uuid", invitation.toString()).build());

        assertEquals(JSONBuilder.anJSON().add("error", "invitation_not_found").build(), NetworkOwnerEndpoint.revoke(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(false);
        invitations.add(this.invitation);
        networks.add(new Network(network, UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean(), "network_" + rand.nextInt(100)));
        PowerMockito.when(this.invitation.getNetwork()).thenReturn(network);
        PowerMockito.when(this.invitation.getUUID()).thenReturn(invitation);
        PowerMockito.when(this.invitation.isRequest()).thenReturn(true);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.revoke(data, UUID.randomUUID()));

        PowerMockito.when(this.invitation.isRequest()).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "no_permissions").build(), NetworkOwnerEndpoint.revoke(data, UUID.randomUUID()));

        PowerMockito.when(Device.checkPermissions(Mockito.any(), Mockito.any())).thenReturn(true);

        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(false);

        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.revoke(data, UUID.randomUUID()));

        PowerMockito.when(this.invitation.isRequest()).thenReturn(true);
        assertEquals(JSONBuilder.anJSON().add("error", "device_not_online").build(), NetworkOwnerEndpoint.revoke(data, UUID.randomUUID()));

        PowerMockito.when(this.invitation.isRequest()).thenReturn(false);
        PowerMockito.when(Device.isOnline(Mockito.any())).thenReturn(true);

        PowerMockito.when(this.invitation, "revoke").thenAnswer((InvocationOnMock invocationOnMock) -> null);

        assertEquals(JSONBuilder.anJSON().add("result", true).build(), NetworkOwnerEndpoint.revoke(data, UUID.randomUUID()));
    }
}
