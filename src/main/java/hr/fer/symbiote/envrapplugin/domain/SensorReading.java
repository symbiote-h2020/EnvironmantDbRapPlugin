package hr.fer.symbiote.envrapplugin.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SensorReading {
	@Id
	private Integer id;
	private String temperature, pressure, humidity, co, no2, so2;

	public SensorReading() {
	}

	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getTemperature() {
		return temperature;
	}

	public void setTemperature(String temperature) {
		this.temperature = temperature;
	}

	public String getPressure() {
		return pressure;
	}

	public void setPressure(String pressure) {
		this.pressure = pressure;
	}

	public String getHumidity() {
		return humidity;
	}

	public void setHumidity(String humidity) {
		this.humidity = humidity;
	}

	public String getCo() {
		return co;
	}

	public void setCo(String co) {
		this.co = co;
	}

	public String getNo2() {
		return no2;
	}

	public void setNo2(String no2) {
		this.no2 = no2;
	}

	public String getSo2() {
		return so2;
	}

	public void setSo2(String so2) {
		this.so2 = so2;
	}

	@Override
	public String toString() {
		return "SensorReading [id=" + id + ", temperature=" + temperature + ", pressure=" + pressure + ", humidity="
				+ humidity + ", co=" + co + ", no2=" + no2 + ", so2=" + so2 + "]";
	}
}
