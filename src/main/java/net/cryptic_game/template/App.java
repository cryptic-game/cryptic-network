package net.cryptic_game.template;

import org.apache.log4j.BasicConfigurator;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.template.endpoint.TemplateEndpoint;

public class App extends MicroService {

	public App() {
		super("template");

		addUserEndpoint(new TemplateEndpoint());
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();

		App app = new App();

		app.start();
	}

}
