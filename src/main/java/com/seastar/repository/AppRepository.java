package com.seastar.repository;

import com.seastar.model.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by osx on 17/3/14.
 */
public interface AppRepository extends JpaRepository<App, Integer> {
    @Query("select max(a.id) from App a")
    Integer findMaxId();
}
