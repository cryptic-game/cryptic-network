package net.cryptic_game.microservice.network;

import org.apache.log4j.BasicConfigurator;

import net.cryptic_game.microservice.MicroService;

public class App extends MicroService {

    private App() {
        super("network");
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();

        new App();
    }

}
