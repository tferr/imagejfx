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
package ijfx.service.overlay;

import ijfx.ui.main.ImageJFX;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJService;
import net.imagej.display.ImageDisplay;
import net.imagej.display.OverlayService;
import net.imagej.measure.StatisticsService;

import net.imagej.ops.OpService;
import net.imagej.overlay.LineOverlay;
import net.imagej.overlay.Overlay;
import net.imagej.overlay.PolygonOverlay;
import net.imagej.overlay.RectangleOverlay;
import net.imglib2.RandomAccess;
import net.imglib2.ops.pointset.HyperVolumePointSet;
import net.imglib2.ops.pointset.PointSet;
import net.imglib2.ops.pointset.PointSetIterator;
import net.imglib2.ops.pointset.RoiPointSet;
import net.imglib2.roi.PolygonRegionOfInterest;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.util.ColorRGB;

/**
 *
 * @author Cyril MONGIS, 2015
 */
@Plugin(type = Service.class)
public class OverlayStatService extends AbstractService implements ImageJService {

    @Parameter
    private OverlayService overlayService;

    @Parameter
    private DatasetService datasetService;

    @Parameter
    private OpService opService;

    @Parameter
    private StatisticsService statService;

    
    
    final private Logger logger = ImageJFX.getLogger();

    public final static String LBL_MEAN = "Mean";
    public final static String LBL_MEDIAN = "Median";
    public final static String LBL_AREA = "Area";
    public final static String LBL_MAX = "Max";
    public final static String LBL_MIN = "Min";
    public final static String LBL_SD = "Std. Dev.";
    public final static String LBL_VARIANCE = "Variance";
    public final static String LBL_PIXEL_COUNT = "Pixel count";
    
    public final static String LBL_MBR = "Minimum Bounding Rectangle";
    public final static String LBL_CENTROID = "Center of Gravity";
    public final static String LBL_MAX_FERET_DIAMETER = "Feret Diameter";
    public final static String LBL_MIN_FERET_DIAMETER = "Min. Feret Diameter";
    public final static String LBL_LONG_SIDE_MBR = "Long Side MBR";
    public final static String LBL_SHORT_SIDE_MBR = "Short Side MBR";
    public final static String LBL_ASPECT_RATIO ="Aspect ratio";
    public final static String LBL_CONVEXITY = "Convexity";
    public final static String LBL_SOLIDITY = "Solidity";
    public final static String LBL_CIRCULARITY = "Circularity";
    public final static String LBL_THINNES_RATIO = "Thinnes ratio";

    public HashMap<String, Double> getStat(ImageDisplay imageDisplay, Overlay overlay) {

       /*
         HashMap<String, ParallelMeasurement> measures = new HashMap<>();
          HashMap<String, Double> stats = new HashMap<>();
         DescriptiveStatistics statistics = new DescriptiveStatistics(ArrayUtils.toPrimitive(getValueList(imageDisplay, overlay)));
        
        measures.put(LBL_MIN, () -> statistics.getMin());
        measures.put(LBL_MAX, () -> statistics.getMax());
        measures.put(LBL_MEAN, () -> statistics.getMean());
        measures.put("Std. Dev.", () -> statistics.getStandardDeviation());
        measures.put("Variance", () -> statistics.getVariance());
        measures.put(LBL_MEDIAN, () -> statistics.getPercentile(50));
        
        //measures.put(LBL_AREA, () -> statService.geometricMean(ds, rps));
        
        
        for (String key : measures.keySet()) {

            logger.fine("Calculating " + key);
            ParallelMeasurement runnable = measures.get(key);
            Double value = runnable.measure();

            logger.fine(String.format("Calculated %s = %.0f", key, value));
            stats.put(key, value);
        };
        logger.fine("finished");*/
       
       
       
        return getStatistics(getOverlayStatistics(imageDisplay, overlay));
    }
   
    
    public Double[] getValueList(ImageDisplay imageDisplay, Overlay overlay) {
        
         if(overlay instanceof LineOverlay) return getValueList(imageDisplay,(LineOverlay)overlay);
         
        // getting the dataset corresponding to the display
        final Dataset ds = datasetService.getDatasets(imageDisplay).get(0);
        
        // Array containing all the pixel values
        ArrayList<Double> values = new ArrayList<>(10000);
        
        
        // RoiPointSet used to iterate through the pixel inside the Roi
        RoiPointSet rps = new RoiPointSet(overlay.getRegionOfInterest());
                 
        
        PointSetIterator psc = rps.cursor();
        
        // Random access of the dataset
        RandomAccess<RealType<?>> randomAccess = ds.randomAccess();
         
        // position of the image display
        long[] position = new long[imageDisplay.numDimensions()];
        imageDisplay.localize(position);
        psc.reset();
        int c = 0;
        
        // getting the 
        while(psc.hasNext()) {
            psc.fwd();
            long[] roiPosition = psc.get();
            
            for(int i =0;i!=roiPosition.length;i++) {
                position[i] = roiPosition[i];
            }
            randomAccess.setPosition(position);
            values.add(randomAccess.get().getRealDouble());
            c++;
        }
        System.out.printf("%d values retrieved\n",c);
        return values.toArray(new Double[values.size()]);
        
    }
    
