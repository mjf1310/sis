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

import org.apache.sis.util.Static;

/**
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public final class GeoJSONTypes extends Static {

    public static final String FEATURE_COLLECTION = "FeatureCollection";
    public static final String FEATURE = "Feature";

    public static final String POINT = "Point";
    public static final String LINESTRING = "LineString";
    public static final String POLYGON = "Polygon";
    public static final String MULTI_POINT = "MultiPoint";
    public static final String MULTI_LINESTRING = "MultiLineString";
    public static final String MULTI_POLYGON = "MultiPolygon";
    public static final String GEOMETRY_COLLECTION = "GeometryCollection";

    public static final String CRS_NAME = "name";
    public static final String CRS_LINK = "link";

    public static final String CRS_TYPE_PROJ4 = "proj4";
    public static final String CRS_TYPE_OGCWKT = "ogcwkt";
    public static final String CRS_TYPE_ESRIWKT = "esriwkt";

}
