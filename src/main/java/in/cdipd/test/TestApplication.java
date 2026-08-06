package in.cdipd.test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class TestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}

	@Bean
	CommandLineRunner printTestTable(JdbcTemplate jdbcTemplate) {
		return args -> jdbcTemplate.queryForList("select * from get_test_data()")
				.forEach(row -> System.out.println(row));
	}

}
