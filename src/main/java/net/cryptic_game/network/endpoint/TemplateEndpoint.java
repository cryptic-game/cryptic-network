package net.cryptic_game.network.endpoint;

import java.util.UUID;

import org.json.simple.JSONObject;

import net.cryptic_game.microservice.endpoint.UserEndpoint;

public class TemplateEndpoint extends UserEndpoint {

	public TemplateEndpoint() {
		super(new String[] { "template" });
	}

	@Override
	public JSONObject execute(JSONObject data, UUID user) {
		return data;
	}

}
