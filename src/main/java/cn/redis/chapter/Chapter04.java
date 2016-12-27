/**
 * 深圳金融电子结算中心
 * Copyright (c) 1995-2016 All Rights Reserved.
 */
package cn.redis.chapter;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;

/**
 * 第四章学习,主要是redis事务
 * @author HuHui
 * @version $Id: Chapter04.java, v 0.1 2016年12月26日 下午9:10:53 HuHui Exp $
 */
public class Chapter04 {

    /**
     * @param args
     */
    public static void main(String[] args) {
        new Chapter04().run();
    }

    public void run() {

        Jedis conn = new Jedis("168.33.131.55");
        conn.select(15);

        testListItem(conn);
    }

    public void testListItem(Jedis conn) {

        System.out.println("初始化操作");

        String seller = "userX";
        String item = "itemT";
        conn.sadd("inventory:" + seller, item);
        Set<String> i = conn.smembers("inventory:" + seller);

        System.out.println("用户的仓库包含以下:");
        for (String member : i) {
            System.out.println("  " + member);
        }

        assert i.size() > 0;
        System.out.println();

        System.out.println("开始将商品放入市场");
        boolean l = listItem(conn, item, seller, 12);
        System.out.println("商品放入市场是否成功?" + l);
        assert l;
        Set<Tuple> r = conn.zrangeWithScores("market:", 0, -1);

        System.out.println("市场中存在商品:");
        for (Tuple tuple : r) {
            System.out.println("  " + tuple.getElement() + ", " + tuple.getScore());
        }

        assert r.size() > 0;

    }

    public boolean listItem(Jedis conn, String itemId, String sellerId, double price) {

        String inventory = "inventory:" + sellerId;
        String item = itemId + '.' + sellerId;
        long end = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < end) {
            conn.watch(inventory);
            if (!conn.sismember(inventory, itemId)) {
                conn.unwatch();
                return false;
            }

            Transaction trans = conn.multi();
            trans.zadd("market:", price, item);
            trans.srem(inventory, itemId);
            List<Object> results = trans.exec();

            //如果返回为空,则说明被监控的key发生了变化
            if (results == null) {
                continue;
            }
            return true;
        }

        return false;
    }

}
