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
package ijfx.service.display;

import ijfx.core.stats.IjfxStatisticService;
import ijfx.ui.main.ImageJFX;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mongis.utils.TimedBuffer;
import mongis.utils.UUIDMap;
import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.measure.StatisticsService;
import net.imglib2.display.ColorTable;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.scijava.display.DisplayService;
import org.scijava.display.event.DisplayUpdatedEvent;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Gives information on display range of an image display
 *
 * @author Cyril MONGIS, 2015
 */
@Plugin(type = Service.class)
public class DisplayRangeService extends AbstractService implements ImageJService {

    @Parameter
    private DisplayService displayService;

    @Parameter
    private ImageDisplayService imageDisplayService;

    @Parameter
    private EventService eventService;

    @Parameter
    private StatisticsService statService;

    @Parameter
    private IjfxStatisticService ijfxStatsService;

    
    private final TimedBuffer<Runnable> rangeUpdateQueue = new TimedBuffer<Runnable>(100);
    
    
    public DisplayRangeService() {
        super();
        
        rangeUpdateQueue.setAction(this::executeLast);
        
    }
    
    private void executeLast(List<Runnable> task) {
        ImageJFX.getThreadQueue().submit(task.get(task.size()-1));
    }
    
    private CalibratedAxis getChannelAxis() {

        for (CalibratedAxis a : getAxisList()) {
            if (a.type().getLabel().toLowerCase().contains("channel")) {
                return a;
            }
        }
        return null;

    }

    private ArrayList<CalibratedAxis> getAxisList() {
        return getAxisList(displayService.getActiveDisplay(ImageDisplay.class));

    }

    private ArrayList<CalibratedAxis> getAxisList(ImageDisplay display) {
        ArrayList<CalibratedAxis> axises = new ArrayList<>();

        for (int i = 0; i != display.numDimensions(); i++) {
            axises.add(display.axis(i));
        }

        return axises;

    }

    public double getChannelMinimum(ImageDisplay display, int channel) {
        return imageDisplayService.getActiveDatasetView(display).getChannelMin(channel);
    }
    
    public double getChannelMaximum(ImageDisplay display, int channel) {
        return imageDisplayService.getActiveDatasetView(display).getChannelMax(channel);
    }
    
    public double getCurrentViewMinimum() {
        return imageDisplayService.getActiveDatasetView().getChannelMin(getCurrentChannelId());
    }

    public double getCurrentViewMaximum() {
        return imageDisplayService.getActiveDatasetView().getChannelMax(getCurrentChannelId());
    }

    public double getCurrentPixelMinimumValue() {
        return statService.minimum(imageDisplayService.getActiveDataset(displayService.getActiveDisplay(ImageDisplay.class)));
        // return getDisplayStatistics(displayService.getActiveDisplay(ImageDisplay.class), getCurrentChannelId()).getMin();
    }

    public double getCurrentPixelMaximumValue() {

        // getDisplayStatistics(displayService.getActiveDisplay(ImageDisplay.class), getCurrentChannelId()).getMax();
        return statService.maximum(imageDisplayService.getActiveDataset(displayService.getActiveDisplay(ImageDisplay.class)));
    }

    public int getCurrentChannelId() {
        int channelId = imageDisplayService.getActiveDatasetView().getIntPosition(Axes.CHANNEL);
        return channelId;

    }
    
    public int getChannelId(ImageDisplay display) {
        int channelId = imageDisplayService.getActiveDatasetView(display).getIntPosition(Axes.CHANNEL);
        return channelId == -1 ? 0 : channelId;
    }
    
    public ColorTable getColorTable(ImageDisplay display) {
        return imageDisplayService.getActiveDataset(display).getColorTable(getChannelId(display));
    }

    private void updateDisplayRange(ImageDisplay imageDisplay,int channel,double min, double max) {
        
        final Dataset dataset = imageDisplayService.getActiveDataset(imageDisplay);
        final DatasetView datasetView = imageDisplayService.getActiveDatasetView(imageDisplay);
        dataset.setChannelMinimum(channel, min);
        dataset.setChannelMaximum(channel, max);
        datasetView.setChannelRange(channel, min, max);
        
        
       datasetView.getProjector().map();
        eventService.publishLater(new DisplayUpdatedEvent(imageDisplay, DisplayUpdatedEvent.DisplayUpdateLevel.UPDATE));
    }
    
    
    public void updateCurrentDisplayRange(double min, double max) {
        if(Double.isNaN(min ) || Double.isNaN(max)) return;
        
        
        final int currentChannel = getCurrentChannelId();
        
        final double currentMin = imageDisplayService.getActiveDataset().getChannelMinimum(currentChannel);
        final double currentMax = imageDisplayService.getActiveDataset().getChannelMaximum(currentChannel);
        
        if(currentMin == min && currentMax == max) return;
        
        imageDisplayService.getActiveDataset().setChannelMaximum(getCurrentChannelId(), max);
        imageDisplayService.getActiveDataset().setChannelMinimum(getCurrentChannelId(), min);
        imageDisplayService.getActiveDatasetView().setChannelRange(getCurrentChannelId(), min, max);
      
        
        
       rangeUpdateQueue.add(() -> {
          imageDisplayService.getActiveDatasetView().getProjector().map();
          eventService.publishLater(new DisplayUpdatedEvent(displayService.getActiveDisplay(), DisplayUpdatedEvent.DisplayUpdateLevel.UPDATE));
        });

    }   

   

    private String getDisplayChannelId(ImageDisplay display, int channel) {
        return UUID.nameUUIDFromBytes(new String(display.hashCode() + "" + channel).getBytes()).toString();
    }
    
}
