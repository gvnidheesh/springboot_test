package in.cdipd.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeakController {

    private static final List<Connection> LEAKS = new CopyOnWriteArrayList<>();

    private final DataSource dataSource;

    public LeakController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Acquire a connection and keep it open (simulate leak)
    @PostMapping("/leak")
    public String leak() throws SQLException {
        Connection c = dataSource.getConnection();
        LEAKS.add(c);
        return "leaked, total=" + LEAKS.size();
    }

    // Open and immediately close (control)
    @PostMapping("/leakOnceClose")
    public String leakOnceClose() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            // no-op
        }
        return "opened-and-closed";
    }

    // Close and clear all leaked connections
    @PostMapping("/leakClear")
    public String clear() {
        int closed = 0;
        for (Connection c : List.copyOf(LEAKS)) {
            try {
                c.close();
                closed++;
            } catch (Exception e) {
                // ignore
            } finally {
                LEAKS.remove(c);
            }
        }
        return "closed=" + closed + " remaining=" + LEAKS.size();
    }

    @GetMapping("/leakCount")
    public String count() {
        return "leaked=" + LEAKS.size();
    }

    @GetMapping("/leakList")
    public List<String> list() {
        return Collections.singletonList("size=" + LEAKS.size());
    }

}
