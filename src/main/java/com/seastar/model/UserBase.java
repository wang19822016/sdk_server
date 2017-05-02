package com.seastar.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by osx on 16/12/12.
 */
@Entity
@Table(name = "user_base", indexes = {
        @Index(name = "single_sku_index", columnList = "name")
})
public class UserBase {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "password", nullable = false, length = 32)
    private String password;

    @Column(name = "email", length = 40)
    private String email;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time", columnDefinition = "DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    public UserBase() {}

    public UserBase(Long id, String name, String password, String email, Integer status, Date createTime) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.email = email;
        this.status = status;
        this.createTime = createTime;
    }

    public UserBase(Long id, String password, String email, Integer status, Date createTime) {
        this.id = id;
        this.name = String.format("ST%06d", id);
        this.password = password;
        this.email = email;
        this.status = status;
        this.createTime = createTime;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public Integer getStatus() {
        return status;
    }

    public Date getCreateTime() {
        return createTime;
    }
}
