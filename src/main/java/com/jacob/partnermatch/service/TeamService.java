package com.jacob.partnermatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jacob.partnermatch.model.domain.Team;
import com.jacob.partnermatch.model.domain.User;
import com.jacob.partnermatch.model.request.*;
import com.jacob.partnermatch.model.vo.TeamUserVO;

import java.util.List;

/**
* @author jacoe
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-06-06 16:06:00
*/
public interface TeamService extends IService<Team> {
    /**
     * 新建队伍
     *
     * @param team      队伍信息
     * @param loginUser 当前用户
     * @return 队伍id
     */
    Long addTeam(Team team, User loginUser);
    /**
     * 获取队伍列表
     *
     * @param teamListRequest 请求体
     * @param loginUser       登录用户
     * @return List<TeamUserVO>
     */
    List<TeamUserVO> getTeamList(TeamListRequest teamListRequest, User loginUser);
    /**
     * 更新队伍信息
     * @param teamUpdateRequest 请求体
     * @param loginUser 登录用户
     * @return 是否更新成功
     */
    Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);
    /**
     * 加入队伍
     * @param teamJoinRequest 请求体
     * @param loginUser 登录用户
     * @return 是否加入成功
     */
    Boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest 请求体
     * @param loginUser 当前用户
     * @return 退出是否成功
     */
    Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 解散队伍
     * @param teamDeleteRequest 请求体
     * @param loginUser 当前用户
     * @return 解散是否成功
     */
    Boolean deleteTeam(TeamDeleteRequest teamDeleteRequest, User loginUser);
}
