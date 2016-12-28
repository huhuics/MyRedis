/**
 * 深圳金融电子结算中心
 * Copyright (c) 1995-2016 All Rights Reserved.
 */
package cn.redis.chapter.demo;

/**
 * 
 * @author HuHui
 * @version $Id: MyCar.java, v 0.1 2016年12月28日 下午9:57:52 HuHui Exp $
 */
public class MyCar {

    /**
     * @param args
     */
    public static void main(String[] args) {
        new MyCar().run();
    }

    public void run() {
        Car car = new Car();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            audi(car);
        }
        long end = System.currentTimeMillis();
        System.out.println("反射耗时:" + (end - start) + "");

        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            car.run("audi", "80");
        }
        end = System.currentTimeMillis();
        System.out.println("正常调用耗时:" + (end - start) + "");

        //通过反射方法调用比正常调用大约慢了一个数量级,约20倍左右
    }

    private void carRun(Car car, String methodName, String name, String speed) {
        try {
            car.getClass().getDeclaredMethod(methodName, String.class, String.class).invoke(car, name, speed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void audi(Car car) {
        carRun(car, "run", "audi", "80");
    }

    public void bmw(Car car) {
        carRun(car, "run", "bmw", "100");
    }

    public void benz(Car car) {
        carRun(car, "run", "benz", "120");
    }

}
