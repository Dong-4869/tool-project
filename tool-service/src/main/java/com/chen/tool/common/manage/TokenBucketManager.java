package com.chen.tool.common.manage;

import com.chen.tool.common.domain.dto.TokenBucketDTO;
import com.chen.tool.common.util.RedisUtils;
import com.chen.tool.common.util.RedissonLockUtil;
import org.springframework.stereotype.Component;

/**
 * 令牌桶管理
 * @author 13103
 */
@Component
public class TokenBucketManager {

    public void createTokenBucket(String key, long capacity, double refillRate) {
        RedissonLockUtil.executeWithLock("TokenBucketManager:"+key,()->{
            TokenBucketDTO tokenBucket = RedisUtils.get(key, TokenBucketDTO.class);
            if(tokenBucket == null) {
                tokenBucket = new TokenBucketDTO(capacity, refillRate);
                double expireTime = tokenBucket.getCapacity() *  (1.00 / tokenBucket.getRefillRate());
                RedisUtils.set(key,tokenBucket,(long)expireTime);
            }
            return null;
        });
    }

    public boolean tryAcquire(String key, int permits) {
        return RedissonLockUtil.executeWithLock("TokenBucketManager:"+key,()-> {
            TokenBucketDTO tokenBucket = RedisUtils.get(key, TokenBucketDTO.class);
            if (tokenBucket != null) {
                boolean acquire = tokenBucket.tryAcquire(permits);
                double expireTime = tokenBucket.getCapacity() *  (1.00 / tokenBucket.getRefillRate());
                RedisUtils.set(key,tokenBucket,(long)expireTime);
                return acquire;
            }
            return false;
        });
    }

    public void deductionToken(String key, int permits) {
        RedissonLockUtil.executeWithLock("TokenBucketManager:"+key,()-> {
            TokenBucketDTO tokenBucket = RedisUtils.get(key, TokenBucketDTO.class);
            if (tokenBucket != null) {
                tokenBucket.deductionToken(permits);
                double expireTime = tokenBucket.getCapacity() *  (1.00 / tokenBucket.getRefillRate());
                RedisUtils.set(key,tokenBucket,(long)expireTime);
            }
            return null;
        });
    }
}
