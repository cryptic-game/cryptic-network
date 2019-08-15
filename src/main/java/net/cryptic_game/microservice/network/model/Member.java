package net.cryptic_game.microservice.network.model;

import net.cryptic_game.microservice.db.Database;
import net.cryptic_game.microservice.model.Model;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import org.json.simple.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "network_member")
public class Member extends Model {

	@Type(type="uuid-char")
	private UUID device;
	@Type(type="uuid-char")
	private UUID network;

	private static String tablename = "network_member";

	private Member(UUID uuid, UUID device, UUID network) {
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
		Map<String, Object> jsonMap = new HashMap<>();

		jsonMap.put("uuid", getUUID().toString());
		jsonMap.put("network", getNetwork().toString());
		jsonMap.put("device", getDevice().toString());

		return new JSONObject(jsonMap);
	}

	public static List<Network> getNetworks(UUID device) {
		Session session = Database.getInstance().openSession();

		Criteria crit = session.createCriteria(Member.class);
		crit.add(Restrictions.eq("device", device));
		List<Network> results = crit.list();

		session.close();
		return results;
	}

	public static List<Member> getMembers(UUID network) {
		Session session = Database.getInstance().openSession();

		Criteria crit = session.createCriteria(Member.class);
		crit.add(Restrictions.eq("network", network));
		List<Member> results = crit.list();

		session.close();
		return results;
	}

	public static Member create(UUID device, UUID network) {
		UUID uuid = UUID.randomUUID();
		Member member = new Member(uuid, device, network);

		Session session = Database.getInstance().openSession();
		session.beginTransaction();

		session.save(member);

		session.getTransaction().commit();
		session.close();

		return member;
	}
}
