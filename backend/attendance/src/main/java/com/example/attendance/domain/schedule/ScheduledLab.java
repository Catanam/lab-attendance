package com.example.attendance.domain.schedule;

import com.example.attendance.domain.academic.AcademicTerm;
import com.example.attendance.domain.academic.Course;
import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.StudentGroup;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_labs")
public class ScheduledLab {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String weekday;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    protected ScheduledLab() {}

    public ScheduledLab(AcademicTerm term, Course course, StudentGroup group, Room room,
                        String weekday, LocalTime startTime, LocalTime endTime, Integer yearOfStudy) {
        this.term = term;
        this.course = course;
        this.group = group;
        this.room = room;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
        this.yearOfStudy = yearOfStudy;
    }

    public UUID getId() { return id; }
    public AcademicTerm getTerm() { return term; }
    public Course getCourse() { return course; }
    public StudentGroup getGroup() { return group; }
    public Room getRoom() { return room; }
    public String getWeekday() { return weekday; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getYearOfStudy() { return yearOfStudy; }

    public void setTerm(AcademicTerm v) { this.term = v; }
    public void setCourse(Course v) { this.course = v; }
    public void setGroup(StudentGroup v) { this.group = v; }
    public void setRoom(Room v) { this.room = v; }
    public void setWeekday(String v) { this.weekday = v; }
    public void setStartTime(LocalTime v) { this.startTime = v; }
    public void setEndTime(LocalTime v) { this.endTime = v; }
    public void setYearOfStudy(Integer v) { this.yearOfStudy = v; }
}
