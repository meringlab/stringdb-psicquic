/**
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
package org.string_db.psicquic.index


import psidev.psi.mi.search.SearchResult;
import psidev.psi.mi.search.Searcher;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.Interactor

import org.junit.Test;
import org.scalatest.Suite

/**
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class PsimitabIntegrationTest extends Suite {

    //	@org.junit.Ignore
    @Test
    def searchById() = {
        assertResults(runSearch("DRD2"), 1, "9606.ENSP00000347474");
        assertResults(runSearch("Su(z)2"), 1, "7227.CG3905-PA");
    }

    //	@org.junit.Ignore
    @Test
    def searchBySpecies() = {
        assertResults(runSearch("homo sapiens"), 1, "9606.ENSP00000347474");
        assertResults(runSearch("sapiens"), 1, "9606.ENSP00000347474");
        assertResults(runSearch("drosophila"), 1, "7227.CG3905-PA");
        assertResults(runSearch("drosa"), 0, null);
    }

    def assertResults(r: SearchResult[BinaryInteraction[_ <: Interactor]], numResults: Int, firstResultId: String) = {
        assert(numResults === r.getData().size());
        if (numResults > 0) {
            expect(firstResultId) {
                r.getData().get(0).getInteractorA().getIdentifiers().iterator().next().getIdentifier()
            }
        }
    }

    def runSearch(query: String): SearchResult[BinaryInteraction[_ <: Interactor]] = {
        val index = Searcher.buildIndexInMemory(TestData.makeStream(), true, false);
        return Searcher.search(query, index, 0, 10, null);
    }

}