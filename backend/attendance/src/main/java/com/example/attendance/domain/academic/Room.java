package com.example.attendance.domain.academic;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    private String building;

    protected Room() {}

    public Room(String name, String building) {
        this.name = name;
        this.building = building;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getBuilding() { return building; }

    public void setName(String v) { this.name = v; }
    public void setBuilding(String v) { this.building = v; }
}
