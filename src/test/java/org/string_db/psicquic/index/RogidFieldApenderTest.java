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

import com.google.common.collect.ImmutableMap;
import org.hupo.psi.calimocho.model.Row;
import org.junit.Test;
import uk.ac.ebi.intact.irefindex.seguid.RogidGenerator;

import static org.junit.Assert.assertEquals;
import static org.string_db.psicquic.index.RowBuilderTest.assertRowsEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class RogidFieldApenderTest {

    final FieldBuilder rogidBuilder = new RogidFieldBuilder(
            9606,
            ImmutableMap.of(
                    975673, "MGLTVSALFSRIFGKKQMRILMVGLDAAGKTTILYKLKLGEIVTTIPTIGFNVETVEYKN" +
                            "ICFTVWDVGGQDKIRPLWRHYFQNTQGLIFVVDSNDRERVQESADELQKMLQEDELRDAV" +
                            "LLVFANKQDMPNAMPVSELTDKLGLQHLRSRTWYVQATCATQGTGLYDGLDWLSHELSKR",
                    978054, "MTDGILGKAATMEIPIHGNGEARQLPEDDGLEQDLQQVMVSGPNLNETSIVSGGYGGSGD" +
                            "GLIPTGSGRHPSHSTTPSGPGDEVARGIAGEKFDIVKKWGINTYKCTKQLLSERFGRGSR" +
                            "TVDLELELQIELLRETKRKYESVLQLGRALTAHLYSLLQTQHALGDAFADLSQKSPELQE" +
                            "EFGYNAETQKLLCKNGETLLGAVNFFVSSINTLVTKTMEDTLMTVKQYEAARLEYDAYRT" +
                            "DLEELSLGPRDAGTRGRLESAQATFQAHRDKYEKLRGDVAIKLKFLEENKIKVMHKQLLL" +
                            "FHNAVSAYFAGNQKQLEQTLQQFNIKLRPPGAEKPSWLEEQ"
            )
    );
    final Row expected = new RowBuilder()
            .withChecksumA("da8exbGR3MGxZ6CPZqLvqJbyUYI9606")
            .withChecksumB("q3QMUWlBIW82Szrv9jbMR6ikOZg9606")
            .build();
    final RowBuilder recorder = new RowBuilder();

    @Test
    public void both_columns() throws Exception {
        rogidBuilder.proteins(975673, 978054).addTo(recorder);
        assertRowsEquals(expected, recorder.build());
    }

    @Test
    public void testRogid() throws Exception {
        RogidGenerator rogidGenerator = new RogidGenerator();
        final String rogid = rogidGenerator.calculateRogid("MTDGILGKAATMEIPIHGNGEARQLPEDDGLEQDLQQVMVSGPNLNETSIVSGGYGGSGD" +
                "GLIPTGSGRHPSHSTTPSGPGDEVARGIAGEKFDIVKKWGINTYKCTKQLLSERFGRGSR" +
                "TVDLELELQIELLRETKRKYESVLQLGRALTAHLYSLLQTQHALGDAFADLSQKSPELQE" +
                "EFGYNAETQKLLCKNGETLLGAVNFFVSSINTLVTKTMEDTLMTVKQYEAARLEYDAYRT" +
                "DLEELSLGPRDAGTRGRLESAQATFQAHRDKYEKLRGDVAIKLKFLEENKIKVMHKQLLL" +
                "FHNAVSAYFAGNQKQLEQTLQQFNIKLRPPGAEKPSWLEEQ", "9606");

        assertEquals("q3QMUWlBIW82Szrv9jbMR6ikOZg9606", rogid);
    }
}
