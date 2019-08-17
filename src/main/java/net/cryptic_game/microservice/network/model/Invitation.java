package net.cryptic_game.microservice.network.model;

import net.cryptic_game.microservice.db.Database;
import net.cryptic_game.microservice.model.Model;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.json.simple.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invitation")
public class Invitation extends Model {

    @Type(type = "uuid-char")
    private UUID device;
    @Type(type = "uuid-char")
    private UUID network;
    private boolean request;

    private Invitation(UUID uuid, UUID device, UUID network, boolean request) {
        this.uuid = uuid;
        this.device = device;
        this.network = network;
        this.request = request;
    }

    public Invitation() {}

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

    public void revoke() {
        this.delete();
    }

    public void accept() {
        Network network = Network.get(this.network);

        if (network != null) {
            network.addMember(this.device);
        }

        this.delete();
    }

    public JSONObject serialize() {
        return JSONBuilder.anJSON()
                .add("uuid", uuid.toString())
                .add("network", network.toString())
                .add("device", device.toString())
                .add("request", request).build();
    }

    public static List<Invitation> getInvitationsOfDevice(UUID device, boolean request) {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Invitation> criteria = builder.createQuery(Invitation.class);
        Root<Invitation> from = criteria.from(Invitation.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("device"), device), builder.equal(from.get("request"), request));

        TypedQuery<Invitation> typed = session.createQuery(criteria);

        List<Invitation> results = typed.getResultList();

        session.close();
        return results;
    }

    public static List<Invitation> getInvitationsOfNetwork(UUID network, boolean request) {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Invitation> criteria = builder.createQuery(Invitation.class);
        Root<Invitation> from = criteria.from(Invitation.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("network"), network), builder.equal(from.get("request"), request));

        TypedQuery<Invitation> typed = session.createQuery(criteria);

        List<Invitation> results = typed.getResultList();

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
