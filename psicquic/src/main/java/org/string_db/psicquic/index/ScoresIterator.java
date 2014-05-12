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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator over scores data.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class ScoresIterator implements Iterator<String> {

    private static final int BUFFER_SIZE = DatabaseManager.FETCH_SIZE;

    final List<String> buffer = new ArrayList<String>(BUFFER_SIZE + 1);

    private final DatabaseManager db;

    public ScoresIterator(DatabaseManager db) {
        this.db = db;
    }

    public boolean hasNext() {
        return !(buffer.isEmpty() && !db.hasData());
    }

    public String next() {
        try {
            fill();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (buffer.isEmpty()) {
            // throw an exc?
            return null;
        }
        return buffer.remove(0);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void fill() throws SQLException {
        if (buffer.isEmpty()) {
            for (int i = 0; i < BUFFER_SIZE && db.hasData(); i++) {
                String interaction = db.getNextRecord();
                buffer.add(interaction);
            }
        }
    }

}
