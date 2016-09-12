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
package ijfx.plugins.adapter;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.VariableAxis;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author Cyril MONGIS, 2015
 * @author Tuan anh TRINH
 */
@Plugin(type = Service.class)
public class DefaultIJ1Service extends AbstractService implements IJ1Service {

    @Parameter
    public DatasetService datasetService;

    @Parameter
    ImageDisplayService imageDisplayService;

    @Parameter
    EventService eventService;





    @Override
    public ImagePlus getInput(Dataset dataset) {
        return unwrapDataset(dataset);
    }

    /**
     *
     * @param imp
     * @param dataset
     * @return
     */
    @Override
    public Dataset setOutput(ImagePlus imp, Dataset dataset) {
            dataset = wrapDataset(imp);

        return dataset;
    }

    /**
     *
     * @param dataset
     * @return
     */
    @Override
    public <T extends NumericType<T>> ImagePlus  unwrapDataset(Dataset dataset) {
        RandomAccessibleInterval<T> r = (RandomAccessibleInterval<T>) dataset.<T>getImgPlus();
        ImagePlus wrapImage = ImageJFunctions.wrap((RandomAccessibleInterval<T>)dataset, "");
        return wrapImage;
    }

    @Override
    public Dataset wrapDataset(ImagePlus imp) {
        Img img = ImagePlusAdapter.wrapImgPlus(imp.duplicate());
        return datasetService.create(img);
    }

    @Override
    public void configureImagePlus(ImagePlus imp, ImageDisplay imageDisplay) {

        imp.setC(imageDisplay.getIntPosition(Axes.CHANNEL));
        imp.setZ(imageDisplay.getIntPosition(Axes.Z));
        imp.setT(imageDisplay.getIntPosition(Axes.TIME));

    }



    public int getNumberOfSlices(Dataset dataset) {

        return (int) (dataset.getImgPlus().size() / (dataset.dimension(0) * dataset.dimension(1)));

    }
    
    @Override
     public void copyColorTable(Dataset dataset, Dataset output) {
        output.initializeColorTables(dataset.getColorTableCount());
        for (int i = 0; i < dataset.getColorTableCount(); i++) {
            output.setColorTable(dataset.getColorTable(i), i);
        }
    }
    
    @Override
    public void copyAxesInto(Dataset dataset, Dataset output){
        for (int d = 0; d < dataset.numDimensions(); d++) {
            final CalibratedAxis axis = dataset.axis(d);
            final CalibratedAxis axisOutput = output.axis(d);
            axisOutput.setType(axis.type());
            axisOutput.setUnit(axis.unit());
            if (!(axisOutput instanceof VariableAxis)) {
                continue; // nothing else to do
            }
            final VariableAxis varAxis = (VariableAxis) axisOutput;

            varAxis.vars().stream().forEach((var) -> {
                varAxis.set(var, varAxis.get(var));
            });
        }
    }



}
