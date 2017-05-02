package com.seastar.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by osx on 16/12/13.
 */
@Entity
@Table(name = "app_google")
public class AppGoogle {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "`key`", nullable = false, length = 1024)
    private String key;

    public int getId() {
        return id;
    }


    public String getKey() {
        return key;
    }
}
