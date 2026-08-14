package com.itjob.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itjob.entities.SeedVersion;

public interface SeedVersionRepo extends JpaRepository<SeedVersion, UUID> {
}
