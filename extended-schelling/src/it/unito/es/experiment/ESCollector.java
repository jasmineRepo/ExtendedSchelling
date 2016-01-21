package it.unito.es.experiment;

import it.unito.es.data.Statistics;
import it.unito.es.model.ESAgent;
import it.unito.es.model.ESModel;
import it.zero11.microsim.annotation.ModelParameter;
import it.zero11.microsim.data.db.DatabaseUtils;
import it.zero11.microsim.engine.AbstractSimulationCollectorManager;
import it.zero11.microsim.engine.SimulationEngine;
import it.zero11.microsim.engine.SimulationManager;
import it.zero11.microsim.event.EventGroup;
import it.zero11.microsim.event.EventListener;
import it.zero11.microsim.statistics.CrossSection;
import it.zero11.microsim.statistics.IDoubleSource;
import it.zero11.microsim.statistics.functions.MeanArrayFunction;
import it.zero11.microsim.statistics.functions.MultiTraceFunction;

import org.apache.log4j.Logger;

public class ESCollector extends AbstractSimulationCollectorManager implements EventListener {
	// variables declaration

	private final static Logger log = Logger.getLogger(ESCollector.class);

	@ModelParameter(description="persists all the population in the database")
	private Boolean saveMicrodata = false;
	
	@ModelParameter(description="persists only aggregate statistics in the database")
	private Boolean saveMacrodata = true;
	
	public CrossSection.Integer csSatisfaction;
	public MeanArrayFunction fMeanSatisfaction;
	public MultiTraceFunction.Integer fTracePopSize;
	
	private Statistics stats;
	
	private EventGroup eventGroup;

	public ESCollector(SimulationManager model) 
	{
		super(model);		
	}
	
	// *** METHODS ***
	
	public enum Processes {
		Update,
		ScheduleNextYear,
		DumpInfo;
	}

	public void buildObjects() {
		stats = new Statistics();
		csSatisfaction = new CrossSection.Integer(((ESModel) getManager()).getAgentsList(), ESAgent.class, "satisfaction", false);
		fMeanSatisfaction = new MeanArrayFunction(csSatisfaction);
		fTracePopSize = new MultiTraceFunction.Integer(this, "getPopulationSize", true);
	}

	public void buildSchedule() {
		//ESModel model = (ESModel) getManager();
	
		eventGroup = new EventGroup();
		eventGroup.addEvent(this, Processes.DumpInfo);
		eventGroup.addEvent(this, Processes.Update);
		eventGroup.addEvent(this, Processes.ScheduleNextYear);
		
		getEngine().getEventList().schedule(eventGroup, 1);
		// reduce persistency frequency
//		getEngine().getEventList().schedule(eventGroup, (int) Math.log10(model.getAgentsList().size()));	
	
	}
	
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		case DumpInfo:
			try {
				if (saveMacrodata) DatabaseUtils.snap(DatabaseUtils.getOutEntityManger(), 
						(long) SimulationEngine.getInstance().getCurrentRunNumber(), 
						getEngine().getTime(), 
						stats);
				
				if (saveMicrodata) DatabaseUtils.snap(DatabaseUtils.getOutEntityManger(), 
						(long) SimulationEngine.getInstance().getCurrentRunNumber(), 
						getEngine().getTime(), 
						((ESModel) getManager()).getAgentsList());
			} catch (Exception e) {
				log.error(e.getMessage());				
			}
			break;		
		case Update:
			update();
			break;
		case ScheduleNextYear:
			getEngine().getEventList().schedule(eventGroup, getEngine().getTime() + 1);
//			getEngine().getEventList().schedule(eventGroup, getEngine().getTime() + (int) Math.log10(((ESModel) getManager()).getAgentsList().size()));
			break;
		}
	}
	
	public void update()
	{
		stats.setYear(((ESModel) getManager()).getYear());
		fMeanSatisfaction.updateSource();
		stats.setMeanSatisfaction(fMeanSatisfaction.getDoubleValue(IDoubleSource.Variables.Default));
		fTracePopSize.updateSource();
		stats.setPopSize(fTracePopSize.getLastValue());
	}
	
	public int getPopulationSize() {
		return ((ESModel) getManager()).getAgentsList().size();
	}

	public double getMeanSatisfaction() {
		return fMeanSatisfaction.getDoubleValue(null);
//		return meanSatisfaction.getDoubleValue(IDoubleSource.Variables.Default);
		
	}

	public Boolean getSaveMicrodata() {
		return saveMicrodata;
	}

	public void setSaveMicrodata(Boolean saveMicrodata) {
		this.saveMicrodata = saveMicrodata;
	}

	public Boolean getSaveMacrodata() {
		return saveMacrodata;
	}

	public void setSaveMacrodata(Boolean saveMacrodata) {
		this.saveMacrodata = saveMacrodata;
	}
	
	
	
}