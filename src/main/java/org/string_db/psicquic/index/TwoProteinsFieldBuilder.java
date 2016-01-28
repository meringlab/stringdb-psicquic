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

import org.apache.log4j.Logger;
import org.string_db.ProteinExternalId;
import uk.ac.ebi.intact.irefindex.seguid.RogidGenerator;
import uk.ac.ebi.intact.irefindex.seguid.SeguidException;

import java.util.Map;
import java.util.Set;

/**
 * Base class for all {@link FieldBuilder}s
 * that work with two interactors, for example unique IDs, aliases, etc.
 * <p/>
 * An instance can be reused but setting new pairs of
 * {@link TwoProteinsFieldBuilder#proteins(Integer, Integer)})}.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
abstract class TwoProteinsFieldBuilder extends FieldBuilder {
    private Integer firstProteinId;
    private Integer secondProteinId;

    protected TwoProteinsFieldBuilder() {
    }

    protected TwoProteinsFieldBuilder(FieldBuilder next) {
        super(next);
    }

    @Override
    FieldBuilder proteins(Integer firstId, Integer secondId) {
        //TODO assertNotNull
        this.firstProteinId = firstId;
        this.secondProteinId = secondId;
        next.proteins(firstId, secondId);
        return this;//this != next.proteins() !!
    }

    /**
     * Append {@link org.hupo.psi.calimocho.model.Field}(s) to the given {@code rowBuilder}.
     * The method is idempotent, calling it more than once gives the same result.
     *
     * @param rowBuilder
     * @return the given {@core rowBuilder}
     */
    @Override
    protected void append(RowBuilder rowBuilder) {
        if (firstProteinId != null && secondProteinId != null) {
            appendInteractors(rowBuilder);
            //reset ids so that the next to() call doesn't include them again
            firstProteinId = null;
            secondProteinId = null;
        }
    }

    protected abstract void appendInteractors(RowBuilder rowBuilder);

    protected Integer getFirstProteinId() {
        return firstProteinId;
    }

    protected Integer getSecondProteinId() {
        return secondProteinId;
    }
}


/**
 * Adds <a href='http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1014'>string</a> external ids
 * and optionally <a href='http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0486'>UniProtKB</a> ids as
 * {@link org.hupo.psi.calimocho.tab.util.Mitab25ColumnKeys#KEY_ID_A}
 * and {@link org.hupo.psi.calimocho.tab.util.Mitab25ColumnKeys#KEY_ID_B}.
 * <p/>
 * UniProt ids are not unique in STRINGDB so this violates the format in a way,
 * but it allows better integration with other PSICQUIC providers.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 * @see <a href='http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI:0444&termName=database%20citation'></a>
 */
class IdsFieldBuilder extends TwoProteinsFieldBuilder {
    protected final Map<Integer, ProteinExternalId> proteins;
    protected final Map<Integer, String> uniprotids;

    IdsFieldBuilder(Map<Integer, ProteinExternalId> proteins, Map<Integer, String> uniprotids, FieldBuilder next) {
        super(next);
        this.proteins = proteins;
        this.uniprotids = uniprotids;
    }

    IdsFieldBuilder(Map<Integer, ProteinExternalId> proteins, Map<Integer, String> uniprotids) {
        this.proteins = proteins;
        this.uniprotids = uniprotids;
    }

    @Override
    protected void appendInteractors(RowBuilder rowBuilder) {
        checkIfExternalIdsExist();
        rowBuilder.withIdA(proteins.get(getFirstProteinId()));
        rowBuilder.withIdB(proteins.get(getSecondProteinId()));
        if (uniprotids.containsKey(getFirstProteinId())) {
            rowBuilder.withIdA(uniprotids.get(getFirstProteinId()));
        }
        if (uniprotids.containsKey(getSecondProteinId())) {
            rowBuilder.withIdB(uniprotids.get(getSecondProteinId()));
        }
    }

    private void checkIfExternalIdsExist() {
        if (!proteins.containsKey(getFirstProteinId())) {
            throw new IllegalArgumentException("no external id found for " + getFirstProteinId());
        }
        if (!proteins.containsKey(getSecondProteinId())) {
            throw new IllegalArgumentException("no external id  found for " + getSecondProteinId());
        }
    }
}


