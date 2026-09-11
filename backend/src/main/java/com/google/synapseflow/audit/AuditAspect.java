package com.google.synapseflow.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditActivity(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            String actor = "SYSTEM";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                actor = auth.getName();
            }

            String clientIp = "127.0.0.1";
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                clientIp = request.getRemoteAddr();
            }

            log.info("Auditable action executed: action={}, aggregateType={}, actor={}, clientIp={}",
                    auditable.action(), auditable.aggregateType(), actor, clientIp);

        } catch (Exception e) {
            log.error("Failed to execute audit aspect logging", e);
        }
    }
}
