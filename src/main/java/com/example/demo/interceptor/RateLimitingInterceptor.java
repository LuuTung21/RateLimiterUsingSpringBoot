package com.example.demo.interceptor;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    @Value("${rate.limit.requests}")
    private int maxRequests;

    @Value("${rate.limit.duration.seconds}")
    private long timeWindowsSeconds;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        // 1. Get the client's IP Address
        String ipAddress = request.getRemoteAddr();

        // 2. Construct a unique Redis key
        // We use a fixed window: all seconds from 0-59 fall into the same minute
        long currentMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
        String redisKey = "rate_limit:" + ipAddress + ":" + currentMinute;

        // 3. Increment the request count for the key
        Long requestCount = redisTemplate.opsForValue().increment(redisKey);

        // 4. Set an expiration on the key only if it's new (count is 1).
        if (requestCount != null && requestCount == 1){
            redisTemplate.expire(redisKey, timeWindowsSeconds, TimeUnit.SECONDS);
        }

        // 5. Check if the count has exceeded the limit.
        if (requestCount != null && requestCount > maxRequests){
            // 6. If the limit is exceeded, block the request.
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too Many Requests");
            return false; // Stop the request from proceeding.
        }

        return true; // Allow the request to proceed.
    }
}
