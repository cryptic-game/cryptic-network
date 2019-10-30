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
public class NetworkTest {

    private Random rand;
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
    private CriteriaQuery<Network> criteriaQuery;

    @Mock
    private Root<Network> root;

    @Mock
    private Query<Network> typedQuery;

    @Before
    public void setUp() {
        rand = new Random();
        networks = new ArrayList<>();

        PowerMockito.mockStatic(Database.class);
        PowerMockito.when(Database.getInstance()).thenReturn(database);
        PowerMockito.when(database.openSession()).thenReturn(session);
        PowerMockito.when(session.getTransaction()).thenReturn(transaction);

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
        PowerMockito.when(criteriaBuilder.createQuery(Network.class)).thenReturn(criteriaQuery);
        PowerMockito.when(criteriaQuery.from(Network.class)).thenReturn(root);
        PowerMockito.when(session.createQuery(criteriaQuery)).thenReturn(typedQuery);

        PowerMockito.mockStatic(MicroService.class);
        PowerMockito.when(MicroService.getInstance()).thenReturn(microService);
        PowerMockito.when(microService.contactMicroService(Mockito.eq("device"), Mockito.eq(new String[]{"owner"}), Mockito.any()))
                .thenReturn(JSONBuilder.anJSON().add("owner", UUID.randomUUID()).build());
    }

    @Test
    public void constructorTest() {
        assertTrue(new Network() instanceof Model);

        UUID uuid = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        boolean hidden = rand.nextBoolean();
        String name = "network_" + rand.nextInt(100);

        Network network = new Network(uuid, owner, user, hidden, name);

        assertEquals(uuid, network.getUUID());
        assertEquals(owner, network.getOwner());
        assertEquals(user, network.getUser());
        assertEquals(hidden, network.isHidden());
        assertEquals(name, network.getName());
    }

    @Test
    public void serializeTest() {
        UUID uuid = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        boolean hidden = rand.nextBoolean();
        String name = "network_" + rand.nextInt(100);

        Network network = new Network(uuid, owner, user, hidden, name);

        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("uuid", uuid.toString());
        jsonMap.put("hidden", hidden);
        jsonMap.put("owner", owner.toString());
        jsonMap.put("name", name);

        assertEquals(new JSONObject(jsonMap), network.serialize());
    }

    @Test
    public void addMemberTest() {
        networks = new ArrayList<>();

        UUID owner = UUID.randomUUID();
        boolean hidden = rand.nextBoolean();
        String name = "network_" + rand.nextInt(100);

        Network network = Network.create(owner, name, hidden);

        UUID device = UUID.randomUUID();

        Member member = network.addMember(device);

        assertEquals(device, member.getDevice());
        assertEquals(network.getUUID(), member.getNetwork());
    }

    @Test
    public void getTest() {
        networks = new ArrayList<>();

        UUID owner = UUID.randomUUID();
        boolean hidden = rand.nextBoolean();
        String name = "network_" + rand.nextInt(100);
        Network created = Network.create(owner, name, hidden);

        Network network = Network.get(created.getUUID());

        assertEquals(name, network.getName());
    }

    @Test
    public void createTest() {
        UUID owner = UUID.randomUUID();
        boolean hidden = rand.nextBoolean();
        String name = "network_" + rand.nextInt(100);

        Network network = Network.create(owner, name, hidden);

        assertEquals(name, network.getName());
        assertEquals(hidden, network.isHidden());
    }


    @Test
    public void checkNameTest() {
        String legalName = "network_" + rand.nextInt(100);
        String illegalName1 = "net";
        String illegalName2 = "network_network_network";
        String illegalName3 = "network network";

        assertTrue(Network.checkName(legalName));
        assertFalse(Network.checkName(illegalName1));
        assertFalse(Network.checkName(illegalName2));
        assertFalse(Network.checkName(illegalName3));
    }

    @Test
    public void getPublicNetworksTest() {
        networks = new ArrayList<>();
        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Network> publicNetworks = new ArrayList<>();
            for(Network all : networks) {
                if(!all.isHidden()) {
                    publicNetworks.add(all);
                }
            }
            return publicNetworks;
        });

        UUID owner = UUID.randomUUID();
        String name = "network_" + rand.nextInt(100);

        Network.create(owner, name, false);

        List<Network> publicNetworks = Network.getPublicNetworks();

        assertEquals(1, publicNetworks.size());
        assertEquals(name, publicNetworks.get(0).getName());
    }

    @Test
    public void getNetworksOfUserTest() {
        networks = new ArrayList<>();

        UUID user = UUID.randomUUID();
        PowerMockito.when(microService.contactMicroService(Mockito.eq("device"), Mockito.eq(new String[]{"owner"}), Mockito.any()))
                .thenReturn(JSONBuilder.anJSON().add("owner", user.toString()).build());

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Network> userNetworks = new ArrayList<>();
            for(Network all : networks) {
                if(all.getUser().equals(user)) {
                    userNetworks.add(all);
                }
            }
            return userNetworks;
        });

        UUID owner = UUID.randomUUID();
        String name = "network_" + rand.nextInt(100);
        boolean hidden = rand.nextBoolean();

        Network.create(owner, name, hidden);

        List<Network> userNetworks = Network.getNetworksOfUser(user);

        assertEquals(1, userNetworks.size());
        assertEquals(name, userNetworks.get(0).getName());
    }

    @Test
    public void getNetworksTest() {
        networks = new ArrayList<>();
        UUID owner = UUID.randomUUID();

        Mockito.when(typedQuery.getResultList()).thenAnswer((InvocationOnMock invocationOnMock) -> {
            List<Network> userNetworks = new ArrayList<>();
            for(Network all : networks) {
                if(all.getOwner().equals(owner)) {
                    userNetworks.add(all);
                }
            }
            return userNetworks;
        });

        String name = "network_" + rand.nextInt(100);
        boolean hidden = rand.nextBoolean();

        Network.create(owner, name, hidden);

        List<Network> userNetworks = Network.getNetworks(owner);

        assertEquals(1, userNetworks.size());
        assertEquals(name, userNetworks.get(0).getName());
    }

    @Test
    public void getNetworkByNameTest() {
        networks = new ArrayList<>();
        String name = "network_" + rand.nextInt(100);

        Mockito.when(typedQuery.getSingleResult()).thenAnswer((InvocationOnMock invocationOnMock) -> { ;
            for(Network all : networks) {
                if(all.getName().equals(name)) {
                    return all;
                }
            }
            return null;
        });

        UUID owner = UUID.randomUUID();
        boolean hidden = rand.nextBoolean();

        Network.create(owner, name, hidden);

        Network network = Network.getNetworkByName(name);

        assertNotNull(network);
        assertEquals(name, network.getName());
    }
}
