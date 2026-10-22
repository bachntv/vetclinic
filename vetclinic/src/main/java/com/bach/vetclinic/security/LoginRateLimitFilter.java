package com.bach.vetclinic.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component @Order(-200)
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private record Window(long minute,AtomicInteger count){}
    private final ConcurrentHashMap<String,Window> attempts=new ConcurrentHashMap<>();
    private final int limit;
    public LoginRateLimitFilter(@Value("${app.auth.login-requests-per-minute:20}") int limit){this.limit=limit;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        if(!request.getServletPath().equals("/api/auth/login")||!request.getMethod().equals("POST")){chain.doFilter(request,response);return;}
        long minute=System.currentTimeMillis()/60000;
        attempts.entrySet().removeIf(e->e.getValue().minute()<minute);
        if(attempts.size()>=10000&&!attempts.containsKey(request.getRemoteAddr())){reject(response);return;}
        Window w=attempts.compute(request.getRemoteAddr(),(ip,old)->old==null||old.minute()!=minute?new Window(minute,new AtomicInteger()):old);
        if(w.count().incrementAndGet()>limit){reject(response);return;}
        chain.doFilter(request,response);
    }
    private void reject(HttpServletResponse response)throws IOException{
        response.setStatus(429);response.setContentType("application/problem+json;charset=UTF-8");response.setHeader("Retry-After","60");
        response.getWriter().write("{\"status\":429,\"detail\":\"Quá nhiều lần đăng nhập. Vui lòng thử lại sau một phút.\"}");
    }
}
