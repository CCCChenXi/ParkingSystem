package com.xigeandwillian.parkingsystem;

import org.junit.jupiter.api.Test;

class WaitNotifyWayTest {

    private static final Object LOCK = new Object();
    private static boolean flag = false;

    @Test
    void testWaitNotify() throws InterruptedException {
        Thread threadB = new Thread(() -> {
            synchronized (LOCK) {
                while (!flag) {
                    try {
                        System.out.println("线程 B：条件不满足，我先睡会（释放锁）...");
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("线程 B：被唤醒了，开始干活！");
            }
        });

        Thread threadA = new Thread(() -> {
            synchronized (LOCK) {
                System.out.println("线程 A：正在准备数据...");
                flag = true;
                LOCK.notify();
                System.out.println("线程 A：已发出唤醒通知，但我还没出 synchronized 块，锁还没给它...");
            }
        });

        threadB.start();
        Thread.sleep(1000);
        threadA.start();

        threadA.join();
        threadB.join();
    }

}
