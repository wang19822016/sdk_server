package com.seastar.repository;

import com.seastar.model.PayInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by osx on 17/3/16.
 */
public interface PayInfoRepository extends JpaRepository<PayInfo, Long> {
}