/**
 * Add <a href='http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1014'>string</a> protein names
 * as {@link org.hupo.psi.calimocho.tab.util.Mitab25ColumnKeys#KEY_ALIAS_A}
 * and {@link org.hupo.psi.calimocho.tab.util.Mitab25ColumnKeys#KEY_ALIAS_B}.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class AliasFieldBuilder extends TwoProteinsFieldBuilder {
    private final Map<Integer, String> proteinNames;

    AliasFieldBuilder(Map<Integer, String> proteinNames) {
        this.proteinNames = proteinNames;
    }

    AliasFieldBuilder(Map<Integer, String> proteinNames, FieldBuilder next) {
        super(next);
        this.proteinNames = proteinNames;
    }

    @Override
    protected void appendInteractors(RowBuilder rowBuilder) {
        checkIfNamesExist();
        rowBuilder.withAliasA("string", proteinNames.get(getFirstProteinId()));
        rowBuilder.withAliasB("string", proteinNames.get(getSecondProteinId()));
    }

    private void checkIfNamesExist() {
        if (!proteinNames.containsKey(getFirstProteinId())) {
            throw new IllegalArgumentException("no name found for " + getFirstProteinId());
        }
        if (!proteinNames.containsKey(getSecondProteinId())) {
            throw new IllegalArgumentException("no name found for " + getFirstProteinId());
        }
    }
}


/**
 * Add <a href='http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0481'>refseq</a> ids
 * as alternative ids.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 *         TODO add Ensembl ids
 */
class RefseqAlternativeIdsFieldBuilder extends TwoProteinsFieldBuilder {
    private final Map<Integer, Set<String>> stringdbRefseqIds;

    public RefseqAlternativeIdsFieldBuilder(Map<Integer, Set<String>> stringdbRefseqIds) {
        this.stringdbRefseqIds = stringdbRefseqIds;
    }

    public RefseqAlternativeIdsFieldBuilder(Map<Integer, Set<String>> stringdbRefseqIds, FieldBuilder next) {
        super(next);
        this.stringdbRefseqIds = stringdbRefseqIds;
    }

    @Override
    protected void appendInteractors(RowBuilder rowBuilder) {
        if (stringdbRefseqIds.containsKey(getFirstProteinId())) {
            for (String altId : stringdbRefseqIds.get(getFirstProteinId())) {
                rowBuilder.withAltIdA("refseq", altId);
            }
        }
        if (stringdbRefseqIds.containsKey(getSecondProteinId())) {
            for (String altId : stringdbRefseqIds.get(getSecondProteinId())) {
                rowBuilder.withAltIdB("refseq", altId);
            }
        }
    }
}


/**
 * Calculate interactors ROGIDs based on their sequences and tax id as
 * {@link org.hupo.psi.calimocho.tab.util.Mitab26ColumnKeys#KEY_CHECKSUM_A} and
 * {@link org.hupo.psi.calimocho.tab.util.Mitab26ColumnKeys#KEY_CHECKSUM_B}
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 * @see <a href='https://docs.google.com/document/pub?id=11HpddNs-Bt5a4KOPGCXWivJ4MROYFbY2nhpk4PkvbTA'>Data Distribution Best Practices</a>
 */
class RogidFieldBuilder extends TwoProteinsFieldBuilder {

    private static final Logger log = Logger.getLogger(RogidFieldBuilder.class);

    private final RogidGenerator rogidGenerator = new RogidGenerator();

    private final String taxid;
    private final Map<Integer, String> proteinSequences;

    RogidFieldBuilder(Integer speciesId, Map<Integer, String> proteinSequences) {
        this.taxid = speciesId.toString();
        this.proteinSequences = proteinSequences;
    }

    @Override
    protected void appendInteractors(RowBuilder rowBuilder) {
        try {
            final String rogidA = rogidGenerator.calculateRogid(proteinSequences.get(getFirstProteinId()), taxid);
            final String rogidB = rogidGenerator.calculateRogid(proteinSequences.get(getSecondProteinId()), taxid);
            rowBuilder.withChecksumA(rogidA);
            rowBuilder.withChecksumB(rogidB);
        } catch (SeguidException e) {
            log.error("error calculating checksum for " + getFirstProteinId() + " - " + getSecondProteinId(), e);
        }
    }
}
