/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geojson;

import org.locationtech.jts.geom.Geometry;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.geojson.binding.GeoJSONFeature;
import org.apache.sis.storage.geojson.binding.GeoJSONFeatureCollection;
import org.apache.sis.storage.geojson.binding.GeoJSONGeometry;
import org.apache.sis.storage.geojson.binding.GeoJSONObject;
import org.apache.sis.storage.geojson.utils.GeoJSONParser;
import org.apache.sis.storage.geojson.utils.GeometryUtils;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.ObjectConverter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.storage.geojson.utils.GeoJSONUtils;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;

/**
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public class GeoJSONReader implements Iterator<Feature>, AutoCloseable {

    private final static Logger LOGGER = Logging.getLogger("org.apache.sis.storage.geojson");
    private final Map<Map.Entry<Class, Class>, ObjectConverter> convertersCache = new HashMap<>();

    private GeoJSONObject jsonObj = null;
    private Boolean toRead = true;

    protected final ReadWriteLock rwlock;
    protected final FeatureType featureType;
    protected final Path jsonFile;
    protected Feature current = null;
    protected int currentFeatureIdx = 0;

    /**
     * A flag indicating if we should read identifiers from read stream. it's
     * activated if the feature type given at built contains an {@link AttributeConvention#IDENTIFIER_PROPERTY}.
     */
    protected final boolean hasIdentifier;
    final Function idConverter;
    final CoordinateReferenceSystem crs;
    final String geometryName;

    public GeoJSONReader(Path jsonFile, FeatureType featureType, ReadWriteLock rwLock) {
        hasIdentifier = GeoJSONUtils.hasIdentifier(featureType);
        if (hasIdentifier) {
            idConverter = GeoJSONUtils.getIdentifierConverter(featureType);
        } else {
            // It should not be used, but we don't set it to null in case someons use it by mistake.
            idConverter = input -> input;
        }

        final PropertyType defaultGeometry = GeoJSONUtils.getDefaultGeometry(featureType);
        crs = GeoJSONUtils.getCRS(defaultGeometry);
        geometryName = defaultGeometry.getName().toString();

        this.jsonFile = jsonFile;
        this.featureType = featureType;
        this.rwlock = rwLock;
    }

    public FeatureType getFeatureType() {
        return featureType;
    }

    @Override
    public boolean hasNext() throws BackingStoreException {
        read();
        return current != null;
    }

    @Override
    public Feature next() throws BackingStoreException {
        read();
        final Feature ob = current;
        current = null;
        if(ob == null){
            throw new BackingStoreException("No more records.");
        }
        return ob;
    }

    private void read() throws BackingStoreException {
        if(current != null) return;

        //first call
        if (toRead) {
            rwlock.readLock().lock();
            try {
                jsonObj = GeoJSONParser.parse(jsonFile, true);
            } catch (IOException e) {
                throw new BackingStoreException(e);
            } finally {
                toRead = false;
                rwlock.readLock().unlock();
            }
        }

        if (jsonObj instanceof GeoJSONFeatureCollection) {
            final GeoJSONFeatureCollection fc = (GeoJSONFeatureCollection) jsonObj;
            rwlock.readLock().lock();
            try {
                if (fc.hasNext()) {
                    current = toFeature(fc.next());
                    currentFeatureIdx++;
                }
            } finally {
                rwlock.readLock().unlock();
            }
        } else if (jsonObj instanceof GeoJSONFeature) {
            current = toFeature((GeoJSONFeature)jsonObj);
            jsonObj = null;
        } else if (jsonObj instanceof GeoJSONGeometry) {
            current = toFeature((GeoJSONGeometry)jsonObj);
            jsonObj = null;
        }
    }

    /**
     * Convert a GeoJSONFeature to geotk Feature.
     * @param jsonFeature
     * @param featureId
     * @return
     */
    protected Feature toFeature(GeoJSONFeature jsonFeature) throws BackingStoreException {

        //Build geometry
        final Geometry geom = GeometryUtils.toJTS(jsonFeature.getGeometry(), crs);

        //empty feature
        final Feature feature = featureType.newInstance();
        if (hasIdentifier) {
            Object id = jsonFeature.getId();
            if (id == null) {
                id = currentFeatureIdx;
            }
            feature.setPropertyValue(AttributeConvention.IDENTIFIER_PROPERTY.toString(), idConverter.apply(id));
        }
        feature.setPropertyValue(geometryName, geom);

        //recursively fill other properties
        final Map<String, Object> properties = jsonFeature.getProperties();
        fillFeature(feature, properties);

        return feature;
    }

    /**
     * Recursively fill a ComplexAttribute with properties map
     * @param feature
     * @param properties
     */
    private void fillFeature(Feature feature, Map<String, Object> properties) throws BackingStoreException {
        final FeatureType featureType = feature.getType();

        for(final PropertyType type : featureType.getProperties(true)) {

            final String attName = type.getName().toString();
            final Object value = properties.get(attName);
            if(value==null) continue;

            if (type instanceof FeatureAssociationRole ) {
                final FeatureAssociationRole asso = (FeatureAssociationRole) type;
                final FeatureType assoType = asso.getValueType();
                final Class<?> valueClass = value.getClass();

                if (valueClass.isArray()) {
                    Class<?> base = value.getClass().getComponentType();

                    if (!Map.class.isAssignableFrom(base)) {
                        LOGGER.log(Level.WARNING, "Invalid complex property value " + value);
                    }

                    final int size = Array.getLength(value);
                    if (size > 0) {
                        //list of objects
                        final List<Feature> subs = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            final Feature subComplexAttribute = assoType.newInstance();
                            fillFeature(subComplexAttribute, (Map) Array.get(value, i));
                            subs.add(subComplexAttribute);
                        }
                        feature.setPropertyValue(attName,subs);
                    }
                } else if (value instanceof Map) {
                    final Feature subComplexAttribute = assoType.newInstance();
                    fillFeature(subComplexAttribute, (Map) value);
                    feature.setPropertyValue(attName, subComplexAttribute);
                }

            } else if(type instanceof AttributeType) {
                final Attribute<?> property = (Attribute<?>) feature.getProperty( type.getName().toString());
                fillProperty(property, value);
            }
        }
    }

    /**
     * Try to convert value as expected in PropertyType description.
     * @param prop
     * @param value
     */
    private void fillProperty(Attribute prop, Object value) throws BackingStoreException {

        Object convertValue = null;
        try {
            if (value != null) {
                final AttributeType<?> propertyType = prop.getType();
                final Class binding = propertyType.getValueClass();

                if (value.getClass().isArray() && binding.isArray()) {

                    int nbdim = 1;
                    Class base = value.getClass().getComponentType();
                    while (base.isArray()) {
                        base = base.getComponentType();
                        nbdim++;
                    }

                    convertValue = rebuildArray(value, base, nbdim);

                } else {
                    convertValue = convert(value, binding);
                }
            }
        } catch (UnconvertibleObjectException e1) {
            throw new BackingStoreException(String.format("Inconvertible property %s : %s",
                    prop.getName().tip().toString(), e1.getMessage()), e1);
        }

        prop.setValue(convertValue);
    }

    /**
     * Rebuild nDim arrays recursively
     * @param candidate
     * @param componentType
     * @param depth
     * @return Array object
     * @throws UnconvertibleObjectException
     */
    private Object rebuildArray(Object candidate, Class componentType, int depth) throws UnconvertibleObjectException {
        if(candidate==null) return null;

        if(candidate.getClass().isArray()){
            final int size = Array.getLength(candidate);
            final int[] dims = new int[depth];
            dims[0] = size;
            final Object rarray = Array.newInstance(componentType, dims);
            depth--;
            for(int k=0; k<size; k++){
                Array.set(rarray, k, rebuildArray(Array.get(candidate, k), componentType, depth));
            }
            return rarray;
        }else{
            return convert(candidate, componentType);
        }
    }

    /**
     * Convert value object into binding class
     * @param value
     * @param binding
     * @return
     * @throws UnconvertibleObjectException
     */
    private Object convert(Object value, Class binding) throws UnconvertibleObjectException {
        AbstractMap.SimpleEntry<Class, Class> key = new AbstractMap.SimpleEntry<>(value.getClass(), binding);
        ObjectConverter converter = convertersCache.get(key);

        if (converter == null) {
            converter = ObjectConverters.find(value.getClass(), binding);
            convertersCache.put(key, converter);
        }
        return converter.apply(value);
    }

    /**
     * Convert a GeoJSONGeometry to Feature.
     * @param jsonGeometry
     * @return
     */
    protected Feature toFeature(GeoJSONGeometry jsonGeometry) {
        final Feature feature = featureType.newInstance();
        final Geometry geom = GeometryUtils.toJTS(jsonGeometry, crs);
        feature.setPropertyValue(geometryName, geom);
        return feature;
    }

    @Override
    public void remove() {
        throw new BackingStoreException("Not supported on reader.");
    }

    @Override
    public void close() {
        try {
            // If our object is a feature collection, it could get an opened connexion to a file. We must dispose it.
            if (jsonObj instanceof AutoCloseable) {
                ((AutoCloseable) jsonObj).close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot close a read resource.", e);
        }
    }
}
