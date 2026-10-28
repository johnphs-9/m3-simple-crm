package sg.edu.ntu.m3_simple_crm.repository;

import sg.edu.ntu.m3_simple_crm.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Student entity.
 * Provides standard CRUD operations and custom query methods.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

}