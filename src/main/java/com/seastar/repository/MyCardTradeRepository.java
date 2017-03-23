package com.seastar.repository;

import com.seastar.model.MyCardTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

/**
 * Created by osx on 17/3/22.
 */
public interface MyCardTradeRepository extends JpaRepository<MyCardTrade, Integer> {
    MyCardTrade findByMycardTradeNo(String mycardTradeNO);

    @Query("select o from MyCardTrade o where o.tradeDateTime > ?1 and o.tradeDateTime < ?2")
    List<MyCardTrade> findByTime(Date beginTime, Date endTime);
}
