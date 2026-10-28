package sg.edu.ntu.m3_simple_crm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.ntu.m3_simple_crm.exceptions.GlobalExceptionHandler;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.ntu.m3_simple_crm.model.Customer;
import sg.edu.ntu.m3_simple_crm.repository.CustomerRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerApiRegressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomerRepository customerRepository;

    private Customer existingCustomer;
    private static final long MISSING_ID = Long.MAX_VALUE;

    @BeforeEach
    void seedCustomer() {
        existingCustomer = customerRepository.saveAndFlush(Customer.builder()
                .firstName("ReviewCase").lastName("Customer")
                .email("review@example.com").contactNo("12345678")
                .jobTitle("Review Scientist").yearOfBirth(1990).build());
    }

    @Test
    void missingCustomerGetReturnsStructured404() throws Exception {
        assertError(mockMvc.perform(get("/customers/{id}", MISSING_ID)), 404);
    }

    @Test
    void missingCustomerUpdateReturnsStructured404() throws Exception {
        assertError(mockMvc.perform(put("/customers/{id}", MISSING_ID)
                .contentType(MediaType.APPLICATION_JSON).content(validJson())), 404);
    }

    @Test
    void missingCustomerDeleteReturnsStructured404() throws Exception {
        assertError(mockMvc.perform(delete("/customers/{id}", MISSING_ID)), 404);
    }

    @Test
    void missingInteractionCustomerReturnsStructured404() throws Exception {
        assertError(mockMvc.perform(post("/customers/{id}/interactions", MISSING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"remarks\":\"Follow up\",\"interactionDate\":\"2026-10-27\"}")), 404);
    }

    @Test
    void malformedJsonReturnsStructured400() throws Exception {
        assertError(mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON).content("{")), 400);
    }

    @Test
    void nonNumericIdReturnsStructured400() throws Exception {
        assertError(mockMvc.perform(get("/customers/abc")), 400);
    }

    @Test
    void missingSearchParameterReturnsStructured400() throws Exception {
        assertError(mockMvc.perform(get("/customers/search")), 400);
        assertError(mockMvc.perform(get("/customers/search/job")), 400);
    }

    @Test
    void unknownRouteReturnsStructured404() throws Exception {
        assertError(mockMvc.perform(get("/route-that-does-not-exist")), 404);
    }

    @Test
    void unsupportedMethodRetains405AndAllowHeader() throws Exception {
        assertError(mockMvc.perform(patch("/customers")), 405)
                .andExpect(header().exists("Allow"));
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("firstName", " ", "First name is mandatory"),
                Arguments.of("lastName", "", "Last name is mandatory"),
                Arguments.of("email", "invalid-email", "Email should be valid"),
                Arguments.of("contactNo", "123", "Contact number must be exactly 8 characters"),
                Arguments.of("contactNo", "123456789", "Contact number must be exactly 8 characters"),
                Arguments.of("jobTitle", "X", "Job title must be between 2 and 50 characters"),
                Arguments.of("jobTitle", "X".repeat(51), "Job title must be between 2 and 50 characters"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidCreateAndUpdateReturn400WithoutChangingData(
            String field, String value, String expectedMessage) throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put(field, value);
        String json = objectMapper.writeValueAsString(payload);
        long countBefore = customerRepository.count();

        assertError(mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON).content(json)), 400)
                .andExpect(jsonPath("$.message").value(expectedMessage));
        assertError(mockMvc.perform(put("/customers/{id}", existingCustomer.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json)), 400)
                .andExpect(jsonPath("$.message").value(expectedMessage));

        assertEquals(countBefore, customerRepository.count());
        assertEquals("ReviewCase", customerRepository.findById(existingCustomer.getId()).orElseThrow().getFirstName());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 50})
    void validLengthBoundariesAllowCreateAndUpdate(int jobTitleLength) throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("jobTitle", "X".repeat(jobTitleLength));
        String json = objectMapper.writeValueAsString(payload);
        mockMvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/customers/{id}", existingCustomer.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCustomer.getId().intValue()));
        customerRepository.flush();
        assertEquals("X".repeat(jobTitleLength), existingCustomer.getJobTitle());
    }

    @Test
    void jobTitleSearchQueriesPersistedCustomers() throws Exception {
        mockMvc.perform(get("/customers/search/job").param("jobTitle", "Review Scientist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(existingCustomer.getId().intValue()));
        mockMvc.perform(get("/customers/search/job").param("jobTitle", "No Matching Job"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void firstNameSearchStillWorks() throws Exception {
        mockMvc.perform(get("/customers/search").param("firstName", "ReviewCase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(existingCustomer.getId().intValue()));
    }

    @Test
    void deletingCustomerStillReturns204() throws Exception {
        mockMvc.perform(delete("/customers/{id}", existingCustomer.getId()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertFalse(customerRepository.existsById(existingCustomer.getId()));
    }

    @Test
    void interactionRouteStillAssociatesCustomer() throws Exception {
        mockMvc.perform(post("/customers/{id}/interactions", existingCustomer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"remarks\":\"Follow up\",\"interactionDate\":\"2026-10-27\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer.id").value(existingCustomer.getId().intValue()));
    }

    @Test
    void unexpectedFailureReturnsGenericStructured500() throws Exception {
        MockMvc failingMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        assertError(failingMvc.perform(get("/failure")), 500)
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @RestController
    static class FailingController {
        @GetMapping("/failure")
        public String fail() {
            throw new IllegalStateException("Internal database connection details");
        }
    }

    private ResultActions assertError(ResultActions result, int status) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firstName", "Updated");
        payload.put("lastName", "Customer");
        payload.put("email", "updated@example.com");
        payload.put("contactNo", "87654321");
        payload.put("jobTitle", "Engineer");
        payload.put("yearOfBirth", 1990);
        return payload;
    }

    private String validJson() {
        return objectMapper.writeValueAsString(validPayload());
    }
}
