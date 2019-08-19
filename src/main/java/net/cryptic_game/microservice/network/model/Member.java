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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "network_member")
public class Member extends Model {

    @Type(type = "uuid-char")
    private UUID device;
    @Type(type = "uuid-char")
    private UUID network;

    private Member(UUID uuid, UUID device, UUID network) {
        this.uuid = uuid;
        this.device = device;
        this.network = network;
    }

    public Member() {}

    public UUID getDevice() {
        return device;
    }

    public UUID getNetwork() {
        return network;
    }

    public JSONObject serialize() {
        return JSONBuilder.anJSON()
                .add("uuid", getUUID().toString())
                .add("network", getNetwork().toString())
                .add("device", getDevice().toString()).build();
    }

    public static List<Network> getNetworks(UUID device) {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Member> criteria = builder.createQuery(Member.class);
        Root<Member> from = criteria.from(Member.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("device"), device));
        TypedQuery<Member> typed = session.createQuery(criteria);

        List<Member> results = typed.getResultList();
        List<Network> networks = new ArrayList<>();
        for(Member member : results) {
            networks.add(Network.get(member.getNetwork()));
        }

        session.close();
        return networks;
    }

    public static List<Member> getMembers(UUID network) {
        Session session = Database.getInstance().openSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Member> criteria = builder.createQuery(Member.class);
        Root<Member> from = criteria.from(Member.class);

        criteria.select(from);
        criteria.where(builder.equal(from.get("network"), network));
        TypedQuery<Member> typed = session.createQuery(criteria);

        List<Member> results = typed.getResultList();

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
