import java.util.concurrent.Semaphore;

/**
 * @author Jay
 * @date 2025/8/30 21:40
 * @description
 */
public class test {
    private final Object lock = new Object();
    private int count = 0;

    public static void main(String[] args) {

        test test = new test();

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (test.lock) {
                    System.out.println("t2 检测开始");
                    if (test.count != 5) {
                        try {
                            test.lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    test.lock.notify();
                    System.out.println("t2 检测结束");
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (test.lock) {
                    for (int i = 0; i < 10; i++) {
                        System.out.println("t1 打印" + test.count);
                        test.count++;
                        if (test.count == 5) {
                            test.lock.notify();
                            try {
                                test.lock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    System.out.println("t1 打印结束");
                }
            }
        }).start();
    }
}
