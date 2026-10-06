package com.capstone.management;

import com.capstone.management.security.JwtService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {
    @Test
    void generatesAndParsesToken() {
        JwtService s = new JwtService("0123456789012345678901234567890123456789", 3600000);
        var claims = s.parse(s.generate("alice", "ADMIN"));
        assertEquals("alice", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }
}
