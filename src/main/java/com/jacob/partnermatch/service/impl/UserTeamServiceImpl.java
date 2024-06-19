package com.jacob.partnermatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jacob.partnermatch.model.domain.UserTeam;
import com.jacob.partnermatch.service.UserTeamService;
import com.jacob.partnermatch.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author jacoe
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-06-06 16:14:40
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




