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
package ijfx.ui.datadisplay.table;

import ijfx.ui.main.ImageJFX;
import java.util.logging.Logger;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;
import jfxtras.scene.control.window.CloseIcon;
import jfxtras.scene.control.window.Window;
import net.imagej.table.TableDisplay;
import org.scijava.display.DisplayService;
import org.scijava.display.event.DisplayActivatedEvent;
import org.scijava.display.event.DisplayDeletedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;

/**
 *
 * @author Cyril MONGIS, 2015
 */
public class TableWindow extends Window {
    Logger logger = ImageJFX.getLogger();
    @Parameter
    DisplayService displayService;
    
    @Parameter
    EventService eventService;
    private TableDisplay tableDisplay;
    public TableWindow() {
        super();
        CloseIcon closeIcon = new CloseIcon(this);
        getRightIcons().add(closeIcon);


    }

    public TableWindow(TableDisplay display) {
        this();
        tableDisplay = display;
        setContentPane(new TableDisplayView(tableDisplay));
        for (EventType<? extends MouseEvent> t : new EventType[]{MouseEvent.MOUSE_CLICKED, MouseEvent.DRAG_DETECTED, MouseEvent.MOUSE_PRESSED}) {
            addEventHandler(t, this::putInFront);
            getContentPane().addEventHandler(t, this::putInFront);
        }
        display.getContext().inject(this);
                this.setOnCloseAction(event -> {
            eventService.publishLater(new DisplayDeletedEvent(tableDisplay));
        });

    }
    public void putInFront(MouseEvent event) {
        putInFront();
    }
    public void putInFront() {
        //  System.out.println("putting display to front");
        logger.info("Putting in front " + tableDisplay);
        displayService.setActiveDisplay(tableDisplay);
        displayService.getContext().toString();
        setMoveToFront(true);
    }
    
        @EventHandler
    protected void onActiveDisplayChanged(DisplayActivatedEvent event) {
        if (event.getDisplay() == tableDisplay) {
           
            setFocused(true);
        } else {
            setFocused(false);
        }
    }

}
