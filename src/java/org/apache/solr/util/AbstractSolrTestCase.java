/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.solr.util;

import org.apache.solr.request.*;
import org.apache.solr.util.TestHarness;

import org.xml.sax.SAXException;
import junit.framework.TestCase;
import javax.xml.xpath.XPathExpressionException;

import java.util.*;
import java.io.*;

/**
 * An Abstract base class that makes writing Solr JUnit tests "easier"
 *
 * <p>
 * Test classes that subclass this need only specify the path to the
 * schema.xml file (:TODO: the solrconfig.xml as well) and write some
 * testMethods.  This class takes care of creating/destroying the index,
 * and provides several assert methods to assist you.
 * </p>
 *
 * @see #setUp
 * @see #tearDown
 */
public abstract class AbstractSolrTestCase extends TestCase {

  /**
   * Harness initialized by initTestHarness.
   *
   * <p>
   * For use in test methods as needed.
   * </p>
   */
  protected TestHarness h;
  /**
   * LocalRequestFactory initialized by initTestHarness using sensible
   * defaults.
   *
   * <p>
   * For use in test methods as needed.
   * </p>
   */
  protected TestHarness.LocalRequestFactory lrf;

  /**
   * Subclasses must define this method to return the name of the
   * schema.xml they wish to use.
   */
  public abstract String getSchemaFile();
    
  /**
   * Subclasses must define this method to return the name of the
   * solrconfig.xml they wish to use.
   */
  public abstract String getSolrConfigFile();

  /**
   * The directory used to story the index managed by the TestHarness h
   */
  protected File dataDir;
    
  /**
   * Initializes things your test might need
   *
   * <ul>
   * <li>Creates a dataDir in the "java.io.tmpdir"</li>
   * <li>initializes the TestHarness h using this data directory, and getSchemaPath()</li>
   * <li>initializes the LocalRequestFactory lrf using sensible defaults.</li>
   * </ul>
   *
   */
  public void setUp() throws Exception {

    dataDir = new File(System.getProperty("java.io.tmpdir")
                       + System.getProperty("file.separator")
                       + getClass().getName() + "-" + getName() + "-"
                       + System.currentTimeMillis());
    dataDir.mkdirs();
    h = new TestHarness(dataDir.getAbsolutePath(),
                        getSolrConfigFile(),
                        getSchemaFile());
    lrf = h.getRequestFactory
      ("standard",0,20,"version","2.2");

  }
    
  /**
   * Shuts down the test harness, and makes the best attempt possible
   * to delete dataDir, unless the system property "solr.test.leavedatadir"
   * is set.
   */
  public void tearDown() throws Exception {
    h.close();
    String skip = System.getProperty("solr.test.leavedatadir");
    if (null != skip && 0 != skip.trim().length()) {
      System.err.println("NOTE: per solr.test.leavedatadir, dataDir will not be removed: " + dataDir.getAbsolutePath());
    } else {
      if (!recurseDelete(dataDir)) {
        System.err.println("!!!! WARNING: best effort to remove " + dataDir.getAbsolutePath() + " FAILED !!!!!");
      }
    }
  }

  /** Validates an update XML String is successful
   */
  public void assertU(String update) {
    assertU(null, update);
  }
    
  /** Validates an update XML String is successful
   */
  public void assertU(String message, String update) {
    try {
      String m = (null == message) ? "" : message + " ";

      String res = h.validateUpdate(update);
      if (null != res) {
        fail(m + "update was not successful: " + res);
      }
    } catch (SAXException e) {
      throw new RuntimeException("Invalid XML", e);
    }
  }

  /** Validates a query matches some XPath test expressions and closes the query */
  public void assertQ(SolrQueryRequest req, String... tests) {
    assertQ(null, req, tests);
  }
  
  /** Validates a query matches some XPath test expressions and closes the query */
  public void assertQ(String message, SolrQueryRequest req, String... tests) {
    try {
      String m = (null == message) ? "" : message + " ";
      String response = h.query(req);
      String results = h.validateXPath(response, tests);
      if (null != results) {
        fail(m + "query failed XPath: " + results +
             " xml response was: " + response);
      }
    } catch (XPathExpressionException e1) {
      throw new RuntimeException("XPath is invalid", e1);
    } catch (Exception e2) {
      throw new RuntimeException("Exception during query", e2);
    }
  }

  /**
   * @see TestHarness#optimize
   */
  public String optimize(String... args) {
    return h.optimize();
  }
  /**
   * @see TestHarness#commit
   */
  public String commit(String... args) {
    return h.commit();
  }

  /**
   * Generates a simple &lt;add&gt;&lt;doc&gt;... XML String with no options
   *
   * @param fieldsAndValues 0th and Even numbered args are fields names odds are field values.
   * @see #add
   * @see #doc
   */
  public String adoc(String... fieldsAndValues) {
    Doc d = doc(fieldsAndValues);
    return add(d);
  }
    
  /**
   * Generates an &lt;add&gt;&lt;doc&gt;... XML String with options
   * on the add.
   *
   * @param doc the Document to add
   * @param args 0th and Even numbered args are param names, Odds are param values.
   * @see #add
   * @see #doc
   */
  public String add(Doc doc, String... args) {
    try {
      StringWriter r = new StringWriter();
            
      // this is anoying
      if (null == args || 0 == args.length) {
        r.write("<add>");
        r.write(doc.xml);
        r.write("</add>");
      } else {
        XML.writeUnescapedXML(r, "add", doc.xml, (Object[])args);
      }
            
      return r.getBuffer().toString();
    } catch (IOException e) {
      throw new RuntimeException
        ("this should never happen with a StringWriter", e);
    }
  }

  /**
   * Generates a &lt;delete&gt;... XML string for an ID
   *
   * @see TestHarness#deleteById
   */
  public String delI(String id) {
    return h.deleteById(id);
  }
  /**
   * Generates a &lt;delete&gt;... XML string for an query
   *
   * @see TestHarness#deleteByQuery
   */
  public String delQ(String q) {
    return h.deleteByQuery(q);
  }
  
  /**
   * Generates a simple &lt;doc&gt;... XML String with no options
   *
   * @param fieldsAndValues 0th and Even numbered args are fields names, Odds are field values.
   * @see TestHarness#makeSimpleDoc
   */
  public Doc doc(String... fieldsAndValues) {
    Doc d = new Doc();
    d.xml = h.makeSimpleDoc(fieldsAndValues).toString();
    return d;
  }

  /**
   * Generates a SolrQueryRequest using the LocalRequestFactory
   * @see #lrf
   */
  public SolrQueryRequest req(String... q) {
    return lrf.makeRequest(q);
  }
    
  /** Neccessary to make method signatures un-ambiguous */
  public static class Doc {
    public String xml;
    public String toString() { return xml; }
  }

  public static boolean recurseDelete(File f) {
    if (f.isDirectory()) {
      for (File sub : f.listFiles()) {
        if (!recurseDelete(sub)) {
          return false;
        }
      }
    }
    return f.delete();
  }

    
}
