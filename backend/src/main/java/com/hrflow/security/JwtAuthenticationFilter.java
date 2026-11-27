package com.hrflow.security;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hrflow.common.util.Vn;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Xác thực Bearer JWT, rồi đối chiếu lại tài khoản trong DB ở mỗi request: trạng thái ACTIVE,
 * chưa tới ngày vô hiệu hóa, token_version khớp (đăng xuất/đổi mật khẩu thu hồi token cũ).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JdbcTemplate jdbc;

    public JwtAuthenticationFilter(JwtService jwtService, JdbcTemplate jdbc) {
        this.jwtService = jwtService;
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                long userId = Long.parseLong(claims.getSubject());
                int tv = claims.get("tv", Integer.class);
                List<Map<String, Object>> rows = jdbc.queryForList("""
                        SELECT a.id, a.email, a.role, a.status, a.employee_id, a.token_version,
                               a.disable_at, a.locked_until, e.status AS emp_status, e.termination_date
                        FROM user_account a LEFT JOIN employee e ON e.id = a.employee_id
                        WHERE a.id = ?
                        """, userId);
                if (!rows.isEmpty()) {
                    Map<String, Object> r = rows.get(0);
                    LocalDate today = Vn.today();
                    java.sql.Date disableAt = (java.sql.Date) r.get("disable_at");
                    java.sql.Date termination = (java.sql.Date) r.get("termination_date");
                    boolean terminated = "TERMINATED".equals(r.get("emp_status"))
                            && (termination == null || !termination.toLocalDate().isAfter(today));
                    boolean active = "ACTIVE".equals(r.get("status"))
                            && ((Number) r.get("token_version")).intValue() == tv
                            && (disableAt == null || disableAt.toLocalDate().isAfter(today))
                            && !terminated;
                    if (active) {
                        Number emp = (Number) r.get("employee_id");
                        AuthUser user = new AuthUser(userId, emp == null ? null : emp.longValue(),
                                (String) r.get("email"), (String) r.get("role"));
                        var auth = new UsernamePasswordAuthenticationToken(user, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
