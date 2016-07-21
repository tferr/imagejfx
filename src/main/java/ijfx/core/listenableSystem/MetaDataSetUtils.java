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
package ijfx.core.listenableSystem;

import ijfx.core.metadata.MetaData;
import ijfx.core.metadata.MetaDataKeyPrioritizer;
import ijfx.core.metadata.MetaDataSet;
import ijfx.ui.explorer.Explorable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * @author cyril
 */
public class MetaDataSetUtils {
    public static Set<String> getAllPossibleKeys(List<MetaDataSet> setList) {
        Set<String> possibleKeys = new HashSet<>();
        setList.forEach(set->{
            possibleKeys.addAll(set.keySet());
        });
        return possibleKeys;
    }
    
     public static double[] getMetadatas(Explorable explorable, List<String> metadataKeys) {

        final double[] result = new double[metadataKeys.size()];
        for (int i = 0; i < metadataKeys.size(); i++) {
            String str = metadataKeys.get(i);
            result[i] = explorable.getMetaDataSet().get(str).getDoubleValue();
        }
        return result;
    }
     
     public String exportToCSV(List<MetaDataSet> mList, String separator,boolean includeHheader,String[] priority) {
         
         // getting all the possible keys of the list of metadataset
         Collection<String> keys = getAllPossibleKeys(mList);
         
         if(priority == null) {
             priority = new String[0];
         }
         
         
         // getting ordering the keys by priority
         List<String> orderedKeys = keys
                 .stream()
                 .sorted(
                         new MetaDataKeyPrioritizer(Stream.of(priority).collect(Collectors.toSet())))
                 .collect(Collectors.toList());
         
         //
         return IntStream
                 .range(0,mList.size())
                 .parallel()
                 .mapToObj(i->{
                     
                     // for each key, we get the associated string value
                     return orderedKeys
                             .stream()
                             .map(key->mList.get(i).getOrDefault(key, MetaData.NULL)
                                     .getStringValue())
                             .collect(Collectors.toList());
                     
                 })
                 .map(valueList->valueList.stream().collect(Collectors.joining(separator)))
                 .collect(Collectors.joining("\n"));
         
         
         
         
     }
     
     
     
}
