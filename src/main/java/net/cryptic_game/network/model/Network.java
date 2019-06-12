package net.cryptic_game.network.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import net.cryptic_game.microservice.model.Model;

public class Network extends Model {

	private UUID owner;
	private boolean hidden;
	private String name;

	private static String tablename = "network";
	
	static {
		db.update("CREATE TABLE IF NOT EXISTS `" + tablename
				+ "` (uuid VARCHAR(36) PRIMARY KEY, name TEXT, owner VARCHAR(36), hidden BOOLEAN);");
	}

	private Network(UUID uuid, UUID owner, boolean hidden, String name) {
		super(tablename);
		this.uuid = uuid;
		this.owner = owner;
		this.hidden = hidden;
		this.name = name;
	}

	public UUID getOwner() {
		return owner;
	}

	public boolean isHidden() {
		return hidden;
	}

	public String getName() {
		return name;
	}

	public void update() {
		db.update("UPDATE `" + tablename + "` SET `owner`=?, `hidden`=?, `name`=? WHERE `uuid`=?",
				owner.toString(), hidden, name, uuid.toString());
	}

	public Member addMemeber(UUID member) {
		return Member.create(member, this.getUUID());
	}

	public static Network get(UUID uuid) {
		ResultSet rs = db.getResult("SELECT `owner`, `name`, `hidden` FROM `" + tablename + "` WHERE `uuid`=?",
				uuid.toString());

		try {
			if (rs.next()) {
				return new Network(uuid, UUID.fromString(rs.getString("owner")), rs.getBoolean("hidden"),
						rs.getString("name"));
			}
		} catch (SQLException e) {
		}

		return null;
	}

	public static Network create(UUID owner, String name, boolean hidden) {
		UUID uuid = UUID.randomUUID();

		db.update("INSERT INTO `" + tablename + "` (`uuid`, `owner`, `name`, `hidden`) VALUES (?, ?, ?, ?)",
				uuid.toString(), owner.toString(), name, hidden);

		return new Network(uuid, owner, hidden, name);
	}

}
