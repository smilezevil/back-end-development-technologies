package com.smilezevil.hotelbooking.entity;

import com.smilezevil.hotelbooking.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "hotel")
public class Hotel extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    @NotAudited
    @Column(columnDefinition = "TEXT")
    private String description;
}