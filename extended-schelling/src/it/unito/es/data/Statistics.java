package it.unito.es.data;

import microsim.data.db.PanelEntityKey;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name="Statistics")
public class Statistics {

	@Id
	private PanelEntityKey id;
	
	private int year;
	
	private Double meanSatisfaction;
	
	private Integer popSize;

	public PanelEntityKey getId() {
		return id;
	}

	public void setId(PanelEntityKey panelId) {
		this.id = panelId;
	}
	
	
	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public Double getMeanSatisfaction() {
		return meanSatisfaction;
	}

	public void setMeanSatisfaction(Double meanSatisfaction) {
		this.meanSatisfaction = meanSatisfaction;
	}

	public Integer getPopSize() {
		return popSize;
	}

	public void setPopSize(Integer popSize) {
		this.popSize = popSize;
	}

}
