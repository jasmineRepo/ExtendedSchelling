package it.unito.es.experiment;

import it.zero11.microsim.engine.AbstractSimulationObserverManager;
import it.zero11.microsim.engine.SimulationCollectorManager;
import it.zero11.microsim.engine.SimulationManager;
import it.zero11.microsim.event.CommonEventType;
import it.zero11.microsim.event.EventGroup;
import it.zero11.microsim.event.EventListener;
import it.zero11.microsim.gui.GuiUtils;
import it.zero11.microsim.gui.colormap.FixedColorMap;
import it.zero11.microsim.gui.plot.TimeSeriesSimulationPlotter;
import it.zero11.microsim.gui.space.LayerObjectGridDrawer;
import it.zero11.microsim.gui.space.LayeredSurfaceFrame;
import it.zero11.microsim.statistics.IIntSource;

import it.unito.es.model.*;

import java.awt.Color;

import org.apache.log4j.Logger;

public class ESObserver extends AbstractSimulationObserverManager implements EventListener {

	private final static Logger log = Logger.getLogger(ESObserver.class);

	private LayeredSurfaceFrame map;
  	private TimeSeriesSimulationPlotter satisfactionPlotter, populationPlotter;
  	private FixedColorMap colorMap;
  	
	public ESObserver(SimulationManager manager, SimulationCollectorManager collectorManager) {
		super(manager, collectorManager);
	}
	
	public enum Processes {
	}

	public void buildObjects() {
		final ESModel model = (ESModel) getManager();
		final ESCollector collector = (ESCollector) getCollectorManager();
		
	    colorMap = new FixedColorMap(2);
	    colorMap.addColor(0,Color.red);
	    colorMap.addColor(1,Color.black);
	    
	    LayerObjectGridDrawer city = new LayerObjectGridDrawer("city", model.getGrid(), ESAgent.class, "color", false, colorMap); 
		    
	    map = new LayeredSurfaceFrame(model.getxSize(), model.getySize(), 6);
		map.setTitle("map");
		map.addLayer(city);
		GuiUtils.addWindow(map, 260, 0, model.getGrid().getXSize()*6+10, model.getGrid().getYSize()*6+30);
		
		satisfactionPlotter = new TimeSeriesSimulationPlotter("% satisfied", "satisfaction");
		satisfactionPlotter.addSeries("", collector.fMeanSatisfaction);
		GuiUtils.addWindow(satisfactionPlotter, 1000, 25, 360, 360); 
	
		populationPlotter = new TimeSeriesSimulationPlotter("city size", "population size");
		populationPlotter.addSeries("", (IIntSource) collector.fTracePopSize); 				
		GuiUtils.addWindow(populationPlotter, 750, 25, 360, 360); 
		
		log.debug("Observer objects created");
	}
	
	public void buildSchedule() {
		EventGroup eventGroup = new EventGroup();

		eventGroup.addEvent(map, CommonEventType.Update);
		eventGroup.addEvent(satisfactionPlotter, CommonEventType.Update);
		eventGroup.addEvent(populationPlotter, CommonEventType.Update);
		
		getEngine().getEventList().schedule(eventGroup, 0, 1);
							
		log.debug("Observer schedule created");
	}
	
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		}
	}
	
}	