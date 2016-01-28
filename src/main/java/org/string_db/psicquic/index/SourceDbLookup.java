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

import java.util.*;

/**
 * For a pair of proteins, locate all shared {@code evidence.sets},
 * and map corresponding {@code collection_id} to the appropriate
 * <a href='http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI:0444&termName=database%20citation'>database citation term</a>.
 * <p/>
 * <p/>
 * The mapping of {@code evidence.collections} to the MI:0444 term is:
 * <pre>
 * bind:     http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0462
 * biocarta: http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1108
 * biocyc:   http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1105
 * dip:      http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0465
 * pdb:      http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0460
 * grid:     http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0463
 * hprd:     http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0468
 * intact:   http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0469
 * kegg:     http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0470
 * mint:     http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0471
 * GO:       http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0448
 * PID       http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1107
 * reactome: http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0467
 * </pre>
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class SourceDbLookup {
    final static Map<String, Pair<String, String>> collections = new HashMap<String, Pair<String, String>>();
    private static final Logger log = Logger.getLogger(SourceDbLookup.class);

    static {
        collections.put("bind", new Pair("MI:0462", "bind"));
        collections.put("biocarta", new Pair("MI:1108", "biocarta"));
        collections.put("biocyc", new Pair("MI:1105", "biocyc"));
        collections.put("brenda", new Pair("MI:0846", "brenda"));
        collections.put("cell_ontology", new Pair("MI:0831", "cell_ontology"));
        collections.put("chebi", new Pair("MI:0474", "chebi"));
        collections.put("ChEBI", new Pair("MI:0474", "chebi"));
        collections.put("chembl_compound", new Pair("MI:0967", "chembl_compound"));
        collections.put("dip", new Pair("MI:0465", "dip"));
        collections.put("emdb", new Pair("MI:0936", "emdb"));
        collections.put("eMDB", new Pair("MI:0936", "emdb"));
        collections.put("flannotator", new Pair("MI:1043", "flannotator"));
        collections.put("gene_ontology", new Pair("MI:0448", "gene_ontology"));
        collections.put("grid", new Pair("MI:0463", "grid"));
        collections.put("GO_complexes", new Pair("MI:0448", "go_complexes"));
        collections.put("hprd", new Pair("MI:0468", "hprd"));
        collections.put("intact", new Pair("MI:0469", "intact"));
        collections.put("interpro", new Pair("MI:0449", "interpro"));
        collections.put("interPro", new Pair("MI:0449", "interpro"));
        collections.put("kegg_pathways", new Pair("MI:0470", "kegg_pathways"));
        collections.put("mint", new Pair("MI:0471", "mint"));
        collections.put("mpidb", new Pair("MI:0903", "mpidb"));
        collections.put("omim", new Pair("MI:0480", "omim"));
        collections.put("OMIM", new Pair("MI:0480", "omim"));
        collections.put("pdb", new Pair("MI:0460", "pdb"));
        collections.put("pdbe", new Pair("MI:0460", "rcsb_pdb"));
        collections.put("PID", new Pair("MI:1107", "pid"));
        collections.put("pid", new Pair("MI:1107", "pid"));
        collections.put("pmc", new Pair("MI:1042", "pmc"));
        collections.put("PMC", new Pair("MI:1042", "pmc"));
        collections.put("pride", new Pair("MI:0738", "pride"));
        collections.put("PRIDE", new Pair("MI:0738", "pride"));
        collections.put("reactome", new Pair("MI:0467", "reactome"));
        collections.put("rcsb_pdb", new Pair("MI:0460", "rcsb_pdb"));
        collections.put("uniprot", new Pair("MI:0486", "uniprot")); //todo check this one
        collections.put("ww_pdb", new Pair("MI:0460", "pdb"));

        /**
         "efo";"''";"Experimental Factor Ontology"
         "proteomexchange";"''";"Proteomexchange data, indirectly accessed via references in other databases, late 2014."
         */
    }


    /**
     * evidence.sets_items table
     */
    final Map<Integer, Set<String>> proteinsSets;
    /**
     * evidence.sets table
     */
    final Map<String, String> setsCollections;

    SourceDbLookup(Map<Integer, Set<String>> proteinsSets, Map<String, String> setsCollections) {
        this.proteinsSets = proteinsSets;
        this.setsCollections = setsCollections;
    }


    Set<Pair<String, String>> getSourceDbs(Integer proteinA, Integer proteinB) {
        if (!proteinsSets.containsKey(proteinA) || !proteinsSets.containsKey(proteinB)) {
            log.warn("no set for: " + proteinA + " " + proteinB);
            return Collections.emptySet();
        }
        Set<Pair<String, String>> results = new HashSet<Pair<String, String>>();
        //find overlap
        final Set<String> setB = proteinsSets.get(proteinB);
        for (String set : proteinsSets.get(proteinA)) {
            if (setB.contains(set)) {
                final String collection = setsCollections.get(set);
                if (!collections.containsKey(collection)) {
                    log.error(collection + " not found!");
                } else {
                    results.add(collections.get(collection));
                }
            }
        }
        return results;
    }
}
