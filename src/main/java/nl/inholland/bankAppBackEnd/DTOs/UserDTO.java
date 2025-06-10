package nl.inholland.bankAppBackEnd.DTOs;

import nl.inholland.bankAppBackEnd.models.User;

public class UserDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String bsnNumber;
    private String role;
    private boolean approved;

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getBsnNumber() {
        return bsnNumber;
    }

    public String getRole() {
        return role;
    }

    public boolean isApproved() {
        return approved;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setBsnNumber(String bsnNumber) {
        this.bsnNumber = bsnNumber;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    // Factory method
    public static UserDTO fromEntity(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setBsnNumber(user.getBsnNumber());
        dto.setRole(user.getRole() != null ? user.getRole().toString() : null);
        dto.setApproved(user.isApproved());
        return dto;
    }
}