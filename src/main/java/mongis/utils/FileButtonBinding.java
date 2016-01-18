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
package mongis.utils;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;

/**
 *
 * A helper class to create buttons that allow the selection of the directory. When clicking on the button,
 * it opens a file saving dialog or open dialog
 * 
 * Ex : 
 * 
 * Button aButton = new Button();
 * 
 * FileButtonBinding fileButtonBinding = 
 *      new FileButtonBinding(aButton)
 *      .setSaveDialog(true)
 *      .
 * 
 * fileButtonBinding
 * 
 * 
 * @author cyril
 */
public class FileButtonBinding {
     
    private final Button button;
    
    
    private final ObjectProperty<File> fileProperty = new SimpleObjectProperty<>(null);
    
    private String buttonDefaultText = "Choose a directory ...";
    
    
    
    public FileButtonBinding(Button b) {
        this.button = b;
        
        button.setOnAction(this::onClick);
        fileProperty.addListener(this::onFileChanged);
        
        
        onFileChanged(null,null,fileProperty.getValue());
        button.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_ALT));
    }
    
    protected void onClick(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();

        File saveFolder = chooser.showDialog(null);

        if (saveFolder != null) {
            fileProperty.setValue(saveFolder);
        }
    }

    public String getButtonDefaultText() {
        return buttonDefaultText;
    }

    public FileButtonBinding setButtonDefaultText(String buttonDefaultText) {
        this.buttonDefaultText = buttonDefaultText;
        return this;
    }
    
    
    protected void onFileChanged(Observable obs, File oldValue, File newValue) {
        if(newValue == null ) {
            button.setText(buttonDefaultText);
        }
        else {
            button.setText(newValue.getParentFile().getName() + " / " + newValue.getName());
        }
    }
    
    
    
    public ObjectProperty<File> fileProperty() {
        return fileProperty;
    }
    
    
}