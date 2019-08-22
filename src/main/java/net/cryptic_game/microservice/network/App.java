package net.cryptic_game.microservice.network;

import net.cryptic_game.microservice.db.Database;
import org.apache.log4j.BasicConfigurator;

import net.cryptic_game.microservice.MicroService;

public class App extends MicroService {

    private App() {
        super("network");

        new Database();
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();

        new App();
    }

}
