package com.example.mentalhealth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

//=========== USER LOGIN HISTORY ============
@Entity
@Table(name = "user_login_history", indexes = {
    @Index(name = "idx_user_login", columnList = "user_id,login_time")
})
public class UserLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime = LocalDateTime.now();

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    // Constructors
    public UserLoginHistory() {
    }

    public UserLoginHistory(User user) {
        this.user = user;
        this.loginTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    // Helper method to calculate session duration
    public Long getSessionDurationMinutes() {
        if (logoutTime == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.MINUTES.between(loginTime, logoutTime);
    }
}