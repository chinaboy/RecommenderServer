package com.eureka.recommend;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by zhouqiang on 11/2/17.
 */
public class RecommendTask implements Callable {

    DataSource dataSource;
    Recommender recommender;
    Long userId;
    int howMany;

     public RecommendTask(DataSource dataSource,
             Recommender recommender,
             Long userId,
             int howMany){
         this.dataSource = dataSource;
         this.recommender = recommender;
         this.howMany = howMany;
         this.userId = userId;
     }

     public Object call() throws Exception{
        List<RecommendedItem> recommendations = null;
            try {
                Connection conn = dataSource.getConnection();
                recommendations = recommender.recommend(userId, howMany);
                for(RecommendedItem item : recommendations) {
                    PreparedStatement insertRecommendations = conn.prepareStatement("INSERT INTO recommendations (userid, movieid) VALUES(?, ?)");
                    insertRecommendations.setString(1, String.valueOf(userId));
                    insertRecommendations.setString(2, String.valueOf(item.getItemID()));
                    insertRecommendations.executeQuery();
                }
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            catch (TasteException e) {
                e.printStackTrace();
                return null;
            }

        return recommendations;
    }
}
