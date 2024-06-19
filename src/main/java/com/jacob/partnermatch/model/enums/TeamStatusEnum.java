package com.jacob.partnermatch.model.enums;

public enum TeamStatusEnum {

    PUBLIC(0,"公开"),
    PRIVATE(1,"私密"),
    ENCRYPT(2,"加密");
    private int status;
    private String description;
    TeamStatusEnum(int status, String description) {
        this.status = status;
        this.description = description;
    }

    public static TeamStatusEnum getStatue(Integer statue) {
        if (statue == null) {
            return null;
        }
        TeamStatusEnum[] statues = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : statues) {
            if (teamStatusEnum.getStatus() == statue) {
                return teamStatusEnum;
            }
        }
        return null;

    }

    public int getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
