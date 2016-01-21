package it.unito.es.experiment;

import microsim.engine.AbstractSimulationObserverManager;
import microsim.engine.SimulationCollectorManager;
import microsim.engine.SimulationManager;
import microsim.event.CommonEventType;
import microsim.event.EventGroup;
import microsim.event.EventListener;
import microsim.gui.GuiUtils;
import microsim.gui.colormap.FixedColorMap;
import microsim.gui.plot.TimeSeriesSimulationPlotter;
import microsim.gui.space.LayerObjectGridDrawer;
import microsim.gui.space.LayeredSurfaceFrame;
import microsim.statistics.IIntSource;

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