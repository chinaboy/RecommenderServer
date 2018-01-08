package com.eureka.recommend;

/**
 * Created by zhouqiang on 11/2/17.
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.jdbc.PostgreSQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;


import org.springframework.web.bind.annotation.*;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by zhouqiang on 10/21/17.
 */
/* @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class}) */
@EnableDiscoveryClient
@SpringBootApplication
public class RecommendationServer {

    private ExecutorService executor;
    private PoolingDataSource dataSource;
    private Recommender recommender;
    private PostgreSQLJDBCDataModel model;
    private GenericObjectPool connectionPool;
    private Log logger;
    private ArrayList<RecommendationFuture> queue;

    @PostConstruct
    public void init() throws ClassNotFoundException{
        logger = LogFactory.getLog(RecommendationServer.class);

        Class.forName("org.postgresql.Driver");

        executor = Executors.newSingleThreadExecutor();

        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:postgresql://localhost:5432/movies", "zhouqiang", "Zhou1989");
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,null);

        connectionPool = new GenericObjectPool(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);

        dataSource = new PoolingDataSource(connectionPool);

        try {
            logger.info("start building a recommendation model...");
            model = new PostgreSQLJDBCDataModel(
                    dataSource,
                    "ratings",
                    "userid",
                    "movieid",
                    "rating",
                    ""
            );

            // last parameter is to control number of iteration for als
            // give it one to make it fast during debugging
            // one hundred will take at least half a day to finish
            recommender = new SVDRecommender(model, new ALSWRFactorizer(model, 10, 0.8, 1));
            logger.info("finish building...");
        }catch(TasteException e){
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/user/{userid}", method = RequestMethod.GET)
    public void worker(@PathVariable("userid") Long userId, int howMany){
        logger.info("Looking up user with ID of " + userId);

        Future<List<RecommendedItem>> recommendFuture = null;
        Callable<List<RecommendedItem>> task = new RecommendTask(dataSource, recommender, userId, howMany);

        Future<List<RecommendedItem>> result = executor.submit( task );
        queue.add(new RecommendationFuture(userId, recommendFuture, new Timestamp(System.currentTimeMillis())) );
    }

    public static void main(String[] args) {
        System.out.println("classpath=" + System.getProperty("java.class.path"));
        System.setProperty("spring.config.name", "recommendation-server");
        SpringApplication.run(RecommendationServer.class, args);
    }
}
