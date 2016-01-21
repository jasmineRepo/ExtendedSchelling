package it.unito.es.model;

import it.unito.es.model.enums.Gender;
import it.zero11.microsim.annotation.ModelParameter;
import it.zero11.microsim.data.MultiKeyCoefficientMap;
import it.zero11.microsim.data.db.DatabaseUtils;
import it.zero11.microsim.data.excel.ExcelAssistant;
import it.zero11.microsim.engine.AbstractSimulationManager;
import it.zero11.microsim.engine.SimulationEngine;
import it.zero11.microsim.event.EventGroup;
import it.zero11.microsim.event.EventList;
import it.zero11.microsim.event.EventListener;
import it.zero11.microsim.event.SingleTargetEvent;
import it.zero11.microsim.event.SystemEvent;
import it.zero11.microsim.event.SystemEventType;
import it.zero11.microsim.space.DenseObjectSpace;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class ESModel extends AbstractSimulationManager implements EventListener {

	private final static Logger log = Logger.getLogger(ESModel.class);

	// PARAMETERS/COEFFICIENTS
	private MultiKeyCoefficientMap pBirth;
	private MultiKeyCoefficientMap pDeathM;
	private MultiKeyCoefficientMap pDeathF;

	// ENTITIES
	private DenseObjectSpace grid;

	private List<ESAgent> agentsList;
		
	@ModelParameter(description="Base year (valid range 2002-2059)")
	private Integer baseYear = 2002;
	
	@ModelParameter(description="Final year (valid range 2002-2060)")
	private Integer finalYear = 2060;
	
	@ModelParameter
	private Integer xSize = 50;
	
	@ModelParameter
	private Integer ySize = 50;
	
	@ModelParameter
	private Double popDensity = .8;
	
	@ModelParameter
	private Double color0Share = .5;
	
	@ModelParameter
	private Double minTolerance = 0.0;
	
	@ModelParameter
	private Double maxTolerance = 1.0;
	
	@ModelParameter
	private Integer maxAgeMovers = 65;
		
	@ModelParameter(description="Loaded population is 5,000 individuals")
	private Boolean loadPopulation = false;
	
	private int year; 
	// Simulation time increases only after all agents have updated their location choice.
	// This is done in order not to wait too long before seeing action on the screen in interactive mode.

	private int agentsCounter = 0; // to assign unique id to newborns
	
	private EventGroup yearlyEvents;
	
	public enum Processes {
		UpdateSimulationTime,
		ScheduleNextYear;
	}
	
	@SuppressWarnings("unchecked")
	public void buildObjects()
	{
		// PARAMETERS / COEFFICIENTS
		pBirth = ExcelAssistant.loadCoefficientMap("input/p_birth.xls", "Foglio1", 1, 59);
		pDeathM = ExcelAssistant.loadCoefficientMap("input/p_death_m.xls", "Foglio1", 1, 59);
		pDeathF = ExcelAssistant.loadCoefficientMap("input/p_death_f.xls", "Foglio1", 1, 59);
		
		// GRID
		if (loadPopulation) {
			setxSize(120);
			setySize(120);
		}
		grid = new DenseObjectSpace(xSize, ySize);
		
		// AGENTS
		if (loadPopulation) {
			agentsList = (List<ESAgent>) DatabaseUtils.loadTable(ESAgent.class); //a) read from file
			for (ESAgent agent : agentsList) {
				agent.setGrid(grid);
				agent.setModel(this);
				double tolerance = SimulationEngine.getRnd().nextDouble() * (maxTolerance - minTolerance) + minTolerance;
				agent.setTolerance(tolerance);
			}
		} else
			agentsList = new ArrayList<ESAgent>();
		
		// b) create from scratch
		if (agentsList.size() == 0) {
			for (int i=0; i<popDensity*xSize*ySize; i++){
				boolean done = false;
				// gender
				Gender gender = (SimulationEngine.getRnd().nextDouble() > 0.49 ? Gender.Male : Gender.Female);
				// age
				int age = SimulationEngine.getRnd().nextInt(90);
				// color
				int color = (SimulationEngine.getRnd().nextDouble() < color0Share ? 0 : 1); 
				// tolerance;
				double tolerance = SimulationEngine.getRnd().nextDouble() * (maxTolerance - minTolerance) + minTolerance;
				// position
				while (!done){
					int x = SimulationEngine.getRnd().nextInt(xSize);
					int y = SimulationEngine.getRnd().nextInt(ySize);
					if (grid.get(x,y) == null) {
						ESAgent agent = new ESAgent(this, i, x, y, age, gender, tolerance, color);
						agentsList.add(agent);
						done = true;
					}
				}
			}
		}
		agentsCounter = agentsList.size();
		year = baseYear; 

		log.debug("Model objects created");
	}

	
	public void buildSchedule() {
		EventList eventList = getEngine().getEventList();
	
		// every tick
		EventGroup tickEvents = new EventGroup();
		//tickEvents.addEvent(agentsList.get(SimulationEngine.getRnd().nextInt(agentsList.size())), ESAgent.Processes.Step);
		tickEvents.addCollectionEvent(agentsList, ESAgent.Processes.Step, false);
		eventList.schedule(tickEvents, 0, 1);
			
		// every year
		yearlyEvents = new EventGroup();
		yearlyEvents.addEvent(this, ESModel.Processes.UpdateSimulationTime);
		yearlyEvents.addCollectionEvent(agentsList, ESAgent.Processes.Age, false);
		yearlyEvents.addCollectionEvent(agentsList, ESAgent.Processes.GiveBirth, false);
		yearlyEvents.addCollectionEvent(agentsList, ESAgent.Processes.Death, false);		
		yearlyEvents.addEvent(this, ESModel.Processes.ScheduleNextYear);
		eventList.schedule(yearlyEvents, 1 );		
//		eventList.schedule(yearlyEvents, (int) Math.log10(agentsList.size()) );
	
		log.debug("Model schedule created");
	}
	
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		case UpdateSimulationTime:
			year ++;
			if (year >= finalYear)
				getEngine().end();
			break;
		case ScheduleNextYear:
			getEngine().getEventList().schedule(yearlyEvents, getEngine().getTime() + 1);
//			getEngine().getEventList().schedule(yearlyEvents, getEngine().getTime() + (int) Math.log10(agentsList.size()));
			break;
		}
	}
	
	public void addAgent(ESAgent agent) {agentsList.add(agent);}
	
	public void removeAgent(ESAgent agent) {agentsList.remove(agent);}
		
	public List<ESAgent> getAgentsList(){return agentsList;}

	public Integer getxSize() {
		return xSize;
	}


	public void setxSize(Integer xSize) {
		this.xSize = xSize;
	}


	public Integer getySize() {
		return ySize;
	}


	public void setySize(Integer ySize) {
		this.ySize = ySize;
	}


	public DenseObjectSpace getGrid() {
		return grid;
	}


	public Double getPopDensity() {
		return popDensity;
	}


	public Double getColor0Share() {
		return color0Share;
	}


	public Double getMinTolerance() {
		return minTolerance;
	}


	public Double getMaxTolerance() {
		return maxTolerance;
	}


	public static Logger getLog() {
		return log;
	}


	public void setpBirth(MultiKeyCoefficientMap pBirth) {
		this.pBirth = pBirth;
	}


	public void setpDeathM(MultiKeyCoefficientMap pDeathM) {
		this.pDeathM = pDeathM;
	}


	public void setpDeathF(MultiKeyCoefficientMap pDeathF) {
		this.pDeathF = pDeathF;
	}


	public void setGrid(DenseObjectSpace grid) {
		this.grid = grid;
	}


	public void setAgentsList(List<ESAgent> agentsList) {
		this.agentsList = agentsList;
	}


	public void setPopDensity(Double popDensity) {
		this.popDensity = popDensity;
	}


	public void setColor0Share(Double color0Share) {
		this.color0Share = color0Share;
	}


	public void setMinTolerance(Double minTolerance) {
		this.minTolerance = minTolerance;
	}


	public void setMaxTolerance(Double maxTolerance) {
		this.maxTolerance = maxTolerance;
	}

	public void setYear(int year) {
		this.year = year;
	}


	public void setAgentsCounter(int agentsCounter) {
		this.agentsCounter = agentsCounter;
	}

	public MultiKeyCoefficientMap getpBirth() {return pBirth;}

	public MultiKeyCoefficientMap getpDeathM() {return pDeathM;}

	public MultiKeyCoefficientMap getpDeathF() {return pDeathF;}

	public Integer getStartYear() {return baseYear;}
		
	public int getAgentsCounter(){return agentsCounter;}
	
	public void increaseAgentsCounter(){agentsCounter++;}
	

	public Integer getBaseYear() {
		return baseYear;
	}


	public void setBaseYear(Integer baseYear) {
		this.baseYear = baseYear;
	}


	public int getYear() {
		return year;
	}


	public Boolean getLoadPopulation() {
		return loadPopulation;
	}


	public void setLoadPopulation(Boolean loadPopulation) {
		this.loadPopulation = loadPopulation;
	}


	public Integer getFinalYear() {
		return finalYear;
	}


	public void setFinalYear(Integer finalYear) {
		this.finalYear = finalYear;
	}


	public Integer getMaxAgeMovers() {
		return maxAgeMovers;
	}


	public void setMaxAgeMovers(Integer maxAgeMovers) {
		this.maxAgeMovers = maxAgeMovers;
	}

	
}	