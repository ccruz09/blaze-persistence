/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.impl.function.datetime.epochmicro;

import com.blazebit.persistence.spi.FunctionRenderContext;
import com.blazebit.persistence.spi.TemplateRenderer;

/**
 * @author Christian Beikov
 * @since 1.4.0
 */
public class OracleEpochMicrosecondFunction extends EpochMicrosecondFunction {

    private final TemplateRenderer paramRenderer;

    public OracleEpochMicrosecondFunction() {
        super("to_number(to_char(cast(?1 as timestamp),'FF6')) " +
                "+ " + (1000000L) + " * trunc(extract(second from (cast(?1 as timestamp) - to_date('1970-01-01', 'yyyy-mm-dd')))) " +
                "+ " + (1000000L * 60L) + " * extract(minute from (cast(?1 as timestamp) - to_date('1970-01-01', 'yyyy-mm-dd'))) " +
                "+ " + (1000000L * 60L * 60L) + " * extract(hour from (cast(?1 as timestamp) - to_date('1970-01-01', 'yyyy-mm-dd'))) " +
                "+ " + (1000000L * 24L * 60L * 60L) + " * extract(day from (cast(?1 as timestamp) - to_date('1970-01-01', 'yyyy-mm-dd')))");
        this.paramRenderer = new TemplateRenderer("(select to_number(to_char(t1,'FF6')) " +
                "+ " + (1000000L) + " * trunc(1000 * (extract(second from (t1 - to_date('1970-01-01', 'yyyy-mm-dd'))))) " +
                "+ " + (1000000L * 60) + " * extract(minute from (t1 - to_date('1970-01-01', 'yyyy-mm-dd'))) " +
                "+ " + (1000000L * 60 * 60) + " * extract(hour from (t1 - to_date('1970-01-01', 'yyyy-mm-dd'))) " +
                "+ " + (1000000L * 24 * 60 * 60) + " * extract(day from (t1 - to_date('1970-01-01', 'yyyy-mm-dd'))) " +
                "from (select cast(?1 as timestamp) as t1 from dual))");
    }

    @Override
    protected void renderDiff(FunctionRenderContext context) {
        boolean firstContains = context.getArgument(0).contains("?");
        if (firstContains && context.getArgument(0).length() != 1) {
            throw new IllegalArgumentException("Only simple parameters or expressions are allowed because of a needed reordering in SQL which is otherwise not possible! Expressions ['" + context.getArgument(0) + "', '" + context.getArgument(1) + "'] do not comply!");
        }
        if (firstContains) {
            paramRenderer.start(context).addArgument(0).build();
        } else {
            // Reuse the expression multiple times
            renderer.start(context).addArgument(0).build();
        }
    }
}

