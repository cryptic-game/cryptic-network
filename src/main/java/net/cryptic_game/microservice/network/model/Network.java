package net.cryptic_game.microservice.network.model;

import net.cryptic_game.microservice.MicroService;
import net.cryptic_game.microservice.db.Database;
import net.cryptic_game.microservice.model.Model;
import net.cryptic_game.microservice.utils.JSON;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.json.simple.JSONObject;

import javax.persistence.Entity;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "network_network")
public class Network extends Model {

    @Type(type = "uuid-char")
    private UUID owner;
    @Type(type = "uuid-char")
    private UUID user;
    private boolean hidden;
    private String name;

    private Network(UUID uuid, UUID owner, UUID user, boolean hidden, String name) {
        this.uuid = uuid;
        this.owner = owner;
        this.user = user;
        this.hidden = hidden;
        this.name = name;
    }

    public Network() {}

    public UUID getOwner() {
        return owner;
    }

    public UUID getUser() {
        return user;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getName() {
        return name;
    }

    public void update() {
        Session session = Database.getInstance().openSession();
        session.beginTransaction();

        session.save(this);

        session.getTransaction().commit();
        session.close();
    }

    public Member addMember(UUID member) {
        return Member.create(member, this.getUUID());
    }

    public JSONObject serialize() {
        return JSONBuilder.anJSON()
                .add("uuid", getUUID().toString())
                .add("hidden", isHidden())
                .add("owner", getOwner().toString())
                .add("name", getName()).build();
    }

    public static Network get(UUID uuid) {
        Session session = Database.getInstance().openSession();
        session.beginTransaction();

        Network network = session.get(Network.class, uuid);

        session.getTransaction().commit();
        session.close();

        return network;
    }

    public static List<Network> getPublicNetworks() {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = builder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("hidden"), false));
        TypedQuery<Network> typed = session.createQuery(criteria);

        List<Network> results = typed.getResultList();

        session.close();
        return results;
    }

    public static List<Network> getNetworksOfUser(UUID user) {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = builder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("user"), user));
        TypedQuery<Network> typed = session.createQuery(criteria);

        List<Network> results = typed.getResultList();

        session.close();
        return results;
    }

    public static List<Network> getNetworks(UUID device) {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = builder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("owner"), device));
        TypedQuery<Network> typed = session.createQuery(criteria);

        List<Network> results = typed.getResultList();

        session.close();
        return results;
    }

    public static int getCountOfNetworksByDevice(UUID device) {
        return Member.getNetworks(device).size();
    }

    public static Network create(UUID owner, String name, boolean hidden) {
        UUID uuid = UUID.randomUUID();

        JSONObject response = MicroService.getInstance().contactMicroService("device", new String[]{"owner"}, JSONBuilder.anJSON().add("device_uuid", owner.toString()).build());
        UUID user = new JSON(response).getUUID("owner");

        Network network = new Network(uuid, owner, user, hidden, name);

        Session session = Database.getInstance().openSession();
        session.beginTransaction();

        session.save(network);

        session.getTransaction().commit();
        session.close();

        return network;
    }

    public static Network getNetworkByName(String name) {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = builder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("name"), name));
        TypedQuery<Network> typed = session.createQuery(criteria);

        Network result;

        try {
            result = typed.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            session.close();
        }

        return result;
    }

    public static boolean checkName(String name) {
        return name.length() >= 5 && name.length() <= 20 && !name.contains(" ");
    }

}
