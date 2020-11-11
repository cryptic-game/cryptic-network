package net.cryptic_game.microservice.network.model;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.microservice.model.Model;
import net.cryptic_game.microservice.sql.SqlService;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MicroService.class, SqlService.class})
public class InvitationTest {

    private Random rand;
    private List<Invitation> invitations;
    private List<Network> networks;
    private List<Member> members;

    @Mock
    private MicroService microService;

    @Mock
    private SqlService sqlService;

    @Mock
    private Transaction transaction;

    @Mock
    private Session session;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<Invitation> criteriaQuery;

    @Mock
    private Root<Invitation> root;

    @Mock
    private Query<Invitation> typedQuery;

    @Before
    public void setUp() throws Exception {
        rand = new Random();
        invitations = new ArrayList<>();
        networks = new ArrayList<>();
        members = new ArrayList<>();

        PowerMockito.mockStatic(SqlService.class);
        PowerMockito.when(SqlService.getInstance()).thenReturn(sqlService);
        PowerMockito.when(sqlService.openSession()).thenReturn(session);
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

        PowerMockito.when(session.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        PowerMockito.when(criteriaBuilder.createQuery(Invitation.class)).thenReturn(criteriaQuery);
        PowerMockito.when(criteriaQuery.from(Invitation.class)).thenReturn(root);
        PowerMockito.when(session.createQuery(criteriaQuery)).thenReturn(typedQuery);

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

    @Test
    public void getInvitationOfDeviceTest() {
        invitations = new ArrayList<>();

        UUID device = UUID.randomUUID();

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnInvitations = new ArrayList<>();
            for(Invitation all : invitations) {
                if(all.getDevice().equals(device) && all.isRequest()) {
                    returnInvitations.add(all);
                }
            }
            return returnInvitations;
        });

        Invitation.create(device, UUID.randomUUID(), true);

        List<Invitation> invitationList = Invitation.getInvitationsOfDevice(device, true);

        assertEquals(1, invitationList.size());
        assertEquals(device, invitationList.get(0).getDevice());

        invitations = new ArrayList<>();

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnInvitations = new ArrayList<>();
            for(Invitation all : invitations) {
                if(all.getDevice().equals(device) && !all.isRequest()) {
                    returnInvitations.add(all);
                }
            }
            return returnInvitations;
        });

        Invitation.create(device, UUID.randomUUID(), false);

        invitationList = Invitation.getInvitationsOfDevice(device, false);

        assertEquals(1, invitationList.size());
        assertEquals(device, invitationList.get(0).getDevice());
    }

    @Test
    public void getInvitationOfNetworkTest() {
        invitations = new ArrayList<>();

        UUID network = UUID.randomUUID();

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnInvitations = new ArrayList<>();
            for(Invitation all : invitations) {
                if(all.getNetwork().equals(network) && all.isRequest()) {
                    returnInvitations.add(all);
                }
            }
            return returnInvitations;
        });

        Invitation.create(UUID.randomUUID(), network, true);

        List<Invitation> invitationList = Invitation.getInvitationsOfNetwork(network, true);

        assertEquals(1, invitationList.size());
        assertEquals(network, invitationList.get(0).getNetwork());

        invitations = new ArrayList<>();

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnInvitations = new ArrayList<>();
            for(Invitation all : invitations) {
                if(all.getNetwork().equals(network) && !all.isRequest()) {
                    returnInvitations.add(all);
                }
            }
            return returnInvitations;
        });

        Invitation.create(UUID.randomUUID(), network, false);

        invitationList = Invitation.getInvitationsOfNetwork(network, false);

        assertEquals(1, invitationList.size());
        assertEquals(network, invitationList.get(0).getNetwork());
    }

    @Test
    public void getInvitationOfUserTest() {
        invitations = new ArrayList<>();

        UUID user = UUID.randomUUID();
        PowerMockito.when(microService.contactMicroService(Mockito.eq("device"), Mockito.eq(new String[]{"owner"}), Mockito.any()))
                .thenReturn(JSONBuilder.anJSON().add("owner", user.toString()).build());

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Invitation> returnInvitations = new ArrayList<>();
            for(Invitation all : invitations) {
                if(all.getUser().equals(user)) {
                    returnInvitations.add(all);
                }
            }
            return returnInvitations;
        });

        Invitation.create(UUID.randomUUID(), UUID.randomUUID(), rand.nextBoolean());

        List<Invitation> invitationList = Invitation.getInvitationsOfUser(user);

        assertEquals(1, invitationList.size());
        assertEquals(user, invitationList.get(0).getUser());
    }
}
