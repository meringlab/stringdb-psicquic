package org.string_db.psicquic.index;

import ch.uzh.molbio.common.db.PostgresConnector;
import org.junit.Test;

import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class DatabaseManagerTest {

    final String scoresQuery = "SELECT d.node_id_a, d.evidence_scores"
            + " FROM  network.node_node_links d "
            + "  WHERE d.node_id_a = 3562319 and d.node_id_b = 3565461;";

    @Test
    public void testUnpackScores() throws Exception {
        PostgresConnector db = MitabIndexer.openPostgresConnector();
        try {
            ResultSet rs = db.getCursorBasedResultSet(scoresQuery, 10);
            assertTrue(rs.next());
            Integer[][] scores = (Integer[][]) rs.getArray(2).getArray();
            assertEquals(2, scores.length);

            assertEquals(2, scores[0][0]);
            assertEquals(566, scores[0][1]);

            assertEquals(7, scores[1][0]);
            assertEquals(190, scores[1][1]);

        } finally {
            db.shutdown();
        }

    }

}
