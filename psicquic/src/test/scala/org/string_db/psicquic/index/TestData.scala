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


import java.util.HashMap

/**
 * can't use object here, junit complains because it needs a no-arg constructor.
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 *
 */
//@org.junit.Ignore
class TestData {
    /**
     * seems like @org.junit.Ignore doesn't work at the class level
     */
    //	@org.junit.Ignore
    @org.junit.Test
    def dummy() = {}
}

object TestData {

    val proteins = new HashMap[Integer, Pair[String, String]]()
    val species = new HashMap[Integer, Pair[Integer, String]]()

    species.put(9606, new Pair[Integer, String](9606, "Homo sapiens"));
    species.put(7227, new Pair[Integer, String](7227, "Drosophila melanogaster"));

    proteins.put(1, new Pair[String, String]("7227.CG3905-PA", "Su(z)2"));
    proteins.put(2, new Pair[String, String]("7227.CG8409-PA", "Su(var)205"));
    proteins.put(3, new Pair[String, String]("9606.ENSP00000347474", "DRD2"));
    proteins.put(4, new Pair[String, String]("9606.ENSP00000270349", "SLC6A3"));

    val firstInteraction = "string:7227.CG3905-PA	string:7227.CG8409-PA	string:\"Su(z)2\"	" +
      "string:\"Su(var)205\"	-	-	-	-	-	taxid:7227(Drosophila melanogaster)	" +
      "taxid:7227(Drosophila melanogaster)	-	-	-	score:529|texmining_score:529";

    val secondInteraction = "string:9606.ENSP00000347474	string:9606.ENSP00000270349	string:\"DRD2\"	" +
      "string:\"SLC6A3\"	-	-	-	-	-	taxid:9606(Homo sapiens)	taxid:9606(Homo sapiens)	-	-	-	" +
      "score:983|coexpression_score:636|texmining_score:956";

    val interactions = java.util.Arrays.asList(firstInteraction + "\n", secondInteraction + "\n")

    def makeStream(): StringPsimitabInputStream = {
        return new StringPsimitabInputStream(new DatabaseManager()) {
            override def getIterator() = {
                interactions.iterator
            }

            override def close() = {}
        };
    }

}