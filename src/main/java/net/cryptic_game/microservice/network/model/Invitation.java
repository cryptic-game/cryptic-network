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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Entity
@Table(name = "invitation")
public class Invitation extends Model {

	@Type(type="uuid-char")
	private UUID device;
	@Type(type="uuid-char")
	private UUID network;
	private boolean request;

	private Invitation(UUID uuid, UUID device, UUID network, boolean request) {
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

	public void revoke() { this.delete(); }

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
		Session session = Database.getInstance().openSession();

		Criteria crit = session.createCriteria(Invitation.class);
		crit.add(Restrictions.eq("device", device));
		crit.add(Restrictions.eq("request", request));
		List<Invitation> results = crit.list();

		session.close();
		return results;
	}

	public static List<Invitation> getInvitationsOfNetwork(UUID network, boolean request) {
		Session session = Database.getInstance().openSession();

		Criteria crit = session.createCriteria(Invitation.class);
		crit.add(Restrictions.eq("network", network));
		crit.add(Restrictions.eq("request", request));
		List<Invitation> results = crit.list();

		session.close();
		return results;
	}
	public static Invitation getInvitation(UUID uuid) {
		Session session = Database.getInstance().openSession();
		session.beginTransaction();

		Invitation invitation = session.get(Invitation.class, uuid);

		session.getTransaction().commit();
		session.close();

		return invitation;
	}

	public static Invitation request(UUID device, UUID network) {
		return create(device, network, true);
	}

	public static Invitation invite(UUID device, UUID network) {
		return create(device, network, false);
	}

	private static Invitation create(UUID device, UUID network, boolean request) {
		UUID uuid = UUID.randomUUID();

		Invitation invitation = new Invitation(uuid, device, network, request);

		Session session = Database.getInstance().openSession();
		session.beginTransaction();

		session.save(invitation);

		session.getTransaction().commit();
		session.close();

		return invitation;
	}
}
