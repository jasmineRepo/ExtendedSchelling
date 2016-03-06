package it.unito.es.model;

import it.unito.es.model.enums.Gender;
import microsim.data.MultiKeyCoefficientMap;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.space.turtle.DigitalTurtle;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

@Table(name="ES_AGENT")
@Entity(name="ESAgent")
public class ESAgent extends DigitalTurtle implements EventListener {

	private static final Logger log = Logger.getLogger(ESAgent.class);
	
	private static final long serialVersionUID = 7731999082865205069L;

	// variables declaration
	@Id
	private PanelEntityKey id;

	private Integer year;
	
	@Transient
	private ESModel model;

	private Integer age;

	@Enumerated(EnumType.ORDINAL)
	private Gender gender;
	
	@Column(name = "mother_id")
	private Long motherId;
	
	private Boolean maternity = false;

	private Double tolerance;
	private Integer satisfaction;

	private int color;
	
	public enum Processes {
		CheckLocalSatisfaction, 
		Step,
		Age,
		GiveBirth,
		Death;
	}
	
	protected ESAgent() {
		super(null);
	}
	
	public ESAgent(ESModel model, int n, int x, int y, int age, Gender gender, double tolerance, int color) {
		super(model.getGrid(), x, y);
		
		id = new PanelEntityKey();
		id.setId((long) n);
		this.model = model;
		this.age = age;
		this.gender = gender;
		this.tolerance = tolerance;
		this.color = color;
		this.satisfaction = 0;
		this.year = model.getYear();
	}

