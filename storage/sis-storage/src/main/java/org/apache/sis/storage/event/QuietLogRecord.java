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
package org.apache.sis.storage.event;

import java.util.logging.Level;
import java.util.logging.LogRecord;


/**
 * A log record to be logged without stack trace, unless the user specified it explicitly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
final class QuietLogRecord extends LogRecord {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5652099235767670922L;

    /**
     * {@code true} if the user invoked {@link #setThrown(Throwable)} explicitly.
     * In such case, {@link #clearImplicitThrown()} will not reset the throwable to null.
     */
    private boolean explicitThrown;

    /**
     * Creates a new log record for the given message and exception.
     */
    QuietLogRecord(final Level level, final String message, final Exception exception) {
        super(level, message);
        super.setThrown(exception);
    }

    /**
     * Sets the throwable to the given value. The given throwable will not be cleared
     * when the record will be logged.
     */
    @Override
    public void setThrown(final Throwable thrown) {
        explicitThrown = true;
        super.setThrown(thrown);
    }

    /**
     * Clears the throwable if it has not been explicitly set by the user.
     * Otherwise do nothing.
     */
    void clearImplicitThrown() {
        if (!explicitThrown) {
            super.setThrown(null);
        }
    }
}
