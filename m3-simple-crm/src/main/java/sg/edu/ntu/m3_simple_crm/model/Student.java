package sg.edu.ntu.m3_simple_crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a Student entity in the School Management System.
 * Each student is associated with a course via a many-to-one relationship.
 */
@Entity
@Table(name = "student")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Student {

    /**
     * Unique identifier for the student.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * First name of the student.
     */
    @Column(nullable = false)
    @NonNull
    private String firstName;

    /**
     * Last name of the student.
     */
    @Column(nullable = false)
    @NonNull
    private String lastName;

    /**
     * Email address of the student.
     */
    @Column(nullable = false, unique = true)
    @NonNull
    private String email;

    /**
     * Many-to-one relationship with Course.
     * Each student belongs to exactly one course.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @NonNull
    private Course course;
}