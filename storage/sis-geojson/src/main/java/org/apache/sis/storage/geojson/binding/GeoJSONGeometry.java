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
package org.apache.sis.storage.geojson.binding;

import org.apache.sis.storage.geojson.utils.GeoJSONTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public class GeoJSONGeometry extends GeoJSONObject implements Serializable {

    public GeoJSONGeometry() {
    }

    /**
     * POINT
     */
    public static class GeoJSONPoint extends GeoJSONGeometry {

        private double[] coordinates = null;

        public GeoJSONPoint() {
            setType(GeoJSONTypes.POINT);
        }

        public double[] getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(double[] coordinates) {
            this.coordinates = coordinates;
        }

    }

    /**
     * MULTI-POINT
     */
    public static class GeoJSONMultiPoint extends GeoJSONGeometry {

        private double[][] coordinates = null;

        public GeoJSONMultiPoint() {
            setType(GeoJSONTypes.MULTI_POINT);
        }

        public double[][] getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(double[][] coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * LINESTRING
     */
    public static class GeoJSONLineString extends GeoJSONGeometry {

        private double[][] coordinates = null;

        public GeoJSONLineString() {
            setType(GeoJSONTypes.LINESTRING);
        }

        public double[][] getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(double[][] coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * MULTI-LINESTRING
     */
    public static class GeoJSONMultiLineString extends GeoJSONGeometry {

        private double[][][] coordinates = null;

        public GeoJSONMultiLineString() {
            setType(GeoJSONTypes.MULTI_LINESTRING);
        }

        public double[][][] getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(double[][][] coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * POLYGON
     */
    public static class GeoJSONPolygon extends GeoJSONGeometry {

        private double[][][] coordinates = null;

        public GeoJSONPolygon() {
            setType(GeoJSONTypes.POLYGON);
        }

        public double[][][] getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(double[][][] coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * MULTI-POLYGON
     */
    public static  class GeoJSONMultiPolygon extends GeoJSONGeometry {

        private double[][][][] coordinates = null;

        public GeoJSONMultiPolygon() {
            setType(GeoJSONTypes.MULTI_POLYGON);
        }

        public double[][][][] getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(double[][][][] coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * GEOMETRY-COLLECTION
     */
    public static class GeoJSONGeometryCollection extends GeoJSONGeometry {

        protected List<GeoJSONGeometry> geometries = new ArrayList<GeoJSONGeometry>();

        public GeoJSONGeometryCollection() {
            setType(GeoJSONTypes.GEOMETRY_COLLECTION);
        }

        public List<GeoJSONGeometry> getGeometries() {
            return geometries;
        }

        public void setGeometries(List<GeoJSONGeometry> geometries) {
            this.geometries = geometries;
        }
    }
}
