/**
 * Copyright 2014 The European Bioinformatics Institute, University of Zürich, SIB, and others.
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
import org.apache.solr.client.solrj.SolrServerException;
import org.hupo.psi.calimocho.model.Row;
import org.hupo.psi.mi.psicquic.indexing.batch.reader.MitabCalimochoLineMapper;
import org.string_db.DbFacade;
import org.string_db.StringDbScores;
import org.string_db.UniprotAC;
import org.string_db.psicquic.AppProperties;
import org.string_db.psicquic.SearchServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates the PSICQUIC index based on the data from string-db: postgresql database + uniprot ids mapping list.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class StringdbSolrIndexer {

    private static final Logger log = Logger.getLogger(StringdbSolrIndexer.class);
    private static final DbFacade db = new DbFacade(AppProperties.instance.getProteinRepository(),
            AppProperties.instance.getSpeciesRepository(),
            AppProperties.instance.getGenericQueryProcessor());
    protected final Map<Integer, String> uniprotIds;
    protected final SearchServer searchServer;

    public StringdbSolrIndexer(SearchServer searchServer, Map<Integer, UniprotAC> uniprotAcs) throws Exception {
        this.uniprotIds = new HashMap<>();
        for (Map.Entry<Integer, UniprotAC> e : uniprotAcs.entrySet()) {
            this.uniprotIds.put(e.getKey(), e.getValue().toString());
        }
        this.searchServer = searchServer;
        //        this shouldn't run if assertions are disabled: -disableassertions
        assert (indexDummyInteraction());
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        log.info("indexing to: " + AppProperties.instance.solrUrl);
        List<Integer> species = db.loadCoreSpecies();
        final SolrServerConnection solrServerConnection = new SolrServerConnection(AppProperties.instance.solrUrl);

        final Map<Integer, Set<String>> linkouts = db.loadUniProtLinkouts();
        final Map<Integer, UniprotAC> uniprotIds = new HashMap((int) (linkouts.size() * 1.2));
        for (Integer id : linkouts.keySet()) {
            uniprotIds.put(id, new UniprotAC(linkouts.get(id).iterator().next()));
        }

        final StringdbSolrIndexer indexer = new StringdbSolrIndexer(solrServerConnection, uniprotIds);
        indexer.indexSpecies(species);
        log.info("indexing done in: " + ((System.currentTimeMillis() - start) / (1000 * 60)) + "min");
    }

    void indexSpecies(List<Integer> speciesIds) throws Exception {
        long start = System.currentTimeMillis();

        for (Integer spcId : speciesIds) {
            int interactionCount = 1;
            log.info("indexing " + spcId + " (" + speciesIds.indexOf(spcId) + ". out of " + speciesIds.size() +
                    " in " + ((System.currentTimeMillis() - start) / (1000 * 60)) + "min)");
            long spc = System.currentTimeMillis();

            StringdbRowBuilder stringdbRowBuilder = StringdbRowBuilder.builder(db).build(spcId, uniprotIds);
            StringDbScoresDataReader scoresReader = new StringDbScoresDataReader(db, AppProperties.instance.getJdbcTemplate(), spcId);
            log.info("scores reader created, sending to solr...");
            while (scoresReader.next()) {
                final StringDbScores stringDbScores = scoresReader.get();
                for (Row row : stringdbRowBuilder.build(stringDbScores)) {
                    searchServer.add(row);
                    if (interactionCount++ % 20000 == 0) {
//                            commit(spcId);
                        log.trace(interactionCount + " docs indexed");
                    }

                }
            }
            commit(spcId);
            log.info(spcId + " total interactions: " + interactionCount + ", done in: " + ((System.currentTimeMillis() - spc) / (1000 * 60)) + "min");
            System.gc();
        }
    }

    private void commit(Integer spcId) throws SolrServerException, IOException {
        try {
            searchServer.commit(false);
        } catch (RuntimeException e) {
            log.error("error indexing '" + spcId +
                    "':\n\t: " + e.getMessage());

        }
    }

    /**
     * test run just to make sure solr accepts writes
     *
     * @return
     */
    private boolean indexDummyInteraction() {
        try {
            searchServer.deleteAll();
            searchServer.commit(true);
            assert (0 == searchServer.countIndexedDocuments());

            String interaction = "uniprotkb:P21333\tuniprotkb:P07228\tintact:EBI-350432\tintact:EBI-5606437\tuniprotkb:FLNA(gene name)|uniprotkb:FLN(gene name synonym)|uniprotkb:FLN1(gene name synonym)|uniprotkb:Alpha-filamin(gene name synonym)|uniprotkb:Filamin-1(gene name synonym)|uniprotkb:Endothelial actin-binding protein(gene name synonym)|uniprotkb:Actin-binding protein 280(gene name synonym)|uniprotkb:Non-muscle filamin(gene name synonym)\tuniprotkb:ITGB1(gene name)|uniprotkb:CSAT antigen(gene name synonym)|uniprotkb:JG22 antigen(gene name synonym)|uniprotkb:RGD-receptor(gene name synonym)\tpsi-mi:\"MI:0018\"(two hybrid)\tLoo DT et al.(1998)\tpubmed:9722563|imex:IM-17229\ttaxid:4932(yeasx)|taxid:4932(\"Saccharomyces cerevisiae (Baker's yeast)\")|taxid:9606(human)|taxid:9606(Homo sapiens)\ttaxid:9031(chick)|taxid:9031(\"Gallus gallus (Chicken)\")\tpsi-mi:\"MI:0915\"(physical association)\tpsi-mi:\"MI:1124\"(mbinfo)\tintact:EBI-5606332\t-\t-\tpsi-mi:\"MI:0499\"(unspecified role)\tpsi-mi:\"MI:0499\"(unspecified role)\tpsi-mi:\"MI:0498\"(prey)\tpsi-mi:\"MI:0496\"(bait)\tpsi-mi:\"MI:0326\"(protein)\tpsi-mi:\"MI:0326\"(protein)\trefseq:NP_001104026.1|refseq:NP_001447.2|interpro:IPR001589|interpro:IPR001715|interpro:IPR001298|interpro:IPR017868|interpro:IPR013783|interpro:IPR014756|go:\"GO:0015629\"|go:\"GO:0005938\"|go:\"GO:0005829\"|go:\"GO:0005576\"|go:\"GO:0005634\"|go:\"GO:0005886\"|go:\"GO:0051015\"|go:\"GO:0034988\"|go:\"GO:0001948\"|go:\"GO:0042803\"|go:\"GO:0048365\"|go:\"GO:0004871\"|go:\"GO:0008134\"|go:\"GO:0051764\"|go:\"GO:0031532\"|go:\"GO:0034329\"|go:\"GO:0051220\"|go:\"GO:0045184\"|go:\"GO:0007195\"|go:\"GO:0042177\"|go:\"GO:0043433\"|go:\"GO:0030168\"|go:\"GO:0002576\"|go:\"GO:0043123\"|go:\"GO:0042993\"|go:\"GO:0034394\"|go:\"GO:0050821\"|go:\"GO:0043113\"|ensembl:ENSG00000196924|ensembl:ENSG00000196924|reactome:REACT_604|ipi:IPI00302592|ipi:IPI00333541|rcsb pdb:2AAV|rcsb pdb:2BP3|rcsb pdb:2BRQ|rcsb pdb:2J3S|rcsb pdb:2JF1|rcsb pdb:2K3T|rcsb pdb:2K7P|rcsb pdb:2K7Q|rcsb pdb:2W0P|rcsb pdb:2WFN|rcsb pdb:3CNK|rcsb pdb:3HOC|rcsb pdb:3HOP|rcsb pdb:3HOR|rcsb pdb:3ISW|rcsb pdb:3RGH|go:\"GO:0031523\"|go:\"GO:0090307\"|uniprotkb:Q8NF52(secondary-ac)|intact:EBI-1103234(intact-secondary)|uniprotkb:Q5HY53(secondary-ac)|uniprotkb:Q5HY55(secondary-ac)|reactome:REACT_111155|go:\"GO:0017160\"\tgo:\"GO:0008305\"|go:\"GO:0042470\"|go:\"GO:0004872\"|go:\"GO:0007229\"|interpro:IPR013111|interpro:IPR015812|interpro:IPR014836|interpro:IPR002369|interpro:IPR012896|interpro:IPR003659|interpro:IPR016201|interpro:IPR002035|ipi:IPI00735136|go:\"GO:0007160\"|go:\"GO:0007275\"|go:\"GO:0005515\"\timex:IM-17229-1(imex-primary)\t-\t-\tcuration depth:imex curation|full coverage:Only protein-protein interactions|figure legend:Fig. 3,4,5,6,7 and 8.|isoform-comment:The human filamin consists of three isoforms. The protein included in this entry is Filamin A.\t-\t-\t2012/02/23\t2012/03/01\tcrc64:6C1A07041DF50142\tcrc64:2F6FEFCDF2C80457\tintact-crc:08C4486B755C70C0\ttrue\tnecessary binding region:2171-2647\tnecessary binding region:757-800|mutation decreasing interaction:764-764|mutation disrupting interaction:788-788|mutation disrupting interaction:788-788|mutation decreasing interaction:788-788|mutation decreasing interaction:797-797|mutation decreasing interaction:785-785|mutation decreasing interaction:786-786|mutation decreasing interaction:797-797\t-\t-\tpsi-mi:\"MI:0396\"(predetermined participant)\tpsi-mi:\"MI:0396\"(predetermined participant)";
            final Row row = new MitabCalimochoLineMapper().mapLine(interaction, 0);
            searchServer.add(row);
            searchServer.commit(true);
            final Long results = searchServer.countIndexedDocuments();
            assert (1 == results);
            searchServer.deleteAll();
            searchServer.commit(true);
            return true;
        } catch (Exception e) {
            log.error("failed to index a test document", e);
            throw new ExceptionInInitializerError(e);
        }
    }
}

class UniprotIdsLoader {
    private static final Logger log = Logger.getLogger(UniprotIdsLoader.class);

    public Map<Integer, UniprotAC> loadUniprotIds(String file) {
        Map<Integer, UniprotAC> map = new HashMap<Integer, UniprotAC>();
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    String[] records = line.trim().split("\\s");
                    map.put(Integer.valueOf(records[0]), new UniprotAC(records[1]));
                } catch (Exception e) {
                    log.warn("bad record: " + line);
                }

            }
            r.close();
        } catch (Exception e) {//Catch exception if any
            throw new RuntimeException(e);
        }
        return map;
    }

}

