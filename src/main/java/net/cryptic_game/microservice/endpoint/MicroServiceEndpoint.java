package net.cryptic_game.microservice.endpoint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MicroServiceEndpoint {

    String[] path();

    String[] keys();

    Class<?>[] types();

}
