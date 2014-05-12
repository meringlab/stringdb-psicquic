package org.string_db.psicquic.index;

import ch.uzh.molbio.common.db.PostgresConnector;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class MitabRecordBuilderTest {

    @Test
    public void test_makeMitabRecords() throws Exception {
        PostgresConnector db = MitabIndexer.openPostgresConnector();
        try {
            Map<Integer, Pair<String, String>> proteins = new HashMap();
            proteins.put(975673, new Pair<String, String>("9606.ENSP00000000233", "ARF5"));
            proteins.put(975854, new Pair<String, String>("9606.ENSP00000254584", "ARFIP2"));

            Map<Integer, Pair<Integer, String>> species = new HashMap();
            species.put(9606, new Pair<Integer, String>(9606, "Homo sapiens"));
            Map<Integer, Set<String>> ps = new HashMap();
            Set<String> p1set = new HashSet();
            p1set.add("PDB_2b6h");
            p1set.add("biogrid:193555");
            p1set.add("hprd:35158");
            p1set.add("BCID:11510");
            ps.put(975673, p1set);

            Set<String> p2set = new HashSet();
            p2set.add("NCIN2378");
            p2set.add("PDB_1i4d");
            p2set.add("biogrid:154256");
            p2set.add("biogrid:193555");
            p2set.add("intact:EBI-1031247");
            p2set.add("irefindex:AqUwGAnKlayVm2H+Q2PHMjbQX4A(rigid)");
            p2set.add("mint:MINT-66731");
            p2set.add("BCID:11510");
            ps.put(975854, p2set);

            Map<String, String> sc = new HashMap();
            sc.put("PDB_2b6h", "pdb");
            sc.put("biogrid:172154", "grid");
            sc.put("hprd:35158", "hrpd");

            sc.put("NCIN2378", "PID");
            sc.put("PDB_1i4d", "pdb");
            sc.put("biogrid:154256", "grid");
            sc.put("biogrid:193555", "grid");
            sc.put("intact:EBI-1031247", "intact");
            sc.put("irefindex:AqUwGAnKlayVm2H+Q2PHMjbQX4A(rigid)", "intact");
            sc.put("mint:MINT-66731", "mint");
            sc.put("BCID:11510", "bcid");
            Map<Integer, String> uniprotids = new HashMap();
            uniprotids.put(975673, "P84085");
            uniprotids.put(975854, "Q15027");


            Map<Integer, List<String>> refseqIds = new HashMap();
            refseqIds.put(975673, Arrays.asList(new String[]{"NM_001662", "NP_001653.1"}));
            refseqIds.put(975854, Arrays.asList(new String[]{"XM_290852"}));

            MitabRecordBuilder cut = new MitabRecordBuilder(db, proteins, species, ps, sc, uniprotids, refseqIds);
            Collection<String> r = cut.makeMitabRecords(975673, 975854, 9606, 982, 0, 0, 0, 0, 0, 0, 771, 0, 0, 0, 928, 0);
            assertEquals(1, r.size());
            assertEquals("string:9606.ENSP00000000233|uniprotkb:P84085\tstring:9606.ENSP00000254584|uniprotkb:Q15027\trefseq:NM_001662|refseq:NP_001653.1\trefseq:XM_290852\tstring:\"ARF5\"\tstring:\"ARFIP2\"\tpsi-mi:\"MI:0045\"(experimental interaction detection)\t-\tpubmed:11478794\ttaxid:9606(Homo sapiens)\ttaxid:9606(Homo sapiens)\t-\tpsi-mi:\"MI:0463\"(grid)\tbiogrid:193555\tscore:771", r.iterator().next().trim());

        } finally {
            db.shutdown();
        }
    }

}
