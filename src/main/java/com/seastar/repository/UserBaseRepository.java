package com.seastar.repository;

import com.seastar.model.UserBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by osx on 17/3/14.
 */
public interface UserBaseRepository extends JpaRepository<UserBase, Long> {

    @Query("select max(user.id) from UserBase user where user.id <= 22000000000000000")
    public Long getMaxId();

    public UserBase findByName(String name);
}
