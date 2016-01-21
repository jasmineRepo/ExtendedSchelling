package it.unito.es.experiment;

import it.unito.es.model.ESModel;
import it.zero11.microsim.engine.ExperimentBuilder;
import it.zero11.microsim.engine.SimulationEngine;
import it.zero11.microsim.gui.shell.MicrosimShell;

public class ESStart implements ExperimentBuilder {

	public static void main(String[] args) {
		boolean showGui = true;
		
		ESStart experimentBuilder = new ESStart();
		
		SimulationEngine engine = SimulationEngine.getInstance();
		MicrosimShell gui = null;
		if (showGui) {
			gui = new MicrosimShell(engine);		
			gui.setVisible(true);
		}
		
		engine.setExperimentBuilder(experimentBuilder);
			
		engine.setup();
		
	}
	
	public void buildExperiment(SimulationEngine engine) {
		ESModel model = new ESModel();
		ESCollector collector = new ESCollector(model);
		ESObserver observer = new ESObserver(model, collector);
				
		engine.addSimulationManager(model);
		engine.addSimulationManager(collector);
		engine.addSimulationManager(observer);		
	}	
}	