package com.mooar.market.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 应用关闭标记与在途任务闸门
 *
 * ContextClosedEvent 在 Bean 销毁之前发布，此时置位并等待在途定时任务结束，
 * 使扫块/提现任务不会在数据库连接池、RPC 客户端关闭之后继续访问，
 * 避免停机时产生大量无意义的异常日志，也避免中断被当成业务失败。
 */
@Component
public class AppShutdown implements ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AppShutdown.class);
    private static final long WAIT_SECONDS = 5;

    private volatile boolean shuttingDown;
    private final ReentrantLock gate = new ReentrantLock();

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shuttingDown = true;
        try {
            if (gate.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) {
                gate.unlock();
            } else {
                log.warn("在途定时任务未在 {}s 内结束，继续关闭流程", WAIT_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * 定时任务入口：关闭开始后不再执行新任务，执行中的任务会被关闭流程等待
     */
    public void runQuietly(Runnable task) {
        if (shuttingDown) {
            return;
        }
        gate.lock();
        try {
            if (shuttingDown) {
                return;
            }
            task.run();
        } finally {
            gate.unlock();
        }
    }
}
