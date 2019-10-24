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
package org.apache.sis.internal.referencing.provider;

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for <cite>"Mercator (variant C)"</cite> projection (EPSG:1044).
 *
 * <div class="note"><b>Note on naming:</b>
 * The "Regional Mercator" class name is inspired by MapInfo practice, while not exactly the same projection.
 * The idea is that this class stands for the Mercator projection giving the most control to the user for
 * fitting a Mercator projection to a particular area of interest.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.6
 * @module
 */
@XmlTransient
public class RegionalMercator extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5957081563587752477L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "1044";

    /**
     * The operation parameter descriptor for the <cite>Latitude of false origin</cite> (φf) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Latitude of false origin </td></tr>
     *   <tr><td> OGC:     </td><td> latitude_of_origin </td></tr>
     *   <tr><td> ESRI:    </td><td> Latitude_Of_Origin </td></tr>
     *   <tr><td> NetCDF:  </td><td> latitude_of_projection_origin </td></tr>
     *   <tr><td> GeoTIFF: </td><td> FalseOriginLat </td></tr>
     *   <tr><td> Proj4:   </td><td> lat_0 </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Value domain: (-90.0 … 90.0)°</li>
     * </ul>
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_FALSE_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Easting at false origin</cite> (Ef) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Easting at false origin </td></tr>
     *   <tr><td> OGC:     </td><td> false_easting </td></tr>
     *   <tr><td> ESRI:    </td><td> False_Easting </td></tr>
     *   <tr><td> NetCDF:  </td><td> false_easting </td></tr>
     *   <tr><td> GeoTIFF: </td><td> FalseOriginEasting </td></tr>
     *   <tr><td> Proj4:   </td><td> x_0 </td></tr>
     * </table>
     */
    public static final ParameterDescriptor<Double> EASTING_AT_FALSE_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Northing at false origin</cite> (Nf) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Northing at false origin </td></tr>
     *   <tr><td> OGC:     </td><td> false_northing </td></tr>
     *   <tr><td> ESRI:    </td><td> False_Northing </td></tr>
     *   <tr><td> NetCDF:  </td><td> false_northing </td></tr>
     *   <tr><td> GeoTIFF: </td><td> FalseOriginNorthing </td></tr>
     *   <tr><td> Proj4:   </td><td> y_0 </td></tr>
     * </table>
     */
    public static final ParameterDescriptor<Double> NORTHING_AT_FALSE_ORIGIN;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        LATITUDE_OF_FALSE_ORIGIN = createLatitude(builder
                .addNamesAndIdentifiers(Mercator1SP.LATITUDE_OF_ORIGIN)
                .rename(Citations.EPSG, "Latitude of false origin")
                .rename(Citations.GEOTIFF, "FalseOriginLat")
                .reidentify(Citations.EPSG, "8821")
                .reidentify(Citations.GEOTIFF, "3085"), false);

        EASTING_AT_FALSE_ORIGIN = createShift(builder
                .addNamesAndIdentifiers(FALSE_EASTING)
                .rename(Citations.EPSG, "Easting at false origin")
                .rename(Citations.GEOTIFF, "FalseOriginEasting")
                .reidentify(Citations.EPSG, "8826")
                .reidentify(Citations.GEOTIFF, "3086"));

        NORTHING_AT_FALSE_ORIGIN = createShift(builder
                .addNamesAndIdentifiers(FALSE_NORTHING)
                .rename(Citations.EPSG, "Northing at false origin")
                .rename(Citations.GEOTIFF, "FalseOriginNorthing")
                .reidentify(Citations.EPSG, "8827")
                .reidentify(Citations.GEOTIFF, "3087"));

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName("Mercator (variant C)")
                .createGroupForMapProjection(
                        Mercator2SP.STANDARD_PARALLEL,
                        Mercator1SP.LONGITUDE_OF_ORIGIN,    // Really "natural origin", not "false origin".
                        LATITUDE_OF_FALSE_ORIGIN,
                        EASTING_AT_FALSE_ORIGIN,
                        NORTHING_AT_FALSE_ORIGIN);
    }

    /**
     * Constructs a new provider.
     */
    public RegionalMercator() {
        super(PARAMETERS);
    }
}
