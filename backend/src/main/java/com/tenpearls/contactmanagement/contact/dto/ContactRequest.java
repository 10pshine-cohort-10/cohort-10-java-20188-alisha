package com.tenpearls.contactmanagement.contact.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class ContactRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @Size(max = 150)
    private String title;
    @Size(max = 150)
    private String company;
    @Size(max = 100)
    private String department;
    @Size(max = 255)
    private String address;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String state;
    @Size(max = 100)
    private String country;
    @Size(max = 50)
    private String category;
    private Boolean isFavorite;
    @Size(max = 30)
    private String avatarColor;
    private String notes;

    private List<@NotNull @Valid EmailItem> emails = new ArrayList<>();

    private List<@NotNull @Valid PhoneItem> phones = new ArrayList<>();

    public static class EmailItem {
        @Size(max = 64)
        private String id;
        @NotBlank(message = "Email value is required")
        @Email(message = "Invalid email format")
        @Size(max = 255)
        private String email;
        @NotBlank(message = "Email label is required")
        @Size(max = 30)
        @Pattern(regexp = "work|personal|other", message = "Email label must be work, personal, or other")
        private String label;
        private Boolean isPrimary;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    }

    public static class PhoneItem {
        @Size(max = 64)
        private String id;
        @NotBlank(message = "Phone number is required")
        @Size(max = 60)
        private String number;
        @NotBlank(message = "Phone label is required")
        @Size(max = 30)
        @Pattern(regexp = "work|mobile|home|personnel|other", message = "Phone label is not supported")
        private String label;
        private Boolean isPrimary;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<EmailItem> getEmails() { return emails; }
    public void setEmails(List<EmailItem> emails) { this.emails = emails; }

    public List<PhoneItem> getPhones() { return phones; }
    public void setPhones(List<PhoneItem> phones) { this.phones = phones; }
}
