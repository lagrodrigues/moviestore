package com.lagrodrigues.moviestore.user;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Optional<User> findByApiKey(String apiKey) {
        return find("apiKey", apiKey).firstResultOptional();
    }

    public Optional<User> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }
}
