package com.lagrodrigues.moviestore.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(nullable = false, unique = true, length = 100)
    public String username;

    @Column(name = "api_key", nullable = false, unique = true, length = 100)
    public String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public UserRole role;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