    protected Double[] getValueList(ImageDisplay imageDisplay, LineOverlay overlay) {
        
        
        final Dataset ds = datasetService.getDatasets(imageDisplay).get(0);
        
        System.out.printf("Num dimensions %d\n",overlay.numDimensions());
        
         RandomAccess<RealType<?>> randomAccess = ds.randomAccess();
         
         int x0 = new Double(overlay.getLineStart(0)).intValue();
         int y0 = new Double(overlay.getLineStart(1)).intValue();
         int x1 = new Double(overlay.getLineEnd(0)).intValue();
         int y1 = new Double(overlay.getLineEnd(1)).intValue();
         
         List<int[]> pixels = Bresenham.findLine(x0, y0, x1, y1);
         
         
         long[] position = new long[imageDisplay.numDimensions()];
         Double[] values = new Double[pixels.size()];
         
         int i = 0;
         for(int[] coordinate : pixels) {
             position[0] = coordinate[0];
             position[1] = coordinate[1];
             randomAccess.setPosition(position);
             
             values[i] = randomAccess.get().getRealDouble();
             i++;
         }
         
        return values;
    }

    private interface ParallelMeasurement {

        public Double measure();
    }

    public class OverlayStats extends HashMap<String, Double> {

    }

    private PointSet getRegion(ImageDisplay display, Overlay overlay) {
        if (overlay != null) {
            return new RoiPointSet(overlay.getRegionOfInterest());
        }
        long[] pt1 = new long[display.numDimensions()];
        long[] pt2 = new long[display.numDimensions()];
        // current plane only
        pt1[0] = 0;
        pt1[1] = 0;
        pt2[0] = display.dimension(0) - 1;
        pt2[1] = display.dimension(1) - 1;
        for (int i = 2; i < display.numDimensions(); i++) {
            pt1[i] = pt2[i] = display.getLongPosition(i);
        }
        return new HyperVolumePointSet(pt1, pt2);
    }

