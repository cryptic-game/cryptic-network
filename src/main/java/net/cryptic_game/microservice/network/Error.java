package net.cryptic_game.microservice.network;

public enum Error {

    ERROR_DEVICE_NOT_ONLINE("device_not_online");

    final String name;

    Error(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
