/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.testsuite.update.correlated.updatableonly;

import com.blazebit.persistence.testsuite.base.jpa.assertion.AssertStatementBuilder;
import com.blazebit.persistence.testsuite.base.jpa.category.NoDatanucleus;
import com.blazebit.persistence.testsuite.base.jpa.category.NoEclipselink;
import com.blazebit.persistence.testsuite.entity.Document;
import com.blazebit.persistence.view.FlushMode;
import com.blazebit.persistence.view.FlushStrategy;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import com.blazebit.persistence.view.testsuite.update.AbstractEntityViewUpdateDocumentTest;
import com.blazebit.persistence.view.testsuite.update.correlated.updatableonly.model.UpdatablePersonView;
import com.blazebit.persistence.view.testsuite.update.correlated.updatableonly.model.UpdatableDocumentView;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Christian Beikov
 * @since 1.3.0
 */
@RunWith(Parameterized.class)
// NOTE: No Datanucleus support yet
@Category({ NoDatanucleus.class, NoEclipselink.class})
public class EntityViewUpdateCorrelatedUpdatableOnlySubviewTest extends AbstractEntityViewUpdateDocumentTest<UpdatableDocumentView> {

    public EntityViewUpdateCorrelatedUpdatableOnlySubviewTest(FlushMode mode, FlushStrategy strategy, boolean version) {
        super(mode, strategy, version, UpdatableDocumentView.class);
    }

    @Parameterized.Parameters(name = "{0} - {1} - VERSIONED={2}")
    public static Object[][] combinations() {
        return MODE_STRATEGY_VERSION_COMBINATIONS;
    }

    @Override
    protected void registerViewTypes(EntityViewConfiguration cfg) {
        cfg.addEntityView(UpdatablePersonView.class);
    }

    @Test
    public void testSimpleUpdate() {
        // Given
        final UpdatableDocumentView docView = getDoc1View();
        clearQueries();
        
        // When
        docView.setName("newDoc");
        update(docView);

        // Then
        AssertStatementBuilder builder = assertUnorderedQuerySequence();

        if (!isQueryStrategy()) {
            fullFetch(builder);
        }

        builder.update(Document.class)
                .validate();

        assertNoUpdateAndReload(docView);
        assertEquals("newDoc", docView.getName());
        assertEquals(doc1.getName(), docView.getName());
    }

    @Test
    public void testUpdateWithSubview() {
        // Given
        final UpdatableDocumentView docView = getDoc1View();
        UpdatablePersonView newPerson = getP2View(UpdatablePersonView.class);
        clearQueries();

        // When
        try {
            docView.setResponsiblePerson(newPerson);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Setting instances of type"));
        }
    }

    @Test
    public void testUpdateWithModifySubview() {
        // Given
        final UpdatableDocumentView docView = getDoc1View();
        UpdatablePersonView newPerson = getP2View(UpdatablePersonView.class);
        clearQueries();

        // When
        newPerson.setName("newOwner");
        try {
            docView.setResponsiblePerson(newPerson);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Setting instances of type"));
        }
    }

    @Test
    public void testUpdateWithModifyExisting() {
        // Given
        final UpdatableDocumentView docView = getDoc1View();
        clearQueries();

        // When
        docView.getResponsiblePerson().setName("newOwner");
        update(docView);

        // Then
        AssertStatementBuilder builder = assertUnorderedQuerySequence();

        if (isQueryStrategy()) {
            if (isFullMode()) {
                fullUpdate(builder);
            }
        } else {
            if (isFullMode()) {
                fullFetch(builder);
            }
        }
        builder.validate();

        assertNoUpdateAndReload(docView);
        Assert.assertEquals("pers1", p1.getName());
    }

    @Test
    public void testUpdateToNull() {
        // Given
        final UpdatableDocumentView docView = getDoc1View();
        clearQueries();

        // When
        docView.setResponsiblePerson(null);
        update(docView);

        // Then
        AssertStatementBuilder builder = assertUnorderedQuerySequence();

        if (isFullMode()) {
            if (isQueryStrategy()) {
                builder.update(Document.class);
            } else {
                fullFetch(builder);
                if (version) {
                    builder.update(Document.class);
                }
            }
        }
        builder.validate();

        assertNoUpdateAndReload(docView);
        Assert.assertNull(docView.getResponsiblePerson());
    }

    @Override
    protected AssertStatementBuilder fullFetch(AssertStatementBuilder builder) {
        return builder.assertSelect()
                .fetching(Document.class)
                .and();
    }

    @Override
    protected AssertStatementBuilder fullUpdate(AssertStatementBuilder builder) {
        return builder.update(Document.class);
    }

    @Override
    protected AssertStatementBuilder versionUpdate(AssertStatementBuilder builder) {
        return builder.update(Document.class);
    }
}
