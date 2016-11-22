package net.tirasa.test.fiqlnewoperator;

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;

public class App {

    public static void main(final String[] args) {
        SyncopeFiqlParser<SearchBean> fiqlParser = new SyncopeFiqlParser<>(
                SearchBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES, null);

        SearchCondition<SearchBean> parsed = fiqlParser.parse("name=~ami*,level=gt=10");

        SQLPrinterVisitor<SearchBean> visitor = new SQLPrinterVisitor<>();
        visitor.visit(parsed);
        System.out.println("===> Visit result:\n" + visitor.getVisitorState().get());
    }

}
