package net.tirasa.test.fiqlnewoperator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.ConditionType;

import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractUntypedSearchConditionVisitor;

public class SQLPrinterVisitor<T> extends AbstractUntypedSearchConditionVisitor<T, String> {

    private String table;

    private String tableAlias;

    private List<String> columns;

    // Can be useful when some other code will build Select and From clauses.
    public SQLPrinterVisitor() {
        this(null, null, Collections.<String>emptyList());
    }

    public SQLPrinterVisitor(String table, String... columns) {
        this(null, table, Arrays.asList(columns));
    }

    public SQLPrinterVisitor(Map<String, String> fieldMap,
            String table,
            List<String> columns) {
        this(fieldMap, table, null, columns);
    }

    public SQLPrinterVisitor(Map<String, String> fieldMap,
            String table,
            String tableAlias,
            List<String> columns) {
        super(fieldMap);

        this.columns = columns;
        this.table = table;
        this.tableAlias = tableAlias;
    }

    @Override
    public void visit(SearchCondition<T> sc) {
        StringBuilder sb = getStringBuilder();

        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                String name = getRealPropertyName(statement.getProperty());
                String originalValue = getPropertyValue(name, statement.getValue());
                validatePropertyValue(name, originalValue);

                String value = SearchUtils.toSqlWildcardString(originalValue, isWildcardStringMatch());
                value = SearchUtils.duplicateSingleQuoteIfNeeded(value);

                if (tableAlias != null) {
                    name = tableAlias + "." + name;
                }

                ConditionType ct = sc.getConditionType();
                String effectiveName = name;
                String effectiveValue = "'" + value + "'";
                if (sc instanceof SyncopeFiqlSearchCondition && sc.getConditionType() == ConditionType.CUSTOM) {
                    SyncopeFiqlSearchCondition<T> sfsc = (SyncopeFiqlSearchCondition<T>) sc;
                    if (SyncopeFiqlParser.IEQ.equals(sfsc.getOperator())) {
                        ct = ConditionType.EQUALS;
                    } else if (SyncopeFiqlParser.NIEQ.equals(sfsc.getOperator())) {
                        ct = ConditionType.NOT_EQUALS;
                    }
                    effectiveName = "LOWER(" + name + ")";
                    effectiveValue = "LOWER(" + value + ")";
                }

                sb.append(effectiveName).append(" ").append(
                        conditionTypeToSqlOperator(ct, value, originalValue))
                        .append(" ").append(effectiveValue);
            }
        } else {
            boolean first = true;
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                if (!first) {
                    sb.append(" ").append(sc.getConditionType().toString()).append(" ");
                } else {
                    first = false;
                }
                sb.append("(");
                saveStringBuilder(sb);
                condition.accept(this);
                sb = getStringBuilder();
                sb.append(")");
            }
        }

        saveStringBuilder(sb);
    }

    @Override
    protected StringBuilder getStringBuilder() {
        StringBuilder sb = super.getStringBuilder();
        if (sb == null) {
            sb = new StringBuilder();
            if (table != null) {
                SearchUtils.startSqlQuery(sb, table, tableAlias, columns);
            }
        }
        return sb;
    }

    private static String conditionTypeToSqlOperator(ConditionType ct, String value, String originalValue) {
        final boolean wildcardAvailable = SearchUtils.containsWildcard(originalValue);
        String op;
        switch (ct) {
            case EQUALS:
                op = wildcardAvailable ? "LIKE" : "=";
                break;
            case NOT_EQUALS:
                op = wildcardAvailable ? "NOT LIKE" : "<>";
                break;
            case GREATER_THAN:
                op = ">";
                break;
            case GREATER_OR_EQUALS:
                op = ">=";
                break;
            case LESS_THAN:
                op = "<";
                break;
            case LESS_OR_EQUALS:
                op = "<=";
                break;
            default:
                String msg = String.format("Condition type %s is not supported", ct.name());
                throw new RuntimeException(msg);
        }
        return op;
    }
}
