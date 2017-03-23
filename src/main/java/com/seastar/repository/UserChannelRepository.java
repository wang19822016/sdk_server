package com.seastar.repository;

import com.seastar.model.UserChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by osx on 17/3/14.
 */
public interface UserChannelRepository extends JpaRepository<UserChannel, Long> {
    public UserChannel findByChannelIdAndChannelType(String channelId, int channelType);
    public List<UserChannel> findByUserId(Long userId);
    public void deleteByChannelIdAndChannelType(String channelId, int channelType);
}
