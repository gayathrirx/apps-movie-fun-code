package org.superbiz.moviefun.albums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Configuration
@EnableAsync
@EnableScheduling
public class AlbumsUpdateScheduler {

    private static final long SECONDS = 1000;
    private static final long MINUTES = 60 * SECONDS;

    private final AlbumsUpdater albumsUpdater;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private JdbcTemplate jdbcTemplate;

    private static boolean tableCreated = false;

    public AlbumsUpdateScheduler(AlbumsUpdater albumsUpdater, DataSource datasource) {
        this.albumsUpdater = albumsUpdater;
        jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(datasource);

    }


    @Scheduled(initialDelay = 15 * SECONDS, fixedRate = 2 * MINUTES)
    public void run() {
        try {
            if (startAlbumSchedulerTask()) {
                logger.debug("Starting albums update");
                albumsUpdater.update();
                //jdbcTemplate.execute("INSERT INTO album_scheduler_task (started_at) VALUES (CURRENT_TIMESTAMP)");
                logger.debug("Finished albums update");

            } else {
                logger.debug("Nothing to start");
            }


        } catch (Throwable e) {
            logger.error("Error while updating albums", e);
        }
    }

    private boolean startAlbumSchedulerTask() {

        if (!tableCreated ) {
            createTable();
            tableCreated = true;
        }

     /*   MyResultSetExtractor extractor = new MyResultSetExtractor();
        jdbcTemplate.query("select started_at from album_scheduler_task order by started_at desc limit 1", extractor);

        long currentTs = System.currentTimeMillis();

        if ( extractor.startedAt != null && extractor.startedAt.getTime() - System.currentTimeMillis() > 2 * 60 * 1000) {
            return true;
        }
        return false;*/
        int updatedRows = jdbcTemplate.update(
                "UPDATE album_scheduler_task" +
                        " SET started_at = now()" +
                        " WHERE started_at IS NULL" +
                        " OR started_at < date_sub(now(), INTERVAL 2 MINUTE)"
        );

        return updatedRows > 0;
    }


/*    private class MyResultSetExtractor implements ResultSetExtractor {

        public Timestamp startedAt;

        @Override
        public Object extractData(ResultSet resultSet) throws SQLException, DataAccessException {
            if (resultSet != null) {
                startedAt = resultSet.getTimestamp("started_at");
            }
            return null;
        }
    }*/

   private void createTable() {

       logger.warn("invoking createTable()...");

       String tableName = jdbcTemplate.query("SELECT table_name FROM information_schema.tables where table_name='album_scheduler_task'", //new MyResultSetExtractor());
               new ResultSetExtractor<String>()
               {
                   public String extractData(ResultSet resultSet) throws SQLException,
                           DataAccessException {
                       if (resultSet != null && resultSet.next()) {
                           return resultSet.getString("table_name");
                       }
                       return null;
                   }
               });

       logger.warn("Table name returned...:" + tableName);

       if (tableName == null) {
           //createTable..
           logger.warn("Creating Tables...");
           jdbcTemplate.execute("CREATE TABLE album_scheduler_task (started_at TIMESTAMP NULL DEFAULT NULL)");
           jdbcTemplate.execute("INSERT INTO album_scheduler_task (started_at) VALUES (NULL)");
       }
   }

}
