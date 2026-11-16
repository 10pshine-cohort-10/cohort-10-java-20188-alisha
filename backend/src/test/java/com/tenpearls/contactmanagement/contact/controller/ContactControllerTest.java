package com.tenpearls.contactmanagement.contact.controller;

import com.tenpearls.contactmanagement.contact.dto.ContactRequest;
import com.tenpearls.contactmanagement.contact.entity.Contact;
import com.tenpearls.contactmanagement.common.exception.GlobalExceptionHandler;
import com.tenpearls.contactmanagement.common.exception.ResourceNotFoundException;
import com.tenpearls.contactmanagement.contact.service.ContactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Authentication auth;

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ContactController contactController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contactController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        auth = new UsernamePasswordAuthenticationToken("usr_001", null, List.of());
    }

    @Test
    @DisplayName("GET /api/v1/contacts returns paginated contact list")
    void listContacts_Returns200() throws Exception {
        Contact c = new Contact();
        c.setId("cnt_1");
        c.setFirstName("Marcus");
        c.setLastName("Vance");

        when(contactService.getContacts(eq("usr_001"), anyString(), anyString(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(Objects.requireNonNull(List.of(Objects.requireNonNull(c)))));
        when(contactService.countTotal("usr_001")).thenReturn(1L);
        when(contactService.countFavorites("usr_001")).thenReturn(0L);

        mockMvc.perform(get("/api/v1/contacts").principal(Objects.requireNonNull(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.contacts[0].firstName").value("Marcus"));
    }

    @Test
    @DisplayName("POST /api/v1/contacts returns 201 Created")
    void createContact_Returns201() throws Exception {
        ContactRequest req = new ContactRequest();
        req.setFirstName("Elena");
        req.setLastName("Rostova");

        Contact created = new Contact();
        created.setId("cnt_2");
        created.setFirstName("Elena");
        created.setLastName("Rostova");

        when(contactService.createContact(eq("usr_001"), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/contacts")
                        .principal(Objects.requireNonNull(auth))
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("cnt_2"));
    }

    @Test
    @DisplayName("GET /api/v1/contacts/{id} returns 404 when not found")
    void getContact_NotFound_Returns404() throws Exception {
        when(contactService.getContactById("usr_001", "cnt_404"))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/contacts/cnt_404").principal(Objects.requireNonNull(auth)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
