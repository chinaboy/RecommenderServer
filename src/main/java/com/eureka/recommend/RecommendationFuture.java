package com.eureka.recommend;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;


/**
 * Created by zhouqiang on 11/2/17.
 */
public class RecommendationFuture {
    public long userId;
    public Future<List<RecommendedItem>> future;
    public Timestamp timestamp;

    public RecommendationFuture(long userId, Future<List<RecommendedItem>> future, Timestamp timestamp) {
        this.userId = userId;
        this.future = future;
        this.timestamp = timestamp;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Future<List<RecommendedItem>> getFuture() {
        return future;
    }

    public void setFuture(Future<List<RecommendedItem>> future) {
        this.future = future;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

