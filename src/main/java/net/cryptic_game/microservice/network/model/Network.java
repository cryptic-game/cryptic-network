package net.cryptic_game.microservice.network.model;

import net.cryptic_game.microservice.db.Database;
import net.cryptic_game.microservice.model.Model;
import net.cryptic_game.microservice.utils.JSONBuilder;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import org.json.simple.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "network")
public class Network extends Model {

    @Type(type = "uuid-char")
    private UUID owner;
    private boolean hidden;
    private String name;

    private Network(UUID uuid, UUID owner, boolean hidden, String name) {
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

        Criteria crit = session.createCriteria(Network.class);
        crit.add(Restrictions.eq("hidden", false));
        List<Network> results = crit.list();

        session.close();
        return results;
    }

    public static List<Network> getNetworks(UUID device) {
        Session session = Database.getInstance().openSession();

        Criteria crit = session.createCriteria(Network.class);
        crit.add(Restrictions.eq("owner", device));
        List<Network> results = crit.list();

        session.close();
        return results;
    }

    public static int getCountOfNetworksByDevice(UUID device) {
        return Member.getNetworks(device).size();
    }

    public static Network create(UUID owner, String name, boolean hidden) {
        UUID uuid = UUID.randomUUID();
        Network network = new Network(uuid, owner, hidden, name);

        Session session = Database.getInstance().openSession();
        session.beginTransaction();

        session.save(network);

        session.getTransaction().commit();
        session.close();

        return network;
    }

    public static Network getNetworkByName(String name) {
        Session session = Database.getInstance().openSession();

        Criteria crit = session.createCriteria(Network.class);
        crit.add(Restrictions.eq("name", name));
        List<Network> results = crit.list();

        session.close();

        if (results.size() == 0) return null;

        return results.get(0);
    }

    public static boolean checkName(String name) {
        return name.length() >= 5 && name.length() <= 20 && !name.contains(" ");
    }

    @Override
    public void delete() {
        super.delete();

        List<Invitation> invitations = Invitation.getInvitationsOfNetwork(uuid, true);
        invitations.addAll(Invitation.getInvitationsOfNetwork(uuid, false));

        invitations.forEach(Model::delete);
    }
}
