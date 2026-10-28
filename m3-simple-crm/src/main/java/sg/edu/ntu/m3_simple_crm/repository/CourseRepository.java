package sg.edu.ntu.m3_simple_crm.repository;

import sg.edu.ntu.m3_simple_crm.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for Course entity.
 * Provides standard CRUD operations and custom query methods.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {
}