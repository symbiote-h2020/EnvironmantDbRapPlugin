package hr.fer.symbiote.envrapplugin;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;

import com.opencsv.bean.CsvToBeanBuilder;

import hr.fer.symbiote.envrapplugin.domain.SensorReading;
import hr.fer.symbiote.envrapplugin.domain.SensorRepository;

@Configuration
public class InitDb implements CommandLineRunner {
	@Autowired
	SensorRepository repo;

	@Override
	public void run(String... args) throws Exception {
		List<SensorReading> readings = loadData();

		// prepare data for saving
		int i = 0;
		for(SensorReading reading: readings) {
			reading.setId(i++);
		}
		
		repo.save(readings);
		
		readings.stream().forEach(System.out::println);
	}

	private List<SensorReading> loadData() throws IllegalStateException, FileNotFoundException {
		String fileName = "mjerenja.csv";
		return new CsvToBeanBuilder<SensorReading>(new FileReader(fileName))
			       .withType(SensorReading.class).build().parse();
	}

}
