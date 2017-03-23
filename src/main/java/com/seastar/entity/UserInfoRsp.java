package com.seastar.entity;

/**
 * Created by osx on 17/3/22.
 */
public class UserInfoRsp {
    private String username;
    private String email;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
