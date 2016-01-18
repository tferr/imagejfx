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
package ijfx.core.metadata;

import java.util.HashMap;
import java.util.Map;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Cyril MONGIS, 2015
 */
public interface MetaData {
    /**
     * Transforms a metaDataSet to a string to string hashMap. This method can be useful if one wants to write this metaDataSet to a file in
     * a string format. 
     * @param metaDataSet
     * @return 
     */
     public static HashMap<String, String> metaDataSetToMap(Map<String,MetaData> metaDataSet) {
        HashMap<String, String> map = new HashMap<>();
        for (String key : metaDataSet.keySet()) {
            map.put(key, metaDataSet.get(key).getValue().toString());
        }
        return map;
    }
    public String getName();
    public void setName(String name);
    public void setValue(Object value);
    public String getStringValue();
    public Integer getIntegerValue();
    public Object getValue();
    public Double getDoubleValue();
   
    
    public final static int TYPE_STRING = 0;
    public final static int TYPE_INTEGER = 1;
    public final static int TYPE_DOUBLE = 2;
    public final static int TYPE_NOT_SET = -1;
    public final static int TYPE_UNKNOWN = 3;
    
    
    
    public int getType();
    public int getOrigin();
    
    
    public final static int ORIGIN_BASIC = 0;
    public final static int ORIGIN_RAW = 1;
    public final static int ORIGIN_ADDED = 2;
    public final static int ORIGIN_CALCULTAED = 3;
    
    
    public final static String WIDTH = "Width";
    public final static String HEIGHT = "Height";
    public final static String CHANNEL = "Channel";
    public final static String X_POSITION = "X";
    public final static String Y_POSITION = "Y";
    public final static String Z_POSITION = "Z";
    public final static String TIME="Time";
    public final static String DATE = "Date";
    public final static String YEAR = "Year";
    public final static String MONTH = "Month";
    public final static String DAY = "Day";
    public final static String WELL_NAME = "Well";
    public final static String POSITION = "Pos";    
    
    public final static String SLICE_NUMBER = "Slice number";
    public final static String FILE_NAME = "File Name";
    public final static String FOLDER_NAME = "Folder Name";
    public final static String ABSOLUTE_PATH = "Absolute path";
    public final static String CHANNEL_NAME = "Channel name";
    public final static String SEQUENCE_NUMBER = "Seq";
    
    public final static String ZSTACK_NUMBER="ZStack";
    
    public final static String PLANE_INDEX="Plane Index";
    
    // addedd an other useful comment
}