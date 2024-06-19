package com.jacob.partnermatch.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jacob.partnermatch.model.domain.User;
import com.jacob.partnermatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Component
@Slf4j
public class preCacheJob {
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    private List<Long> mainUserList=Arrays.asList(1L);

    @Scheduled(cron = "0 57 17 * * ?")
    private void doPreCacheRecommendUser(){
        RLock lock = redissonClient.getLock("partnerMatch:preCacheJob:recommmendUser:lock");
        System.out.println("getLock: " + Thread.currentThread().getId());

        try {
            if(lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                for(Long userId:mainUserList){
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<User>(1, 20), queryWrapper);
                    String redisKey = String.format("partnermatch:user:recommend:%s", userId);
                    try {
                        redisTemplate.opsForValue().set(redisKey, userPage,300000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("set RedisKey error",e);
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            if(lock.isHeldByCurrentThread()){
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
