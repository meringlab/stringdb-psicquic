/*
 * Copyright 2014 University of Zürich, SIB, and others.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.string_db.psicquic.index;

import java.util.Collection;

/**
 * The {@code FieldAppender} allows construction of a chain of
 * {@link org.hupo.psi.calimocho.model.Field} appenders, that (eventually)
 * populate {@link RowBuilder}.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 * @see <a href="http://code.google.com/p/psimi/wiki/PsimiTab27Format">MITAB27 format</a>
 */
public abstract class FieldBuilder {

    private static final FieldBuilder NO_OP = new NoOp();
    /**
     * Next appender in the chain.
     */
    protected FieldBuilder next;

    FieldBuilder() {
        // Init to a no-op appender so it's never null.
        next = NO_OP;
    }

    FieldBuilder(FieldBuilder next) {
        this.next = next;
    }

    /**
     * Append builder to the chain (at the end)
     *
     * @param last will be the last builder in the chain
     * @return this instance
     */
    FieldBuilder chain(FieldBuilder last) {
        if (NO_OP.equals(this.next)) {
            this.next = last;
        } else {
            //push this one down the chain
            this.next.chain(last);
        }
        return this;
    }

    final RowBuilder addTo(RowBuilder rowBuilder) {
        append(rowBuilder);
        //can't rely on NoOp coz this has to be final so no other class can override it
        //(is there a way in java to allow just a single class to override a method?)
        return next != null ? next.addTo(rowBuilder) : rowBuilder;
    }

    FieldBuilder proteins(Integer firstId, Integer secondId) {
        next.proteins(firstId, secondId);
        return this;
    }

    /**
     * Append {@link org.hupo.psi.calimocho.model.Field}(s) to the given {@code rowBuilder}
     *
     * @param rowBuilder
     */
    protected abstract void append(RowBuilder rowBuilder);

    private static final class NoOp extends FieldBuilder {
        private NoOp() {
            this.next = null;
        }

        @Override
        protected void append(RowBuilder rowBuilder) {
        }

        //implement as no-op so we don't have to check for null when calling {@code proteins())
        @Override
        FieldBuilder proteins(Integer firstId, Integer secondId) {
            return this;
        }

        @Override
        public String toString() {
            return "NoOp";
        }
    }
}


/**
 * Add NCBI Taxonomy identifier for both interactors, and species name as text.
 * <p/>
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class TaxonFieldBuilder extends FieldBuilder {

    protected final String speciesName;
    protected int speciesId;

    TaxonFieldBuilder(int speciesId, String speciesName, FieldBuilder next) {
        super(next);
        this.speciesName = speciesName;
        this.speciesId = speciesId;
    }

    TaxonFieldBuilder(int speciesId, String speciesName) {
        this.speciesId = speciesId;
        this.speciesName = speciesName;
    }

    @Override
    protected void append(RowBuilder rowBuilder) {
        rowBuilder.withTaxId(speciesId, speciesName);
    }
}

/**
 * Adds NCBI Taxonomy identifier for both interactors, and species name(s) as text.
 * A species can have more than one name, for example: 'Homo Sapiens' and 'human' for 9606.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class MultipleTaxonNamesFieldBuilder extends FieldBuilder {
    protected Collection<String> speciesNames;
    protected int speciesId;

    MultipleTaxonNamesFieldBuilder(int speciesId, Collection<String> speciesNames) {
        this.speciesId = speciesId;
        this.speciesNames = speciesNames;
    }

    @Override
    protected void append(RowBuilder rowBuilder) {
        for (String name : speciesNames) {
            rowBuilder.withTaxId(speciesId, name);
        }
    }
}


/**
 * STRINGDB's only got proteins so this class adds
 * <a href='http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0326'>MI:0326</a> as
 * {@link org.hupo.psi.calimocho.tab.util.Mitab26ColumnKeys#KEY_INTERACTOR_TYPE_A}
 * and {@link org.hupo.psi.calimocho.tab.util.Mitab26ColumnKeys#KEY_INTERACTOR_TYPE_A}
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 * @see <a href='http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI%3A0313&termName=interactor%20type'>iteractor type ontology term</a>
 * @see <a href='http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0326'>MI:0326</a>
 */
class InteractorTypeFieldBuilder extends FieldBuilder {
    InteractorTypeFieldBuilder() {
    }

    InteractorTypeFieldBuilder(FieldBuilder next) {
        super(next);
    }

    @Override
    protected void append(RowBuilder rowBuilder) {
        rowBuilder.withInteractorTypeA("MI:0326").withInteractorTypeB("MI:0326");
    }
}