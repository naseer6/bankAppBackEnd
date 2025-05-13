package nl.inholland.bankAppBackEnd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationEntryPoint customEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // ⚠ Only for testing
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
                .headers().frameOptions().disable() // Allow H2 Console
                .and()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**", "/api/users/register", "/api/users/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/accounts/create").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 🔥 No sessions
                );

        // ✅ Add JWT filter before Spring's default one
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
