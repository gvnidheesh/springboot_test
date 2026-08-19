package in.cdipd.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	private static final int PROBE_TIMEOUT_SECONDS = 2;

	private final String datasourceUrl;
	private final String datasourceUsername;
	private final String datasourcePassword;
	private final RedisConnectionFactory redisConnectionFactory;

	public HealthController(
			@Value("${spring.datasource.url}") String datasourceUrl,
			@Value("${spring.datasource.username}") String datasourceUsername,
			@Value("${spring.datasource.password}") String datasourcePassword,
			RedisConnectionFactory redisConnectionFactory) {
		this.datasourceUrl = datasourceUrl;
		this.datasourceUsername = datasourceUsername;
		this.datasourcePassword = datasourcePassword;
		this.redisConnectionFactory = redisConnectionFactory;
	}

	@GetMapping("/status")
	public Map<String, Object> status() {
		boolean dbUp = isDbUp();
		boolean redisUp = isRedisUp();
		return Map.of("db", dbUp, "redis", redisUp, "allUp", dbUp && redisUp);
	}

	private boolean isDbUp() {
		try {
			DriverManager.setLoginTimeout(PROBE_TIMEOUT_SECONDS);
			try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUsername, datasourcePassword)) {
				return connection.isValid(PROBE_TIMEOUT_SECONDS);
			}
		} catch (SQLException e) {
			return false;
		}
	}

	private boolean isRedisUp() {
		try (var connection = redisConnectionFactory.getConnection()) {
			return "PONG".equalsIgnoreCase(connection.ping());
		} catch (Exception e) {
			return false;
		}
	}

}
