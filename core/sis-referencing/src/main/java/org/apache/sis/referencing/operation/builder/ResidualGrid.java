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
package org.apache.sis.referencing.operation.builder;

import java.util.Arrays;
import java.util.function.Function;
import javax.measure.quantity.Dimensionless;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;


/**
 * The residuals after an affine approximation has been created for a set of matching control point pairs.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
final class ResidualGrid extends DatumShiftGrid<Dimensionless,Dimensionless> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5207799661806374259L;

    /**
     * Number of source dimensions of the residual grid.
     *
     * @see #INTERPOLATED_DIMENSIONS
     */
    static final int SOURCE_DIMENSION = 2;

    /**
     * The parameter descriptors for the "Localization grid" operation.
     * Current implementation is fixed to {@value #SOURCE_DIMENSION} dimensions.
     *
     * @see #getParameterDescriptors()
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = new ParameterBuilder().setRequired(true);
        final ParameterDescriptor<?>[] grids = new ParameterDescriptor<?>[] {
            builder.addName(Constants.NUM_ROW).createBounded(Integer.class, 2, null, null),
            builder.addName(Constants.NUM_COL).createBounded(Integer.class, 2, null, null),
            builder.addName("grid_x").create(Matrix.class, null),
            builder.addName("grid_y").create(Matrix.class, null)
        };
        PARAMETERS = builder.addName("Localization grid").createGroup(grids);
    }

    /**
     * Sets the parameters of the {@code InterpolatedTransform} which uses that localization grid.
     * The given {@code parameters} must have been created from {@link #PARAMETERS} descriptor.
     * This method sets the matrix parameters using views over the {@link #offsets} array.
     */
    @Override
    public void getParameterValues(final Parameters parameters) {
        final Matrix denormalization = gridToTarget.getMatrix();
        if (parameters instanceof ContextualParameters) {
            /*
             * The denormalization matrix computed by InterpolatedTransform is the inverse of the normalization matrix.
             * This inverse is not suitable for the transform created by LocalizationGridBuilder; we need to replace it
             * by the linear regression. We do not want to define a public API in `DatumShiftGrid` for that purpose yet
             * because it would complexify that class (we would have to define API contract, etc.).
             */
            MatrixSIS m = ((ContextualParameters) parameters).getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
            m.setMatrix(denormalization);
        }
        final int[] size = getGridSize();
        parameters.parameter(Constants.NUM_ROW).setValue(size[1]);
        parameters.parameter(Constants.NUM_COL).setValue(size[0]);
        parameters.parameter("grid_x").setValue(new Data(0, denormalization));
        parameters.parameter("grid_y").setValue(new Data(1, denormalization));
    }

    /**
     * Number of grid cells along the <var>x</var> axis.
     * This is <code>{@linkplain #getGridSize()}[0]</code> as a field for performance reasons.
     */
    private final int nx;

    /**
     * The residual data, as translations to apply on the result of affine transform.
     * In this flat array, index of target dimension varies fastest, then column index, then row index.
     * Single precision instead of double is presumed sufficient because this array contains only differences,
     * not absolute positions. Absolute positions will be computed by adding {@code double} values to those offsets.
     */
    private final float[] offsets;

    /**
     * Conversion from translated coordinates (after the datum shift has been applied) to "real world" coordinates.
     * If we were doing NADCON or NTv2 transformations with {@link #isCellValueRatio()} = {@code true} (source and
     * target coordinates in the same coordinate system with axis units in degrees), that conversion would be the
     * inverse of {@link #getCoordinateToGrid()}. But in this {@code ResidualGrid} case, we need to override with
     * the linear regression computed by {@link LocalizationGridBuilder}.
     */
    final LinearTransform gridToTarget;

    /**
     * The best translation accuracy that we can expect from this file.
     *
     * @see #getCellPrecision()
     */
    private final double accuracy;

    /**
     * Creates a new residual grid.
     *
     * @param sourceToGrid  conversion from the "real world" source coordinates to grid indices including fractional parts.
     * @param gridToTarget  conversion from grid coordinates to the final "real world" coordinates.
     * @param residuals     the residual data, as translations to apply on the result of affine transform.
     * @param precision     desired precision of inverse transformations in unit of grid cells.
     */
    ResidualGrid(final LinearTransform sourceToGrid, final LinearTransform gridToTarget,
            final int nx, final int ny, final float[] residuals, final double precision)
    {
        super(Units.UNITY, sourceToGrid, new int[] {nx, ny}, true, Units.UNITY);
        this.gridToTarget = gridToTarget;
        this.offsets      = residuals;
        this.accuracy     = precision;
        this.nx           = nx;
    }

    /**
     * Returns a description of the values in this grid. Grid values may be given as matrices or tensors.
     * Current implementation provides values in the form of {@link Matrix} objects on the assumption
     * that the number of {@linkplain #getGridSize() grid} dimensions is {@value #SOURCE_DIMENSION}.
     *
     * <div class="note"><b>Note:</b>
     * the number of {@linkplain #getGridSize() grid} dimensions determines the parameter type: if that number
     * is greater than {@value #SOURCE_DIMENSION}, then parameters would need to be represented by tensors instead
     * than matrices. By contrast, the {@linkplain #getTranslationDimensions() number of dimensions of translation
     * vectors} only determines how many matrix or tensor parameters appear.</div>
     *
     * @return a description of the values in this grid.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return PARAMETERS;
    }

    /**
     * Returns the number of dimensions of the translation vectors interpolated by this shift grid.
     */
    @Override
    public int getTranslationDimensions() {
        return SOURCE_DIMENSION;
    }

    /**
     * Returns the desired precision in iterative calculation performed by inverse transform.
     * The returned value is in unit of grid cell, i.e. a value of 1 is the size of one cell.
     * This unit of measurement is fixed by {@link #isCellValueRatio()} = {@code true}.
     */
    @Override
    public double getCellPrecision() {
        return accuracy;
    }

    /**
     * Returns the cell value at the given dimension and grid index.
     * Those values are components of <em>translation</em> vectors.
     */
    @Override
    public double getCellValue(int dim, int gridX, int gridY) {
        return offsets[(gridX + gridY*nx) * SOURCE_DIMENSION + dim];
    }

    /**
     * View over one target dimension of the localization grid. Used for populating the {@link ParameterDescriptorGroup}
     * that describes the {@code MathTransform}. Those parameters are themselves used for formatting Well Known Text.
     * Current implementation can be used only when the number of grid dimensions is {@value #INTERPOLATED_DIMENSIONS}.
     * If a grid has more dimensions, then tensors would need to be used instead than matrices.
     *
     * <p>This implementation can not be moved to the {@link DatumShiftGrid} parent class because this class assumes
     * that the translation vectors are added to the source coordinates. This is not always true; for example France
     * Geocentric interpolations add the translation to coordinates converted to geocentric coordinates.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.0
     * @since   1.0
     * @module
     */
    private final class Data extends FormattableObject implements Matrix, Function<int[],Number> {
        /** Coefficients from the denormalization matrix for the row corresponding to this dimension. */
        private final double c0, c1, c2;

        /** Creates a new matrix for the specified dimension. */
        Data(final int dim, final Matrix denormalization) {
            c0 = denormalization.getElement(dim, 0);
            c1 = denormalization.getElement(dim, 1);
            c2 = denormalization.getElement(dim, 2);
        }

        @SuppressWarnings("CloneInNonCloneableClass")
        @Override public Matrix  clone()                            {return this;}
        @Override public boolean isIdentity()                       {return false;}
        @Override public int     getNumCol()                        {return nx;}
        @Override public int     getNumRow()                        {return getGridSize()[1];}
        @Override public Number  apply     (int[] p)                {return getElement(p[1], p[0]);}
        @Override public void    setElement(int y, int x, double v) {throw new UnsupportedOperationException();}

        /** Computes the matrix element in the given row and column. */
        @Override public double  getElement(final int y, final int x) {
            return c0 * (x + getCellValue(0, x, y)) +                // TODO: use Math.fma with JDK9.
                   c1 * (y + getCellValue(1, x, y)) +
                   c2;
        }

        /**
         * Returns a short string representation on one line. This appears as a single row
         * in the table formatted for {@link ParameterDescriptorGroup} string representation.
         */
        @Override public String toString() {
            final int[] size = getGridSize();
            return new StringBuilder(80).append('[')
                    .append(getElement(0, 0)).append(", …, ")
                    .append(getElement(size[1] - 1, size[0] - 1))
                    .append(']').toString();
        }

        /**
         * Returns a multi-lines string representation. This appears in the Well Known Text (WKT)
         * formatting of {@link org.opengis.referencing.operation.MathTransform}.
         */
        @Override protected String formatTo(final Formatter formatter) {
            final Object[] numbers = WKTUtilities.cornersAndCenter(this, getGridSize(), 3);
            final Vector[] rows = new Vector[numbers.length];
            final Statistics stats = new Statistics(null);          // For computing accuracy.
            Vector before = null;
            for (int j=0; j<rows.length; j++) {
                final Vector row = Vector.create(numbers[j], false);
                /*
                 * Estimate an accuracy to use for formatting values. This computation is specific to ResidualGrid
                 * since it assumes that values in each corner are globally increasing or decreasing. Consequently
                 * the differences between consecutive values are assumed a good indication of desired accuracy
                 * (this assumption does not hold for arbitrary matrix).
                 */
                Number right = null;
                for (int i=row.size(); --i >= 0;) {
                    final Number n = row.get(i);
                    if (n != null) {
                        final double value = n.doubleValue();
                        if (right != null) {
                            stats.accept(Math.abs(right.doubleValue() - value));
                        }
                        if (before != null) {
                            final Number up = before.get(i);
                            if (up != null) {
                                stats.accept(Math.abs(up.doubleValue() - value));
                            }
                        }
                    }
                    right = n;
                }
                before  = row;
                rows[j] = row;
            }
            final int accuracy = Numerics.suggestFractionDigits(stats);
            formatter.newLine();
            formatter.append(rows, Math.max(0, accuracy));
            formatter.setInvalidWKT(Matrix.class, null);
            return "Matrix";
        }
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {                        // Optimization for a common case.
            return true;
        }
        if (super.equals(other)) {
            final ResidualGrid that = (ResidualGrid) other;
            return Numerics.equals(accuracy, that.accuracy) &&
                    gridToTarget.equals(that.gridToTarget) &&
                    Arrays.equals(offsets, that.offsets);
        }
        return false;
    }

    /**
     * Returns a hash code value for this datum shift grid.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(offsets) + 37 * gridToTarget.hashCode();
    }
}
