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

import org.hupo.psi.calimocho.key.CalimochoKeys;
import org.hupo.psi.calimocho.model.DefaultField;
import org.hupo.psi.calimocho.model.DefaultRow;
import org.hupo.psi.calimocho.model.Field;
import org.hupo.psi.calimocho.model.Row;
import org.hupo.psi.calimocho.tab.util.Mitab25ColumnKeys;
import org.hupo.psi.calimocho.tab.util.Mitab26ColumnKeys;
import org.string_db.ProteinExternalId;

/**
 * A {@link Row} builder
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 * @see <a href='https://code.google.com/p/psicquic/wiki/MITAB27Format'>MITAB27 Format</a>
 */
public class RowBuilder {

    protected Row row = new DefaultRow();

    /**
     * Returns a created {@link Row}, containing the elements provided to this builder.
     *
     * @return
     */
    public Row build() {
        return row;
    }

    /**
     * Add unique identifier for interactor A
     *
     * @param externalId unique id
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withIdA(ProteinExternalId externalId) {
        return withIdA("string", externalId.toString());
    }

    /**
     * Add unique identifier for interactor A
     *
     * @param uniprotkbId
     * @return
     */
    public RowBuilder withIdA(String uniprotkbId) {
        return withIdA("uniprotkb", uniprotkbId);
    }

    /**
     * Add unique identifier for interactor A
     *
     * @param database database citation, as defined in MI:0444
     * @param idA      unique id
     * @return this {@code MitabRowBuilder} instance
     */
    private RowBuilder withIdA(String database, String idA) {
        row.addField(Mitab25ColumnKeys.KEY_ID_A, defaultField(database, idA.toString()));
        return this;
    }

    /**
     * Add unique identifier for interactor A
     *
     * @param externalId unique id
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withIdB(ProteinExternalId externalId) {
        return withIdB("string", externalId.toString());
    }

    /**
     * Add unique identifier for interactor A
     *
     * @param uniprotkbId
     * @return
     */
    public RowBuilder withIdB(String uniprotkbId) {
        return withIdB("uniprotkb", uniprotkbId);
    }

    /**
     * Add unique identifier for interactor B
     *
     * @param database database citation, as defined in MI:0444
     * @param idB      unique id
     * @return this {@code MitabRowBuilder} instance
     */
    private RowBuilder withIdB(String database, String idB) {
        row.addField(Mitab25ColumnKeys.KEY_ID_B, defaultField(database, idB));
        return this;
    }

