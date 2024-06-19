package com.jacob.partnermatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jacob.partnermatch.common.ErrorCode;
import com.jacob.partnermatch.exception.BusinessException;
import com.jacob.partnermatch.model.domain.Team;
import com.jacob.partnermatch.mapper.TeamMapper;
import com.jacob.partnermatch.model.domain.User;
import com.jacob.partnermatch.model.domain.UserTeam;
import com.jacob.partnermatch.model.enums.TeamStatusEnum;
import com.jacob.partnermatch.model.request.*;
import com.jacob.partnermatch.model.vo.TeamUserVO;
import com.jacob.partnermatch.model.vo.UserVO;
import com.jacob.partnermatch.service.TeamService;
import com.jacob.partnermatch.service.UserService;
import com.jacob.partnermatch.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author jacoe
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-06-06 16:06:00
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(Team team, User loginUser) {


        //1. 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long loginUserId = loginUser.getId();
        //3. 校验信息
        //    1. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum <= 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数错误");
        }
        //    2. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称错误");
        }
        //    3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述格式错误");
        }

        //    4. status 是否公开（int）不传默认为 0（公开）
        Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statue = TeamStatusEnum.getStatue(status);
//        if(statue==null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"状态错误");
//        }
        //        如果 status 是加密状态，一定要有密码，且密码 <= 32
        if (status == 2) {
            String password = team.getPassword();
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式错误");
            }
        }
        //    5. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍已超时");
        }
        //    6. 校验用户最多创建 5 个队
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", loginUserId);
        long count = this.count(teamQueryWrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍数量超出限制");
        }
        //4. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(loginUserId);
        boolean save = this.save(team);
        Long teamId = team.getId();
        if (!save || teamId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }

        //5. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(loginUserId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        boolean save1 = userTeamService.save(userTeam);
        if (!save1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        return teamId;

    }


    @Override
    public List<TeamUserVO> getTeamList(TeamListRequest teamListRequest, User loginUser) {
        //todo分页展示队伍列表，
        boolean isAdmin = userService.isAdmin(loginUser);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        //1. 根据id、idList、名称、最大人数、描述、用户id、状态等搜索队伍 ，信息流中不展示已过期的队伍。
        if (teamListRequest != null) {
            Long id = teamListRequest.getId();
            if (id != null && id > 0) {
                teamQueryWrapper.eq("id", id);
            }
            List<Long> idList = teamListRequest.getIdList();
            if (idList != null && !idList.isEmpty()) {
                teamQueryWrapper.in("id", idList);
            }
            Long userId = teamListRequest.getUserId();
            if (userId != null && userId > 0) {
                teamQueryWrapper.eq("userId", userId);
            }
            Integer maxNum = teamListRequest.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                teamQueryWrapper.eq("maxNum", maxNum);
            }
            String name = teamListRequest.getName();
            if (StringUtils.isNotBlank(name)) {
                teamQueryWrapper.like("name", name);
            }
            String description = teamListRequest.getDescription();
            if (StringUtils.isNotBlank(description)) {
                teamQueryWrapper.like("description", description);
            }
            //可以通过某个关键词同时对名称和描述查询
            String searchText = teamListRequest.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                teamQueryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            Integer statue = teamListRequest.getStatus();
            TeamStatusEnum statueEnum = TeamStatusEnum.getStatue(statue);
            if(statueEnum != null) {
                teamQueryWrapper.eq("status", statueEnum.getStatus());

            }
//            if (statueEnum == null) {vzzxxzzxxxdddttg
//                statueEnum = TeamStatusEnum.PUBLIC;
//            }
            //**只有管理员才能查看非公开的房间**
            if (!isAdmin && TeamStatusEnum.PRIVATE.equals(statueEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }

        }
        //2. 不展示已过期的队伍（根据过期时间筛选）
        teamQueryWrapper.and(qw -> qw.isNull("expireTime").or().gt("expireTime", new Date()));
        List<Team> teamList = this.list(teamQueryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        //关联创建人用户信息
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long createTeamUserId = team.getUserId();
            if (createTeamUserId == null) {
                continue;
            }
            User createuser = userService.getById(createTeamUserId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            if (createuser != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(createuser, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        //3.判断当前用户是否加入队伍
        List<Long> teamIdList = teamUserVOList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.in("teamId", teamIdList);
        userTeamQueryWrapper.eq("userId", loginUser.getId());
        //当前用户加入的所有队伍
        List<UserTeam> loginUserTeamList = userTeamService.list(userTeamQueryWrapper);
        Set<Long> hasJoinTeamId = loginUserTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
        teamUserVOList.forEach(teamUserVO -> {
            if (hasJoinTeamId.contains(teamUserVO.getId())) {
                teamUserVO.setHasJoin(true);
            }
        });
        //4.获取每个队伍加入的人数
        QueryWrapper<UserTeam> teamJoinNumQueryWrapper = new QueryWrapper<>();
        teamJoinNumQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> teamUserList = userTeamService.list(teamJoinNumQueryWrapper);
        Map<Long, List<UserTeam>> teamUserMap = teamUserList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamUserVOList.forEach(teamUserVO -> {
            teamUserVO.setHasJoinNum(teamUserMap.getOrDefault(teamUserVO.getId(), new ArrayList<>()).size());
        });
        return teamUserVOList;
    }


    @Override
    public Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        //1. 判断请求参数是否为空
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 查询队伍是否存在
        Long teamId = teamUpdateRequest.getId();
        if(teamId == null || teamId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        //3. 只有管理员或者队伍的创建者可以修改
        if (!userService.isAdmin(loginUser) && !Objects.equals(team.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //4. 如果用户传入的新值和老值一致，就不用 update 了（可自行实现，降低数据库使用次数）
        //5. 如果队伍状态改为加密，必须要有密码
        if (teamUpdateRequest.getStatus() == TeamStatusEnum.ENCRYPT.getStatus() &&
                StringUtils.isBlank(teamUpdateRequest.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密房间必须要有密码");

        }
        //6. 更新成功
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);

    }


    @Override
    public Boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        //其他人、未满、未过期，允许加入多个队伍，但是要有个上限  P0

        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //队伍必须存在
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        //只能加入未过期的队伍
        Date expireTime = team.getExpireTime();
        if (expireTime!=null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
//        //不能加入自己的队伍
//        if(team.getUserId().equals(loginUser.getId())){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止加入自己创建的队伍");
//        }
        //禁止加入私有的队伍
        if (team.getStatus() == TeamStatusEnum.PRIVATE.getStatus()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //5. 如果加入的队伍是加密的，必须密码匹配才可以
        if (team.getStatus() == TeamStatusEnum.ENCRYPT.getStatus()
                && !team.getPassword().equals(teamJoinRequest.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        RLock lock = redissonClient.getLock("partnermatch:joinTeam:lock");
        Long userId = loginUser.getId();
        try {
            while(true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());

                    //只能加入未满的队伍
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("teamId", teamId);
                    List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                    if (userTeamList.size() >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    //不能重复加入已加入的队伍（幂等性）
                    userTeamQueryWrapper.eq("userId", userId);
                    long count = userTeamService.count(userTeamQueryWrapper);
                    if (count >= 1) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "已加入该队伍");
                    }
                    //1. 用户最多加入 5 个队伍
                    long hasJoinTeam = userTeamService.count(new QueryWrapper<UserTeam>().eq("userId", userId));
                    if (hasJoinTeam > 5) {
                        throw new BusinessException(ErrorCode.NO_AUTH, "加入的队伍超过限制");
                    }
                    //6. 新增队伍 - 用户关联信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setTeamId(teamId);
                    userTeam.setUserId(userId);
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        Long teamId = teamQuitRequest.getTeamId();

        //校验请求参数
        if (teamId == null || teamId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 校验队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        //3. 校验我是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        userTeamQueryWrapper.eq("userId", loginUser.getId());
        long hasJoinTeam = userTeamService.count(userTeamQueryWrapper);
        if (hasJoinTeam !=1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入该队伍");
        }


        //4. 如果队伍
        QueryWrapper<UserTeam> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("teamId", teamId);
        teamQueryWrapper.last("order by id asc");
        List<UserTeam> joinUserList = userTeamService.list(teamQueryWrapper);
        //1. 只剩一人，队伍解散
        if (joinUserList.size() ==1) {
            this.removeById(teamId);
            userTeamService.removeById(teamId);
        }else{
            // 2. 还有其他人
            if(Objects.equals(team.getUserId(), loginUser.getId())){
                //1. 如果是队长退出队伍，权限转移给第二早加入的用户 —— 先来后到（只用取 id 最小的 2 条数据）
                team.setUserId(joinUserList.get(1).getUserId());
                boolean result = this.updateById(team);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队长失败");
                }
            }

            //        2. 非队长，自己退出队伍

        }
        //移除关系
        return userTeamService.remove(userTeamQueryWrapper);

    }

    @Override
    public Boolean deleteTeam(TeamDeleteRequest teamDeleteRequest, User loginUser) {
        //1. 校验请求参数
        Long teamId = teamDeleteRequest.getId();
        if (teamId == null || teamId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 校验队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        //3. 校验你是不是队伍的队长
        if(!Objects.equals(team.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //4. 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"解散队伍失败");
        }
        //5. 删除队伍
        return this.removeById(teamId);

    }
}




