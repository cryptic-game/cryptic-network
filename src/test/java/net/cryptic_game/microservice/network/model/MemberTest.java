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
public class MemberTest {

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
}
