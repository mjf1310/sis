package org.apache.sis.filter;

import java.util.function.Function;

import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.extent.GeographicBoundingBox;

import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.Features;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.feature.Geometries;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * Naïve implementation of SQLMM ST_Envelope operation. Compute bounding box of a geometry. Coordinate reference
 * system unchanged.
 *
 * @author Alexis Manin (Geomatys)
 */
public class ST_Envelope extends AbstractFunction implements FeatureExpression {

    public static final String NAME = "ST_Envelope";

    private final Worker worker;
    public ST_Envelope(Expression[] parameters) {
        super(NAME, parameters, null);
        if (parameters == null || parameters.length != 1) throw new MismatchedDimensionException(
                String.format(
                    "Single parameter expected for %s operation: source Geometry. However, %d arguments were provided",
                    NAME, parameters == null? 0 : parameters.length
                )
        );

        final Expression parameter = parameters[0];
        if (parameter instanceof Literal) worker = new LiteralEnvelope((Literal) parameter);
        else if (parameter instanceof FeatureExpression) worker = new FeatureEnvelope((FeatureExpression) parameter);
        else throw new UnsupportedOperationException("Given parameter must either be a literal or a feature expression");
    }

    @Override
    public Object evaluate(Object object) {
        return worker.evaluate(object);
    }

    @Override
    public PropertyType expectedType(FeatureType type) {
        return worker.type(type);
    }

    /**
     * An implementation of ST_Envelope working on a literal. It is a special case where computation can be done only
     * once at built time, save both CPU time and memory, by caching result as a unique reference. Also, it allows to
     * merge parameter validation with real computation, ensuring that operator instance will consistently return result
     */
    private class LiteralEnvelope implements Worker {

        final Envelope result;
        final AttributeType resultType;

        public LiteralEnvelope(Literal source) {
            Object value = source == null? null : source.getValue();
            ensureNonNull("Source value", value);
            final Envelope tmpResult = tryGet(value);

            if (tmpResult == null) {
                throw new IllegalArgumentException("Given value is of unsupported type: "+value.getClass());
            }

            result = new ImmutableEnvelope(tmpResult);
            resultType = new DefaultAttributeType(null, Envelope.class, 1, 1, null);
        }

        @Override
        public PropertyType type(FeatureType target) {
            return resultType;
        }

        @Override
        public Envelope evaluate(Object target) {
            return result;
        }
    }

    private class FeatureEnvelope implements Worker {

        final FeatureExpression source;
        final Function evaluator;

        private FeatureEnvelope(FeatureExpression source) {
            this.source = source;
            if (source instanceof Expression) {
                final Expression exp = (Expression) source;
                evaluator = exp::evaluate;
            } else if (source instanceof Function) {
                evaluator = (Function) source;
            } else throw new UnsupportedOperationException("Cannot create envelope operation from a feature expression which is not a function");
        }

        @Override
        public PropertyType type(FeatureType target) {
            final PropertyType expressionType = source.expectedType(target);
            final AttributeType<?> attr = Features.castOrUnwrap(expressionType)
                    .orElseThrow(() -> new UnsupportedOperationException("Cannot evaluate given expression because it does not create attribute values"));
            // If given expression evaluates directly to a bbox, there's no need for a conversion step.
            if (Envelope.class.isAssignableFrom(attr.getValueClass())) {
                return expressionType;
            }

            final int minOccurs = attr.getMinimumOccurs();
            final AttributeType<?> crsCharacteristic = attr.characteristics().get(AttributeConvention.CRS_CHARACTERISTIC);
            AttributeType[] crsParam = crsCharacteristic == null? null : new AttributeType[]{crsCharacteristic};
            return new DefaultAttributeType<>(null, Envelope.class, Math.min(1, minOccurs), 1, null, crsParam);
        }

        @Override
        public Envelope evaluate(Object target) {
            final Object extractedValue = evaluator.apply(target);
            if (extractedValue == null) return null;
            final Envelope env = tryGet(extractedValue);
            if (env == null) throw new RuntimeException("A value is present, but its envelope cannot be determined");
            if (env.getCoordinateReferenceSystem() == null) {
                // TODO: how to determine CRS ?
            }

            return env;
        }
    }

    private interface Worker {
        PropertyType type(FeatureType target);
        Envelope evaluate(Object target);
    }

    private static Envelope tryGet(Object value) {
        if (value == null) return null;

        if (value instanceof GeographicBoundingBox) {
            return new GeneralEnvelope((GeographicBoundingBox)value);
        } else if (value instanceof Envelope) {
            return (Envelope) value;
        } else if (value instanceof CharSequence) {
            // Maybe it's a WKT format, so we will try to read it
            value = Geometries.fromWkt(value.toString())
                    .orElseThrow(() -> new IllegalArgumentException("No geometry provider found to read WKT"));
        }

        return Geometries.getEnvelope(value);
    }
}