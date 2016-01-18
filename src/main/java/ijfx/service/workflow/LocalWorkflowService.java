/*
 * /*
 *     This file is part of ImageJ FX.
 *
 *     ImageJ FX is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ImageJ FX is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
 *
 * 	Copyright 2015,2016 Cyril MONGIS, Michael Knop
 *
 */
package ijfx.service.workflow;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import ijfx.ui.main.ImageJFX;
import ijfx.service.ui.LoadingScreenService;
import ijfx.service.workflow.DefaultWorkflow;
import ijfx.service.workflow.Workflow;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import mongis.utils.SimpleTask;

/**
 *
 * @author Cyril MONGIS, 2015
 */
@Plugin(type = Service.class)
public class LocalWorkflowService extends AbstractService implements MyWorkflowService{

    
    private ObservableList<Workflow> workflowList = FXCollections.observableArrayList();
    
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private String paramFile = ImageJFX.getConfigFile("workflowList.js");
    
    Logger logger = ImageJFX.getLogger();
    
    @Parameter
    LoadingScreenService loadingScreenService;
    
   
    
    @Override
    public void initialize() {
        super.initialize();
        load();
    }
    
    
    @Override
    public ObservableList<Workflow> getWorkflowList() {
        return workflowList;
    }

    @Override
    public boolean addWorkflow(Workflow workflow) {
        workflowList.add(workflow);
        
        // start a new task in the background that saves the workflows
        loadingScreenService.backgroundTask(new SimpleTask(()->save(),"Saving workflows...").startInNewThread(), false);
        
        return true;
    }

    @Override
    public boolean deleteWorkflow(Workflow workflow) {
        workflowList.remove(workflow);
        
        // see addWorkflow
        loadingScreenService.backgroundTask(new SimpleTask(()->save(),"Saving workflows...").startInNewThread(), false);
        return true;
    }
    
    private void load()  {
        
        try {
            objectMapper.readValue(new File(paramFile), WorkflowList.class);
        } catch (IOException ex) {
            logger.info("No workflows saved.");
        }
        
    }
    
    private void save() {
        try {
            File f = new File(paramFile);
            f.mkdirs();
            ImageJFX.getConfigDirectory();
            objectMapper.writeValue(f, new WorkflowList(workflowList));
        } catch (IOException ex) {
            ImageJFX.getLogger();
        }
    }
    
    
    private class WorkflowList {
        
        public WorkflowList(List<? extends Workflow> list) {
            setWorkflowList(workflows);
        }
        
        @JsonIgnore
        private List<? extends Workflow> workflows = new ArrayList<Workflow>();
        
        @JsonGetter("workflows")
        public List<? extends Workflow> getWorkflowList() {
            return workflows;
        }
        
        public void setWorkflowList(List<? extends Workflow> workflowList) {
            this.workflows = workflowList;
        }
        
        @JsonSetter("workflows")
        public void deserialize(ArrayList<DefaultWorkflow> workflowList) {
            workflows = workflowList;
        }
    }
    
}