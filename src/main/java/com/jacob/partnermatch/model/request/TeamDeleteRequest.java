package com.jacob.partnermatch.model.request;

import lombok.Data;

@Data
public class TeamDeleteRequest {
    /**
     * 要解散的队伍id
     */
    private Long id;
}
