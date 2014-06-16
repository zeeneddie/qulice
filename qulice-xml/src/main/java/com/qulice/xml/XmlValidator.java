/**
 * Copyright (c) 2011-2014, Qulice.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the Qulice.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qulice.xml;

import com.jcabi.log.Logger;
import com.jcabi.xml.StrictXML;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XSDDocument;
import com.qulice.spi.Environment;
import com.qulice.spi.ValidationException;
import com.qulice.spi.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

/**
 * Validates XML files for formatting.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@SuppressWarnings(
    { "PMD.AvoidInstantiatingObjectsInLoops", "PMD.ExceptionAsFlowControl" }
)
public final class XmlValidator implements Validator {

    @Override
    public void validate(final Environment env) throws ValidationException {
        try {
            for (final File file : env.files("*.xml")) {
                final String name = file.getAbsolutePath().substring(
                    env.basedir().toString().length()
                );
                if (env.exclude("xml", name)) {
                    Logger.info(this, "%s: skipped", name);
                    continue;
                }
                Logger.info(this, "%s: to be validated", name);
                final XML document = new XMLDocument(file);
                final String schema = XmlValidator
                    .schemaLocation(document, name);
                try {
                    new StrictXML(
                        document,
                        new XSDDocument(URI.create(schema).toURL())
                    );
                } catch (final IllegalStateException ex) {
                    if (ex.getCause() != null
                        && ex.getCause() instanceof SAXException) {
                        Logger.warn(
                            // @checkstyle LineLength (1 line)
                            this, "Failed to validate file %s against schema %s. Cause: %s",
                            name,
                            schema,
                            ex.toString()
                        );
                    } else {
                        throw new IllegalStateException(ex);
                    }
                } catch (final IllegalArgumentException ex) {
                    throw new ValidationException(ex);
                }
            }
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Extract schemaLocation from xml document.
     * Tries xsi:schemaLocation and xsi:noNamespaceSchemaLocation attributes.
     * @param document XML document
     * @param name File name of document
     * @return SchemaLocation
     * @throws ValidationException if attribute is missing.
     */
    private static String schemaLocation(final XML document, final String name)
        throws ValidationException {
        final List<String> allschemas = new ArrayList<String>(16);
        final List<String> schemas = document
            .xpath("/*/@xsi:schemaLocation");
        if (!schemas.isEmpty()) {
            allschemas.add(StringUtils.substringAfter(schemas.get(0), " "));
        }
        final List<String> nonamespace = document
            .xpath("/*/@xsi:noNamespaceSchemaLocation");
        if (!nonamespace.isEmpty()) {
            allschemas.add(nonamespace.get(0));
        }
        if (!allschemas.isEmpty()) {
            return allschemas.get(0);
        }
        throw new ValidationException(
            String.format(
                "XML validation exception: missing schema in %s",
                name
            )
        );
    }
}
