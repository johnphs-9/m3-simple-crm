package sg.edu.ntu.m3_simple_crm.service;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import sg.edu.ntu.m3_simple_crm.model.Customer;
import sg.edu.ntu.m3_simple_crm.repository.CustomerRepository;

@Component
public class DataLoader {
    private CustomerRepository customerRepository;

    // @Autowired
    public DataLoader(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostConstruct
    public void loadData() {
        // Clear the database first
        customerRepository.deleteAll();

        // Load seed data
        customerRepository.save(new Customer("Tony", "Stark"));
        customerRepository.save(new Customer("Bruce", "Banner"));
        customerRepository.save(new Customer("Peter", "Parker"));
        customerRepository.save(new Customer("Stephen", "Strange"));
    }
}