    /**
     * Add alternative identifier for interactor A
     *
     * @param database  database citation, as defined in MI:0444
     * @param keyAltIdA alternative id
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withAltIdA(String database, String keyAltIdA) {
        row.addField(Mitab25ColumnKeys.KEY_ALTID_A, defaultField(database, keyAltIdA));
        return this;
    }

    /**
     * Add alternative identifier for interactor B
     *
     * @param database  database citation, as defined in MI:0444
     * @param keyAltIdB alternative id
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withAltIdB(String database, String keyAltIdB) {
        row.addField(Mitab25ColumnKeys.KEY_ALTID_B, defaultField(database, keyAltIdB));
        return this;
    }

    /**
     * @param database database citation, as defined in MI:0444
     * @param aliasA
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withAliasA(String database, String aliasA) {
        row.addField(Mitab25ColumnKeys.KEY_ALIAS_A, defaultField(database, aliasA));
        return this;
    }

    /**
     * @param database database citation, as defined in MI:0444
     * @param aliasB
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withAliasB(String database, String aliasB) {
        row.addField(Mitab25ColumnKeys.KEY_ALIAS_B, defaultField(database, aliasB));
        return this;
    }

    /**
     * Add interaction detection method
     *
     * @param term
     * @param text
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withDetectionMethod(String term, String text) {
        row.addField(Mitab25ColumnKeys.KEY_DETMETHOD, defaultField("psi-mi", term, text));
        return this;
    }

    /**
     * Add NCBI Taxonomy identifier for both interactors, and species name as text (in parenthesis)
     *
     * @param species
     * @param name
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withTaxId(Integer species, String name) {
        row.addField(Mitab25ColumnKeys.KEY_TAXID_A, defaultField("taxid", species.toString(), name));
        row.addField(Mitab25ColumnKeys.KEY_TAXID_B, defaultField("taxid", species.toString(), name));
        return this;
    }

    /**
     * Add interaction type, taken from <a href='http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI:0190&termName=interaction%20type'>
     * MI:0190 term</a>
     *
     * @param term
     * @param text
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withInteractionType(String term, String text) {
        row.addField(Mitab25ColumnKeys.KEY_INTERACTION_TYPE, defaultField(term, text));
        return this;
    }

    /**
     * Add source database, taken from <a href='http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI:0444&termName=database%20citation'>database citation</a>
     *
     * @param term
     * @param text
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withSourceDatabase(String term, String text) {
        row.addField(Mitab25ColumnKeys.KEY_SOURCE, defaultField("psi-mi", term, text));
        return this;
    }

    /**
     * Add interaction id from the corresponding source database
     *
     * @param term
     * @param text
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withInteractionId(String term, String text) {
        row.addField(Mitab25ColumnKeys.KEY_INTERACTION_ID, defaultField(term, text));
        return this;
    }

    /**
     * Add score
     *
     * @param score
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withConfidence(Integer score) {
        row.addField(Mitab25ColumnKeys.KEY_CONFIDENCE, defaultField("score", score == null ? "-" : score.toString()));
        return this;
    }

    //2.6 format extension:

    /**
     * Add type for interactor A taken from <a href='http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI%3A0313&termName=interactor%20type'>Mi:0313</a>
     *
     * @param type
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withInteractorTypeA(String type) {
        row.addField(Mitab26ColumnKeys.KEY_INTERACTOR_TYPE_A, defaultField("psi-mi", type));
        return this;
    }

    /**
     * * Add type for interactor B taken from <a href='http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI%3A0313&termName=interactor%20type'>Mi:0313</a>
     *
     * @param type
     * @return this {@code MitabRowBuilder} instance
     */
    public RowBuilder withInteractorTypeB(String type) {
        row.addField(Mitab26ColumnKeys.KEY_INTERACTOR_TYPE_B, defaultField("psi-mi", type));
        return this;
    }

    /**
     * Add ROGID checksum for interactor A
     *
     * @param checksum
     * @return this {@code MitabRowBuilder} instance
     * @see <a href='http://www.ncbi.nlm.nih.gov/pubmed/18823568'>iRefIndex</a>
     */
    public RowBuilder withChecksumA(String checksum) {
        row.addField(Mitab26ColumnKeys.KEY_CHECKSUM_A, defaultField("rogid", checksum));
        return this;
    }

    /**
     * * Add ROGID checksum for interactor A
     *
     * @param checksum
     * @return this {@code MitabRowBuilder} instance
     * @see <a href='http://www.ncbi.nlm.nih.gov/pubmed/18823568'>iRefIndex</a>
     */
    public RowBuilder withChecksumB(String checksum) {
        row.addField(Mitab26ColumnKeys.KEY_CHECKSUM_B, defaultField("rogid", checksum));
        return this;
    }

    /**
     * Helper method, creates a default with with key/value/text.
     *
     * @param key
     * @param value
     * @param text
     * @return
     */
    private Field defaultField(String key, String value, String text) {
        final Field field = defaultField(key, value);
        field.set(CalimochoKeys.TEXT, text);
        return field;
    }

    /**
     * Helper method, creates a default with with key/value.
     * <p/>
     * {@link org.hupo.psi.calimocho.model.FieldBuilder} is not very useful,
     * doesn't allow setting the {@link CalimochoKeys#TEXT}, and throws a checked exception.
     *
     * @param key
     * @param value
     * @return
     */
    private Field defaultField(String key, String value) {
        DefaultField field = new DefaultField();
        field.set(CalimochoKeys.KEY, key);
        field.set(CalimochoKeys.DB, key);
        field.set(CalimochoKeys.VALUE, value);
        return field;
    }
}