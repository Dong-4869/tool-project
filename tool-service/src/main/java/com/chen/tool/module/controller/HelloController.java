package com.chen.tool.module.controller;

import com.chen.tool.common.annotation.FrequencyControl;
import com.chen.tool.common.annotation.RedissonLock;
import com.chen.tool.common.enums.FrequencyControlConstant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ChenXD
 * @date 2024/05/12/17:22
 * @description
 */
@RestController
public class HelloController {

    @RedissonLock(key = "1")
    @GetMapping("/hello/1")
    public String hello() throws InterruptedException {
        Thread.sleep(1000);
        return "hello";
    }

    @FrequencyControl(strategy = FrequencyControlConstant.TOKEN_BUCKET,
            target = FrequencyControl.Target.INPUT,
            spEl = "hello2",capacity = 10,refillRate = 1)
    @GetMapping("/hello/2")
    public String hello2() {
        return "hello2";
    }

}
