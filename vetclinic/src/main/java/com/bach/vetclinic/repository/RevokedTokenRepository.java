package com.bach.vetclinic.repository;
import com.bach.vetclinic.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
public interface RevokedTokenRepository extends JpaRepository<RevokedToken,String>{void deleteByExpiresAtBefore(Instant now);}
