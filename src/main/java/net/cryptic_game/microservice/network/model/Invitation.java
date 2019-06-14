package net.cryptic_game.microservice.network.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.cryptic_game.microservice.model.Model;

public class Invitation extends Model {

	private static String tablename = "invitation";

	static {
		db.update("CREATE TABLE IF NOT EXISTS `" + tablename
				+ "` (uuid VARCHAR(36) PRIMARY KEY, device VARCHAR(36), network VARCHAR(36), request BOOLEAN);");
	}

	private UUID device;
	private UUID network;
	private boolean request;

	private Invitation(UUID uuid, UUID device, UUID network, boolean request) {
		super(tablename);

		this.uuid = uuid;
		this.device = device;
		this.network = network;
		this.request = request;
	}

	public UUID getDevice() {
		return device;
	}

	public UUID getNetwork() {
		return network;
	}

	public boolean isRequest() {
		return request;
	}

	public void deny() {
		this.delete();
	}

	public void accept() {
		Network network = Network.get(this.network);
		
		if(network != null) {
			network.addMemeber(this.device);
		}
		
		this.delete();
	}

	public static List<Invitation> getInvitations(UUID device) {
		List<Invitation> list = new ArrayList<Invitation>();

		ResultSet rs = db.getResult("SELECT `uuid`, `network`, `request` FROM `" + tablename + "` WHERE `device`=?",
				device.toString());

		try {
			while (rs.next()) {
				list.add(new Invitation(UUID.fromString(rs.getString("uuid")), device,
						UUID.fromString(rs.getString("network")), rs.getBoolean("request")));
			}
		} catch (SQLException e) {
		}

		return list;
	}

	public static Invitation request(UUID device, UUID network) {
		return create(device, network, true);
	}

	public static Invitation invite(UUID device, UUID network) {
		return create(device, network, false);
	}

	private static Invitation create(UUID device, UUID network, boolean request) {
		UUID uuid = UUID.randomUUID();

		db.update(
				"INSERT INTO `" + tablename + "` (`uuid`, `device`, `network`, `request`) VALUES (?, ?, ?, ?)",
				uuid.toString(), device.toString(), network.toString(), request);

		return new Invitation(uuid, device, network, request);
	}

}
