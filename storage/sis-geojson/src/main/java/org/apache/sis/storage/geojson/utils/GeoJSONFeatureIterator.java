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
package org.apache.sis.storage.geojson.utils;

import java.io.Closeable;
import java.util.Iterator;
import org.apache.sis.storage.geojson.binding.GeoJSONFeature;
import org.apache.sis.util.collection.BackingStoreException;

/**
 * Custom FeatureIterator used for lazy parsing of GeoJSONFeature in a json file.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public interface GeoJSONFeatureIterator<F extends GeoJSONFeature> extends Iterator<F>, Closeable {

    @Override
    F next() throws BackingStoreException;

    @Override
    boolean hasNext() throws BackingStoreException;

    @Override
    void close() throws BackingStoreException;

}
