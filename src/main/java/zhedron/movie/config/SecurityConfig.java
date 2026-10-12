package zhedron.movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import zhedron.movie.config.filter.JwtFilter;
import zhedron.movie.services.impl.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/user/registration",
            "/api/login",
            "/api/refreshtoken",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/mediacontent",
            "/api/mediacontent/{id:\\d+}",
            "/api/mediacontent/start-date/end-date",
            "/api/mediacontent/stream/photo/{id:\\d+}",
            "/api/season/{id:\\d+}",
            "/api/episode/{id:\\d+}",
            "/api/film/{id:\\d+}",
            "/api/actor/{actorId:\\d+}",
            "/api/actor/stream/{actorId:\\d+}",
            "/api/comment/{id:\\d+}/{mediaContentId:\\d+}",
            "/api/subtitle/{id:\\d+}",
            "/api/subtitle/datasrt/**"
    };

    private static final String[] ADMIN_ENDPOINTS = {
            "/api/mediacontent/**",
            "/api/season/**",
            "/api/film/**",
            "/api/episode/**",
            "/api/user/change-role/**",
            "/api/actor/create",
            "/api/actor/delete/**",
            "/api/actor/update/**",
            "/api/subtitle/delete/**",
            "/api/subtitle/upload",
            "/api/film/add/subtitle/{subtitleId:\\d+}/{filmId:\\d+}",
            "/api/film/remove/subtitle/{subtitleId:\\d+}/{filmId:\\d+}",
            "/api/episode/add/subtitle/{subtitleId:\\d+}/{episodeId:\\d+}",
            "/api/episode/remove/subtitle/{subtitleId:\\d+}/{episodeId:\\d+}"
    };

    private static final String[] AUTHENTICATED_ENDPOINTS = {
            "/api/comment/**",
            "/api/favorite/**"
    };

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(ADMIN_ENDPOINTS).hasAuthority("ADMIN")
                        .requestMatchers(AUTHENTICATED_ENDPOINTS).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService());
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());

        return daoAuthenticationProvider;
    }
}
