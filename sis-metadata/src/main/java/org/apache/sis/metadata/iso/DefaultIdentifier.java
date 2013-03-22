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
package org.apache.sis.metadata.iso;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;


/**
 * Value uniquely identifying an object within a namespace.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Identifier_Type", propOrder = {
    "code",
    "authority"
})
@XmlRootElement(name = "MD_Identifier")
public class DefaultIdentifier extends ISOMetadata implements Identifier {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7459062382170865919L;

    /**
     * Alphanumeric value identifying an instance in the namespace.
     */
    private String code;

    /**
     * Identifier of the version of the associated code space or code, as specified
     * by the code space or code authority. This version is included only when the
     * {@linkplain #getCode code} uses versions. When appropriate, the edition is
     * identified by the effective date, coded using ISO 8601 date format.
     */
    private String version;

    /**
     * Organization or party responsible for definition and maintenance of the
     * {@linkplain #getCode code}.
     */
    private Citation authority;

    /**
     * Construct an initially empty identifier.
     */
    public DefaultIdentifier() {
    }

    /**
     * Creates an identifier initialized to the given code.
     *
     * @param code The alphanumeric value identifying an instance in the namespace,
     *             or {@code null} if none.
     */
    public DefaultIdentifier(final String code) {
        this.code = code;
    }

    /**
     * Creates an identifier initialized to the given authority and code.
     *
     * @param code      The alphanumeric value identifying an instance in the namespace,
     *                  or {@code null} if none.
     * @param authority The organization or party responsible for definition and maintenance
     *                  of the code, or {@code null} if none.
     */
    public DefaultIdentifier(final String code, final Citation authority) {
        this.code = code;
        this.authority = authority;
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultIdentifier castOrCopy(final Identifier object) {
        if (object == null || object instanceof DefaultIdentifier) {
            return (DefaultIdentifier) object;
        }
        final DefaultIdentifier copy = new DefaultIdentifier();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Alphanumeric value identifying an instance in the namespace.
     */
    @Override
    @XmlElement(name = "code", required = true)
    public synchronized String getCode() {
        return code;
    }

    /**
     * Sets the alphanumeric value identifying an instance in the namespace.
     *
     * @param newValue The new code, or {@code null}.
     */
    public synchronized void setCode(final String newValue) {
        checkWritePermission();
        code = newValue;
    }

    /**
     * Identifier of the version of the associated code, as specified by the code space or
     * code authority. This version is included only when the {@linkplain #getCode() code}
     * uses versions. When appropriate, the edition is identified by the effective date,
     * coded using ISO 8601 date format.
     *
     * @return The version, or {@code null} if not available.
     */
    public synchronized String getVersion() {
        return version;
    }

    /**
     * Sets an identifier of the version of the associated code.
     *
     * @param newValue The new version.
     */
    public synchronized void setVersion(final String newValue) {
        checkWritePermission();
        version = newValue;
    }

    /**
     * Organization or party responsible for definition and maintenance of the
     * {@linkplain #getCode() code}.
     *
     * @return The authority, or {@code null} if not available.
     */
    @Override
    @XmlElement(name = "authority")
    public synchronized Citation getAuthority() {
        return authority;
    }

    /**
     * Sets the organization or party responsible for definition and maintenance of the
     * {@linkplain #getCode code}.
     *
     * @param newValue The new authority.
     */
    public synchronized void setAuthority(final Citation newValue) {
        checkWritePermission();
        authority = newValue;
    }
}
