package com.example.attendance.domain.staff;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "professors")
public class Professor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    @Column(name = "office_room")
    private String officeRoom;

    @Column(name = "office_hours")
    private String officeHours;

    protected Professor() {}

    public Professor(String title, String fullName, String email, String officeRoom, String officeHours) {
        this.title = title;
        this.fullName = fullName;
        this.email = email;
        this.officeRoom = officeRoom;
        this.officeHours = officeHours;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getOfficeRoom() { return officeRoom; }
    public String getOfficeHours() { return officeHours; }

    public void setTitle(String v) { this.title = v; }
    public void setFullName(String v) { this.fullName = v; }
    public void setEmail(String v) { this.email = v; }
    public void setOfficeRoom(String v) { this.officeRoom = v; }
    public void setOfficeHours(String v) { this.officeHours = v; }
}
