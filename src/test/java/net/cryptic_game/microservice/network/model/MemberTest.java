package net.cryptic_game.microservice.network.model;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.microservice.db.Database;
import net.cryptic_game.microservice.model.Model;
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
@PrepareForTest({MicroService.class, Database.class})
public class MemberTest {

    private List<Member> members;
    private List<Network> networks;

    @Mock
    private MicroService microService;

    @Mock
    private Database database;

    @Mock
    private Transaction transaction;

    @Mock
    private Session session;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<Member> criteriaQuery;

    @Mock
    private Root<Member> root;

    @Mock
    private Query<Member> typedQuery;

    @Before
    public void setUp() {
        members = new ArrayList<>();

        PowerMockito.mockStatic(Database.class);
        PowerMockito.when(Database.getInstance()).thenReturn(database);
        PowerMockito.when(database.openSession()).thenReturn(session);
        PowerMockito.when(session.getTransaction()).thenReturn(transaction);

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

        PowerMockito.when(session.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        PowerMockito.when(criteriaBuilder.createQuery(Member.class)).thenReturn(criteriaQuery);
        PowerMockito.when(criteriaQuery.from(Member.class)).thenReturn(root);
        PowerMockito.when(session.createQuery(criteriaQuery)).thenReturn(typedQuery);

        PowerMockito.mockStatic(MicroService.class);
        PowerMockito.when(MicroService.getInstance()).thenReturn(microService);
        PowerMockito.when(microService.contactMicroService(Mockito.eq("device"), Mockito.eq(new String[]{"owner"}), Mockito.any()))
                .thenReturn(JSONBuilder.anJSON().add("owner", UUID.randomUUID()).build());
    }

    @Test
    public void constructorTest() {
        assertTrue(new Member() instanceof Model);

        UUID uuid = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID network = UUID.randomUUID();

        Member member = new Member(uuid, device, user, network);

        assertEquals(uuid, member.getUUID());
        assertEquals(device, member.getDevice());
        assertEquals(user, member.getUser());
        assertEquals(network, member.getNetwork());
    }

    @Test
    public void serializeTest() {
        UUID uuid = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();

        Member member = new Member(uuid, device, UUID.randomUUID(), network);

        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("uuid", uuid.toString());
        jsonMap.put("device", device.toString());
        jsonMap.put("network", network.toString());

        assertEquals(new JSONObject(jsonMap), member.serialize());
    }

    @Test
    public void createTest() {
        members = new ArrayList<>();
        UUID device = UUID.randomUUID();
        UUID network = UUID.randomUUID();

        Member.create(device, network);

        assertEquals(1, members.size());

        Member member = members.get(0);

        assertEquals(device, member.getDevice());
        assertEquals(network, member.getNetwork());
    }

    @Test
    public void getNetworksTest() {
        members = new ArrayList<>();
        networks = new ArrayList<>();

        UUID device = UUID.randomUUID();

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Member> returnMembers = new ArrayList<>();
            for(Member all : members) {
                if(all.getDevice().equals(device)) {
                    returnMembers.add(all);
                }
            }
            return returnMembers;
        });

        Network network = Network.create(device, "network", true);

        List<Network> networkList = Member.getNetworks(device);

        assertEquals(1, networkList.size());
        assertEquals(network.getUUID(), networkList.get(0).getUUID());
    }

    @Test
    public void getMembersTest() {
        members = new ArrayList<>();
        networks = new ArrayList<>();

        UUID device = UUID.randomUUID();
        Network network = Network.create(device, "network", true);

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Member> returnMembers = new ArrayList<>();
            for(Member all : members) {
                if(all.getNetwork().equals(network.getUUID())) {
                    returnMembers.add(all);
                }
            }
            return returnMembers;
        });

        List<Member> memberList = Member.getMembers(network.getUUID());

        assertEquals(1, memberList.size());
        assertEquals(network.getUUID(), memberList.get(0).getNetwork());
    }

    @Test
    public void getMembershipOfUserTest() {
        members = new ArrayList<>();

        UUID user = UUID.randomUUID();
        PowerMockito.when(microService.contactMicroService(Mockito.eq("device"), Mockito.eq(new String[]{"owner"}), Mockito.any()))
                .thenReturn(JSONBuilder.anJSON().add("owner", user.toString()).build());

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Member> userMembers = new ArrayList<>();
            for(Member all : members) {
                if(all.getUser().equals(user)) {
                    userMembers.add(all);
                }
            }
            return userMembers;
        });

        Member.create(UUID.randomUUID(), UUID.randomUUID());

        List<Member> memberList = Member.getMembershipsOfUser(user);

        assertEquals(1, memberList.size());
        assertEquals(user, memberList.get(0).getUser());
    }
}
