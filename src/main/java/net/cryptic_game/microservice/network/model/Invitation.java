package net.cryptic_game.microservice.network.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;

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

		if (network != null) {
			network.addMemeber(this.device);
		}

		this.delete();
	}

	public JSONObject serialize() {
		Map<String, Object> jsonMap = new HashMap<String, Object>();

		jsonMap.put("uuid", getUUID().toString());
		jsonMap.put("network", getNetwork().toString());
		jsonMap.put("device", getDevice().toString());
		jsonMap.put("request", isRequest());

		return new JSONObject(jsonMap);
	}

	public static List<Invitation> getInvitationsOfDevice(UUID device, boolean request) {
		List<Invitation> list = new ArrayList<>();

		ResultSet rs = db.getResult("SELECT `uuid`, `network` FROM `" + tablename + "` WHERE `device`=? AND `request`=?",
				device.toString(), request);

		try {
			while (rs.next()) {
				list.add(new Invitation(UUID.fromString(rs.getString("uuid")), device,
						UUID.fromString(rs.getString("network")), request));
			}
		} catch (SQLException e) {
		}

		return list;
	}

	public static List<Invitation> getInvitationsOfNetwork(UUID network, boolean request) {
		List<Invitation> list = new ArrayList<>();

		ResultSet rs = db.getResult("SELECT `uuid`, `device` FROM `" + tablename + "` WHERE `network`=? AND `request`=?",
				network.toString(), request);

		try {
			while (rs.next()) {
				list.add(new Invitation(UUID.fromString(rs.getString("uuid")), UUID.fromString(rs.getString("device")),
						network, request));
			}
		} catch (SQLException e) {
		}

		return list;
	}
	public static Invitation getInvitation(UUID uuid) {
		ResultSet rs = db.getResult("SELECT `device`, `network`, `request` FROM `" + tablename + "` WHERE `uuid`=?",
				uuid.toString());

		try {
			if (rs.next()) {
				return new Invitation(uuid, UUID.fromString(rs.getString("device")),
						UUID.fromString(rs.getString("network")), rs.getBoolean("request"));
			}
		} catch (SQLException e) {
		}

		return null;
	}

	public static Invitation request(UUID device, UUID network) {
		return create(device, network, true);
	}

	public static Invitation invite(UUID device, UUID network) {
		return create(device, network, false);
	}

	private static Invitation create(UUID device, UUID network, boolean request) {
		UUID uuid = UUID.randomUUID();

		db.update("INSERT INTO `" + tablename + "` (`uuid`, `device`, `network`, `request`) VALUES (?, ?, ?, ?)",
				uuid.toString(), device.toString(), network.toString(), request);

		return new Invitation(uuid, device, network, request);
	}
}
