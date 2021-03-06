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
package ijfx.service.overlay;

import net.imagej.display.ImageDisplay;
import net.imagej.overlay.Overlay;
import org.scijava.plugin.Parameter;

/**
 *
 * @author Cyril MONGIS, 2016
 */
public class DefaultOverlayStatistics implements OverlayStatistics {

    private final Overlay overlay;

    private final OverlayShapeStatistics shapeStats;

    private final PixelStatistics pixelStats;

    @Parameter
    private OverlayStatService overlayStatsService;

    public DefaultOverlayStatistics(Overlay overlay, OverlayShapeStatistics shapeStats, PixelStatistics pixelStats) {
        this.overlay = overlay;
        this.shapeStats = shapeStats;
        this.pixelStats = pixelStats;
    }

    
    
    
    public DefaultOverlayStatistics(Overlay overlay, OverlayStatistics stats) {
        this.pixelStats = stats.getPixelStatistics();
        this.shapeStats = stats.getShapeStatistics();
        this.overlay = overlay;
    }
    
    public DefaultOverlayStatistics(ImageDisplay imageDisplay, Overlay overlay) {

        imageDisplay.getContext().inject(this);
        this.overlay = overlay;
        shapeStats = overlayStatsService.getShapeStatistics(overlay);
        pixelStats = new DefaultPixelStatistics(imageDisplay, overlay, imageDisplay.getContext());

    }

    @Override
    public OverlayShapeStatistics getShapeStatistics() {
        return shapeStats;
    }

    @Override
    public PixelStatistics getPixelStatistics() {
        return pixelStats;
    }

    @Override
    public Overlay getOverlay() {
        return overlay;
    }
    
}
