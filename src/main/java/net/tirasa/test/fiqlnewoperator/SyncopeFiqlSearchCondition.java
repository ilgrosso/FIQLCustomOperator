package net.tirasa.test.fiqlnewoperator;

import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.Beanspector;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SimpleSearchCondition;

public class SyncopeFiqlSearchCondition<T> extends SimpleSearchCondition<T> {

    static {
        SUPPORTED_TYPES.add(ConditionType.CUSTOM);
    }

    private String operator;

    public SyncopeFiqlSearchCondition(final ConditionType cType, final T condition) {
        super(cType, condition);
    }

    public SyncopeFiqlSearchCondition(
            final Map<String, ConditionType> getters2operators,
            final Map<String, String> realGetters,
            final Map<String, Beanspector.TypeInfo> propertyTypeInfo,
            final T condition,
            final String operator) {

        super(getters2operators, realGetters, propertyTypeInfo, condition);
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

}
