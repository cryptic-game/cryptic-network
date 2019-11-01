package net.cryptic_game.microservice.network.communication;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.microservice.utils.JSONBuilder;
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

import java.util.UUID;

import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MicroService.class)
public class DeviceTest {

    @Mock
    private MicroService microService;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(MicroService.class);

        MockitoAnnotations.initMocks(this);

        PowerMockito.when(MicroService.getInstance()).thenReturn(microService);
    }

    @Test
    public void isOwnerTest() {
        UUID user = UUID.randomUUID();

        PowerMockito.when(microService.contactMicroService(Mockito.eq("device"), Mockito.any(), Mockito.any()))
                .thenAnswer((InvocationOnMock invocationOnMock) -> JSONBuilder.anJSON().add("owner", user).build());

        PowerMockito.when(microService.contactMicroService(Mockito.eq("service"), Mockito.any(), Mockito.any()))
                .thenAnswer((InvocationOnMock invocationOnMock) -> JSONBuilder.anJSON().add("ok", true).build());

        assertTrue(Device.checkPermissions(UUID.randomUUID(), user));
    }
}
