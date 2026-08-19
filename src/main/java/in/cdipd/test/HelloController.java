package in.cdipd.test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	private static final Logger log = LoggerFactory.getLogger(HelloController.class);
	private static final String LOAD_CACHE_KEY_PREFIX = "load:result:";
	private static final Duration LOAD_CACHE_TTL = Duration.ofSeconds(30);

	private final JdbcTemplate jdbcTemplate;
	private final HikariDataSource hikariDataSource;
	private final StringRedisTemplate redisTemplate;

	public HelloController(JdbcTemplate jdbcTemplate, HikariDataSource hikariDataSource,
			StringRedisTemplate redisTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.hikariDataSource = hikariDataSource;
		this.redisTemplate = redisTemplate;
	}

	@GetMapping("/hello")
	public Map<String, Object> hello() {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from get_test_data()");
		return Map.of(
				"message", "Hello World",
				"data", rows,
				"hikariAutoCommit", hikariDataSource.isAutoCommit());
	}

	@GetMapping("/load")
	public Map<String, Object> load(HttpServletRequest request) {
		log.info("/load called - JSESSIONID={}", request.getSession().getId());

		HikariPoolMXBean poolStats = hikariDataSource.getHikariPoolMXBean();
		log.info("/load called - pool total={} active={} idle={} waiting={}",
				poolStats.getTotalConnections(), poolStats.getActiveConnections(),
				poolStats.getIdleConnections(), poolStats.getThreadsAwaitingConnection());

		//Integer result = jdbcTemplate.queryForObject("select 1 from pg_sleep(10);", Integer.class);
      //Integer result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM generate_series(1, 1000000000);", Integer.class);

		Integer result = jdbcTemplate.queryForObject(
				"""
				SELECT CASE WHEN EXISTS (
					SELECT 1 FROM pes.student_sem_result_final
					WHERE app_time_slot_id = 17 AND sem_id = 1
				) THEN 1 ELSE 0 END AS exists_flag
				""",
				Integer.class);
        return Map.of("result", result);
	}

	@GetMapping("/loadcache")
	public Map<String, Object> loadCache(
			@RequestParam int appTimeSlotId,
			@RequestParam int semId) {
		String cacheKey = LOAD_CACHE_KEY_PREFIX + appTimeSlotId + ":" + semId;

		String cached = redisTemplate.opsForValue().get(cacheKey);
		if (cached != null) {
			Long ttlRemaining = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
			log.info("/loadcache called - cache HIT for key={}, ttlRemaining={}s", cacheKey, ttlRemaining);
			return Map.of("result", Integer.valueOf(cached), "source", "cache", "ttlRemainingSeconds", ttlRemaining);
		}

		log.info("/loadcache called - cache MISS for key={}, querying DB", cacheKey);
		Integer result = jdbcTemplate.queryForObject(
				"""
				SELECT CASE WHEN EXISTS (
					SELECT 1 FROM pes.student_sem_result_final
					WHERE app_time_slot_id = ? AND sem_id = ?
				) THEN 1 ELSE 0 END AS exists_flag
				""",
				Integer.class, appTimeSlotId, semId);
		redisTemplate.opsForValue().set(cacheKey, String.valueOf(result), LOAD_CACHE_TTL);

		return Map.of("result", result, "source", "db", "ttlRemainingSeconds", LOAD_CACHE_TTL.getSeconds());
	}

}
