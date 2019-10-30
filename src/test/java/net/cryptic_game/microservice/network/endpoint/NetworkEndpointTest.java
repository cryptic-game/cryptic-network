package net.cryptic_game.microservice.network.endpoint;

import net.cryptic_game.microservice.network.communication.Device;
import net.cryptic_game.microservice.network.model.Network;
import net.cryptic_game.microservice.utils.JSON;
import net.cryptic_game.microservice.utils.JSONBuilder;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Network.class, Device.class})
public class NetworkEndpointTest {

    private Random rand;
    private List<Network> networks;

    @Before
    public void setUp() {
        rand = new Random();
        networks = new ArrayList<>();

        PowerMockito.mockStatic(Network.class);
        PowerMockito.mockStatic(Device.class);
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
                NetworkEndpoint.getByName(new JSON(JSONBuilder.anJSON().add("uuid", UUID.randomUUID().toString()).build()), UUID.randomUUID()));
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

        PowerMockito.when(Network.getNetworks(Mockito.any())).thenReturn(new ArrayList<Network>());

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
}
