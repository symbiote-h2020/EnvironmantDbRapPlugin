package hr.fer.symbiote.envrapplugin;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.cloud.model.rap.query.Query;
import eu.h2020.symbiote.model.cim.Location;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.model.cim.ObservationValue;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.UnitOfMeasurement;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPluginException;
import eu.h2020.symbiote.rapplugin.messaging.rap.ResourceAccessListener;
import eu.h2020.symbiote.rapplugin.util.Utils;
import hr.fer.symbiote.envrapplugin.domain.SensorReading;
import hr.fer.symbiote.envrapplugin.domain.SensorRepository;

@SpringBootApplication
public class EnvironmantDbRapPluginApplication implements CommandLineRunner {
  public final Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private RapPlugin plugin;

  @Autowired
  private SensorRepository repo;

  private ObjectMapper mapper = new ObjectMapper();
  private DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private Set<String> servingInternalResourceIds = Set.of("multisensor1", "protectedResource1");

  @Override
  public void run(String... args) throws Exception {
    plugin.registerReadingResourceListener(new ResourceAccessListener() {

      @Override
      public String getResourceHistory(List<ResourceInfo> resourceInfo, int top, Query filterQuery) {
        String internalResourceId = Utils.getInternalResourceId(resourceInfo);

        if (servingInternalResourceIds.contains(internalResourceId)) {
          try {
            List<Observation> observations = new LinkedList<>();
            LocalDateTime time = LocalDateTime.now();
            for (int i = 0; i < top; i++) {
              observations.add(createObservation(internalResourceId, time));
              time = time.minusMinutes(15);
            }

            return mapper.writeValueAsString(observations);
          } catch (JsonProcessingException e) {
            throw new RapPluginException(500, "Can not convert observation to JSON", e);
          }
        } else {
          throw new RapPluginException(404, "Sensor not found.");
        }

      }

      @Override
      public String getResource(List<ResourceInfo> resourceInfo) {
        String internalResourceId = Utils.getInternalResourceId(resourceInfo);

        if (servingInternalResourceIds.contains(internalResourceId)) {
          try {
            return mapper.writeValueAsString(createObservation(internalResourceId));
          } catch (JsonProcessingException e) {
            throw new RapPluginException(500, "Can not convert observation to JSON", e);
          }
        } else {
          throw new RapPluginException(404, "Sensor not found.");
        }
      }
    });
  }

  public SensorReading getReading(LocalDateTime time) {
    int id = time.getHour() * 4 + time.getMinute() / 15;
    return repo.findById(id).get();
  }

  public Observation createObservation(String sensorId) {
    return createObservation(sensorId, LocalDateTime.now());
  }

  public Observation createObservation(String sensorId, LocalDateTime time) {
    SensorReading reading = getReading(time);

    Location loc = new WGS84Location(48.2088475, 16.3734492, 158, "Stephansdome", Arrays.asList("City of Wien"));

    LocalDateTime newTime = LocalDateTime.of(time.getYear(), time.getMonth(), time.getDayOfMonth(),
        time.getHour(), (time.getMinute() / 15) * 15);
    String sampleTime = dateFormat.format(newTime);

    ArrayList<ObservationValue> obsList = new ArrayList<>();
    obsList.add(getObservationValueForTemperature(reading));
    obsList.add(getObservationValueForHumidity(reading));
    obsList.add(getObservationValueForPressure(reading));
    obsList.add(getObservationValueForCO(reading));
    obsList.add(getObservationValueForNO2(reading));
    obsList.add(getObservationValueForSO2(reading));

    long timestamp = time.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
    Observation obs = new Observation(sensorId, loc, Long.toString(timestamp), sampleTime, obsList);

    try {
      log.debug("Observation: \n{}", new ObjectMapper().writeValueAsString(obs));
    } catch (JsonProcessingException e) {
      log.error("Can not convert observation to JSON", e);
    }

    return obs;
  }

  // Property IRI:
  // https://github.com/symbiote-h2020/SymbioteCloud/blob/master/resources/docs/property_uris
  // Unit IRI:
  // https://github.com/symbiote-h2020/Ontologies/blob/master/v2.3.0/bim-qu-align-v2.3.0.ttl
  private ObservationValue getObservationValueForTemperature(SensorReading reading) {
    return new ObservationValue(
        reading.getTemperature(),
        new Property("temperature", "http://purl.oclc.org/NET/ssnx/qu/quantity#temperature",
            Arrays.asList("Air temperature")),
        new UnitOfMeasurement("C", "degree Celsius", "http://purl.oclc.org/NET/ssnx/qu/unit#degreeCelsius", null));
  }

  private ObservationValue getObservationValueForCO(SensorReading reading) {
    return new ObservationValue(
        reading.getCo(),
        new Property("carbonMonoxideConcentration",
            "http://www.symbiote-h2020.eu/ontology/bim/property#carbonMonoxideConcentration",
            Arrays.asList("Air carbon monoxide concentration")),
        new UnitOfMeasurement("ug/m3", "microgram per cubic metre",
            "http://purl.oclc.org/NET/ssnx/qu/unit#microgramPerCubicMetre", null));
  }

  private ObservationValue getObservationValueForNO2(SensorReading reading) {
    return new ObservationValue(
        reading.getNo2(),
        new Property("nitrogenDioxideConcentration", "ndc_IRI", Arrays.asList("Air carbon monoxide concentration")),
        new UnitOfMeasurement("ug/m3", "microgram per cubic metre",
            "http://purl.oclc.org/NET/ssnx/qu/unit#microgramPerCubicMetre", null));
  }

  private ObservationValue getObservationValueForPressure(SensorReading reading) {
    return new ObservationValue(
        reading.getPressure(),
        new Property("atmosphericPressure", "http://purl.oclc.org/NET/ssnx/qu/quantity#atmosphericPressure",
            Arrays.asList("Air hydrostatic pressure")),
        new UnitOfMeasurement("hPa", "hectopascal", "http://purl.oclc.org/NET/ssnx/qu/unit#hectopascal", null));
  }

  private ObservationValue getObservationValueForHumidity(SensorReading reading) {
    return new ObservationValue(
        reading.getHumidity(),
        new Property("humidity", "http://purl.oclc.org/NET/ssnx/qu/quantity#humidity", Arrays.asList("Air humidity")),
        new UnitOfMeasurement("%", "percent", "http://purl.oclc.org/NET/ssnx/qu/unit#percent", null));
  }

  private ObservationValue getObservationValueForSO2(SensorReading reading) {
    return new ObservationValue(
        reading.getSo2(),
        new Property("sulphurDioxideConcentration",
            "http://www.symbiote-h2020.eu/ontology/bim/property#sulphurDioxideConcentration",
            Arrays.asList("Air sulphur dioxide concentration")),
        new UnitOfMeasurement("ug/m3", "microgram per cubic metre",
            "http://purl.oclc.org/NET/ssnx/qu/unit#microgramPerCubicMetre", null));
  }

  public static void main(String[] args) {
    SpringApplication.run(EnvironmantDbRapPluginApplication.class, args);
  }
}
