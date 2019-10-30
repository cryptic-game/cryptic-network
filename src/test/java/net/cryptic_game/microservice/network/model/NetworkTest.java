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
}