    /*
    
    TODO: for Pierre
    Modify the OverlayStatisticsService so it returns a OverlayStatistics object
    from an ImageDisplay and an Overlay.
    
    Some statistics depends on the pixel values (you can already easily retrive
    the pixel values using the method (getValueList) but shape related
    statistics must be implemented by you. Pixel value reltated statistics
    can also easily be calculated using DescriptiveStatistics objects
    from Apache 3 Math library (already used in the project).
    
    Be careful becayse, the Overlay can be a PolygonOverlay, a LineOverlay,
    or a Rectangle Overlay. Some shape statistics don't make sens in some case
    so it the implementation should return 0;
    
    You can find quite close implementation of the this shape related statistics
    at :
    https://github.com/thorstenwagner/ij-blob/blob/master/src/main/java/ij/blob/Blob.java
    
    You can copy paste some of it code. You can use java.awt.* object in your
    code but avoid using any ImageProcessor related object.
    
    Things to keep it mind in the implementations : 
        - the statistics should be calculated only once
        - retrieving the pixel value for the statistics is okay
        but shouldn't be kept in memory. So don't keep any DescriptiveStatistics Object
        in memory.
    */
    
   
    public OverlayStatistics getOverlayStatistics(ImageDisplay display, Overlay overlay) {
        
        
        OverlayStatistics overlayStatistics;
        
        if(overlay instanceof LineOverlay)
            overlayStatistics = new LineOverlayStatistics(display, overlay, this.context());
            
        else if(overlay instanceof RectangleOverlay)
            overlayStatistics = new RectangleOverlayStatistics(display, overlay, this.context());
        
        else{
            overlay = cleanOverlay(overlay);
            overlayStatistics = new PolygonOverlayStatistics(display, overlay, this.context());        
        }
        
        return overlayStatistics;
    }
    
    
    public HashMap<String, Double> getStatistics(OverlayStatistics overlayStats) {
        
        HashMap<String, Double> statistics = new HashMap<>();
        
        statistics.put(LBL_MEAN, overlayStats.getPixelStatistics().getMean());
        statistics.put(LBL_MIN, overlayStats.getPixelStatistics().getMin());
        statistics.put(LBL_MAX, overlayStats.getPixelStatistics().getMax());
        statistics.put(LBL_SD, overlayStats.getPixelStatistics().getStandardDeviation());
        statistics.put(LBL_VARIANCE, overlayStats.getPixelStatistics().getVariance());
        statistics.put(LBL_MEDIAN, overlayStats.getPixelStatistics().getMedian());
        statistics.put(LBL_PIXEL_COUNT, (double)overlayStats.getPixelStatistics().getPixelCount());
        statistics.put(LBL_AREA, overlayStats.getArea());        
        
        statistics.put(LBL_MAX_FERET_DIAMETER, overlayStats.getFeretDiameter());
        statistics.put(LBL_MIN_FERET_DIAMETER, overlayStats.getMinFeretDiameter());
        statistics.put(LBL_LONG_SIDE_MBR, overlayStats.getLongSideMBR());
        statistics.put(LBL_SHORT_SIDE_MBR, overlayStats.getShortSideMBR());
        statistics.put(LBL_ASPECT_RATIO, overlayStats.getAspectRatio());
        statistics.put(LBL_CONVEXITY, overlayStats.getConvexity());
        statistics.put(LBL_SOLIDITY, overlayStats.getSolidity());
        statistics.put(LBL_CIRCULARITY, overlayStats.getCircularity());
        statistics.put(LBL_THINNES_RATIO, overlayStats.getThinnesRatio());
        
        return statistics;
    }
    
    
    public void setRandomColor(List<Overlay> overlays){
        
        double GOLDEN_RATIO_CONJUGATE = 0.618033988749895;
        double SATURATION = 0.99;
        double VALUE = 0.99;
        
        double hue = Math.random();
        
        for(int i = 0; i < overlays.size(); i++){
            
            hue = hue + GOLDEN_RATIO_CONJUGATE;
            hue = hue % 1;
            ColorRGB randomColor = hsvtoRGB(hue, SATURATION, VALUE);
            
            overlays.get(i).setFillColor(randomColor);
            overlays.get(i).setLineColor(randomColor);
            overlays.get(i).update();
        }
    }
    
    
    public ColorRGB hsvtoRGB(double hue, double saturation, double value){
        
         int h = (int) (hue * 6);
        double f = hue * 6 - h;
        double p = value * (1 - saturation);
        double q = value * (1 - f * saturation);
        double t = value * (1 - (1 - f) * saturation);

        double r1, g1, b1;
        int r, g, b;
        
        switch (h){
            case 0: r1 = saturation; g1 = t; b1 = p; break;
            case 1: r1 = q; g1 = value; b1 = p; break;
            case 2: r1 = p; g1 = value; b1 = t; break;
            case 3: r1 = p; g1 = q; b1 = value; break;
            case 4: r1 = t; g1 = p; b1 = value; break;
            case 5: r1 = value; g1 = p; b1 = q; break;
            default: throw new RuntimeException(
                    String.format("Could not convert from HSV (%f, %f, %f) to RGB", hue, saturation, value));
        }
        r = (int)(r1*256);
        g = (int)(g1*256);
        b = (int)(b1*256);

        return new ColorRGB(r, g, b);          
    }
    
    
    public Overlay cleanOverlay(Overlay overlay){

        PolygonRegionOfInterest roi = (PolygonRegionOfInterest) overlay.getRegionOfInterest();
        int npoints = roi.getVertexCount();

        if(npoints >= 3){
            
            int i;
            for(i = 0; i <= npoints-1; i++){
                int j = i+1;
                int k = i+2;
                if(i == npoints-1){
                    j = 0;
                    k = 1;
                }
                if(i == npoints-2){
                    k = 0;
                }
                Point pt1 = new Point((int)roi.getVertex(i).getDoublePosition(0), (int)roi.getVertex(i).getDoublePosition(1));
                Point pt2 = new Point((int)roi.getVertex(j).getDoublePosition(0), (int)roi.getVertex(j).getDoublePosition(1));                
                Point pt3 = new Point((int)roi.getVertex(k).getDoublePosition(0), (int)roi.getVertex(k).getDoublePosition(1));
                
                if(areColinear(pt1, pt2, pt3)){
                    roi.removeVertex(j);
                    i--;
                    npoints--;
                }
            }
            
        }
        
        return overlay;
    }
    
    
    public boolean areColinear(Point pt1, Point pt2, Point pt3){
        
        boolean colinear = false;
        
        double signedArea = ( (pt2.getX() - pt1.getX()) * (pt3.getY() - pt1.getY()) ) - ( (pt3.getX() - pt1.getX()) * (pt2.getY() - pt1.getY()) );
        
        colinear = (signedArea == 0.0 );
        
        return colinear;
    }
}
