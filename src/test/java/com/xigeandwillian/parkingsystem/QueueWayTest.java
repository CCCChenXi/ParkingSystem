package com.xigeandwillian.parkingsystem;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class QueueWayTest {

    @Test
    void testBlockingQueue() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

        Thread threadB = new Thread(() -> {
            try {
                String msg = queue.take();
                System.out.println("线程 B 从队列拿到了消息: " + msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread threadA = new Thread(() -> {
            try {
                Thread.sleep(1000);
                queue.put("这是来自线程 A 的秘密信件");
                System.out.println("线程 A 成功发送消息到队列");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        threadB.start();
        threadA.start();

        threadA.join();
        threadB.join();
    }

}
