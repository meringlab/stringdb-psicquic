package org.string_db.psicquic.index;

import org.hupo.psi.calimocho.model.Field;
import org.hupo.psi.calimocho.model.Row;
import org.hupo.psi.mi.psicquic.indexing.batch.reader.MitabCalimochoLineMapper;
import org.junit.Test;
import org.string_db.ProteinExternalId;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class RowBuilderTest {
    final MitabCalimochoLineMapper lineMapper = new MitabCalimochoLineMapper();
    final String interaction = "string:9606.ENSP00000000233|uniprotkb:P84085\tstring:9606.ENSP00000254584|uniprotkb:Q15027\trefseq:NM_001662|refseq:NP_001653.1\trefseq:XM_290852\tstring:\"ARF5\"\tstring:\"ARFIP2\"\tpsi-mi:\"MI:0045\"(experimental interaction detection)\t-\t-\ttaxid:9606(Homo sapiens)\ttaxid:9606(Homo sapiens)\t-\tpsi-mi:\"MI:0463\"(grid)\tbiogrid:193555\tscore:771";

    //ah Row&Field don't implement equals...
    static void assertRowsEquals(Row expected, Row actual) {
        assertEquals("rows have different fileds: " + expected.keySet() + " - " + actual.keySet(), expected.keySet(), actual.keySet());
        final Collection<String> actualColumns = actual.keySet();
        for (String column : expected.keySet()) {
            assertTrue(actualColumns.contains(column));
            final Collection<Field> expectedFields = expected.getFields(column);
            final Collection<Field> actualFields = actual.getFields(column);
            assertEquals(actualFields.toString(), expectedFields.size(), actualFields.size());
            final Iterator<Field> expectedIterator = expectedFields.iterator();
            final Iterator<Field> actualIterator = actualFields.iterator();
            while (expectedIterator.hasNext()) {
                final Field expectedField = expectedIterator.next();
                final Field actualField = actualIterator.next();
                assertEquals(column + " column different: ", expectedField.getEntries(), actualField.getEntries());
            }
        }
    }

    @Test
    public void testRowEquals() throws Exception {
        assertRowsEquals(lineMapper.mapLine(interaction, 1), lineMapper.mapLine(interaction, 2));
    }

    @Test
    public void build_mitab_interaction() throws Exception {
        assertRowsEquals(lineMapper.mapLine(interaction, 1),
                new RowBuilder()
                        .withIdA(new ProteinExternalId("9606.ENSP00000000233")).withIdA("P84085")
                        .withIdB(new ProteinExternalId("9606.ENSP00000254584")).withIdB("Q15027")
                        .withAltIdA("refseq", "NM_001662")
                        .withAltIdA("refseq", "NP_001653.1")
                        .withAltIdB("refseq", "XM_290852")
                        .withAliasA("string", "ARF5")
                        .withAliasB("string", "ARFIP2")
                        .withDetectionMethod("MI:0045", "experimental interaction detection")
                        .withTaxId(9606, "Homo sapiens")
                        .withSourceDatabase("MI:0463", "grid")
                        .withInteractionId("biogrid", "193555")
                        .withConfidence(771)
                        .build()
        );
    }

}
