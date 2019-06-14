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

public class Member extends Model {

	private UUID device;
	private UUID network;

	private static String tablename = "network_member";

	static {
		db.update("CREATE TABLE IF NOT EXISTS `" + tablename
				+ "` (uuid VARCHAR(36) PRIMARY KEY, device VARCHAR(36), network VARCHAR(36));");
	}

	private Member(UUID uuid, UUID device, UUID network) {
		super(tablename);

		this.uuid = uuid;
		this.device = device;
		this.network = network;
	}

	public UUID getDevice() {
		return device;
	}

	public UUID getNetwork() {
		return network;
	}

	public JSONObject serialize() {
		Map<String, Object> jsonMap = new HashMap<String, Object>();

		jsonMap.put("uuid", getUUID().toString());
		jsonMap.put("network", getNetwork().toString());
		jsonMap.put("device", getDevice().toString());

		return new JSONObject(jsonMap);
	}

	public static List<Network> getNetworks(UUID device) {
		List<Network> list = new ArrayList<Network>();

		ResultSet rs = db.getResult("SELECT `network` FROM `" + tablename + "` WHERE `device`=?", device.toString());

		try {
			while (rs.next()) {
				list.add(Network.get(UUID.fromString(rs.getString("network"))));
			}
		} catch (SQLException e) {
		}

		return list;
	}

	public static List<Member> getMembers(UUID network) {
		List<Member> list = new ArrayList<Member>();

		ResultSet rs = db.getResult("SELECT `uuid`, `device` FROM `" + tablename + "` WHERE `network`=?",
				network.toString());

		try {
			while (rs.next()) {
				list.add(new Member(UUID.fromString(rs.getString("uuid")), UUID.fromString(rs.getString("device")),
						network));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;
	}

	protected static Member create(UUID device, UUID network) {
		UUID uuid = UUID.randomUUID();

		db.update("INSERT INTO `" + tablename + "` (`uuid`, `device`, `network`) VALUES (?, ?, ?)", uuid.toString(),
				device.toString(), network.toString());

		return new Member(uuid, device, network);
	}

}
