/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.impl;

import com.blazebit.persistence.FinalSetOperationCriteriaBuilder;
import com.blazebit.persistence.LeafOngoingFinalSetOperationCriteriaBuilder;
import com.blazebit.persistence.LeafOngoingSetOperationCriteriaBuilder;
import com.blazebit.persistence.StartOngoingSetOperationCriteriaBuilder;
import com.blazebit.persistence.parser.expression.ExpressionCopyContext;
import com.blazebit.persistence.spi.SetOperationType;

import java.util.Map;

/**
 *
 * @param <T> The query result type
 * @author Christian Beikov
 * @since 1.1.0
 */
public class LeafOngoingSetOperationCriteriaBuilderImpl<T> extends AbstractCriteriaBuilder<T, LeafOngoingSetOperationCriteriaBuilder<T>, LeafOngoingSetOperationCriteriaBuilder<T>, StartOngoingSetOperationCriteriaBuilder<T, LeafOngoingFinalSetOperationCriteriaBuilder<T>>> implements LeafOngoingSetOperationCriteriaBuilder<T>, LeafOngoingFinalSetOperationCriteriaBuilder<T> {

    public LeafOngoingSetOperationCriteriaBuilderImpl(MainQuery mainQuery, QueryContext queryContext, boolean isMainQuery, Class<T> clazz, BuilderListener<Object> listener, FinalSetOperationCriteriaBuilderImpl<T> finalSetOperationBuilder) {
        super(mainQuery, queryContext, isMainQuery, clazz, null, listener, finalSetOperationBuilder);
    }

    public LeafOngoingSetOperationCriteriaBuilderImpl(AbstractCommonQueryBuilder<T, ?, ?, ?, ?> builder, MainQuery mainQuery, QueryContext queryContext, Map<JoinManager, JoinManager> joinManagerMapping, ExpressionCopyContext copyContext) {
        super(builder, mainQuery, queryContext, joinManagerMapping, copyContext);
    }

    @Override
    AbstractCommonQueryBuilder<T, LeafOngoingSetOperationCriteriaBuilder<T>, LeafOngoingSetOperationCriteriaBuilder<T>, StartOngoingSetOperationCriteriaBuilder<T, LeafOngoingFinalSetOperationCriteriaBuilder<T>>, BaseFinalSetOperationCriteriaBuilderImpl<T, ?>> copy(QueryContext queryContext, Map<JoinManager, JoinManager> joinManagerMapping, ExpressionCopyContext copyContext) {
        return new LeafOngoingSetOperationCriteriaBuilderImpl<>(this, queryContext.getParent().mainQuery, queryContext, joinManagerMapping, copyContext);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public FinalSetOperationCriteriaBuilder<T> endSet() {
        subListener.verifyBuilderEnded();
        this.setOperationEnded = true;
        // Only check the query if it's not empty
        if (isEmpty()) {
            if (finalSetOperationBuilder.setOperationManager.hasSetOperations()) {
                finalSetOperationBuilder.setOperationManager.removeOperand(this);
            }
        } else {
            prepareAndCheck(null);
        }
        listener.onBuilderEnded(this);
        // This final set operation builder is the exception that is already marked as ended here
        finalSetOperationBuilder.setOperationEnded = true;
        return (FinalSetOperationCriteriaBuilder<T>) (FinalSetOperationCriteriaBuilder) finalSetOperationBuilder;
    }

    @Override
    protected BaseFinalSetOperationCriteriaBuilderImpl<T, ?> createFinalSetOperationBuilder(SetOperationType operator, boolean nested) {
        return createFinalSetOperationBuilder(operator, nested, nested);
    }

    @Override
    protected LeafOngoingSetOperationCriteriaBuilder<T> createSetOperand(BaseFinalSetOperationCriteriaBuilderImpl<T, ?> finalSetOperationBuilder) {
        subListener.verifyBuilderEnded();
        listener.onBuilderEnded(this);
        return createLeaf(finalSetOperationBuilder);
    }

    @Override
    protected StartOngoingSetOperationCriteriaBuilder<T, LeafOngoingFinalSetOperationCriteriaBuilder<T>> createSubquerySetOperand(BaseFinalSetOperationCriteriaBuilderImpl<T, ?> finalSetOperationBuilder, BaseFinalSetOperationCriteriaBuilderImpl<T, ?> resultFinalSetOperationBuilder) {
        subListener.verifyBuilderEnded();
        listener.onBuilderEnded(this);
        LeafOngoingFinalSetOperationCriteriaBuilder<T> leafCb = createLeaf(resultFinalSetOperationBuilder);
        return createStartOngoing(finalSetOperationBuilder, leafCb);
    }

}