	// *** METHODS ***
	

	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		case Step:
			step();
			break;
		case CheckLocalSatisfaction:
			checkLocalSatisfaction();
			break;
		case Age:
			age();
			break;
		case GiveBirth:
			if (this.gender.equals(Gender.Female) && this.age >= 15 && this.age <= 50) 
				giveBirth();
			break;
		case Death:
			death();
			break;
		}
	}

	public void step() {
		checkLocalSatisfaction();
		if (satisfaction == 0 && age < model.getMaxAgeMovers() && ! maternity)
			findOtherLocation();
	}

	public int checkLocalSatisfaction() {
		return satisfaction = checkSatisfaction(getX(), getY());
	}

	private int checkSatisfaction(int xx, int yy) {
		double otherColorNumber = 0;
		double totalNumber = 0;
		for (int i = Math.max(0, xx - 1); i <= Math.min(model.getxSize() - 1, xx + 1); i++)
			for (int j = Math.max(0, yy - 1); j <= Math.min(model.getySize() - 1, yy + 1); j++)
				if (grid.get(i, j) != null && grid.get(i, j) != this) {
					ESAgent neighbour = (ESAgent) grid.get(i, j);
					if (neighbour.getColor() != getColor())
						otherColorNumber++;
					totalNumber++;
				}
		if (otherColorNumber / totalNumber > tolerance)
			return 0;
		else
			return 1;
	}

	private void findOtherLocation() {
		outer: for (int d = 1; d < Math.max(model.getxSize(), model.getySize()); d++) {
			int i, j;
			// verticale W
			i = Math.max(0, getX() - d);
			for (j = Math.max(0, getY() - d); j <= Math.min(
					model.getySize() - 1, getY() + d); j++)
				if (grid.get(i, j) == null && checkSatisfaction(i, j) == 1) {
					setXY(i, j);
					satisfaction = 1;
					break outer;
				}
			// verticale E
			i = Math.min(model.getxSize() - 1, getX() + d);
			for (j = Math.max(0, getY() - d); j <= Math.min(
					model.getySize() - 1, getY() + d); j++)
				if (grid.get(i, j) == null && checkSatisfaction(i, j) == 1) {
					setXY(i, j);
					satisfaction = 1;
					break outer;
				}
			// orizzontale N
			j = Math.max(0, getY() - d);
			for (i = Math.max(0, getX() - d - 1); i <= Math.min(
					model.getxSize() - 1, getX() + d - 1); i++)
				if (grid.get(i, j) == null && checkSatisfaction(i, j) == 1) {
					setXY(i, j);
					satisfaction = 1;
					break outer;
				}
			// orizzontale S
			j = Math.min(model.getySize() - 1, getY() + d);
			for (i = Math.max(0, getX() - d - 1); i <= Math.min(
					model.getxSize() - 1, getX() + d - 1); i++)
				if (grid.get(i, j) == null && checkSatisfaction(i, j) == 1) {
					setXY(i, j);
					satisfaction = 1;
					break outer;
				}
		}
		if (satisfaction == 0)
			removeAgent();
	}
	
	public void age() {
		year = model.getYear();
		age += 1;
	}
	
	public void giveBirth() {
		maternity = false;
		double birthProbability = ((Number) model.getpBirth().getValue(this.age, year)).doubleValue();
			
			if (SimulationEngine.getRnd().nextDouble() < birthProbability) {
				
				maternity = true;
				int[] babyLocation = findBabyLocation();
				
				if (babyLocation[0] == -1) {
					removeAgent();  // mother and child move out of the city
				}
				else {
					model.increaseAgentsCounter();
					int babyId = model.getAgentsCounter();
					int babyAge = 0;
					Gender babyGender = (SimulationEngine.getRnd().nextDouble() > 0.49 ? Gender.Male : Gender.Female);
					double babyTolerance = tolerance;
					int babyColor = color;
					
					ESAgent baby = new ESAgent(model, babyId, babyLocation[0], babyLocation[1], babyAge, babyGender, babyTolerance, babyColor);
					baby.setMotherId(id.getId());
					model.addAgent(baby);
				}
		}

	}
	
	private int[] findBabyLocation() {
		int[] location = new int[2]; 
		location[0] = -1; // default value
		location[1] = -1; // default value
		outer: for (int d = 1; d < Math.max(model.getxSize(), model.getySize()); d++) {
			int i, j;
			// verticale W
			i = Math.max(0, getX() - d);
			for (j = Math.max(0, getY() - d); j <= Math.min(
					model.getySize() - 1, getY() + d); j++)
				if (grid.get(i, j) == null) {
					location[0] = i; location[1]=j;
					break outer;
				}
			// verticale E
			i = Math.min(model.getxSize() - 1, getX() + d);
			for (j = Math.max(0, getY() - d); j <= Math.min(
					model.getySize() - 1, getY() + d); j++)
				if (grid.get(i, j) == null) {
					location[0] = i; location[1]=j;
					break outer;
				}
			// orizzontale N
			j = Math.max(0, getY() - d);
			for (i = Math.max(0, getX() - d - 1); i <= Math.min(
					model.getxSize() - 1, getX() + d - 1); i++)
				if (grid.get(i, j) == null) {
					location[0] = i; location[1]=j;;
					break outer;
				}
			// orizzontale S
			j = Math.min(model.getySize() - 1, getY() + d);
			for (i = Math.max(0, getX() - d - 1); i <= Math.min(
					model.getxSize() - 1, getX() + d - 1); i++)
				if (grid.get(i, j) == null) {
					location[0] = i; location[1]=j;
					break outer;
				}
		}
		return location;
	}
	
	public void death() {
		MultiKeyCoefficientMap map = (gender.equals(Gender.Male) ? model.getpDeathM() : model.getpDeathF() ) ;
		
		if (map.getValue(this.age, year) != null) {
			double deathProbability = ((Number) map.getValue(this.age, year)).doubleValue();
			
			if (SimulationEngine.getRnd().nextDouble() < deathProbability) 
				removeAgent();
		}
	}
	
	public void removeAgent() {
		model.removeAgent(this);
		grid.set(getX(), getY(), null);
		log.debug("Removing agent "+id.getId() + " at "+ toString());
	}
	
	public Integer getSatisfaction() {return satisfaction;}
	public Integer getColor() {return color;}
	public void setColor(Integer color) {this.color = color;}
	public void setTolerance(Double tolerance) {this.tolerance = tolerance;}
	public void setModel(ESModel model) {this.model = model;}
	public PanelEntityKey getId() {return id;}
	public Long getMotherId() {return motherId;}
	public void setMotherId(Long motherId) {this.motherId = motherId;}
	public void setAge(Integer age) {this.age = age;}
	public void setGender(Gender gender) {this.gender = gender;}

}
