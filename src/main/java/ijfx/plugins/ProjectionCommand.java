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
package ijfx.plugins;

import static java.lang.Math.toIntExact;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.TypedAxis;
import net.imagej.ops.OpService;
import net.imagej.ops.commands.project.ProjectMethod;
import net.imagej.ops.image.project.DefaultProjectParallel;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imagej.ops.threshold.otsu.ComputeOtsuThreshold;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Tuan anh TRINH
 * @param <T>
 */
@Plugin(type = Command.class, menuPath = "Plugins>Projection Command", attrs = {
    @Attr(name = "no-legacy")})
public class ProjectionCommand<T extends RealType<T>> implements Command {

    @Parameter(type = ItemIO.OUTPUT)
    private Dataset out;

    @Parameter(type = ItemIO.INPUT)
    private Dataset in;

    // TODO: same problem as in the threshold: parameter aggregation ...
//    @Parameter
//    private UnaryComputerOp method;

    // the dimension that will be aggregated


    @Parameter
    private OpService ops;

    @Parameter
    private DatasetService datasetService;

    @Override
    public void run() {
       UnaryComputerOp method = new  DefaultProjectParallel();
        if (out == null) {
            long[] dims = new long[in.numDimensions()];
            in.dimensions(dims);
            AxisType[] axisType = new AxisType[in.numDimensions()];
            CalibratedAxis[] axeArray = new CalibratedAxis[in.numDimensions()];
            in.axes(axeArray);

            for (int i = 0; i < dims.length; i++) {
                axisType[i] = axeArray[i].type();
                dims[i] = toIntExact(in.max(i) + 1);
                if (axeArray[i].type() == Axes.Z) {
                    dims[i] = 1;
                   
                }
            }

            out = datasetService.create(dims, in.getName(), axisType, in.getValidBits(), in.isSigned(), false);
        }
        int axisIndex = in.dimensionIndex(Axes.Z);
        ops.image().project(out.getImgPlus(), in.getImgPlus(), method, axisIndex);
    }
}