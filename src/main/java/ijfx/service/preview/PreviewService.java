/*
    This file is part of ImageJ FX.

    ImageJ FX is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ImageJ FX is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
    
     Copyright 2015,2016 Cyril MONGIS, Michael Knop
	
 */
package ijfx.service.preview;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJService;
import net.imagej.Position;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplayService;
import net.imglib2.RandomAccess;
import net.imglib2.display.ColorTable;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author Tuan anh TRINH
 */
@Plugin(type = Service.class)
public class PreviewService extends AbstractService implements ImageJService {

    @Parameter
    ImageDisplayService imageDisplayService;

    @Parameter
    DatasetService datasetService;

    @Parameter
    CommandService commandService;
    
    private int width;
    private int height;
    private int x;
    private int y;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
    
    public void setParameters(int x, int y, int w, int h)
    {
       this.x = x;
       this.y = y;
       this.width = w;
       this.height = h;
    }
    public PreviewService() {
    }
    
    public Image getImageDisplay(String command) {
        Dataset preview = getPreviewDataset(width, height, 0, 0);
        preview = applyCommand(preview, command);
        BufferedImage bufferedImage = datasetToBufferedImage(preview);

        WritableImage wi = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
        SwingFXUtils.toFXImage(bufferedImage, wi);
        return wi;
    }

    public Dataset getEmptyDataset(Dataset input) {
        AxisType[] axisType = new AxisType[input.numDimensions()];
        CalibratedAxis[] axeArray = new CalibratedAxis[input.numDimensions()];
        input.axes(axeArray);

        long[] dims = new long[axeArray.length];
        for (int i = 0; i < dims.length; i++) {
            axisType[i] = axeArray[i].type();
            dims[i] = 1;//toIntExact(input.max(i) + 1);

        }
        dims[0] = width;
        dims[1] = height;
        return datasetService.create(dims, input.getName(), axisType, input.getValidBits(), input.isSigned(), false);
    }

    public < T extends RealType< T>> Dataset getPreviewDataset(int width, int height, int x, int y) {

        Position activePosition = imageDisplayService.getActivePosition();

        Dataset datasetOrigin = imageDisplayService.getActiveDataset();
        Dataset datasetOutput = getEmptyDataset(datasetOrigin);

        long[] dimension = new long[datasetOrigin.numDimensions() - 2];
        activePosition.localize(dimension);

        long[] position = new long[datasetOrigin.numDimensions()];
        position[0] = 0;
        position[1] = 0;
        System.arraycopy(dimension, 0, position, 2, dimension.length);
        RandomAccess<T> randomAccessOrigin = (RandomAccess<T>) datasetOrigin.randomAccess();
        RandomAccess<T> randomAccessOutput = (RandomAccess<T>) datasetOutput.randomAccess();
        randomAccessOrigin.setPosition(position);

        for (int i = x; i < width + x; i++) {
            randomAccessOrigin.setPosition(i, 0);
            randomAccessOutput.setPosition(i - x, 0);

            for (int j = y; j < height + y; j++) {
                randomAccessOrigin.setPosition(j, 1);
                randomAccessOutput.setPosition(j - y, 1);
                randomAccessOutput.get().set(randomAccessOrigin.get());
            }
        }

//
//        RandomAccess<T> randomAccessOrigin = (RandomAccess<T>) datasetOrigin.randomAccess();
//        Cursor<T> cursorOutput = (Cursor<T>) datasetOutput.cursor();
//        while (cursorOutput.hasNext()) {
//            cursorOutput.fwd();
//            randomAccessOrigin.setPosition(cursorOutput);
//            cursorOutput.get().set(randomAccessOrigin.get());
//            
//        }
        return datasetOutput;
    }

    public BufferedImage datasetToBufferedImage(Dataset dataset) {
        DatasetView activeDataview = imageDisplayService.getActiveDatasetView();

        final DatasetView view = (DatasetView) imageDisplayService.createDataView(dataset);
        Position activePosition = imageDisplayService.getActivePosition();

        view.getData().setChannelMaximum(0, activeDataview.getChannelMax(activePosition.getIntPosition(0)));
        view.getData().setChannelMinimum(0, activeDataview.getChannelMin(activePosition.getIntPosition(0)));

        //view.setPosition(activePosition);
                view.setColorMode(activeDataview.getColorMode());
        view.rebuild();
        List<ColorTable> colorTable = activeDataview.getColorTables();
        long[] dimension = new long[dataset.numDimensions() - 2];
        activePosition.localize(dimension);
        
        //Set LUT if nummber of channel >1
        if (dimension[0] >1)
        {
        view.setColorTable(colorTable.get(activePosition.getIntPosition(0)), 0);
        }
        int maxChannel = (int) activeDataview.getChannelMax(activePosition.getIntPosition(0));
        int minChannel = (int) activeDataview.getChannelMin(activePosition.getIntPosition(0));
        view.setChannelRange(0, minChannel, maxChannel);
        
        view.rebuild();

        return view.getScreenImage().image();
    }
    
    public Dataset applyCommand(Dataset dataset, String command)
    {
        try {
            Map<String,Object> inputMap = new HashMap<>();
            inputMap.put("dataset", dataset);
            inputMap.put("sigma", 3);
            inputMap.put("useUnits", false);
            final Future<CommandModule> futur =commandService.run(command, false, inputMap);
            Map<String,Object> outMap = futur.get().getOutputs();
        System.out.println("ijfx.service.preview.PreviewService.applyCommand()");
        return (Dataset) outMap.get("dataset");
        } catch (InterruptedException ex) {
            Logger.getLogger(PreviewService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(PreviewService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
        
    }

}