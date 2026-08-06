package in.cdipd.test;

import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	private final JdbcTemplate jdbcTemplate;
	private final HikariDataSource hikariDataSource;

	public HelloController(JdbcTemplate jdbcTemplate, HikariDataSource hikariDataSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.hikariDataSource = hikariDataSource;
	}

	@GetMapping("/hello")
	public Map<String, Object> hello() {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from get_test_data()");
		return Map.of(
				"message", "Hello World",
				"data", rows,
				"hikariAutoCommit", hikariDataSource.isAutoCommit());
	}

}
