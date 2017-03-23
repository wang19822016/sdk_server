package com.seastar.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by osx on 16/12/12.
 */
@Entity
@Table(name = "user_channel", indexes = {
        @Index(name = "multi_uc_index", columnList = "channel_id,channel_type"),
        @Index(name = "single_uc_index", columnList = "user_id")
})
public class UserChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "`id`")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "channel_id", nullable = false, length = 60)
    private String channelId;

    @Column(name = "channel_type")
    private Integer channelType;

    @Column(name = "create_time", columnDefinition = "DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    public UserChannel() {}

    public UserChannel(Long userId, String channelId, Integer channelType, Date createTime) {
        this.userId = userId;
        this.channelId = channelId;
        this.channelType = channelType;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getChannelId() {
        return channelId;
    }

    public Integer getChannelType() {
        return channelType;
    }

    public Date getCreateTime() {
        return createTime;
    }
}
