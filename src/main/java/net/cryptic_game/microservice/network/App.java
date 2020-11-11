package net.cryptic_game.microservice.network;

import net.cryptic_game.microservice.MicroService;

public class App extends MicroService {

    private App() {
        super("network");
    }

    public static void main(String[] args) {
        new App();
    }

}
