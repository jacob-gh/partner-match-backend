package com.jacob.partnermatch.model.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class TeamJoinRequest implements Serializable {
    /**
     * 要加入的队伍id
     */
    private Long teamId;
    /**
     * 密码
     */
    private String password;

}
