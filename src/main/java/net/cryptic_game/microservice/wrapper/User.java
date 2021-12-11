package net.cryptic_game.microservice.wrapper;

import java.util.Date;
import java.util.UUID;

public class User {

    UUID uuid;
    String name;
    Date created;
    Date last;

    public User(UUID uuid, String name, Date created, Date last) {
        this.uuid = uuid;
        this.name = name;
        this.created = created;
        this.last = last;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLast() {
        return last;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof User) return this.uuid.equals(((User) obj).getUUID());
        else return super.equals(obj);
    }
}
