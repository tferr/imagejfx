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
package ijfx.ui.module.skin;

import ijfx.ui.module.InputSkinPlugin;
import ijfx.ui.module.input.AbstractInputSkin;
import ijfx.ui.module.input.Input;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import javafx.util.converter.FormatStringConverter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Cyril MONGIS, 2015
 */
@Plugin(type = InputSkinPlugin.class)
public class BooleanInputSkin extends AbstractInputSkinPlugin<Boolean> {

    BooleanProperty value = new SimpleBooleanProperty();
    ComboBox<String> choice;

    public static final String YES = "Yes";
    public static final String NO = "No";

    @Override
    public void init(Input<Boolean> input) {
        
        choice = new ComboBox();
        
         Bindings.bindBidirectional(choice.valueProperty(), value, new StringConverter<Boolean>() {

            @Override
            public String toString(Boolean object) {
                if (object) {
                    return YES;
                } else {
                    return NO;
                }
            }

            @Override
            public Boolean fromString(String string) {
                return string.equals(YES);
            }
        });
        choice.getItems().addAll(YES, NO);
    }
    
    
    
    @Override
    public Property<Boolean> valueProperty() {
        return value;
    }

   

    @Override
    public Node getNode() {
        return choice;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean canHandle(Class<?> clazz) {
        return clazz == boolean.class || clazz == Boolean.class;
    }

    

}