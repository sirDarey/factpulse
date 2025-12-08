package com.sirdarey.factpulse.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_users_phone_no", columnNames = "phone_no")
        }
)
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String phoneNo;

    @Nullable
    private String name;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    @Nullable
    private ZonedDateTime updatedAt;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_user_preference_id"),
            nullable = false
    )
    private List<UserPreference> preferences;


    public User(String phoneNo, @Nullable String name, String timezone) {
        this.phoneNo = phoneNo;
        this.name = name;
        this.timezone = timezone;
        this.createdAt = ZonedDateTime.now();
    }
}