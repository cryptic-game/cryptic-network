package net.cryptic_game.microservice.network.model;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.microservice.db.Database;
import net.cryptic_game.microservice.model.Model;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MicroService.class, Database.class})
public class InvitationTest {

    private Random rand;
    private List<Invitation> invitations;
    private List<Network> networks;
    private List<Member> members;

    @Mock
    private MicroService microService;

    @Mock
    private Database database;

    @Mock
    private Transaction transaction;

    @Mock
    private Session session;

    @Before
    public void setUp() throws Exception {
        rand = new Random();
        invitations = new ArrayList<>();
        networks = new ArrayList<>();
        members = new ArrayList<>();

        PowerMockito.mockStatic(Database.class);
        PowerMockito.when(Database.getInstance()).thenReturn(database);
        PowerMockito.when(database.openSession()).thenReturn(session);
        PowerMockito.when(session.getTransaction()).thenReturn(transaction);

        PowerMockito.when(session.save(Mockito.any(Invitation.class))).thenAnswer((InvocationOnMock invocationOnMock) -> {
            invitations.add(invocationOnMock.getArgument(0));
            return null;
        });

        PowerMockito.when(session.get(Mockito.eq(Invitation.class), Mockito.any(UUID.class))).then((InvocationOnMock invocationOnMock) -> {
            UUID invitation = invocationOnMock.getArgument(1);

            for (Invitation all : invitations) {
                if(all.getUUID().equals(invitation)) {
                    return all;
                }
            }
            return null;
        });

        PowerMockito.when(session, "delete", Mockito.any()).then((InvocationOnMock invocationOnMock) -> {
            Invitation invitation = invocationOnMock.getArgument(0);
            invitations.remove(invitation);
            return null;
        });

        PowerMockito.when(session.save(Mockito.any(Network.class))).thenAnswer((InvocationOnMock invocationOnMock) -> {
            networks.add(invocationOnMock.getArgument(0));
            return null;
        });

        PowerMockito.when(session.get(Mockito.eq(Network.class), Mockito.any(UUID.class))).then((InvocationOnMock invocationOnMock) -> {
            UUID network = invocationOnMock.getArgument(1);

            for (Network all : networks) {
                if(all.getUUID().equals(network)) {
                    return all;
                }
            }
            return null;
        });

        PowerMockito.when(session.save(Mockito.any(Member.class))).thenAnswer((InvocationOnMock invocationOnMock) -> {
            members.add(invocationOnMock.getArgument(0));
            return null;
        });

        PowerMockito.when(session.get(Mockito.eq(Member.class), Mockito.any(UUID.class))).then((InvocationOnMock invocationOnMock) -> {
            UUID member = invocationOnMock.getArgument(1);

            for (Member all : members) {
                if(all.getUUID().equals(member)) {
                    return all;
                }
            }
            return null;
        });

        PowerMockito.mockStatic(MicroService.class);
        PowerMockito.when(MicroService.getInstance()).thenReturn(microService);
        PowerMockito.when(microService.contactMicroService(Mockito.eq("device"), Mockito.eq(new String[]{"owner"}), Mockito.any()))
                .thenReturn(JSONBuilder.anJSON().add("owner", UUID.randomUUID()).build());
    }

    @Test
    public void constructorTest() {
        assertTrue(new Invitation() instanceof Model);

        UUID uuid = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID network = UUID.randomUUID();
        boolean request = rand.nextBoolean();

        Invitation invitation = new Invitation(uuid, device, user, network, request);

        assertEquals(uuid, invitation.getUUID());
        assertEquals(device, invitation.getDevice());
        assertEquals(user, invitation.getUser());
        assertEquals(network, invitation.getNetwork());
        assertEquals(request, invitation.isRequest());
    }

    @Test
    public void serializeTest() {
        UUID uuid = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();
        boolean request = rand.nextBoolean();

        Invitation invitation = new Invitation(uuid, device, UUID.randomUUID(), network, request);

        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("uuid", uuid.toString());
        jsonMap.put("network", network.toString());
        jsonMap.put("device", device.toString());
        jsonMap.put("request", request);

        assertEquals(new JSONObject(jsonMap), invitation.serialize());
    }

    @Test
    public void createTest() {
        invitations = new ArrayList<>();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();
        boolean request = rand.nextBoolean();

        Invitation.create(device, network, request);

        assertEquals(1, invitations.size());

        Invitation invitation = invitations.get(0);

        assertEquals(device, invitation.getDevice());
        assertEquals(network, invitation.getNetwork());
        assertEquals(request, invitation.isRequest());
    }

    @Test
    public void requestTest() {
        invitations = new ArrayList<>();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();

        Invitation.request(device, network);

        assertEquals(1, invitations.size());

        Invitation invitation = invitations.get(0);

        assertEquals(device, invitation.getDevice());
        assertEquals(network, invitation.getNetwork());
        assertTrue(invitation.isRequest());
    }

    @Test
    public void inviteTest() {
        invitations = new ArrayList<>();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();

        Invitation.invite(device, network);

        assertEquals(1, invitations.size());

        Invitation invitation = invitations.get(0);

        assertEquals(device, invitation.getDevice());
        assertEquals(network, invitation.getNetwork());
        assertFalse(invitation.isRequest());
    }

    @Test
    public void denyTest() {
        invitations = new ArrayList<>();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();
        boolean request = rand.nextBoolean();

        Invitation invitation = Invitation.create(device, network, request);
        invitation.deny();

        assertEquals(0, invitations.size());
    }

    @Test
    public void revokeTest() {
        invitations = new ArrayList<>();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();
        boolean request = rand.nextBoolean();

        Invitation invitation = Invitation.create(device, network, request);
        invitation.revoke();

        assertEquals(0, invitations.size());
    }

    @Test
    public void acceptTest() {
        invitations = new ArrayList<>();
        networks = new ArrayList<>();
        members = new ArrayList<>();

        UUID owner = UUID.randomUUID();
        boolean hidden = rand.nextBoolean();
        String name = "network_" + rand.nextInt(100);

        Network network = Network.create(owner, name, hidden);

        UUID device = UUID.randomUUID();
        boolean request = rand.nextBoolean();
        Invitation invitation = Invitation.create(device, network.getUUID(), request);

        invitation.accept();

        assertEquals(2, members.size());

        Member member = members.get(1);

        assertEquals(device, member.getDevice());
        assertEquals(network.getUUID(), member.getNetwork());
    }

    @Test
    public void getTest() {
        invitations = new ArrayList<>();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();
        boolean request = rand.nextBoolean();

        Invitation created = Invitation.create(device, network, request);

        Invitation invitation = Invitation.getInvitation(created.getUUID());

        assertEquals(network, invitation.getNetwork());
    }
}
