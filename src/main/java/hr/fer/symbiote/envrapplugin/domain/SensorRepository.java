package hr.fer.symbiote.envrapplugin.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<SensorReading, Integer>{

}
