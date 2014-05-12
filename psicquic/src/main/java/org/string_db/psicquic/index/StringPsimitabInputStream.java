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
package org.string_db.psicquic.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * InputStream of STRING-Db psimi-tab records. Data comes from <code>ScoresIterator<code>.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class StringPsimitabInputStream extends InputStream {

    private DatabaseManager dbm;
    private Iterator<String> results;

    private byte[] buffer;

    private int pos = -1;

    public StringPsimitabInputStream(DatabaseManager dbm) {
        this.dbm = dbm;
        results = getIterator();
    }

    @Override
    public int read() throws IOException {
        if (buffer == null) {
            if (!results.hasNext()) {
                return -1;
            }
            buffer = results.next().getBytes();
            pos = 0;
        }
        if (pos >= buffer.length) {
            buffer = null;
            pos = -1;
            return -1;
        }
        return buffer[pos++];
    }

    @Override
    public void close() throws IOException {
        dbm.shutdown();
    }

    protected Iterator<String> getIterator() {
        return new ScoresIterator(dbm);
    }

}
