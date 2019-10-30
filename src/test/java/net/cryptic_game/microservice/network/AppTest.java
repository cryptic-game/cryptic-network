package net.cryptic_game.microservice.network;

import net.cryptic_game.microservice.endpoint.MicroServiceEndpoint;
import net.cryptic_game.microservice.endpoint.UserEndpoint;
import net.cryptic_game.microservice.utils.Tuple;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.MethodAnnotationsScanner;

import java.lang.reflect.Method;
import java.util.*;

public class AppTest {

    @Test
    public void testAvailableEndpoints() {
        List<List<String>> userEndpoints = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Collections.singletonList("name")),
                new ArrayList<>(Collections.singletonList("get")),
                new ArrayList<>(Collections.singletonList("public")),
                new ArrayList<>(Collections.singletonList("create")),
                new ArrayList<>(Collections.singletonList("members")),
                new ArrayList<>(Collections.singletonList("member")),
                new ArrayList<>(Collections.singletonList("request")),
                new ArrayList<>(Collections.singletonList("invitations")),
                new ArrayList<>(Collections.singletonList("leave")),
                new ArrayList<>(Collections.singletonList("owner")),
                new ArrayList<>(Collections.singletonList("invite")),
                new ArrayList<>(Collections.singletonList("accept")),
                new ArrayList<>(Collections.singletonList("deny")),
                new ArrayList<>(Collections.singletonList("requests")),
                new ArrayList<>(Collections.singletonList("kick")),
                new ArrayList<>(Collections.singletonList("delete")),
                new ArrayList<>(Collections.singletonList("revoke"))
        ));

        List<List<String>> microServiceEndpoints = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Collections.singletonList("check")),
                new ArrayList<>(Collections.singletonList("delete_user"))
        ));

        Map<List<String>, Tuple<UserEndpoint, Method>> userEndpointsMap = new HashMap<>();
        Map<List<String>, Tuple<MicroServiceEndpoint, Method>> microServiceEndpointsMap = new HashMap<>();

        try {
            Reflections reflections = new Reflections("net.cryptic_game.microservice", new MethodAnnotationsScanner());

            {
                Set<Method> methods = reflections.getMethodsAnnotatedWith(UserEndpoint.class);
                for (Method method : methods) {
                    UserEndpoint methodEndpoint = method.getAnnotation(UserEndpoint.class);

                    userEndpointsMap.put(Arrays.asList(methodEndpoint.path()),
                            new Tuple<>(methodEndpoint, method));
                }
            }
            {
                Set<Method> methods = reflections.getMethodsAnnotatedWith(MicroServiceEndpoint.class);
                for (Method method : methods) {
                    MicroServiceEndpoint methodEndpoint = method.getAnnotation(MicroServiceEndpoint.class);

                    microServiceEndpointsMap.put(Arrays.asList(methodEndpoint.path()),
                            new Tuple<>(methodEndpoint, method));
                }
            }
        } catch (ReflectionsException ignored) {
        }

        for (List<String> endpoint : userEndpoints) {
            if (!userEndpointsMap.keySet().remove(endpoint)) {
                throw new AssertionError("Missing user endpoint");
            }
        }

        for (List<String> endpoint : microServiceEndpoints) {
            if (!microServiceEndpointsMap.keySet().remove(endpoint)) {
                throw new AssertionError("Missing microservice endpoint");
            }
        }

        if (userEndpointsMap.keySet().size() != 0) {
            throw new AssertionError("Too many registered user endpoints");
        }

        if (microServiceEndpointsMap.keySet().size() != 0) {
            throw new AssertionError("Too many registered microservice endpoints");
        }
    }
}
