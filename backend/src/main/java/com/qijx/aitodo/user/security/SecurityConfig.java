package com.qijx.aitodo.user.security;

import java.io.IOException;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter){
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.csrf(csrf -> csrf.disable());

        http.formLogin(formLogin -> formLogin.disable());

        http.httpBasic((httpBasic -> httpBasic.disable()));

        http.logout((logout -> logout.disable()));

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.POST, "/api/users/register", "/api/users/login")
                                    .permitAll()
                                    .requestMatchers("/error")
                                    .permitAll()
                                    .anyRequest()
                                    .authenticated()
                );

        http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> writeUnauthorizedResponse(response)));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException{
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write("{\"message\":\"缺少或无效的登录凭证\"}");
    }
}
