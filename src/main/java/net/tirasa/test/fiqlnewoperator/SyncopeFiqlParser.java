package net.tirasa.test.fiqlnewoperator;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;

public class SyncopeFiqlParser<T> extends FiqlParser<T> {

    public static final String IEQ = "=~";

    public static final String NIEQ = "!~";

    public SyncopeFiqlParser(
            final Class<T> tclass,
            final Map<String, String> contextProperties,
            final Map<String, String> beanProperties) {

        super(tclass, contextProperties, beanProperties);

        operatorsMap.put(IEQ, ConditionType.CUSTOM);
        operatorsMap.put(NIEQ, ConditionType.CUSTOM);

        CONDITION_MAP.put(ConditionType.CUSTOM, IEQ);
        CONDITION_MAP.put(ConditionType.CUSTOM, NIEQ);

        String comparators = GT + "|" + GE + "|" + LT + "|" + LE + "|" + EQ + "|" + NEQ + "|" + IEQ + "|" + NIEQ;
        String s1 = "[\\p{ASCII}]+(" + comparators + ")";
        comparatorsPattern = Pattern.compile(s1);
    }

    @Override
    protected ASTNode<T> parseComparison(final String expr) throws SearchParseException {
        Matcher m = comparatorsPattern.matcher(expr);
        if (m.find()) {
            String propertyName = expr.substring(0, m.start(1));
            String operator = m.group(1);
            String value = expr.substring(m.end(1));
            if ("".equals(value)) {
                throw new SearchParseException("Not a comparison expression: " + expr);
            }

            String name = unwrapSetter(propertyName);

            name = getActualSetterName(name);
            TypeInfoObject castedValue = parseType(propertyName, name, value);
            if (castedValue != null) {
                return new SyncopeComparison(name, operator, castedValue);
            } else {
                return null;
            }
        } else {
            throw new SearchParseException("Not a comparison expression: " + expr);
        }
    }

    private class SyncopeComparison implements ASTNode<T> {

        private final String name;

        private final String operator;

        private final TypeInfoObject tvalue;

        SyncopeComparison(String name, String operator, TypeInfoObject value) {
            this.name = name;
            this.operator = operator;
            this.tvalue = value;
        }

        @Override
        public String toString() {
            return name + " " + operator + " " + tvalue.getObject()
                    + " (" + tvalue.getObject().getClass().getSimpleName() + ")";
        }

        @Override
        public SearchCondition<T> build() throws SearchParseException {
            String templateName = getSetter(name);
            T cond = createTemplate(templateName);
            ConditionType ct = operatorsMap.get(operator);

            if (isPrimitive(cond)) {
                return new SyncopeFiqlSearchCondition<>(ct, cond);
            } else {
                String templateNameLCase = templateName.toLowerCase();
                return new SyncopeFiqlSearchCondition<>(Collections.singletonMap(templateNameLCase, ct),
                        Collections.singletonMap(templateNameLCase, name),
                        Collections.singletonMap(templateNameLCase, tvalue.getTypeInfo()),
                        cond, operator);
            }
        }

        private boolean isPrimitive(T pojo) {
            return pojo.getClass().getName().startsWith("java.lang");
        }

        @SuppressWarnings("unchecked")
        private T createTemplate(final String setter) throws SearchParseException {
            try {
                if (beanspector != null) {
                    beanspector.instantiate().setValue(setter, tvalue.getObject());
                    return beanspector.getBean();
                } else {
                    SearchBean bean = (SearchBean) conditionClass.newInstance();
                    bean.set(setter, tvalue.getObject().toString());
                    return (T) bean;
                }
            } catch (Throwable e) {
                throw new SearchParseException(e);
            }
        }
    }

}
