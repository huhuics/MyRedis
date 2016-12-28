/**
 * 深圳金融电子结算中心
 * Copyright (c) 1995-2016 All Rights Reserved.
 */
package cn.redis.chapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * 第七章学习,主要是搜索
 * @author HuHui
 * @version $Id: Chapter07.java, v 0.1 2016年12月28日 下午7:49:54 HuHui Exp $
 */
public class Chapter07 {

    private static final Pattern     QUERY_RE   = Pattern.compile("[+-]?[a-z']{2,}");

    private static final Pattern     WORDS_RE   = Pattern.compile("[a-z']{2,}");

    private static final Set<String> STOP_WORDS = new HashSet<String>();

    static {
        for (String word : ("able about across after all almost also am among " + "an and any are as at be because been but by can "
                            + "cannot could dear did do does either else ever " + "every for from get got had has have he her hers "
                            + "him his how however if in into is it its just " + "least let like likely may me might most must my "
                            + "neither no nor not of off often on only or other " + "our own rather said say says she should since so "
                            + "some than that the their them then there these " + "they this tis to too twas us wants was we were "
                            + "what when where which while who whom why will " + "with would yet you your").split(" ")) {
            STOP_WORDS.add(word);
        }
    }

    private static String            CONTENT    = "this is some random content, look at how it is indexed.";

    /**
     * @param args
     */
    public static void main(String[] args) {
        new Chapter07().run();
    }

    public void run() {

        Jedis conn = new Jedis("168.33.131.55");
        conn.select(15);
        conn.flushDB();

        testIndexDocument(conn);
        testSetOperations(conn);
    }

    public void testIndexDocument(Jedis conn) {
        System.out.println("\n----- testIndexDocument -----");

        System.out.println("We're tokenizing some content...");

        Set<String> tokens = tokenize(CONTENT);
        System.out.println("Those tokens are: " + Arrays.toString(tokens.toArray()));

        System.out.println("and now we are indexing that content...");
        int count = indexDocument(conn, "docId1", CONTENT);
        System.out.println("tokens.size = " + tokens.size() + ", count = " + count);

        for (String t : tokens) {
            Set<String> members = conn.smembers("idx:" + t);
            System.out.println("members = " + Arrays.toString(members.toArray()));
        }
    }

    public void testSetOperations(Jedis conn) {
        System.out.println("\n----- testSetOperations -----");

        indexDocument(conn, "test", CONTENT);

        Transaction trans = conn.multi();
        String id = intersect(trans, 30, "content", "indexed");
        trans.exec();
        System.out.println(Arrays.toString(conn.smembers("idx:" + id).toArray()));

        trans = conn.multi();
        id = union(trans, 30, "content", "ignored");
        trans.exec();
        System.out.println(Arrays.toString(conn.smembers("idx:" + id).toArray()));

    }

    public Set<String> tokenize(String content) {
        Set<String> words = new HashSet<String>();
        Matcher matcher = WORDS_RE.matcher(content);
        while (matcher.find()) {
            String word = matcher.group().trim();
            if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    public int indexDocument(Jedis conn, String docid, String content) {
        Set<String> words = tokenize(content);
        Transaction trans = conn.multi();
        for (String word : words) {
            trans.sadd("idx:" + word, docid);
        }
        List<Object> execs = trans.exec();
        return execs.size();
    }

    private String setCommon(Transaction trans, String method, int ttl, String... items) {
        String[] keys = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            keys[i] = "idx:" + items[i];
        }

        String id = UUID.randomUUID().toString();
        try {
            trans.getClass().getDeclaredMethod(method, String.class, String[].class).invoke(trans, "idx:" + id, keys);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        trans.expire("idx:" + id, ttl);

        return id;
    }

    public String intersect(Transaction trans, int ttl, String... items) {
        return setCommon(trans, "sinterstore", ttl, items);
    }

    public String union(Transaction trans, int ttl, String... items) {
        return setCommon(trans, "sunionstore", ttl, items);
    }

    public String difference(Transaction trans, int ttl, String... items) {
        return setCommon(trans, "sdiffstore", ttl, items);
    }

}
