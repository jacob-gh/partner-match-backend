package com.jacob.partnermatch.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jacob.partnermatch.common.BaseResponse;
import com.jacob.partnermatch.common.ErrorCode;
import com.jacob.partnermatch.common.ResultUtils;
import com.jacob.partnermatch.exception.BusinessException;
import com.jacob.partnermatch.model.domain.Team;
import com.jacob.partnermatch.model.domain.User;
import com.jacob.partnermatch.model.domain.UserTeam;
import com.jacob.partnermatch.model.request.*;
import com.jacob.partnermatch.model.vo.TeamUserVO;
import com.jacob.partnermatch.service.TeamService;
import com.jacob.partnermatch.service.UserService;
import com.jacob.partnermatch.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static com.jacob.partnermatch.contant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:3000"},allowCredentials="true")
@Slf4j
public class TeamController {
    @Resource
    private UserTeamService userTeamService;

    @Resource
    private TeamService teamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamRequest, HttpServletRequest request) {
        if(teamRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamRequest, team);
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User loginUser = (User) userObj;
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);

    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>>getTeamList(TeamListRequest teamListRequest, HttpServletRequest request) {
        if(teamListRequest ==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        List<TeamUserVO> teamList = teamService.getTeamList(teamListRequest,loginUser);
        return ResultUtils.success(teamList);
    }
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if(teamUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(Long id) {
        if(id==null || id<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if(team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        team.setPassword(null);
        return ResultUtils.success(team);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if(teamJoinRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if(teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"退出队伍失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteteam(@RequestBody TeamDeleteRequest teamDeleteRequest, HttpServletRequest request) {
        if(teamDeleteRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Boolean result = teamService.deleteTeam(teamDeleteRequest, loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"退出队伍失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("list/my/create")
    public BaseResponse<List<TeamUserVO>>getMyTeamList(TeamListRequest teamListRequest, HttpServletRequest request){
        if(teamListRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        teamListRequest.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.getTeamList(teamListRequest, loginUser);
        return ResultUtils.success(teamList);
    }

    @GetMapping("list/my/join")
    public BaseResponse<List<TeamUserVO>>getJoinTeamList(TeamListRequest teamListRequest, HttpServletRequest request){
        if(teamListRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =(User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId());
        List<UserTeam> teamUserList = userTeamService.list(queryWrapper);
        if(CollectionUtils.isEmpty(teamUserList)){
            return ResultUtils.success(null);
        }
        ArrayList<Long> teamIdList = new ArrayList<>();
        teamUserList.forEach(t->teamIdList.add(t.getTeamId()));
        teamListRequest.setIdList(teamIdList);
        List<TeamUserVO> teamListVO = teamService.getTeamList(teamListRequest, loginUser);
        return ResultUtils.success(teamListVO);
    }




}
