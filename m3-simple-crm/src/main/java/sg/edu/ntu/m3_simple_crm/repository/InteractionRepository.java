package sg.edu.ntu.m3_simple_crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.ntu.m3_simple_crm.model.Interaction;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
}
