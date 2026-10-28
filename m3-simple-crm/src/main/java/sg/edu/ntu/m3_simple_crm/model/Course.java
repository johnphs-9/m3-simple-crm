package sg.edu.ntu.m3_simple_crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
// import java.util.HashSet;
// import java.util.Set;

/**
 * Represents a Course entity in the School Management System.
 * Each course can have multiple students.
 */
@Entity
@Table(name = "course")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Course {

    /**
     * Unique identifier for the course.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the course.
     */
    @Column(nullable = false, unique = true)
    @NonNull
    private String courseName;

    /**
     * The course code (unique identifier).
     */
    @Column(nullable = false, unique = true)
    @NonNull
    private String courseCode;
}