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
package ijfx.bridge;

import ijfx.service.batch.BatchService;
import ijfx.ui.datadisplay.image.ImageWindowContainer;
import ijfx.ui.datadisplay.image.ImageWindow;
import ijfx.ui.main.ImageJFX;
import ijfx.ui.datadisplay.table.TableWindow;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import net.imagej.DatasetService;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.table.TableDisplay;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.AbstractUserInterface;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.StatusBar;
import org.scijava.ui.SystemClipboard;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.DisplayWindow;
import mongis.utils.FXUtilities;

/**
 * UI bridge between ImageJFX and ImageJ
 *
 * @author Cyril MONGIS, 2015
 */
@Plugin(type = UserInterface.class, name = "FX UI", priority = 1000)
public class FxUserInterfaceBridge extends AbstractUserInterface {

    @Parameter
    private ImageDisplayService imageDisplayService;

    @Parameter
    private DisplayService displayService;

    @Parameter
    private DatasetService dataset;

    @Parameter
    private UIService uiService;

    @Parameter
    private BatchService batchService;
    
    private static final Logger logger = ImageJFX.getLogger();

    private boolean injection = false;

    private FxStatusBar fxToolbar = FxStatusBar.getInstance();

    private FxPromptDialog lastDialog;

    private File lastOpenedFile;

    public FxUserInterfaceBridge() {

    }

    public void initialize() {
        // making sure the StatusBar is created
        getStatusBar();
    }

    public double getPriority() {
        return 1000.0;
    }

    @Override
    public SystemClipboard getSystemClipboard() {
        return null;
    }

    @Override
    public DisplayWindow createDisplayWindow(Display<?> dspl) {

        // no display creation is supported yet.
        logger.info("Creating a display from " + dspl.toString());
        logger.warning("Display Creation not supported yet !");
        return null;
    }

    @Override
    public DialogPrompt dialogPrompt(String string, String string1, DialogPrompt.MessageType mt, DialogPrompt.OptionType ot) {

        // runs a FX Dialog in the JavaFX Thread and wait for it to be finished
        try {
            FXUtilities.runAndWait(() -> lastDialog = new FxPromptDialog(string, string1, mt, ot));
        } catch (InterruptedException ex) {
            ImageJFX.getLogger();
        } catch (ExecutionException ex) {
            ImageJFX.getLogger();
        }

        return lastDialog;
    }

    @Override
    public File chooseFile(File file, String string) {
        
        logger.info("Choosing file..." + (file != null ? file.getAbsolutePath() : "(no file)") + " " +string);
        
        // starting a file chooser
        final FileChooser chooser = new FileChooser();
        
        if (file != null && file.isDirectory()) {
            chooser.setInitialDirectory(file);
        }

        

        //runs the open file dialog and wait for it
        try {
            FXUtilities.runAndWait(() -> {
                
                
                
                
                if(string != null && string.toLowerCase().contains("save")) {
                
                    lastOpenedFile = chooser.showSaveDialog(null);
                }
                else {
                    lastOpenedFile = chooser.showOpenDialog(null);
                }

            });
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Error when choosing file",ex);
        } 

        return lastOpenedFile;
    }

    @Override
    public void showContextMenu(String string, Display<?> dspl, int i, int i1) {
        logger.warning("Context menu displaying is not supported yet !");
    }

    @Override
    public boolean requiresEDT() {
        return true;
    }

    @Override
    public void dispose() {
        logger.info("Disposing");
    }

    @Override
    public void show(final Display<?> dspl) {
        
        if(batchService.isRunning()) {
            logger.info("BatchService is running... skipping display");
            return;
        }
        
        logger.info("Showing display");
        
        if (dspl instanceof ImageDisplay) {

            displayService.setActiveDisplay(dspl);

            // we always assume that an image is displayed so we create an image window
            // in the image container (singleton pattern)
            final ImageWindow imageWindow = new ImageWindow(dspl);

            Platform.runLater(() -> {
                // creating the @ImageWindow 
                // getting the @ImageWindowContainer unique instance
                ImageWindowContainer.getInstance().getChildren().add(imageWindow);

                // refreshing the window for some reason I forgot
                imageWindow.refreshSourceImage();
            });
        } else if (dspl instanceof TableDisplay) {
            Platform.runLater(() -> {

                ImageWindowContainer.getInstance().getChildren().add(new TableWindow((TableDisplay) dspl));
            });

        } else {
            logger.warning("Cannot show display type :" + dspl.getClass().getSimpleName());
        }
    }

    @Override
    public StatusBar getStatusBar() {
        if (injection == false) {
            getContext().inject(fxToolbar);
            injection = true;
        }
        return fxToolbar;
    }

    public FxStatusBar getFxStatusBar() {
        return fxToolbar;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

}