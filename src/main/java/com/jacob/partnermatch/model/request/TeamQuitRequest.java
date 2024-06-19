package com.jacob.partnermatch.model.request;

import lombok.Data;

@Data
public class TeamQuitRequest {
    /**
     * 要退出的队伍id
     */
    private Long teamId;
}
